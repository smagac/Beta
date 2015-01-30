package scenes.dungeon;

import static scenes.dungeon.Direction.Down;
import static scenes.dungeon.Direction.Left;
import static scenes.dungeon.Direction.Right;
import static scenes.dungeon.Direction.Up;

import java.util.Comparator;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;
import scenes.Messages;
import scenes.Messages.Dungeon.CombatNotify;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import core.components.Combat;
import core.components.Groups;
import core.components.Groups.Monster;
import core.components.Identifier;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.dungeon.Floor;
import core.datatypes.quests.Quest;
import core.service.implementations.ScoreTracker;
import core.service.implementations.ScoreTracker.NumberValues;
import core.service.interfaces.IDungeonContainer;

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
    Array<Entity> actOrder = new Array<Entity>();

    private Engine engine;
    @Inject public IDungeonContainer dungeonService;
    
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
        if (monsters != null) {
            for (int i = 0; i < monsters.size; i++) {
                Entity m = monsters.get(i);
                Position p = Position.Map.get(m);
                if ((p.getX() == x && p.getY() == y)) {
                    return m;
                }
            }
        }

        Position p = Position.Map.get(player);
        if (p.getX() == x && p.getY() == y) {
            return player;
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
            // just move to square
            else {
                p.move(x, y);

                // try to figure out which room you're in now
                if (e == player) {
                    // descend
                    if (x == (int) end[0] && y == (int) end[1]) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Descend);
                    }
                    // ascend
                    else if (x == (int) start[0] && y == (int) start[1]) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Ascend);
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

        final float MULT = (actor == player) ? 2 : 1.25f;

        // ignore if target died at some point along the way
        if (bStats.hp <= 0) {
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
        
        Renderable aChar = actor.getComponent(Renderable.class);
        Renderable bChar = opponent.getComponent(Renderable.class);

        float shiftX, shiftY, x, y;

        Position p = Position.Map.get(actor);
        float scale = engine.getSystem(RenderSystem.class).getScale();
        x = p.getX() * scale;
        y = p.getY() * scale;
        shiftX = bChar.getActor().getX() - x;
        shiftY = bChar.getActor().getY() - y;

        aChar.getActor().clearActions();
        aChar.getActor().addAction(
                Actions.sequence(Actions.moveTo(x, y),
                        Actions.moveTo(x + shiftX / 4f, y + shiftY / 4f, RenderSystem.MoveSpeed / 2f),
                        Actions.moveTo(x, y, RenderSystem.MoveSpeed / 2f)));

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
            int dmg = Math.max(0, (int) (chance * aStats.getStrength()) - bStats.getDefense());
            bStats.hp = Math.max(0, bStats.hp - dmg);

            notification.dmg = dmg;
            if (actor == player) {
                notification.critical = chance > MULT * .8f && dmg > 0;
                Combat combatProp = Combat.Map.get(opponent);
                combatProp.aggress();
            } else {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
            }
            
            if (bStats.hp <= 0) {
                // drop item if opponent killed was not a player
                if (opponent != player){
                    aStats.exp += bStats.exp;
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
                    if (aStats.canLevelUp()) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.LevelUp);
                    }
                    ServiceManager.getService(ScoreTracker.class).increment(NumberValues.Monsters_Killed);
                    engine.removeEntity(opponent);

                    Identifier id = Identifier.Map.get(opponent);

                    String name = id.toString();
                    if (name.endsWith(Monster.Loot)) {
                        dungeonService.getProgress().lootFound++;
                        dungeonService.getProgress().totalLootFound++;
                    } else {
                        dungeonService.getProgress().monstersKilled++;
                        MessageDispatcher.getInstance().dispatchMessage(0, null, null, Quest.Actions.Hunt, name);
                    }
                    
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
                }
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Dead, opponent);
            }
        }
        else {
            notification.attacker = actor;
            notification.opponent = opponent;
            notification.dmg = -1;
        }
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Notify, notification);
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
                moveEntity(nextMove[0], nextMove[1], e);
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
        Position m = Position.Map.get(e);
        Position p = Position.Map.get(player);

        Stats s = statMap.get(e);
        if (s.hp <= 0) {
            return;
        }

        Combat prop = Combat.Map.get(e);

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
        } else if (Groups.playerType.matches(entity)) {
            player = null;
        }
    }
    
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
}
