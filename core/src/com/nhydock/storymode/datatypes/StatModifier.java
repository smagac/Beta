package com.nhydock.storymode.datatypes;

public class StatModifier {
    public final String type;
    public final float hp;
    public final float str;
    public final float def;
    public final float spd;

    public StatModifier(String type, float hp, float str, float def, float spd) {
        this.type = type;
        this.hp = hp;
        this.str = str;
        this.def = def;
        this.spd = spd;
    }
    
    @Override
    public String toString(){
        return this.type;
    }
}