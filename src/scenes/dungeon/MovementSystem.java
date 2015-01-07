package scenes.dungeon;

import static scenes.dungeon.Direction.Down;
import static scenes.dungeon.Direction.Left;
import static scenes.dungeon.Direction.Right;
import static scenes.dungeon.Direction.Up;
import scenes.Messages;
import github.nhydock.ssm.Inject;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;

import core.common.Tracker;
import core.components.Combat;
import core.components.Identifier;
import core.components.Groups.*;
import core.components.Groups;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.dungeon.Floor;
import core.datatypes.quests.Quest;
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

    ComponentMapper<Position> positionMap = ComponentMapper.getFor(Position.class);
    ComponentMapper<Identifier> identityMap = ComponentMapper.getFor(Identifier.class);
    ComponentMapper<Monster> monsterMap = ComponentMapper.getFor(Monster.class);
    ComponentMapper<Combat> combatMap = ComponentMapper.getFor(Combat.class);
    ComponentMapper<Stats> statMap = ComponentMapper.getFor(Stats.class);

    private Entity player;
    Array<Entity> monsters = new Array<Entity>();

    private Engine engine;
    private scenes.dungeon.Scene parentScene;
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
                Position p = positionMap.get(m);
                if ((p.getX() == x && p.getY() == y)) {
                    return m;
                }
            }
        }

        Position p = positionMap.get(player);
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
        Position p = positionMap.get(e);
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
                    Combat c = combatMap.get(e);
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
                        parentScene.setFloor(dungeonService.getProgress().nextFloor());
                    }
                    // ascend
                    else if (x == (int) start[0] && y == (int) start[1]) {
                        parentScene.setFloor(dungeonService.getProgress().prevFloor());
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
            MessageDispatcher.getInstance().dispatchMessage(0, null, null, Messages.Dungeon.FIGHT, opponent);
            return;
        } else if (Groups.bossType.matches(actor)) {
            MessageDispatcher.getInstance().dispatchMessage(0, null, null, Messages.Dungeon.FIGHT, actor);
            return;
        }
        
        Renderable aChar = actor.getComponent(Renderable.class);
        Renderable bChar = opponent.getComponent(Renderable.class);

        float shiftX, shiftY, x, y;

        Position p = positionMap.get(actor);
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

        RenderSystem rs = engine.getSystem(RenderSystem.class);

        if (MathUtils.randomBoolean(1f - (MathUtils.random(.8f, MULT) * bStats.getSpeed()) / 100f)) {
            parentScene.hitSound.play();
            float chance = MathUtils.random(.8f, MULT);
            int dmg = Math.max(0, (int) (chance * aStats.getStrength()) - bStats.getDefense());
            bStats.hp = Math.max(0, bStats.hp - dmg);

            String msg = "";
            if (actor == player) {
                Identifier id = identityMap.get(opponent);
                String name = id.toString();
                id.show();
                if (dmg == 0) {
                    msg = name + " blocked your attack!";
                    rs.hit(opponent, "Block");
                }
                else {
                    if (chance > MULT * .8f) {
                        msg = "CRITICAL HIT!\n";
                    }
                    msg += "You attacked " + name + " for " + dmg + " damage";
                    rs.hit(opponent, "" + dmg);
                }
                Combat combatProp = combatMap.get(opponent);
                combatProp.aggress();
            }
            else {
                Identifier id = identityMap.get(actor);
                String name = id.toString();
                id.show();
                if (dmg == 0) {
                    msg = "You blocked " + name + "'s attack";
                    rs.hit(opponent, "Block");
                }
                else {
                    msg = name + " attacked you for " + dmg + " damage";
                    rs.hit(opponent, "" + dmg);
                }
            }
            parentScene.log(msg);

            if (bStats.hp <= 0) {
                // player is dead
                if (opponent == player) {
                    parentScene.dead();
                }
                // drop enemy item
                else {
                    Combat combat = combatMap.get(opponent);
                    String cmsg = combat.getDeathMessage(identityMap.get(opponent).toString());
                    
                    parentScene.log(cmsg);
                    parentScene.getItem(combat.getDrop());
                    
                    aStats.exp += bStats.exp;
                    if (aStats.canLevelUp()) {
                        parentScene.levelUp();
                    }
                    Tracker.NumberValues.Monsters_Killed.increment();
                    engine.removeEntity(opponent);
                   
                    Identifier id = identityMap.get(opponent);

                    String name = id.toString();
                    if (name.endsWith(Monster.Loot)) {
                        dungeonService.getProgress().lootFound++;
                        dungeonService.getProgress().totalLootFound++;
                    } else {
                        dungeonService.getProgress().monstersKilled++;
                        MessageDispatcher.getInstance().dispatchMessage(0, null, null, Quest.Actions.Hunt, name);
                    }
                }
            }
        }
        else {
            if (actor == player) {
                Identifier id = identityMap.get(opponent);
                String name = id.toString();
                id.show();
                String msg = "You attacked " + name + ", but missed!";
                parentScene.log(msg);
            }
            else {
                Identifier id = identityMap.get(actor);
                String name = id.toString();
                id.show();
                String msg = name + "attacked you, but missed!";
                parentScene.log(msg);
            }
            rs.hit(opponent, "Miss");
        }
    }

    /**
     * Set the system's main player and moves them to their starting position
     * Used to descend to the next level
     * @param entity 
     */
    public void moveToStart(Entity entity) {
        Position p = positionMap.get(entity);

        p.move((int) start[0], (int) start[1]);
        p.update();
    }

    /**
     * Sets an entity to the end position of a level. Used when ascending to a
     * previous level.  Primarily for the player, but can be used for anything.
     */
    public void moveToEnd(Entity entity) {
        Position p = positionMap.get(entity);

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

        Position playerPos = positionMap.get(player);
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

        if (moveEntity(x, y, player)) {
            // execute a turn
            for (int i = 0; i < monsters.size; i++)
            {
                process(monsters.get(i));
            }
            return true;
        }

        return false;
    }

    protected void process(Entity e) {
        Position m = positionMap.get(e);
        Position p = positionMap.get(player);

        Stats s = statMap.get(e);
        if (s.hp <= 0) {
            return;
        }

        Combat prop = combatMap.get(e);

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
        parentScene = null;
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

    public void setScene(Scene scene) {
        parentScene = scene;
    }
}
