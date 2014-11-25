package core;

import com.badlogic.gdx.graphics.Color;

public enum Palette {

    Original(Color.BLACK, Color.WHITE, false), Gameboy(Color.valueOf("204631"), Color.valueOf("D7E894"), false), VirtualBoy(
            Color.valueOf("221313"), Color.valueOf("FE1313"), false), Orange(Color.valueOf("2C0800"), Color
            .valueOf("FFCB28"), false), Tandy(Color.valueOf("001C04"), Color.valueOf("13FF45"), false), Sepia(Color
            .valueOf("8B5F40"), Color.valueOf("E4C392"), true), Vintage(Color.valueOf("4C3A60"), Color
            .valueOf("EDE5CE"), true), Pen(Color.valueOf("19177A"), Color.valueOf("FFFFFF"), false);

    public final Color high;
    public final Color low;
    public final boolean vignette;

    Palette(Color a, Color b, boolean v) {
        low = a;
        high = b;
        vignette = v;
    }

}
