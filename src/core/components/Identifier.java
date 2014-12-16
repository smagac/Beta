package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Array;

public class Identifier extends Component {

    String adjective;
    String suffix;
    String type;
    String description;
    boolean hidden;
    
    Array<String> modifiers;

    public Identifier(String base, String suffix, String... adjectives) {
        this.type = base;
        this.suffix = suffix;
        
        modifiers = new Array<String>(adjectives);
        buildName();
    }

    private void buildName() {
        this.adjective = "";
        for (String adj : modifiers) {
            this.adjective += adj + " ";
        }
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

    public void hide() {
        hidden = true;
    }
    
    public void show() {
        hidden = false;
    }
    
    public void addModifier(String adj) {
        modifiers.add(adj);
        buildName();
    }
    
    /**
     * Removes the first instance of the adjective in the name
     * @param adj
     */
    public void removeModifier(String adj) {
        modifiers.removeValue(adj, false);
        buildName();
    }
}
