package core.components;

import com.artemis.Component;

public class Identifier extends Component {

    String adjective;
    String suffix;
    String type;
    String description;
    boolean hidden;

    public Identifier(String base, String adjective, String suffix, boolean hidden) {
        this.type = base;
        this.adjective = adjective;
        this.suffix = suffix;
        this.hidden = hidden;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", this.adjective, this.type, this.suffix);
    }

    public String getType() {
        return this.type;
    }

    public boolean hidden() {
        return hidden;
    }

    public void show() {
        hidden = false;
    }
}
