package scenes.dungeon;

import static scenes.dungeon.Direction.Down;
import static scenes.dungeon.Direction.Left;
import static scenes.dungeon.Direction.Right;
import static scenes.dungeon.Direction.Up;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

import java.util.Comparator;

import scenes.Messages;
import scenes.Messages.Dungeon.CombatNotify;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.components.Combat;
import core.components.Groups;
import core.components.Groups.Monster;
import core.components.Drop;
import core.components.Equipment;
import core.components.Identifier;
import core.components.Lock;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Item;
import core.datatypes.dungeon.Floor;
import core.datatypes.quests.Quest;
import core.service.implementations.ScoreTracker;
import core.service.implementations.ScoreTracker.NumberValues;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IGame;
import core.service.interfaces.IPlayerContainer;

/**
 * Handles all movement for dungeoning, as well as bump combat
 * 
 * @author nhydock
 *
 */
public class MovementSystem extends EntitySystem implements EntityListener {

    private boolean[][] collision;
    private int[] start, end;
    private int[] nextMove = new int[2];

    ComponentMapper<Monster> monsterMap = ComponentMapper.getFor(Monster.class);
    ComponentMapper<Stats> statMap = ComponentMapper.getFor(Stats.class);

    private Entity player;
    Array<Entity> monsters = new Array<Entity>();
    Array<Entity> equipment = new Array<Entity>();
    Array<Entity> actOrder = new Array<Entity>();

    private Engine engine;
    @Inject public IDungeonContainer dungeonService;
    @Inject public IPlayerContainer playerService;
    
    public void setMap(Floor floorData) {
        // set collision map
        collision = floorData.getBooleanMap();
        start = floorData.getStartPosition();
        end = floorData.getEndPosition();
    }
    
    /**
     * Checks to see if a tile is specifically of type wall
     * 
     * @param x
     * @param y
     * @return true if tile is not passable or if it is out of bounds
     */
    protected boolean isWall(int x, int y) {
        if ((x < 0 || x >= collision.length) || (y < 0 || y >= collision.length))
            return true;
        return collision[x][y];
    }

    /**
     * Checks to see if an entity can move to a tile
     * 
     * @param x
     * @param y
     * @param e
     *            - the entity to move
     * @return true if there is an enemy blocking the way
     */
    private Entity checkFoe(int x, int y, Entity e) {
        for (int i = 0; i < monsters.size; i++) {
            Entity m = monsters.get(i);
            Position p = Position.Map.get(m);
            if ((p.getX() == x && p.getY() == y) && Combat.Map.has(m)) {
                return m;
            }
        }
    
        Position p = Position.Map.get(player);
        if (p.getX() == x && p.getY() == y) {
            return player;
        }
        return null;
    }
    
    private Entity pickup(int x, int y) {
        for (int i = 0; i < equipment.size; i++) {
            Entity e = equipment.get(i);
            Position p = Position.Map.get(e);
            if ((p.getX() == x && p.getY() == y)) {
                return e;
            }
        }
        return null;
    }
    
    /**
     * Moves an entity to a specified location on the map
     * 
     * @param x
     * @param y
     * @param e
     */
    private boolean moveEntity(int x, int y, Entity e) {
        Position p = Position.Map.get(e);
        boolean moved = false;
        
        if (!isWall(x, y)) {
            Entity foe = checkFoe(x, y, e);
            // Handle combat
            if (foe != null) {
                //door handling logic
                if (Lock.Map.has(foe)) {
                    Lock lock = Lock.Map.get(foe);
                    if (lock.unlocked) {
                        if ((!lock.open && e == player) || lock.open) {
                            //move onto the square and open the door
                            p.move(x, y);
                            unlockDoor(foe);
                            moved = true;
                        } else {
                            //do nothing, blocking the path
                            moved = false;
                        }
                    } 
                    else {
                        if (e == player){
                            //bash open the door
                            fight(e, foe);
                            moved = true;
                        } else {
                            //do nothing
                            moved = false;
                        }
                    }
                } else {
                    if (e == player) {
                        fight(e, foe);
                        moved = true;
                    }
                    else {
                        Combat c = Combat.Map.get(e);
                        if (foe == player && !c.isPassive()) {
                            fight(e, foe);
                            moved = true;
                        }
                    } 
                }
            }
            // just move to square
            else {
                p.move(x, y);

                // try to figure out which room you're in now
                if (e == player) {
                    Entity obj = pickup(x, y);
                    // equipment
                    if (obj != null) {
                        //pick up equipment that is laying on the floor if it's been dropped
                        Drop drop = Drop.Map.get(obj);
                        Equipment eq = Equipment.Map.get(player);
                        eq.pickup((Equipment.Piece)drop.reward);
                        equipment.removeValue(obj, true);
                        engine.removeEntity(obj);
                    }
                }

                moved = true;
            }
        }
        return moved;
    }

    /**
     * Make two entities fight
     * 
     * @param actor
     * @param opponent
     */
    private void fight(Entity actor, Entity opponent) {
        Stats aStats = statMap.get(actor);
        Stats bStats = statMap.get(opponent);
        Equipment equipment = Equipment.Map.get(player);
        
        final float MULT = (actor == player) ? 2 : 1.25f;

        // ignore if target died at some point along the way
        if (aStats.hp <= 0 ) {
            return;
        }
        
        if (bStats.hp <= 0 || (opponent != player && !Combat.Map.has(opponent))) {
            return;
        }

        // if one of the actors is a boss, invoke a boss fight
        if (Groups.bossType.matches(opponent)) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.FIGHT, opponent);
            return;
        } else if (Groups.bossType.matches(actor)) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.FIGHT, actor);
            return;
        }
        
        Position p = Position.Map.get(actor);
        Position p1 = Position.Map.get(opponent);
        p.fight(p1.getX(), p1.getY());
        
        //show ids
        {
            Identifier id = Identifier.Map.get(actor);
            id.show();
            id = Identifier.Map.get(opponent);
            id.show();
        }
        
        CombatNotify notification = new CombatNotify();
        notification.attacker = actor;
        notification.opponent = opponent;
    
        if (MathUtils.randomBoolean(1f - (MathUtils.random(.8f, MULT) * bStats.getSpeed()) / 100f)) {
            float chance = MathUtils.random(.8f, MULT);
            int str = aStats.getStrength();
            int def = bStats.getDefense();
            if (actor == player) {
                str += equipment.getSword().getPower();
                equipment.getSword().decay();
            }
            else {
                //shield provides chance to block
                float pow = equipment.getShield().getPower(); 
                if (MathUtils.randomBoolean(pow / Equipment.Piece.MAX_POWER)) {
                    equipment.getShield().decay();
                    def += Integer.MAX_VALUE;    
                }
                //armor lessens damage if the shield doesn't blog
                else {
                    def += equipment.getArmor().getPower();
                    equipment.getArmor().decay();    
                }
            }
            int dmg = (int)Math.max(0, (chance * str) - def);
            
            bStats.hp = Math.max(0, bStats.hp - dmg);

            notification.dmg = dmg;
            if (actor == player) {
                notification.critical = chance > MULT * .8f && dmg > 0;
                Combat combatProp = Combat.Map.get(opponent);
                combatProp.aggress();
            } else {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
            }
        }
        else {
            notification.attacker = actor;
            notification.opponent = opponent;
            notification.dmg = -1;
        }
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, notification);
        

        if (bStats.hp <= 0) {
            //open door instead of handling drops
            if (Lock.Map.has(opponent))
            {
                unlockDoor(opponent);
            }
            // drop item if opponent killed was not a player
            else if (opponent != player){
                
                aStats.exp += bStats.exp;
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
                if (aStats.canLevelUp()) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.LevelUp);
                }
                ServiceManager.getService(ScoreTracker.class).increment(NumberValues.Monsters_Killed);
                Renderable r = Renderable.Map.get(opponent);
                r.setSpriteName("dead");
                r.setDensity(0);
                if (Drop.Map.has(opponent)) {
                    Drop drop = Drop.Map.get(opponent);
                    if (drop.reward instanceof Equipment.Piece) {
                        Equipment.Piece piece = (Equipment.Piece)drop.reward;
                        if (piece instanceof Equipment.Sword) {
                            r.setSpriteName("sword");
                        } else if (piece instanceof Equipment.Shield) {
                            r.setSpriteName("shield");
                        } else if (piece instanceof Equipment.Armor) {
                            r.setSpriteName("armor");
                        }
                        this.equipment.add(opponent);
                    }
                }
                
                Identifier id = Identifier.Map.get(opponent);

                String name = id.toString();
                if (name.endsWith(Monster.Loot)) {
                    dungeonService.getProgress().lootFound++;
                    dungeonService.getProgress().totalLootFound++;
                    engine.removeEntity(opponent);
                }
                else if (name.endsWith(Monster.Key)) {
                    dungeonService.getProgress().keys++;
                    engine.removeEntity(opponent);
                }
                else {
                    dungeonService.getProgress().monstersKilled++;
                    MessageDispatcher.getInstance().dispatchMessage(0, null, null, Quest.Actions.Hunt, name);
                }
                opponent.remove(Monster.class);
                monsters.removeValue(opponent, true);
                    
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
            }
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Dead, opponent);
            opponent.remove(Combat.class);
            
            if (Drop.Map.has(opponent)) {
                Drop drop = Drop.Map.get(opponent);
                if (drop.reward instanceof Item) {
                    Item item = (Item)drop.reward;
                    ServiceManager.getService(IPlayerContainer.class).getInventory().pickup(item);
                    opponent.remove(Drop.class);
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, String.format("Obtained %s", item.fullname()));
                }
            }
        }
    }

    /**
     * Set the system's main player and moves them to their starting position
     * Used to descend to the next level
     * @param entity 
     */
    public void moveToStart(Entity entity) {
        Position p = Position.Map.get(entity);

        p.move((int) start[0], (int) start[1]);
        p.update();
    }

    /**
     * Sets an entity to the end position of a level. Used when ascending to a
     * previous level.  Primarily for the player, but can be used for anything.
     */
    public void moveToEnd(Entity entity) {
        Position p = Position.Map.get(entity);

        p.move((int) end[0], (int) end[1]);
        p.update();
    }


    /**
     * Moves just the player entity and executes a turn
     * 
     * @param direction
     * @return
     */
    public boolean movePlayer(Direction direction) {
        if (direction == null)
            return false;

        if (player == null) {
            return false;
        }
        
        Position playerPos = Position.Map.get(player);
        int x = playerPos.getX();
        int y = playerPos.getY();
        if (direction == Up) {
            y++;
        }
        if (direction == Down) {
            y--;
        }
        if (direction == Right) {
            x++;
        }
        if (direction == Left) {
            x--;
        }

        if (!isWall(x, y)) {
            nextMove[0] = x;
            nextMove[1] = y;
            return true;
        }

        return false;
    }
    
    private void unlockDoor(Entity e) {
        Lock lock = Lock.Map.get(e);
        lock.unlocked = true;
        lock.open = true;
        Renderable.Map.get(e).setSpriteName("opened");
        Renderable.Map.get(e).setDensity(0);
    }
    
    /**
     * Looks in the tiles around the player, if they keys they may unlock certain objects
     */
    public boolean openAction() {
        int keys = dungeonService.getProgress().keys;
        if (keys <= 0) {
            return false;
        }
        
        Position playerPos = Position.Map.get(player);
        int x = playerPos.getX();
        int y = playerPos.getY();
        
        boolean unlocked = false;
        int[][] adjacent = {{x-1, y}, {x, y-1}, {x, y+1}, {x+1, y}};
        for (int[] adj : adjacent) {
            Entity e = checkFoe(adj[0], adj[1], player);
            if (e != null)
            {
                Identifier id = Identifier.Map.get(e);
                String type = id.toString();
                if (type.endsWith(Monster.Door) || 
                    type.endsWith(Monster.Loot) ||
                    type.endsWith("mimic") ||
                    type.endsWith("domimic") ) {
                    
                    unlocked = true;
                    
                    Position p1 = Position.Map.get(e);
                    playerPos.fight(p1.getX(), p1.getY());
                    //is door, unlock
                    if (Lock.Map.has(e)) {
                        unlockDoor(e);
                    } 
                    //kill creature/chest
                    else {
                        ServiceManager.getService(ScoreTracker.class).increment(NumberValues.Monsters_Killed);
                        Renderable.Map.get(e).setSpriteName("dead");
                        Renderable.Map.get(e).setDensity(0);

                        String name = id.toString();
                        if (name.endsWith(Monster.Loot)) {
                            dungeonService.getProgress().lootFound++;
                            dungeonService.getProgress().totalLootFound++;
                            engine.removeEntity(e);
                        }
                        else {
                            dungeonService.getProgress().monstersKilled++;
                            MessageDispatcher.getInstance().dispatchMessage(0, null, null, Quest.Actions.Hunt, name);
                        }
                        
                        monsters.removeValue(e, true);
                    }
                    e.remove(Combat.class);
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, String.format("%s was unlocked", id.toString()));

                    if (Drop.Map.has(e)){
                        Drop drop = Drop.Map.get(e);
                        if (drop.reward instanceof Item) {
                            Item item = (Item)drop.reward;
                            ServiceManager.getService(IPlayerContainer.class).getInventory().pickup(item);
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, String.format("Obtained %s", item.fullname()));
                        }
                    }
                    break;
                }
            }
        }
        
        if (unlocked) {
            keys--;
            dungeonService.getProgress().keys = keys;
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
            return true;
        }
        return false;
    }
    
    /**
     * Tells all entities handled by this system to perform processing
     */
    public void process(){
        actOrder.addAll(monsters);
        actOrder.add(player);
        actOrder.sort(SpeedComparator.instance);
        
        //execute a turn
        for (int i = 0; i < actOrder.size; i++) {
            Entity e = actOrder.get(i);
            if (e == player) {
                Position p = Position.Map.get(player);
                //only move the player entity if they've actually tried to move,
                // ignores player movement unlock, preventing attacking oneself
                if (p.getX() != nextMove[0] || p.getY() != nextMove[1]) {
                    moveEntity(nextMove[0], nextMove[1], e);
                }
            } else {
                process(e);
            }
        }
        
        actOrder.clear();
    }

    /**
     * Handles movement processing for a single entity
     * @param e
     */
    protected void process(Entity e) {
        if (!Combat.Map.has(e)) {
            return;
        }
        Combat prop = Combat.Map.get(e);
        Position m = Position.Map.get(e);
        Position p = Position.Map.get(player);


        // only try moving once the character is in the same room as it
        // try to move towards the player when nearby
        if (prop.isAgro()) {
            // roll for move
            // Stats s = statMap.get(e);
            // chance multiplied since agro
            if (MathUtils.randomBoolean(prop.getMovementRate())) {
                int dX = 0;
                int dY = 0;
                boolean priority = MathUtils.randomBoolean();

                // horizontal priority flip
                if (priority) {
                    if (p.getX() < m.getX())
                        dX = -1;
                    if (p.getX() > m.getX())
                        dX = 1;
                    if (dX == 0) {
                        if (p.getY() < m.getY())
                            dY = -1;
                        if (p.getY() > m.getY())
                            dY = 1;
                    }

                }
                // vertical priority
                else {
                    if (p.getY() < m.getY())
                        dY = -1;
                    if (p.getY() > m.getY())
                        dY = 1;
                    if (dY == 0) {
                        if (p.getX() < m.getX())
                            dX = -1;
                        if (p.getX() > m.getX())
                            dX = 1;
                    }
                }

                // follow player chance
                moveEntity(m.getX() + dX, m.getY() + dY, e);

                if (p.distance(m) > 5) {
                    prop.calm();
                }
            }
        }
        // lazily wander around
        else {
            // roll for move
            // Stats s = statMap.get(e);
            if (MathUtils.randomBoolean(prop.getMovementRate())) {
                int dX = 0;
                int dY = 0;

                dX = MathUtils.random(-1, 1);
                if (dX == 0) {
                    dY = MathUtils.random(-1, 1);
                }

                // follow player chance
                moveEntity(m.getX() + dX, m.getY() + dY, e);

                if (p.distance(m) < 3 && !prop.isPassive()) {
                    prop.aggress();
                }
            }
        }
    }

    /**
     * Dereference as much as we can
     */
    public void dispose() {
        monsters = null;
        player = null;
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void entityAdded(Entity entity) {
        if (Groups.monsterType.matches(entity)) {
            monsters.add(entity);
        } 
        /**
         * Assigns a direct reference to the player in the system for faster access
         */
        else if (Groups.playerType.matches(entity)) {
            player = entity;
        }
    }

    @Override
    public void entityRemoved(Entity entity) {
        if (Groups.monsterType.matches(entity)) {
            monsters.removeValue(entity, true);
        }
        else if (Groups.playerType.matches(entity)) {
            player = null;
        }
    }
    
    /**
     * Comparator used to sort the action order of entities in the system
     * @author nhydock
     *
     */
    private static class SpeedComparator implements Comparator<Entity> {

        static final SpeedComparator instance = new SpeedComparator();
        
        private SpeedComparator(){};
        
        @Override
        public int compare(Entity o1, Entity o2) {
            Stats s1 = Stats.Map.get(o1);
            Stats s2 = Stats.Map.get(o2);
            
            Float spd1 = s1.getSpeed();
            Float spd2 = s2.getSpeed();
            
            return spd1.compareTo(spd2);
        }
        
    }

    public int changeFloor() {
        Position p = Position.Map.get(player);
        int x = p.getX();
        int y = p.getY();
        // descend
        if (x == (int) end[0] && y == (int) end[1]) {
            return Messages.Dungeon.Descend;
        }
        // ascend
        else if (x == (int) start[0] && y == (int) start[1] && !playerService.isHardcore()) {
            return Messages.Dungeon.Ascend;
        }
        return -1;
    }
}
