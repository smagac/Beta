package GenericComponents;

import com.artemis.Component;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Position extends Component {

	private final Vector2 loc;
	private Rectangle room;
	
	public Position(int x, int y)
	{
		loc = new Vector2(x, y);
	}
	
	public Position(int x, int y, Rectangle room)
	{
		this(x, y);
		this.room = room;
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
	
	public void setRoom(Rectangle r)
	{
		room = r;
	}
	
	public Rectangle getRoom()
	{
		return room;
	}
	
	public float distance(Position p)
	{
		return loc.dst(p.loc);
	}
}
