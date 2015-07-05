package com.nhydock.storymode.scenes.dungeon.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.nhydock.storymode.components.Equipment;

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
        equipmentIcon.setPosition(0, 12, Align.bottomLeft);
        
        container.addActor(equipmentIcon);
        
        durabilityBar = new ProgressBar(0, 1.0f, .1f, false, skin, "default");
        durabilityBar.setSize(44, 4);
        durabilityBar.setPosition(24, 0, Align.bottom);
        container.addActor(durabilityBar);
        
        powerValue = new Label("+0", skin, "tag");
        powerValue.setPosition(48, 10, Align.bottomRight);
        powerValue.setAlignment(Align.right);
        container.addActor(powerValue);
        
        container.setSize(48, 48);
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
