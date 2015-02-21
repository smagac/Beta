package core.util.dungeon;

import java.util.Arrays;
import java.util.Random;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import core.datatypes.dungeon.FloorData;

/**
 * System for generating a floor, its rooms, and connecting all paths in the
 * room
 * <p>
 * </p>
 * Algorithms modified from C++ to Java from RoguelikeLib, <br>
 * </br> Roguelike Library is portable open-source library written in C++. It
 * consist of set of classes that can be used in all roguelike games. Classes
 * are categorized to fullfill tasks of random map generation, pathfinding and
 * counting field of view.
 * <p>
 * </p>
 * http://sourceforge.net/projects/roguelikelib/
 * 
 * @author nhydock
 *
 */
public class PathMaker {

    public static final float MAX_SATURATION = .8f;

    public static final int NULL = 0;
    public static final int ROOM = 1;
    public static final int HALL = 2;
    public static final int WALL = 3;
    public static final int UP = 4;
    public static final int DOWN = 5;

    private static final int X = 0;
    private static final int Y = 1;

    // don't allow instantiation
    private PathMaker() {
    }

    public static void run(FloorData f) {
        float[] filled = { 0 };
        Random random = new Random(f.getSeed());
        int roomCount;
        {
            int s = Math.max(5, ((3 * f.getDepth()) / 10) + f.getDepth());
            int e = Math.max(5, ((5 * f.getDepth()) / 10) + f.getDepth());
            roomCount = s + random.nextInt(e - s + 1);
        }
         
        int[][] board = f.getTiles();
        int[] start = f.getStart();
        int[] end = f.getEnd();
        boolean[][] collision = f.getCollision();
        
        float size = board.length * board[0].length;
        Array<Room> rooms = f.getRooms();

        // place a handleful of random rooms until threshold is met
        while (rooms.size < roomCount && filled[0] / size < MAX_SATURATION) {
            int width = random.nextInt(11) + 3;
            int height = random.nextInt(11) + 3;

            Array<int[]> locations = findAllOpenAreas(board, width, height);
            if (locations.size > 0) {
                int[] where = locations.get(random.nextInt(locations.size));
                Room r = new Room(where[0], where[1], width, height);
                rooms.add(r);
                for (int x = r.left(); x <= r.right(); x++) {
                    for (int y = r.bottom(); y <= r.top(); y++) {
                        board[x][y] = ROOM;
                    }
                }

                filled[0] += r.width * r.height;

                for (int x = r.left(); x <= r.right(); x++) {
                    board[x][r.bottom()] = ROOM;
                    board[x][r.top()] = ROOM;
                }
                for (int y = r.bottom(); y <= r.top(); y++) {
                    board[r.left()][y] = ROOM;
                    board[r.right()][y] = ROOM;
                }
            }
            locations = null;
        }

        connectRooms(board, rooms, filled, random);

        // find random spot to place start and end
        int x = 0;
        int y = 0;
        do {
            x = random.nextInt(board.length);
            y = random.nextInt(board[0].length);
        }
        while (board[x][y] != ROOM);
        board[x][y] = UP;
        start[0] = x;
        start[1] = y;

        do {
            x = random.nextInt(board.length);
            y = random.nextInt(board[0].length);
        }
        while (board[x][y] != ROOM);
        board[x][y] = DOWN;
        end[0] = x;
        end[1] = y;
        
        //add wall padding
        for (int i = 0; i < board.length; i++) {
            for (int n = 0; n < board[i].length; n++) {
                if (board[i][n] == NULL) {
                    boolean placeWall = false;
                    for (int a = Math.max(0, i-1); a <= Math.min(board.length-1, i+1) && !placeWall; a++)
                    {
                        for (int b = Math.max(0, n-1); b <= Math.min(board[i].length-1, n+1) && !placeWall; b++)
                        {
                            placeWall = board[a][b] == ROOM;
                        }
                    }
                    if (placeWall) {
                        board[i][n] = WALL;
                    }
                }
            }
        }
        
        for (int i = 0; i < board.length; i++) {
            for (int n = 0; n < board[i].length; n++) {
                collision[i][n] = board[i][n] == NULL || board[i][n] == WALL;
            }
        }
    }

    /**
     * Attempts to find all open areas on the board that this rectangle can fit
     * 
     * @param r
     */
    private static Array<int[]> findAllOpenAreas(int[][] board, int width, int height) {
        Array<int[]> positions = new Array<int[]>();
        boolean[][] good = new boolean[board.length][board[0].length];

        // go across horizontally, finding areas where the rectangle may fit
        // width wise
        for (int y = 1; y < board[0].length-1; ++y) {
            int horizontal_count = 0;
            for (int x = 1; x < board.length-1; ++x) {
                // count up in areas where there is no room
                if (board[x][y] == NULL)
                    horizontal_count++;
                // if we encounter a room, the rectangle can not fit there
                else
                    horizontal_count = 0;

                // when we've reached the edge of our rectangle's width
                // we can mark that this is a safe place to measure from
                if (horizontal_count == width) {
                    good[x - width + 1][y] = true;
                    // increment back one in case the next space is also
                    // acceptable for being a rectangle
                    horizontal_count--;
                }
            }
        }

        // now that count verticals we have established good lines of where a
        // rectangle may start
        // we need to count vertically down where it can fit

        for (int x = 0; x < board.length; ++x) {
            int vertical_count = 0;
            for (int y = 0; y < board[0].length; ++y) {
                // check against only the points that we flagged as potentially
                // okay
                if (good[x][y])
                    vertical_count++;
                // if we didn't flag that point, then we can't fit a rectangle
                // there vertically
                else
                    vertical_count = 0;

                // when our rectangle is fully formed, we can add it as a
                // plausible location
                if (vertical_count == height) {
                    positions.add(new int[] { x, y - height + 1 });
                    vertical_count--;
                }
            }
        }

        return positions;
    }

    /**
     * Connects all rooms to their nearest counterparts using the warshall
     * alrgorithm <br>
     * </br> For now we keep things simple and don't add doors
     */
    private static void connectRooms(int[][] board, Array<Room> rooms, float[] filled, Random random) {
        // no need to connect rooms if there's less than 2 rooms
        if (rooms.size < 2)
            return;

        // for warshall algorithm
        // set the connection matrices
        boolean[][] roomConnections = new boolean[rooms.size][rooms.size];
        boolean[][] closure = new boolean[rooms.size][rooms.size];
        float[][] distanceMatrix = new float[rooms.size][rooms.size];
        int[][][][] closestMatrix = new int[rooms.size][rooms.size][2][2];

        for (int i = 0; i < rooms.size; i++) {
            Arrays.fill(distanceMatrix[i], Integer.MAX_VALUE);
        }

        // go through all rooms to find which ones are closest
        for (int a = 0; a < rooms.size; a++) {
            Room roomA = rooms.get(a);
            for (int b = 0; b < rooms.size; b++) {
                if (a == b)
                    continue;

                Room roomB = rooms.get(b);

                // go around the border of each room to find the smallest
                // distance
                int[] cellA = new int[2];
                int[] cellB = new int[2];
                int[][] closestCells = { new int[2], new int[2] };

                for (int aX = roomA.left(); aX <= roomA.right(); aX++) {
                    for (int aY = roomA.bottom(); aY <= roomA.top(); aY++) {

                        cellA[X] = aX;
                        cellA[Y] = aY;
                        for (int bX = roomB.left(); bX <= roomB.right(); bX++) {
                            for (int bY = roomB.bottom(); bY <= roomB.top(); bY++) {
                                cellB[X] = bX;
                                cellB[Y] = bY;

                                // find the smallest distance between any cell
                                // relation with CellA
                                float distance = Vector2.dst(cellA[X], cellA[Y], cellB[X], cellB[Y]);
                                if (distance < distanceMatrix[a][b] || distance == distanceMatrix[a][b]
                                        && random.nextBoolean()) {
                                    distanceMatrix[a][b] = distance;

                                    // make sure to mark which cells it is that
                                    // are the closest
                                    closestCells[0] = cellA;
                                    closestCells[1] = cellB;
                                }
                            }
                        }
                    }
                }
                // persist the relationship
                closestMatrix[a][b] = closestCells;
            }
        }

        // now go through the generated list of closest cells and connect the
        // rooms that have the shortest distances
        for (int a = 0; a < rooms.size; a++) {
            // find true closest room relative to roomA
            float min = Float.MAX_VALUE;
            int closest = 0;
            for (int b = 0; b < rooms.size; b++) {
                if (a == b)
                    continue;

                float dist = distanceMatrix[a][b];
                if (dist < min) {
                    min = dist;
                    closest = b;
                }
            }
            // get the connecting cells
            int[] from = closestMatrix[a][closest][0];
            int[] to = closestMatrix[a][closest][1];

            // create the tunnel to that closest room
            if (!roomConnections[a][closest] && makeHallway(board, from, to, filled, random)) {
                // flag the rooms as connected both ways
                roomConnections[a][closest] = true;
                roomConnections[closest][a] = true;
            }
        }

        // even though closest rooms may have been connected, we still need to
        // make sure all rooms are connected
        // in a singular weighted path

        for (int conA = 0; conA != -1;) {
            int conB;

            // make sure the transitive closure is marked between already
            // connected rooms
            for (int a = 0; a < rooms.size; a++)
                for (int b = 0; b < rooms.size; b++)
                    closure[a][b] = roomConnections[a][b];

            // we do this every loop to make sure any new changes in connection
            // from the previous loop
            // are carried over to the rest of the graph
            for (int a = 0; a < rooms.size; a++) {
                for (int b = 0; b < rooms.size; b++) {
                    if (closure[a][b] && a != b) {
                        // carry connections through (transitively)
                        for (int c = 0; c < rooms.size; c++) {
                            if (closure[b][c]) {
                                closure[a][c] = true;
                                closure[c][a] = true;
                            }
                        }
                    }
                }
            }

            // check if all rooms are connected
            conA = -1;
            for (int a = 0; a < rooms.size && conA == -1; a++) {
                for (int b = 0; b < rooms.size && conA == -1; b++) {
                    // mark if a isn't connected to the graph at some point
                    if (a != b && !closure[a][b]) {
                        conA = a;
                    }
                }
            }

            // if one wasn't connected, we need to fix it
            if (conA != -1) {
                // for now distance doesn't matter, so we just connect a random
                // one
                do {
                    conB = random.nextInt(rooms.size);
                }
                while (conA == conB);

                int[] from = closestMatrix[conA][conB][0];
                int[] to = closestMatrix[conA][conB][1];

                makeHallway(board, from, to, filled, random);

                roomConnections[conA][conB] = true;
                roomConnections[conB][conA] = true;
            }
        }

    }

    /**
     * Constructs a value hallway between two points
     * 
     * @param from
     * @param to
     * @return true if a hallway could be constructed between the two rooms
     *         false if the points are invalid/outside of the board
     */
    private static boolean makeHallway(int[][] board, int[] from, int[] to, float[] filled, Random random) {
        // ignore out of bounds attempts
        if (!(from[X] >= 0 && from[X] < board.length && from[Y] >= 0 && from[Y] < board[0].length)
                || !(to[X] >= 0 && to[X] < board.length && to[Y] >= 0 && to[Y] < board[0].length)) {
            return false;
        }

        int x1 = from[X];
        int x2 = to[X];
        int y1 = from[Y];
        int y2 = to[Y];

        board[x1][y1] = ROOM;
        board[x2][y2] = ROOM;

        filled[0] += 2;

        // keep track of directional motion
        int dirX, dirY;

        // find initial direction
        if (x2 > x1)
            dirX = 1; // east
        else
            dirX = -1; // west
        if (y2 > y1)
            dirY = 1; // north
        else
            dirY = -1; // south

        // move into random direction
        boolean firstHorizontal = random.nextBoolean();
        boolean secondHorizontal = random.nextBoolean();

        // making a corridor might take awhile, just continue this iterative
        // process
        while (true) {

            if (x1 != x2 && y1 != y2) {
                // adjust the first tile iterator
                if (firstHorizontal)
                    x1 += dirX;
                else
                    y1 += dirY;
            }

            if (x1 != x2 && y1 != y2)
            // still not equal
            {
                // adjust the second tile iterator
                if (secondHorizontal)
                    x2 -= dirX;
                else
                    y2 -= dirY;
            }

            if (board[x1][y1] == NULL) {
                board[x1][y1] = HALL;
                filled[0]++;
            }
            if (board[x2][y2] == NULL) {
                board[x2][y2] = HALL;
                filled[0]++;
            }
            // check once more if the iterators match after moving
            // if the iterators are on the same level, try connecting them
            if (x1 == x2) {
                while (y1 != y2) {
                    // adjust y until we reach destination
                    y1 += dirY;
                    if (board[x1][y1] == NULL) {
                        board[x1][y1] = HALL;
                        filled[0]++;
                    }
                }
                if (board[x1][y1] == NULL) {
                    board[x1][y1] = HALL;
                    filled[0]++;
                }
                // return that we've connected the hallway successfully
                return true;
            }
            // iterators are on the same level horizontally, so we must now
            // connect across
            if (y1 == y2) {
                while (x1 != x2) {
                    // adjust y until we reach destination
                    x1 += dirX;
                    if (board[x1][y1] == NULL) {
                        board[x1][y1] = HALL;
                        filled[0]++;
                    }
                }
                if (board[x1][y1] == NULL) {
                    board[x1][y1] = HALL;
                    filled[0]++;
                }
                return true;
            }
        }
    }
}
