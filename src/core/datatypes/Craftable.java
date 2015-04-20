package core.datatypes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectIntMap;

public class Craftable extends Item {
    /**
     * String - item type Integer - quantity required
     */
    private ObjectIntMap<String> requirements = new ObjectIntMap<String>();

    protected boolean canMake = false;

    private String isReady;
    
    public Craftable() {
    };

    public Craftable(String name, String adj, String... parts) {
        super(name, adj);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            int count = MathUtils.random(1, 5);
            requirements.put(part, count);
        }
        
        isReady = "*"+fullname;
    }

    public ObjectIntMap<String> getRequirements() {
        return requirements;
    }

    public Array<String> getRequirementTypes() {
        return requirements.keys().toArray();
    }

    @Override
    public String toString() {
        return ((canMake) ? isReady : fullname);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + adj.hashCode();
        result = prime * result + name.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.hashCode() == this.hashCode()) {
            return true;
        }
        if (o instanceof String) {
            String s = (String) o;
            return name.equals(s);
        }
        if (o instanceof Craftable) {
            Craftable c = (Craftable) o;
            return fullname.equals(c.fullname);
        }
        if (o instanceof Item) {
            Item i = (Item) o;
            return name.equals(i.name);
        }
        return false;
    }

    @Override
    public void write(Json json) {
        json.writeValue("name", name);
        json.writeValue("adj", adj);

        json.writeObjectStart("requirements");

        for (String key : requirements.keys()) {
            json.writeValue(key, requirements.get(key, 0));
        }

        json.writeObjectEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        name = jsonData.getString("name");
        adj = jsonData.getString("adj");
        fullname = String.format("%s %s", adj, name);
        isReady = "*"+fullname;
        
        JsonValue jv = jsonData.get("requirements");
        requirements.clear();

        for (JsonValue key : jv) {
            requirements.put(key.name, key.asInt());
        }
    }
}