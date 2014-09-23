package scenes.dungeon.ui;
/**
 * Message types for the main state
 * @author nhydock
 *
 */
public class MenuMessage
{
	static final int Movement = -1;
	static final int Assist = 0;
	static final int Heal = 1;
	static final int Leave = 2;
	
	static final int Sacrifice = 1;
	
	public static final int Dead = 0x3000;
	public static final int Exit = 0x3001;
	public static final int LevelUp = 0x3002;
	public static final int Refresh = 0x3003;
}