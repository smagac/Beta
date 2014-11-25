package core.datatypes;

public class StatModifier {
    public final float hp;
    public final float str;
    public final float def;
    public final float spd;

    public StatModifier(float hp, float str, float def, float spd) {
        this.hp = hp;
        this.str = str;
        this.def = def;
        this.spd = spd;
    }
}