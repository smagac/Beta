package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

public class Position extends Component {
    public static final ComponentMapper<Position> Map = ComponentMapper.getFor(Position.class);

    private float x, y;
    private float destX, destY;
    private boolean changed, fight;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position(int[] p) {
        this.x = p[0];
        this.y = p[1];
        this.destX = p[0];
        this.destY = p[1];
    }

    public void move(int x, int y) {
        this.destX = x;
        this.destY = y;
        changed = true;
    }

    public int getX() {
        if (changed) {
            return (int) destX;
        }
        return (int) x;
    }

    public int getY() {
        if (changed) {
            return (int) destY;
        }
        return (int) y;
    }
    
    public int getDestinationX() {
        return (int) destX;
    }
    
    public int getDestinationY() {
        return (int) destY;
    }

    public float distance(Position p) {
        return Vector2.dst(this.x, this.y, p.x, p.y);
    }

    public void update() {
        if (changed) {
            x = destX;
            y = destY;
        }
        changed = false;
        fight = false;
    }
    
    public void fight(int x, int y) {
        fight = true;
        destX = x;
        destY = y;
    }
    
    public boolean isFighting() {
        return fight;
    }

    public boolean hasChanged() {
        return changed;
    }
}
