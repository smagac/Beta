package core;

import com.badlogic.gdx.graphics.Color;

public enum Palette {

	Original(Color.BLACK, Color.WHITE),
	Gameboy(Color.valueOf("204631"), Color.valueOf("D7E894"));
	
	public final Color high;
	public final Color low;
	
	Palette(Color a, Color b)
	{
		low = a;
		high = b;
	}

}
