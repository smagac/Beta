package core.util.dungeon;

import java.util.Arrays;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * System for generating a floor, its rooms, and connecting all paths in the room
 * <p></p>
 * Algorithms modified from C++ to Java from RoguelikeLib,
 * <br></br>
 * Roguelike Library is portable open-source library written in C++. It consist of 
 * set of classes that can be used in all roguelike games. Classes are categorized to 
 * fullfill tasks of random map generation, pathfinding and counting field of view.
 * <p></p>
 * http://sourceforge.net/projects/roguelikelib/
 * @author nhydock
 *
 */
public class PathMaker {

	int[][] board;
	Array<Room> rooms;
	
	static final float MAX_SATURATION = .8f;
	float filled;
	float size;
	
	public static final int NULL = 0;
	public static final int ROOM = 1;
	public static final int HALL = 2;
	public static final int WALL = 3;
	public static final int UP   = 4;
	public static final int DOWN = 5;
	
	public int[][] run(int roomCount, int w, int h)
	{
		rooms = new Array<Room>();
		
		board = new int[w][h];
		size = w*h;
		
		filled = 0;
		rooms.clear();
		
		//place a handleful of random rooms until threshold is met
		while (rooms.size < roomCount && !isSaturated())
		{
			int width = MathUtils.random(10)+3;
			int height = MathUtils.random(10)+3;
			
			Array<Vector2> locations = findAllOpenAreas(width, height);
			if (locations.size > 0)
			{
				Vector2 where = locations.random();
				Room r = new Room((int)where.x, (int)where.y, width, height);
				rooms.add(r);
				for (int x = r.left(); x <= r.right(); x++)
				{
					for (int y = r.bottom(); y <= r.top(); y++)
					{
						board[x][y] = ROOM;
						filled++;
					}
				}
				
				for (int x = r.left(); x <= r.right(); x++)
				{
					board[x][r.bottom()] = WALL;
					board[x][r.top()] = WALL;
				}
				for (int y = r.bottom(); y <= r.top(); y++)
				{
					board[r.left()][y] = WALL;
					board[r.right()][y] = WALL;
				}
			}
		}
		
		connectRooms();
		
		//find random spot to place start and end
		int x = 0;
		int y = 0;
		do
		{
			x = MathUtils.random(0, board.length-1);
			y = MathUtils.random(0, board[0].length-1);
		} while (board[x][y] != ROOM);
		board[x][y] = UP;
		
		do
		{
			x = MathUtils.random(0, board.length-1);
			y = MathUtils.random(0, board[0].length-1);
		} while (board[x][y] != ROOM);
		board[x][y] = DOWN;
		
		return board;
	}
	
	private boolean isSaturated() {
		return (filled/size) > MAX_SATURATION;
	}

	/**
	 * Attempts to find all open areas on the board that this rectangle can fit
	 * @param r
	 */
	private Array<Vector2> findAllOpenAreas(int width, int height)
	{
		Array<Vector2> positions = new Array<Vector2>();
		int[][] good = new int[board.length][board[0].length];
		
		for (int y=0; y < board[0].length; ++y)
			for (int x=0; x < board.length; ++x)
				good[x][y] = 0;

		//go across horizontally, finding areas where the rectangle may fit width wise
		for (int y = 0; y < board[0].length; ++y)
		{
			int horizontal_count = 0;
			for (int x = 0; x < board.length; ++x)
			{
				//count up in areas where there is no room
				if (board[x][y] == NULL)
					horizontal_count++;
				//if we encounter a room, the rectangle can not fit there
				else
					horizontal_count = 0;

				//when we've reached the edge of our rectangle's width
				// we can mark that this is a safe place to measure from
				if (horizontal_count == width)
				{
					good[x-width+1][y] = 1;
					//increment back one in case the next space is also
					// acceptable for being a rectangle
					horizontal_count--;	
				}
			}
		}

		//now that count verticals we have established good lines of where a rectangle may start
		// we need to count vertically down where it can fit

		for (int x=0; x < board.length; ++x)
		{
			int vertical_count=0;
			for (int y = 0; y < board[0].length; ++y)
			{
				//check against only the points that we flagged as potentially okay
				if (good[x][y] == 1)
					vertical_count++;
				//if we didn't flag that point, then we can't fit a rectangle there vertically
				else
					vertical_count=0;

				//when our rectangle is fully formed, we can add it as a plausible location
				if (vertical_count == height)
				{
					positions.add(new Vector2(x, y-height+1));
					vertical_count--;
				}
			}
		}
		
		return positions;
	}

	
	/**
	 * Connects all rooms to their nearest counterparts using the warshall alrgorithm
	 * <br></br>
	 * For now we keep things simple and don't add doors
	 */
	private void connectRooms()
	{
		//no need to connect rooms if there's less than 2 rooms
		if (rooms.size < 2)
			return;

		// for warshall algorithm
		// set the connection matrices
		boolean[][] roomConnections = new boolean[rooms.size][rooms.size];
		boolean[][] closure = new boolean[rooms.size][rooms.size];
		float[][] distanceMatrix = new float[rooms.size][rooms.size];
		Vector2[][][] closestMatrix = new Vector2[rooms.size][rooms.size][];
		
		for (int i = 0; i < rooms.size; i++)
		{
			Arrays.fill(distanceMatrix[i], Integer.MAX_VALUE);
		}

		//go through all rooms to find which ones are closest
		for (int a = 0; a < rooms.size; a++)
		{
			Room roomA = rooms.get(a);
			for (int b = 0; b < rooms.size; b++)
			{
				if (a == b)
					continue;
				
				Room roomB = rooms.get(b);
				
				//go around the border of each room to find the smallest distance
				Vector2 cellA = new Vector2();
				Vector2 cellB = new Vector2();
				Vector2[] closestCells = {new Vector2(), new Vector2()};
				
				
				for (int aX = roomA.left(); aX <= roomA.right(); aX++)
				{
					for (int aY = roomA.bottom(); aY <= roomA.top(); aY++)
					{
						
						cellA = new Vector2(aX, aY);
						for (int bX = roomB.left(); bX <= roomB.right(); bX++)
						{
							for (int bY = roomB.bottom(); bY <= roomB.top(); bY++)
							{
								cellB = new Vector2(bX, bY);
								
								//find the smallest distance between any cell relation with CellA
								float distance = cellA.dst(cellB);
								if (distance < distanceMatrix[a][b] || distance == distanceMatrix[a][b] && MathUtils.randomBoolean())
								{
									distanceMatrix[a][b] = distance;
									
									//make sure to mark which cells it is that are the closest
									closestCells[0].set(cellA);
									closestCells[1].set(cellB);
								}
							}
						}
					}
				}
				//persist the relationship
				closestMatrix[a][b] = closestCells;
			}
		}

		//now go through the generated list of closest cells and connect the rooms that have the shortest distances
		for (int a = 0; a < rooms.size; a++)
		{
			//find true closest room relative to roomA
			float min = Float.MAX_VALUE;
			int closest = 0;
			for (int b = 0; b < rooms.size; b++)
			{
				if (a == b)
					continue;
		
				float dist = distanceMatrix[a][b];
				if (dist < min)
				{
					min = dist;
					closest = b;
				}
			}
			//get the connecting cells
			Vector2 from = closestMatrix[a][closest][0];
			Vector2 to = closestMatrix[a][closest][1];
			
			//create the tunnel to that closest room
			if (!roomConnections[a][closest] && makeHallway(from, to))
			{
				//flag the rooms as connected both ways
				roomConnections[a][closest] = true;
				roomConnections[closest][a] = true;
			}
		}
		
		//even though closest rooms may have been connected, we still need to make sure all rooms are connected
		// in a singular weighted path 

		for (int conA = 0; conA != -1;)
		{
			int conB;
			
			//make sure the transitive closure is marked between already connected rooms
			for (int a = 0; a < rooms.size; a++)
				for (int b = 0; b < rooms.size; b++)
					closure[a][b] = roomConnections[a][b];
			
			//we do this every loop to make sure any new changes in connection from the previous loop
			// are carried over to the rest of the graph
			for (int a = 0; a < rooms.size; a++)
			{
				for (int b = 0; b < rooms.size; b++)
				{
					if (closure[a][b] && a != b)
					{
						//carry connections through (transitively)
						for (int c = 0; c < rooms.size; c++)
						{
							if (closure[b][c])
							{
								closure[a][c] = true;
								closure[c][a] = true;
							}
						}
					}
				}
			}
			
			//check if all rooms are connected
			conA = -1;
			for (int a = 0; a < rooms.size && conA == -1; a++)
			{
				for (int b = 0; b < rooms.size && conA == -1; b++)
				{
					//mark if a isn't connected to the graph at some point
					if (a != b && !closure[a][b])
					{
						conA=a;
					}
				}
			}
			
			//if one wasn't connected, we need to fix it
			if (conA != -1)
			{
				// for now distance doesn't matter, so we just connect a random one
				do {
					conB = MathUtils.random(rooms.size-1);
				} 
				while(conA==conB);
				
				Vector2[] closest = closestMatrix[conA][conB];
				Vector2 from = closest[0];
				Vector2 to = closest[1];

				makeHallway(from, to);

				roomConnections[conA][conB]=true;
				roomConnections[conB][conA]=true;
			}
		}

	}
	
	/**
	 * Constructs a value hallway between two points
	 * @param from
	 * @param to
	 * @return true if a hallway could be constructed between the two rooms
	 * 		   false if the points are invalid/outside of the board
	 */
	private boolean makeHallway(Vector2 from, Vector2 to)
	{
		//ignore out of bounds attempts
		if (!(from.x >= 0 && from.x < board.length && from.y >= 0 && from.y < board[0].length) || 
			!(to.x >= 0 && to.x < board.length && to.y >= 0 && to.y < board[0].length))
		{
			return false;
		}
		
		int x1 = (int)from.x;
		int x2 = (int)to.x;
		int y1 = (int)from.y;
		int y2 = (int)to.y;
		
		board[x1][y1] = HALL;
		board[x2][y2] = HALL;
		
		filled += 2;
		
		//keep track of directional motion
		int dirX, dirY;
		
		//find initial direction
		if (x2 > x1) dirX = 1;		//east
		else dirX = -1;				//west
		if (y2 > y1) dirY = 1;		//north
		else dirY = -1;				//south
		
		//move into random direction
		boolean firstHorizontal = MathUtils.randomBoolean();
		boolean secondHorizontal = MathUtils.randomBoolean();
		
		//making a corridor might take awhile, just continue this iterative process
		while (true)
		{
			
			if (x1 != x2 && y1 != y2)
			{
				//adjust the first tile iterator
				if (firstHorizontal)
					x1 += dirX;
				else
					y1 += dirY;
			}
			
			if (x1 != x2 && y1 != y2)
			//still not equal
			{
				//adjust the second tile iterator
				if (secondHorizontal)
					x2-=dirX;
				else
					y2-=dirY;
			}
			
			if (board[x1][y1] == NULL)
			{
				board[x1][y1] = HALL;
				filled++;
			}
			if (board[x2][y2] == NULL)
			{
				board[x2][y2] = HALL;
				filled++;
			}
			//check once more if the iterators match after moving
			// if the iterators are on the same level, try connecting them
			if (x1 == x2)
			{
				while(y1!=y2)
				{
					//adjust y until we reach destination
					y1 += dirY;
					if (board[x1][y1] == NULL)
					{
						board[x1][y1] = HALL;
						filled++;
					}
				}
				if (board[x1][y1] == NULL)
				{
					board[x1][y1] = HALL;
					filled++;
				}
				//return that we've connected the hallway successfully
				return true;
			}
			// iterators are on the same level horizontally, so we must now connect across
			if (y1 == y2)
			{
				while(x1!=x2)
				{
					//adjust y until we reach destination
					x1 += dirX;
					if (board[x1][y1] == NULL)
					{
						board[x1][y1] = HALL;
						filled++;
					}
				}
				if (board[x1][y1] == NULL)
				{
					board[x1][y1] = HALL;
					filled++;
				}
				return true;
			}
		}
	}
	

	public Array<Room> getRooms() {
		return rooms;
	}
}
