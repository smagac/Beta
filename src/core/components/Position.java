package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class Position extends Component {

    private float x, y;
    private boolean changed;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move(int x, int y) {
        this.x = x;
        this.y = y;
        changed = true;
    }

    public int getX() {
        return (int) x;
    }

    public int getY() {
        return (int) y;
    }

    public float distance(Position p) {
        return Vector2.dst(this.x, this.y, p.x, p.y);
    }

    public void update() {
        changed = false;
    }

    public boolean changed() {
        return changed;
    }
}
