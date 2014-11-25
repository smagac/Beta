package core.datatypes;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Item implements Comparable<Item>, Serializable {
    String adj;
    String name;

    /**
     * Used for JsonSerializable
     */
    public Item() {
    }

    public Item(String name, String adj) {
        this.adj = adj;
        this.name = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + adj.hashCode();
        result = prime * result + name.hashCode();

        return result;
    }

    public String fullname() {
        return String.format("%s %s", adj, name);
    }

    public String descriptor() {
        return adj;
    }

    public String type() {
        return name;
    }

    @Override
    public String toString() {
        return fullname();
    }

    @Override
    public int compareTo(Item o) {
        // only compare names against craftables
        if (o instanceof Craftable) {
            Craftable c = (Craftable) o;
            return name.compareTo(c.name);
        }
        return fullname().compareTo(o.fullname());
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
            return name.equals(c.name);
        }
        if (o instanceof Item) {
            Item i = (Item) o;
            return fullname().equals(i.fullname());
        }
        return false;
    }

    @Override
    public void write(Json json) {
        json.writeValue("name", name);
        json.writeValue("adj", adj);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        name = jsonData.getString("name");
        adj = jsonData.getString("adj");
    }
}