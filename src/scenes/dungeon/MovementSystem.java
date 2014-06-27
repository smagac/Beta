package scenes.dungeon;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.managers.GroupManager;
import com.artemis.systems.EntityProcessingSystem;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
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
public class MovementSystem extends EntityProcessingSystem implements InputProcessor {

	scenes.dungeon.Scene parentScene;
	
	boolean[][] collision;
	Vector2 start, end;
	int floorNum;
	
	@Mapper ComponentMapper<Monster> monsterMap;
	@Mapper ComponentMapper<Position> positionMap;
	@Mapper ComponentMapper<Stats> statMap;
	@Mapper ComponentMapper<Combat> combatMap;
	@Mapper ComponentMapper<Identifier> idMap;
	
	Entity player;
	ImmutableBag<Entity> monsters;
	
	Sound hit;
	
	@SuppressWarnings("unchecked")
	public MovementSystem(int floor)
	{
		super(Aspect.getAspectForAll(Monster.class));
		floorNum = floor;
	}

	@Override
	protected boolean checkProcessing() {
		return true;
	}
	
	protected boolean checkTile(int x, int y, Entity e)
	{
		if ((x < 0 || x >= collision.length) || (y < 0 || y >= collision.length))
			return false;
		
		boolean passable;
		passable = collision[x][y];
		for (int i = 0; i < monsters.size() && passable; i++)
		{
			Entity monster = monsters.get(i);
			Position p = positionMap.get(monster);
			passable = !(p.getX() == x && p.getY() == y);
		}
		Position p = positionMap.get(player);
		passable = passable && !(p.getX() == x && p.getY() == y);
		
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
					parentScene.log("You descend to floor " + (floorNum + 1)) ;
					parentScene.changeFloor(floorNum + 1);
				}
				//ascend
				else if (p.getX() == (int)start.x && p.getY() == (int)start.y)
				{
					if (floorNum - 1 > 0)
					{
						parentScene.log("You ascend to floor " + (floorNum - 1));
					}
					parentScene.changeFloor(floorNum - 1);
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
					fight(e, player);
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
			}
			else
			{
				Identifier id = idMap.get(actor);
				String name = id.toString();
				if (dmg == 0)
				{
					msg = name + " blocked " + name + "'s attack";
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
					parentScene.log("You killed the " + idMap.get(opponent).toString());
					Combat combat = combatMap.get(opponent);
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
	 * @param player
	 */
	public void moveToStart(Entity player)
	{
		this.player = player;
		Position p = positionMap.get(player);
		
		p.move((int)start.x, (int)start.y);
	}

	@Override
	public boolean keyDown(int keycode) {
		
		Position playerPos = positionMap.get(player);
		int x = playerPos.getX();
		int y = playerPos.getY();
		boolean moved = false;
		if (keycode == Keys.UP || keycode == Keys.W)
		{
			y++;
			moved = true;
		}
		if (keycode == Keys.DOWN || keycode == Keys.S)
		{
			y--;
			moved = true;
		}
		if (keycode == Keys.LEFT || keycode == Keys.A)
		{
			x--;
			moved = true;
		}
		if (keycode == Keys.RIGHT || keycode == Keys.D)
		{
			x++;
			moved = true;
		}
		
		if (moved) {
			//make sure enemy list is populated at least once
			if (monsters == null)
			{
				begin();
			}
			moveTo(x, y, player);
			//execute a turn
			process();
		}
		
		return moved;
	}
	
	@Override
	public boolean keyUp(int keycode) { return false; }
	
	@Override
	public boolean keyTyped(char character) { return false; }
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
	
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
	
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
	
	@Override
	public boolean mouseMoved(int screenX, int screenY) { return false; }
	
	@Override
	public boolean scrolled(int amount) { return false; }

	@Override
	protected void process(Entity e) {
		Position m = positionMap.get(e);
		Position p = positionMap.get(player);
		
		if (e.isActive())
		{
			return;
		}
		
		//only try moving once the character is in the same room as it
		//try to move towards the player when nearby
		if (p.distance(m) < 3)
		{
			//roll for move
			Stats s = statMap.get(e);
			//chance multiplied since agro
			if (MathUtils.randomBoolean(Math.min(MathUtils.random(1f, 3f)*s.getSpeed(), 100f) / 100f))
			{
				int dX = 0;
				int dY = 0;
				
				if (p.getX() < m.getX()) dX = -1;
				if (p.getX() > m.getX()) dX = 1;
				if (p.getY() < m.getY()) dY = -1;
				if (p.getY() > m.getY()) dY = 1;
				
				//follow player chance
				moveTo(m.getX() + dX, m.getY() + dY, e);
			}
		}
		//lazily wander around
		else
		{
			//roll for move
			Stats s = statMap.get(e);
			if (MathUtils.randomBoolean(Math.min(MathUtils.random(1f, 2f)*s.getSpeed(), 100f) / 100f))
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
			}
		}
	}
	
	public void setScene(scenes.dungeon.Scene scene)
	{
		this.parentScene = scene;
	}
	
	@Override
	protected void begin() {
		monsters = world.getManager(GroupManager.class).getEntities("monsters");
	}
}
