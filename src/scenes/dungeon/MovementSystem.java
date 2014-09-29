package scenes.dungeon;

import static scenes.dungeon.Direction.Down;
import static scenes.dungeon.Direction.Left;
import static scenes.dungeon.Direction.Right;
import static scenes.dungeon.Direction.Up;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import core.common.Tracker;
import core.components.Combat;
import core.components.Identifier;
import core.components.Monster;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Dungeon;
import core.datatypes.quests.Quest;

/**
 * Handles all movement for dungeoning, as well as bump combat
 * @author nhydock
 *
 */
public class MovementSystem extends EntityProcessingSystem {

	private scenes.dungeon.Scene parentScene;
	
	private boolean[][] collision;
	private Vector2 start, end;
	
	@Mapper private ComponentMapper<Position> positionMap;
	@Mapper private ComponentMapper<Stats> statMap;
	@Mapper private ComponentMapper<Combat> combatMap;
	@Mapper private ComponentMapper<Identifier> idMap;
	
	private Entity player;
	protected ImmutableBag<Entity> monsters;
	
	private Sound hit;
	
	@SuppressWarnings("unchecked")
	/**
	 * Creates a new movement and combat handler
	 * @param floor - floor number representation of this system
	 */
	public MovementSystem(int depth, Dungeon map)
	{
		super(Aspect.getAspectForAll(Monster.class));
		
		//build collision map	
		TiledMapTileLayer floor = map.getFloor(depth).layer;
		collision = new boolean[floor.getWidth()][floor.getHeight()];
		for (int x = 0; x < collision.length; x++)
		{
			for (int y = 0; y < collision[0].length; y++)
			{
				Cell c = floor.getCell(x, y);
				if (c == null || c.getTile() == null)
				{
					collision[x][y] = false;
				}
				else
				{
					TiledMapTile t = c.getTile();
					collision[x][y] = t.getProperties().get("passable", Boolean.class);
					//set as start or end if they're step tiles
					if (t.getId() == 4)
					{
						start = new Vector2(x, y);
					}
					else if (t.getId() == 3)
					{
						end = new Vector2(x, y);
					}
				}
			}
		}
	}

	@Override
	protected boolean checkProcessing() {
		return true;
	}
	
	/**
	 * Checks to see if a tile is specifically of type wall
	 * @param x
	 * @param y
	 * @return true if tile is not passable or if it is out of bounds
	 */
	protected boolean isWall(int x, int y)
	{
		if ((x < 0 || x >= collision.length) || (y < 0 || y >= collision.length))
			return true;
		return !collision[x][y];
	}
	
	/**
	 * Checks to see if an entity can move to a tile
	 * @param x
	 * @param y
	 * @param e - the entity to move
	 * @return true if there is no other entity or wall blocking its way
	 */
	private boolean checkTile(int x, int y, Entity e)
	{
		boolean passable = !isWall(x, y);
		if (monsters != null)
		{
			for (int i = 0; i < monsters.size() && passable; i++)
			{
				Entity monster = monsters.get(i);
				Position p = positionMap.get(monster);
				passable = !(p.getX() == x && p.getY() == y);
			}
		}
		if (passable)
		{
			Position p = positionMap.get(player);
			passable = !(p.getX() == x && p.getY() == y);
		}
		return passable;
	}
	
	/**
	 * Moves an entity to a specified location on the map
	 * @param x
	 * @param y
	 * @param e
	 */
	private void moveTo(int x, int y, Entity e)
	{
		Position p = positionMap.get(e);
		if (checkTile(x, y, e))
		{
			p.move(x, y);
			
			//try to figure out which room you're in now
			if (e == player)
			{
				//descend
				if (p.getX() == (int)end.x && p.getY() == (int)end.y)
				{
					parentScene.descend();
				}
				//ascend
				else if (p.getX() == (int)start.x && p.getY() == (int)start.y)
				{
					parentScene.ascend();
				}
			}
		}
		//Handle combat
		else
		{
			if (e == player)
			{
				for (int i = 0; i < monsters.size(); i++)
				{
					Entity monster = monsters.get(i);
					Position monsterPos = positionMap.get(monster);
					if (monsterPos.getX() == x && monsterPos.getY() == y)
					{
						fight(e, monster);
					}
				}
			}
			else 
			{
				Position playerPos = positionMap.get(player);
				if (playerPos.getX() == x && playerPos.getY() == y)
				{
					Combat c = combatMap.get(e);
					if (!c.isPassive())
					{
						fight(e, player);
					}
				}
			}
		}
		
	}

	/**
	 * Make two entities fight
	 * @param actor
	 * @param opponent
	 */
	private void fight(Entity actor, Entity opponent)
	{
		Stats aStats = statMap.get(actor);
		Stats bStats = statMap.get(opponent);
		
		final float MULT = (actor == player)?2:1.25f;
		
		//ignore if target died at some point along the way
		if (bStats.hp <= 0)
		{
			return;
		}
		
		Renderable aChar = actor.getComponent(Renderable.class);
		Renderable bChar = opponent.getComponent(Renderable.class);
		
		
		float shiftX, shiftY, x, y;
		
		Position p = positionMap.get(actor);
		float scale = world.getSystem(RenderSystem.class).getScale();
		x = p.getX()*scale;
		y = p.getY()*scale;
		shiftX = bChar.getActor().getX()-x;
		shiftY = bChar.getActor().getY()-y;
		
		aChar.getActor().clearActions();
		aChar.getActor().addAction(
			Actions.sequence(
				Actions.moveTo(x, y),
				Actions.moveTo(x + shiftX/4f, y + shiftY/4f, RenderSystem.MoveSpeed/2f),
				Actions.moveTo(x, y, RenderSystem.MoveSpeed/2f)
			)
		);
		
		RenderSystem rs = world.getSystem(RenderSystem.class);
		
		if (MathUtils.randomBoolean(1f-(MathUtils.random(.8f, MULT)*bStats.getSpeed())/100f))
		{
			hit.play();
			float chance = MathUtils.random(.8f, MULT);
			int dmg = Math.max(0, (int)(chance*aStats.getStrength()) - bStats.getDefense());
			bStats.hp = Math.max(0, bStats.hp - dmg);
			
			String msg = "";
			if (actor == player)
			{
				Identifier id = idMap.get(opponent);
				String name = id.toString();
				id.show();
				if (dmg == 0)
				{
					msg = name + " blocked your attack!";
					rs.hit(opponent, "Block");
				}
				else
				{
					if (chance > MULT * .8f)
					{
						msg = "CRITICAL HIT!\n";
					}
					msg += "You attacked " + name + " for " + dmg + " damage";
					rs.hit(opponent, ""+dmg);
				}
				Combat combatProp = combatMap.get(opponent);
				combatProp.aggress();
			}
			else
			{
				Identifier id = idMap.get(actor);
				String name = id.toString();
				id.show();
				if (dmg == 0)
				{
					msg = "You blocked " + name + "'s attack";
					rs.hit(opponent, "Block");
				}
				else
				{
					msg = name + " attacked you for " + dmg + " damage";
					rs.hit(opponent, ""+dmg);
				}
			}
			parentScene.log(msg);
			
			if (bStats.hp <= 0)
			{
				//player is dead
				if (opponent == player)
				{
					parentScene.dead();
				}
				//drop enemy item
				else
				{
					Combat combat = combatMap.get(opponent);
					parentScene.log(combat.getDeathMessage(idMap.get(opponent).toString()));					
					parentScene.getItem(combat.getDrop());
					MessageDispatcher.getInstance().dispatchMessage(0, null, parentScene.playerService.getQuestTracker(), Quest.Actions.Gather, combat.getDrop().fullname());

					aStats.exp += bStats.exp;
					if (aStats.canLevelUp())
					{
						parentScene.levelUp();
					}
					Tracker.NumberValues.Monsters_Killed.increment();
					opponent.deleteFromWorld();
					
					Identifier id = idMap.get(opponent);
					
					if (id.toString().endsWith(Monster.Loot))
					{
						parentScene.progress.lootFound++;
						parentScene.progress.totalLootFound++;
					}
					else
					{
						parentScene.progress.monstersKilled++;
						MessageDispatcher.getInstance().dispatchMessage(0, null, parentScene.playerService.getQuestTracker(), Quest.Actions.Hunt, id.getType());
					}
				}
			}
		}
		else
		{
			if (actor == player)
			{
				Identifier id = idMap.get(opponent);
				String name = id.toString();
				id.show();
				parentScene.log("You attacked " + name + " but missed!");
			}
			else
			{
				Identifier id = idMap.get(actor);
				String name = id.toString();
				id.show();
				parentScene.log(name + " attacked you and missed");
			}
			rs.hit(opponent, "Miss");
		}
	}

	/**
	 * Set the system's main player and moves them to their starting position
	 * Used to descend to the next level
	 */
	public void moveToStart()
	{
		Position p = positionMap.get(player);
		
		p.move((int)start.x, (int)start.y);
	}
	
	/**
	 * Sets the player to the end position of a level.
	 * Used when ascending to a previous level
	 */
	public void moveToEnd()
	{
		Position p = positionMap.get(player);
		
		p.move((int)end.x, (int)end.y);	
	}
	
	/**
	 * Assigns a direct reference to the player in the system for faster access
	 */
	public void setPlayer()
	{
		player = world.getManager(TagManager.class).getEntity("player");
	}

	/**
	 * Moves just the player entity and executes a turn
	 * @param direction
	 * @return
	 */
	public boolean movePlayer(Direction direction) {
		if (direction == null)
			return false;
		
		Position playerPos = positionMap.get(player);
		int x = playerPos.getX();
		int y = playerPos.getY();
		if (direction == Up)
		{
			y++;
		}
		if (direction == Down)
		{
			y--;
		}
		if (direction == Right)
		{
			x++;
		}
		if (direction == Left)
		{
			x--;
		}
		
		if (!isWall(x, y)) {
			moveTo(x, y, player);
			//execute a turn
			process();
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void process(Entity e) {
		Position m = positionMap.get(e);
		Position p = positionMap.get(player);
		
		Stats s = statMap.get(e);
		if (s.hp <= 0)
		{
			return;
		}
		

		Combat prop = combatMap.get(e);
		
		//only try moving once the character is in the same room as it
		//try to move towards the player when nearby
		if (prop.isAgro())
		{
			//roll for move
			//Stats s = statMap.get(e);
			//chance multiplied since agro
			if (MathUtils.randomBoolean(prop.getMovementRate()))
			{
				int dX = 0;
				int dY = 0;
				boolean priority = MathUtils.randomBoolean();
				
				//horizontal priority flip
				if (priority)
				{
					if (p.getX() < m.getX()) dX = -1;
					if (p.getX() > m.getX()) dX = 1;
					if (dX == 0)
					{
						if (p.getY() < m.getY()) dY = -1;
						if (p.getY() > m.getY()) dY = 1;
					}
						
				}
				//vertical priority
				else
				{
					if (p.getY() < m.getY()) dY = -1;
					if (p.getY() > m.getY()) dY = 1;
					if (dY == 0)
					{
						if (p.getX() < m.getX()) dX = -1;
						if (p.getX() > m.getX()) dX = 1;
					}
				}
				
				//follow player chance
				moveTo(m.getX() + dX, m.getY() + dY, e);
				
				if (p.distance(m) > 5)
				{
					prop.calm();
				}
			}
		}
		//lazily wander around
		else
		{
			//roll for move
			//Stats s = statMap.get(e);
			if (MathUtils.randomBoolean(prop.getMovementRate()))
			{
				int dX = 0;
				int dY = 0;
				
				dX = MathUtils.random(-1, 1);
				if (dX == 0)
				{
					dY = MathUtils.random(-1, 1);
				}
				
				//follow player chance
				moveTo(m.getX() + dX, m.getY() + dY, e);
				
				if (p.distance(m) < 3 && !prop.isPassive())
				{
					prop.aggress();
				}
			}
		}
	}
	
	public void setScene(scenes.dungeon.Scene scene)
	{
		this.parentScene = scene;
		if ( scene != null)
			this.hit = parentScene.hitSound;
	}
	
	@Override
	public void begin() {
		monsters = world.getManager(GroupManager.class).getEntities("monsters");
	}
	
	@Override
	public void end() {
		parentScene.refresh();
	}
	
	/**
	 * Dereference as much as we can
	 */
	public void dispose()
	{
		parentScene = null;
		hit = null;
		monsters = null;
		player = null;
	}
}
