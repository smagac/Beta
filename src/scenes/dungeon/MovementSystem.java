package scenes.dungeon;

import static scenes.dungeon.Direction.Down;
import static scenes.dungeon.Direction.Left;
import static scenes.dungeon.Direction.Right;
import static scenes.dungeon.Direction.Up;

import java.util.Arrays;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;
import scenes.Messages;
import scenes.Messages.Dungeon.CombatNotify;
import scenes.dungeon.CombatHandler.Result;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.components.Combat;
import core.components.Drop;
import core.components.Equipment;
import core.components.Groups;
import core.components.Groups.Monster;
import core.components.Identifier;
import core.components.Lock;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Ailment;
import core.datatypes.Health;
import core.datatypes.Item;
import core.datatypes.dungeon.Floor;
import core.datatypes.quests.Quest;
import core.factories.MonsterFactory;
import core.factories.MonsterFactory.MonsterTemplate;
import core.service.implementations.PageFile;
import core.service.implementations.PageFile.NumberValues;
import core.service.interfaces.IDungeonContainer;
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

    Entity player;
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
    
    /**
     * Detects if there is equipment on the ground that can be picked up at the designated location
     * @param x
     * @param y
     * @return
     */
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
    boolean moveEntity(int x, int y, Entity e) {
        Position p = Position.Map.get(e);
        boolean moved = false;
        
        if (!isWall(x, y)) {
            Entity foe = checkFoe(x, y, e);
            // Handle combat
            if (foe != null) {
                //door handling logic
                if (e == player) {
                    fight(e, foe);
                    moved = true;
                }
                else {
                    Combat c = Combat.Map.get(e);
                    if (foe == player && !c.isNaturallyPassive()) {
                        fight(e, foe);
                        moved = true;
                    }
                } 
            }
            // just move to square
            else {
                p.move(x, y);

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

    boolean handleDrop(Entity actor){
        if (Drop.Map.has(actor)) {
            Drop drop = Drop.Map.get(actor);
            if (drop.reward instanceof Item) {
                Item item = (Item)drop.reward;
                if (item == Item.Placeholder){
                    //calculate chance of getting an objective item based on current depth of the player
                    int depth = dungeonService.getProgress().depth;
                    float rareChance = .08f*(depth/5f);
                    
                    if (MathUtils.randomBoolean(rareChance)) {
                        item = dungeonService.getDungeon().getItemFactory().createObjective(playerService);
                    } else {
                        item = dungeonService.getDungeon().getItemFactory().createItem();
                    }
                }
                if (item != null){
                    ServiceManager.getService(IPlayerContainer.class).getInventory().pickup(item);
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, String.format("Obtained %s", item.fullname()));
                }
                actor.remove(Drop.class);
            }
            else if (drop.reward instanceof Equipment.Piece) {
                Equipment.Piece piece = (Equipment.Piece)drop.reward;
                Renderable r = Renderable.Map.get(actor);
                if (piece instanceof Equipment.Sword) {
                    r.setSpriteName("sword");
                } else if (piece instanceof Equipment.Shield) {
                    r.setSpriteName("shield");
                } else if (piece instanceof Equipment.Armor) {
                    r.setSpriteName("armor");
                }
                this.equipment.add(actor);
                return true;
            }
        }
        return false;
    }
    
    private void fight(Entity attacker, Entity opponent) {
        fight(attacker, opponent, false);
    }
    
    /**
     * Make two entities fight
     * 
     * @param actor
     * @param opponent
     * @param spell 
     */
    private void fight(Entity attacker, Entity opponent, boolean spell) {
        if (CombatHandler.isDead(attacker, player) || CombatHandler.isDead(opponent, player)){
            return;
        }

        // if one of the actors is a boss, invoke a boss fight
        if (Groups.bossType.matches(opponent)) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.FIGHT, opponent);
            return;
        } else if (Groups.bossType.matches(attacker)) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.FIGHT, attacker);
            return;
        }
        
        Result results;
        if (!spell) {
            results = CombatHandler.fight(attacker, opponent, player);
        } else {
            results = CombatHandler.magic(attacker, opponent, player);
        }
        
        CombatNotify notification = new CombatNotify();
        notification.attacker = attacker;
        notification.opponent = opponent;
        notification.dmg = results.damage;
        notification.critical = results.critical;
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, notification);
        

        //animate fight
        if (!spell){
            Position p = Position.Map.get(attacker);
            Position p1 = Position.Map.get(opponent);
            p.fight(p1.getX(), p1.getY());
        }
        
        //show ids
        {
            Identifier id = Identifier.Map.get(attacker);
            id.show();
            id = Identifier.Map.get(opponent);
            id.show();
        }
        
        //agro enemies when hit
        if (attacker == player){
            Combat combatProp = Combat.Map.get(opponent);
            combatProp.getAI().changeState(MovementAI.Agro);
        }
        else
        {
            //apply any inflicted ailments
            Array<Ailment> ailments = results.inflicted;
            Health health = playerService.getAilments();
            if (ailments != null) {
                for (Ailment a : ailments) {
                    health.addAilment(a);
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, "You have been inflicted with " + a.toString());
                }
            }
            
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
        }
        
        if (results.killed) {
            //open door instead of handling drops
            if (Lock.Map.has(opponent)) {
                CombatHandler.unlockDoor(opponent);
            }
            else if (Monster.isLoot(opponent)) {
                dungeonService.getProgress().lootFound++;
                dungeonService.getProgress().totalLootFound++;
                engine.removeEntity(opponent);
            }
            else if (Monster.isKey(opponent)) {
                dungeonService.getProgress().keys++;
                engine.removeEntity(opponent);
            }
            else if (opponent != player) {
                CombatHandler.markDead(opponent);
                dungeonService.getProgress().monstersKilled++;
                Identifier id = Identifier.Map.get(opponent); 
                String name = id.toString();
                MessageDispatcher.getInstance().dispatchMessage(null, Quest.Actions.Hunt, name);
                
                Stats playerStats = Stats.Map.get(player);
                playerStats.exp += results.exp;
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
                if (playerStats.canLevelUp()) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.LevelUp);
                }
                ServiceManager.getService(PageFile.class).increment(NumberValues.Monsters_Killed);
                
                //unlock monster profile
                int floor = dungeonService.getProgress().depth;
                MonsterTemplate monster = MonsterFactory.getMonster(id.getType());
                ServiceManager.getService(PageFile.class).discover(monster, floor);
                
                //unlock adjective
                ServiceManager.getService(PageFile.class).discover(id.getModifier(), true);
            }
            
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Dead, opponent);
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
            opponent.remove(Monster.class);
            opponent.remove(Combat.class);
            monsters.removeValue(opponent, true);    
            
            handleDrop(opponent);
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
        
        Health health = playerService.getAilments();
        //randomize movement when confused
        if (health.getAilments().contains(Ailment.CONFUSE, true)) {
            Gdx.app.log("Status", "Player is confused");
            
            x += MathUtils.random(-1, 1);
            if (x != playerPos.getX()) {
                y += MathUtils.random(-1, 1);
            }
            
            if (!isWall(x, y)) {
                nextMove[0] = x;
                nextMove[1] = y;
            }
            //always execute turn when confused, even if it's into a wall
            return true;
        } 
        //normal movement behavior
        else {
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
            
            //do not execute turn when you try to walk into a wall
            if (!isWall(x, y)) {
                nextMove[0] = x;
                nextMove[1] = y;
                return true;
            }
        }        

        return false;
    }
    
    
    
    /**
     * Looks in the tiles around the player, if they keys they may unlock certain objects
     */
    public boolean openAction(Direction dir) {
        int keys = dungeonService.getProgress().keys;
        if (keys <= 0) {
            return false;
        }
        
        Position playerPos = Position.Map.get(player);
        int x = playerPos.getX();
        int y = playerPos.getY();
        int[] adj = {x, y};
        dir.move(adj);
        
        boolean unlocked = false;
        Entity e = checkFoe(adj[0], adj[1], player);
        if (e != null)
        {
            if (Monster.isDoor(e) || Monster.isLoot(e) || Monster.isMimic(e)) {
                String name = Identifier.Map.get(e).toString();
                
                unlocked = true;
                
                Position p1 = Position.Map.get(e);
                playerPos.fight(p1.getX(), p1.getY());
                //is door, unlock
                if (Lock.Map.has(e)) {
                    CombatHandler.unlockDoor(e);
                } 
                //kill creature/chest
                else {
                    if (Monster.isLoot(e)) {
                        dungeonService.getProgress().lootFound++;
                        dungeonService.getProgress().totalLootFound++;
                        engine.removeEntity(e);
                    }
                    else {
                        ServiceManager.getService(PageFile.class).increment(NumberValues.Monsters_Killed);
                        CombatHandler.markDead(e);

                        dungeonService.getProgress().monstersKilled++;
                        MessageDispatcher.getInstance().dispatchMessage(0, null, null, Quest.Actions.Hunt, name);
                    }
                    
                    monsters.removeValue(e, true);
                }
                e.remove(Combat.class);
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, String.format("%s was unlocked", name));

                handleDrop(e);
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
                    //applied all status ailments to player
                    Health health = playerService.getAilments();
                    Stats stats = Stats.Map.get(e);
                    ImmutableArray<Ailment> active = health.getAilments();
                    
                    //handle movement inhibitors
                    if (active.contains(Ailment.SPRAIN, false)){
                        Gdx.app.log("Status", "Player is sprained");
                        //arthritis has higher chance to disable movement and deals damage
                        // when the player does move
                        float disable = .8f;
                        int dmg = 0;
                        Ailment cause;
                        if (active.contains(Ailment.ARTHRITIS, true)) {
                            disable = .4f;
                            dmg = (int)Math.max(stats.maxhp / 20f, 1);
                            cause = Ailment.ARTHRITIS;
                        } else {
                            dmg = (int)Math.max(stats.maxhp / 15f, 1);
                            cause = Ailment.SPRAIN;
                        }
                        if (MathUtils.randomBoolean(disable)) {
                            moveEntity(nextMove[0], nextMove[1], e);
                            CombatNotify notification = new CombatNotify();
                            notification.attacker = null;
                            notification.opponent = e;
                            notification.dmg = dmg;
                            notification.cause = cause;
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, notification);
                            
                            stats.hp = Math.max(0, stats.hp - dmg);
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
                            
                            if (stats.hp <= 0) {
                                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Dead, player);
                            }
                        }
                    }
                    else
                    {
                        moveEntity(nextMove[0], nextMove[1], e);
                    }
                    
                    //handle movement inhibitors
                    if (active.contains(Ailment.POISON, false)){
                        int dmg = 0;
                        Ailment cause;
                        if (active.contains(Ailment.TOXIC, true)) {
                            dmg = (int)Math.max(stats.maxhp / 12f, 1);
                            cause = Ailment.TOXIC;
                        } else {
                            dmg = (int)Math.max(stats.maxhp / 15f, 1);
                            cause = Ailment.POISON;
                        }
                        CombatNotify notification = new CombatNotify();
                        notification.attacker = null;
                        notification.opponent = e;
                        notification.dmg = dmg;
                        notification.cause = cause;
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, notification);
                        
                        stats.hp = Math.max(0, stats.hp - dmg);
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
                        
                        if (stats.hp <= 0) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Dead, player);
                        }
                    }
                    
                    health.update();
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
        prop.getAI().update();
    }

    /**
     * Dereference as much as we can
     */
    public void dispose() {
        monsters = null;
        player = null;
        MovementAI.setWorld(null);
    }
    
    @Override
    public void addedToEngine(Engine engine) {
        this.engine = engine;
        MovementAI.setWorld(this);
    }

    @Override
    public void entityAdded(Entity entity) {
        if (Groups.monsterType.matches(entity)) {
            monsters.add(entity);
            Combat combat = Combat.Map.get(entity);
            combat.initAI(new DefaultStateMachine<Entity>(entity, MovementAI.Wander));
            if (combat.isNaturallyPassive()) {
                combat.getAI().changeState(MovementAI.Passive);
            }
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

    /**
     * Follows a path in a specified direction from the starting position
     * @param dir - direction to follow
     * @param position - the starting position
     * @param action - function that may be performed with each step along the way
     * @return the path followed
     */
    public int[][] followPath(Direction dir, int[] position, Runnable action) {
        Array<int[]> path = new Array<int[]>(int[].class);
        dir.move(position);
        while (!isWall(position[0], position[1])) {
            path.add(Arrays.copyOf(position, 2));
            action.run();
            dir.move(position);
        }
        return path.toArray();
    }
    
    /**
     * Casts a spell along a straight path towards a specified point along an axis.
     * Spell effect continues until it hits a wall.
     * @param cursorDirection
     * @return the path the spell fired along.  Returns null if the spell could not be cast
     */
    public int[][] fireSpell(int[] cursorLocation) {
        Stats stats = Stats.Map.get(player);
        
        if (!stats.canCastSpell()) {
            return null;
        }
        
        Position p = Position.Map.get(player);
        int x = p.getX();
        int y = p.getY();
        final int[] position = {x, y};
        Direction direction;
        if (cursorLocation[0] < x) {
            direction = Direction.Left;
        }
        else if (cursorLocation[0] > x) {
            direction = Direction.Right;
        }
        else if (cursorLocation[1] < y) {
            direction = Direction.Down;
        }
        else {
            direction = Direction.Up;
        }
        
        stats.castSpell();
        //fire a spell along the calculated path
        return followPath(direction, position, 
            new Runnable(){
                @Override
                public void run(){
                    Entity foe = checkFoe(position[0], position[1], player);
                    if (foe != null) {
                        fight(player, foe, true);
                    }
                }
            }
        );
    }
    
}
