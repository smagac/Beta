package scenes.dungeon;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import components.Combat;
import components.Identifier;
import components.Monster;
import components.Position;
import components.Stats;
import core.common.Tracker;

/**
 * Handles all movement for dungeoning, as well as bump combat
 * @author nhydock
 *
 */
public class MovementSystem extends EntityProcessingSystem {

	scenes.dungeon.Scene parentScene;
	
	boolean[][] collision;
	Vector2 start, end;
	
	@Mapper ComponentMapper<Monster> monsterMap;
	@Mapper ComponentMapper<Position> positionMap;
	@Mapper ComponentMapper<Stats> statMap;
	@Mapper ComponentMapper<Combat> combatMap;
	@Mapper ComponentMapper<Identifier> idMap;
	
	Entity player;
	ImmutableBag<Entity> monsters;
	
	Sound hit;

	private boolean enabledInput;
	
	protected static final int
		UP = 0,
		DOWN = 1,
		LEFT = 2,
		RIGHT = 3;
	
	@SuppressWarnings("unchecked")
	/**
	 * Creates a new movement and combat handler
	 * @param floor - floor number representation of this system
	 */
	public MovementSystem(int floor)
	{
		super(Aspect.getAspectForAll(Monster.class));
		enabledInput = true;
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
	protected boolean checkTile(int x, int y, Entity e)
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
	
	protected void moveTo(int x, int y, Entity e)
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
		
		float mult = 1.25f;
		if (actor == player)
		{
			mult = 2f;
		}
		
		//ignore if target died at some point along the way
		if (bStats.hp <= 0)
		{
			return;
		}
		if (MathUtils.randomBoolean(Math.min((MathUtils.random(.8f, mult)*aStats.getSpeed()) / bStats.getSpeed(), 1f)))
		{
			hit.play();
			int dmg = Math.max(0, (int)(MathUtils.random(.8f, mult)*aStats.getStrength()) - bStats.getDefense());
			bStats.hp = Math.max(0, bStats.hp - dmg);
			
			String msg;
			if (actor == player)
			{
				Identifier id = idMap.get(opponent);
				String name = id.toString();
				if (dmg == 0)
				{
					msg = name + " blocked your attack!";
				}
				else
				{
					msg = "You attacked " + name + " for " + dmg + " damage";
				}
				Combat combatProp = combatMap.get(opponent);
				combatProp.aggress();
			}
			else
			{
				Identifier id = idMap.get(actor);
				String name = id.toString();
				if (dmg == 0)
				{
					msg = "You blocked " + name + "'s attack";
				}
				else
				{
					msg = name + " attacked you for " + dmg + " damage";
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
					aStats.exp++;
					if (aStats.levelUp())
					{
						parentScene.log("You have gained a level!");
					}
					Tracker.NumberValues.Monsters_Killed.increment();
					opponent.deleteFromWorld();
				}
			}
		}
		else
		{
			if (actor == player)
			{
				Identifier id = idMap.get(opponent);
				String name = id.toString();
				parentScene.log("You attacked " + name + " but missed!");
			}
			else
			{
				Identifier id = idMap.get(actor);
				String name = id.toString();
				parentScene.log(name + " attacked you and missed");
			}
		}
	}
	
	public void setMap(TiledMapTileLayer map) {
		//build collision map	
		collision = new boolean[map.getWidth()][map.getHeight()];
		for (int x = 0; x < collision.length; x++)
		{
			for (int y = 0; y < collision[0].length; y++)
			{
				Cell c = map.getCell(x, y);
				if (c == null)
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
	
	/**
	 * Set the system's main player and moves them to their starting position
	 */
	public void moveToStart()
	{
		Position p = positionMap.get(player);
		
		p.move((int)start.x, (int)start.y);
	}
	
	public void moveToEnd()
	{
		Position p = positionMap.get(player);
		
		p.move((int)end.x, (int)end.y);	
	}
	
	public void setPlayer()
	{
		player = world.getManager(TagManager.class).getEntity("player");
	}

	public boolean movePlayer(int direction) {

		if (!this.enabledInput){
			return false;
		}
		
		if (direction < UP || direction > RIGHT)
			return false;
		
		Position playerPos = positionMap.get(player);
		int x = playerPos.getX();
		int y = playerPos.getY();
		if (direction == UP)
		{
			y++;
		}
		if (direction == DOWN)
		{
			y--;
		}
		if (direction == RIGHT)
		{
			x++;
		}
		if (direction == LEFT)
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

	public boolean mouseMoved(Vector2 xy)
	{
		if (enabledInput)
		{
			float screenX = xy.x;
			float screenY = xy.y;
			
			for (int i = 0; i < monsters.size(); i++)
			{
				Entity e = monsters.get(i);
				Position p = positionMap.get(e);
				if (screenX == p.getX() && screenY == p.getY())
				{
					p = positionMap.get(player);
					//make sure the dialog won't cover the player
					if ((p.getY() >= screenY+1 && p.getY() < screenY + 3) && 
						(screenX > p.getX()-3 && screenX < p.getX() +3 ))
					{
						screenY -= 3;
					}
					Stats s = statMap.get(e);
					Identifier id = idMap.get(e);
					parentScene.showStats(screenX, screenY, id.toString(), 
							String.format("HP: %3d / %3d", s.hp, s.maxhp)
						);
					return true;
				}
			}
		}
		parentScene.hideStats();
		return false;
	}

	public void inputEnabled(boolean enable)
	{
		this.enabledInput = enable;
	}
	
	@Override
	protected void process(Entity e) {
		Position m = positionMap.get(e);
		Position p = positionMap.get(player);
		
		if (!e.isActive())
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
}
