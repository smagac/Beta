package squidpony.squidgrid.util;

import squidpony.squidutility.jdaygraph.Topology;

/**
 * Represents the eight grid directions and the deltaX, deltaY values associated
 * with those directions.
 *
 * The grid referenced has x positive to the right and y positive downwards on
 * screen.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public enum DirectionIntercardinal implements Topology {

    UP(0, -1, 'w'), DOWN(0, 1, 'x'), LEFT(-1, 0, 'a'), RIGHT(1, 0, 'd'), UP_LEFT(-1, -1, 'q'), UP_RIGHT(1, -1, 'e'), DOWN_LEFT(-1, 1, 'z'), DOWN_RIGHT(1, 1, 'c'), NONE(0, 0, 's');
    /**
     * An array which holds only the four cardinal directions.
     */
    public static final DirectionIntercardinal[] CARDINALS = {UP, DOWN, LEFT, RIGHT};
    /**
     * An array which holds only the four diagonal directions.
     */
    public static final DirectionIntercardinal[] DIAGONALS = {UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT};
    /**
     * An array which holds all eight OUTWARDS directions.
     */
    public static final DirectionIntercardinal[] OUTWARDS = {UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT};
    /**
     * The x coordinate difference for this direction.
     */
    public final int deltaX;
    /**
     * The y coordinate difference for this direction.
     */
    public final int deltaY;

    public final char symbol;

    /**
     * Returns the direction that most closely matches the input.
     *
     * @param x
     * @param y
     * @return
     */
    static public DirectionIntercardinal getDirection(int x, int y) {
        if (x == 0 && y == 0) {
            return NONE;
        }

        double angle = Math.atan2(y, x);
        double degree = Math.toDegrees(angle);
        degree += 90 + 360;//rotate to all positive and 0 is up
        degree %= 360;//normalize
        if (degree < 22.5) {
            return UP;
        } else if (degree < 67.5) {
            return UP_RIGHT;
        } else if (degree < 112.5) {
            return RIGHT;
        } else if (degree < 157.5) {
            return DOWN_RIGHT;
        } else if (degree < 202.5) {
            return DOWN;
        } else if (degree < 247.5) {
            return DOWN_LEFT;
        } else if (degree < 292.5) {
            return LEFT;
        } else if (degree < 337.5) {
            return UP_LEFT;
        } else {
            return UP;
        }
    }

    /**
     * Gets the direction associated with the passed in character. If there is
     * no direction associated then null is returned.
     *
     * @param c
     * @return
     */
    static public DirectionIntercardinal getDirection(char c) {
        for (DirectionIntercardinal d : values()) {
            if (d.symbol == c) {
                return d;
            }
        }
        return null;
    }

    /**
     * Returns the Direction one step clockwise including diagonals.
     *
     * @return
     */
    public DirectionIntercardinal clockwise() {
        switch (this) {
            case UP:
                return UP_RIGHT;
            case DOWN:
                return DOWN_LEFT;
            case LEFT:
                return UP_LEFT;
            case RIGHT:
                return DOWN_RIGHT;
            case UP_LEFT:
                return UP;
            case UP_RIGHT:
                return RIGHT;
            case DOWN_LEFT:
                return LEFT;
            case DOWN_RIGHT:
                return DOWN;
            case NONE:
            default:
                return NONE;
        }
    }

    /**
     * Returns the Direction one step counterclockwise including diagonals.
     *
     * @return
     */
    public DirectionIntercardinal counterClockwise() {
        switch (this) {
            case UP:
                return UP_LEFT;
            case DOWN:
                return DOWN_RIGHT;
            case LEFT:
                return DOWN_LEFT;
            case RIGHT:
                return UP_RIGHT;
            case UP_LEFT:
                return LEFT;
            case UP_RIGHT:
                return UP;
            case DOWN_LEFT:
                return DOWN;
            case DOWN_RIGHT:
                return RIGHT;
            case NONE:
            default:
                return NONE;
        }
    }

    /**
     * Returns the direction directly opposite of this one.
     *
     * @return
     */
    public DirectionIntercardinal opposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            case UP_LEFT:
                return DOWN_RIGHT;
            case UP_RIGHT:
                return DOWN_LEFT;
            case DOWN_LEFT:
                return UP_RIGHT;
            case DOWN_RIGHT:
                return UP_LEFT;
            case NONE:
            default:
                return NONE;
        }
    }

    private DirectionIntercardinal(int x, int y, char symbol) {
        this.deltaX = x;
        this.deltaY = y;
        this.symbol = symbol;
    }
}
