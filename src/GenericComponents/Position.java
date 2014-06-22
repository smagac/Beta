package GenericComponents;

import com.artemis.Component;
import com.badlogic.gdx.math.Vector2;

public class Position extends Component {

	private final Vector2 loc;
	
	public Position(int x, int y)
	{
		loc = new Vector2(x, y);
	}
	
	public void move(int x, int y)
	{
		loc.set(x, y);
	}

	public float getX() {
		return loc.x;
	}
	
	public float getY() {
		return loc.y;
	}
}
