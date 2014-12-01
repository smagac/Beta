package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;

public class Position extends Component {

    private final Vector2 loc;
    private boolean changed;

    public Position(int x, int y) {
        loc = new Vector2(x, y);
    }

    public void move(int x, int y) {
        loc.set(x, y);
        changed = true;
    }

    public int getX() {
        return (int) loc.x;
    }

    public int getY() {
        return (int) loc.y;
    }

    public float distance(Position p) {
        return loc.dst(p.loc);
    }

    public void update() {
        changed = false;
    }

    public boolean changed() {
        return changed;
    }
}
