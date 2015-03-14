package scenes.dungeon.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

import core.components.Equipment;

public class EquipmentBar {

    private Group container;
    
    private static final String powerFormat = "+%d";
    
    private ProgressBar durabilityBar;
    private Label powerValue;
    private Image equipmentIcon;
    
    public EquipmentBar(Class<? extends Equipment.Piece> piece, Skin skin) {
        container = new Group();
        
        if (piece == Equipment.Sword.class) {
            equipmentIcon = new Image(skin, "sword");
        } else if (piece == Equipment.Shield.class) {
            equipmentIcon = new Image(skin, "shield");
        } else {
            equipmentIcon = new Image(skin, "armor");
        }
        equipmentIcon.setSize(32, 32);
        
        container.addActor(equipmentIcon);
        
        durabilityBar = new ProgressBar(0, 1.0f, .1f, true, skin, "default");
        durabilityBar.setSize(24, 80);
        durabilityBar.setPosition(16, 36, Align.bottom);
        container.addActor(durabilityBar);
        
        powerValue = new Label("+0", skin, "promptsm");
        powerValue.setPosition(16, 130, Align.bottom);
        powerValue.setAlignment(Align.center);
        container.addActor(powerValue);
        
        container.setSize(48, 160);
    }
    
    /**
     * Set the power rating of the equipment
     * @param value
     */
    public void setPower(int value) {
        powerValue.setText(String.format(powerFormat, value));
    }
    
    public void updateDurability(int durability, float max) {
        durabilityBar.setValue(durability / max);
    }
    
    public Group getActor() {
        return container;
    }
}
