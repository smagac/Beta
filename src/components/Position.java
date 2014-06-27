package components;

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

	public int getX() {
		return (int)loc.x;
	}
	
	public int getY() {
		return (int)loc.y;
	}
	
	public float distance(Position p)
	{
		return loc.dst(p.loc);
	}

	public void step(Position p) {
		
		
	}
}
