package core.util.dungeon;

import com.badlogic.gdx.math.Rectangle;

/**
 * Specialized rectangle with padding expectancy for our tiles
 * @author nhydock
 *
 */
public class Room extends Rectangle {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Room(int x, int y, int width, int height) {
		super(x, y, width, height);
	}

	public int left()
	{
		return (int)this.x;
	}
	
	public int right()
	{
		return (int)(this.x+this.width)-1;
	}
	
	public int bottom()
	{
		return (int)this.y;
	}
	
	public int top()
	{
		return (int)(this.y+this.height)-1;
	}
	
	public int innerLeft()
	{
		return left()+1;
	}
	
	public int innerRight()
	{
		return right()-1;
	}
	
	public int innerTop()
	{
		return top()-1;
	}
	
	public int innerBottom()
	{
		return bottom()+1;
	}

}
