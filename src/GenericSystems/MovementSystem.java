package GenericSystems;

import GenericComponents.Combat;
import GenericComponents.Monster;
import GenericComponents.Position;
import GenericComponents.Stats;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.annotations.Mapper;
import com.artemis.managers.GroupManager;
import com.artemis.systems.EntityProcessingSystem;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Handles all movement for dungeoning, as well as bump combat
 * @author nhydock
 *
 */
public class MovementSystem extends EntityProcessingSystem implements InputProcessor {

	scenes.dungeon.Scene parentScene;
	
	boolean[][] collision;
	Array<Rectangle> rooms;
	Vector2 start, end;
	Rectangle startRoom, endRoom;
	int floorNum;
	
	@Mapper ComponentMapper<Monster> monsterMap;
	@Mapper ComponentMapper<Position> positionMap;
	@Mapper ComponentMapper<Stats> statMap;
	@Mapper ComponentMapper<Combat> combatMap;
	
	Entity player;
	ImmutableBag<Entity> monsters;
	
	@SuppressWarnings("unchecked")
	public MovementSystem(int floor)
	{
		super(Aspect.getAspectForAll(Position.class, Monster.class));
		floorNum = floor;
	}

	@Override
	protected boolean checkProcessing() {
		return true;
	}
	
	protected boolean checkTile(int x, int y, Entity e)
	{
		Monster m = monsterMap.getSafe(e);
		boolean passable;
		if (m != null)
		{
			Position p = positionMap.get(e);
			passable = p.getRoom().contains(x, y);
		}
		else
		{
			passable = collision[x][y];
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
				Rectangle dest = null;
				for (int i = 0; i < rooms.size && dest == null; i++)
				{
					Rectangle room = rooms.get(i);
					if (room.contains(x, y))
					{
						dest = room;
					}
				}
				p.setRoom(dest);
				
				//descend
				if (p.getX() == (int)end.x && p.getY() == (int)end.y)
				{
					if (parentScene != null)
					{
						parentScene.changeFloor(floorNum++);
					}
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
						fight(player, e);
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
		
		//ignore if target died at some point along the way
		if (bStats.hp <= 0)
		{
			return;
		}
		
		int dmg = aStats.strength - bStats.defense;
		bStats.hp = Math.max(0, bStats.hp - dmg);
		
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
				parentScene.getItem(combat.getDrop());
			}
		}
	}
	
	public void setMap(TiledMapTileLayer map, Array<Rectangle> rooms) {
		this.rooms = rooms;
		
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
				}
			}
		}

		//find start stairs
		startRoom = rooms.random();
		start = new Vector2(MathUtils.random((int)startRoom.x, (int)(startRoom.x + startRoom.width)),
							MathUtils.random((int)startRoom.y, (int)(startRoom.y + startRoom.height)));
		System.out.println(start);
		//find end stairs
		endRoom = rooms.random();
		end = new Vector2(MathUtils.random((int)endRoom.x, (int)(endRoom.x + endRoom.width)),
						  MathUtils.random((int)endRoom.y, (int)(endRoom.y + endRoom.height)));
		System.out.println(end);
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
		p.setRoom(startRoom);
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
		Position monsterPos = positionMap.get(e);
		Position playerPos = positionMap.get(player);
		
		//only try moving once the character is in the same room as it
		if (playerPos.getRoom() == monsterPos.getRoom())
		{
			//try to move towards the player when nearby
			if (playerPos.distance(monsterPos) < 3)
			{
				//TODO follow player
			}
		}
	}
	
	@Override
	protected void begin() {
		monsters = world.getManager(GroupManager.class).getEntities("monsters");
	}
}
