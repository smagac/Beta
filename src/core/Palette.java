package core;

import com.badlogic.gdx.graphics.Color;

public enum Palette {

	Original(Color.BLACK, Color.WHITE),
	Gameboy(Color.valueOf("204631"), Color.valueOf("D7E894")),
	VirtualBoy(Color.valueOf("221313"), Color.valueOf("FE1313"));
	
	
	public final Color high;
	public final Color low;
	
	Palette(Color a, Color b)
	{
		low = a;
		high = b;
	}

}
