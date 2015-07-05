package com.nhydock.scenes.scene2d.ui.extras;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

public class Pointer extends Image {

    public Pointer(Skin skin){
        super(skin, "pointer");
    }
    

    /**
     * Positions the pointer next to an actor
     * 
     * @param focus
     * @param center
     */
    public void setPosition(Actor focus, int alignment) {
        Vector2 v = new Vector2();
        focus.localToStageCoordinates(v);
        
        float xOffset = 0, yOffset = 0;
        if ((alignment & Align.left) == Align.left) {
            xOffset = -getWidth();
        }
        else if ((alignment & Align.center) == Align.center) {
            xOffset = focus.getWidth() / 2 - getWidth() / 2;
        }
        else if ((alignment & Align.right) == Align.right) {
            xOffset = focus.getWidth() + getWidth();
        }

        if ((alignment & Align.top) == Align.top) {
            yOffset = focus.getHeight() - getHeight();
        }
        else if ((alignment & Align.center) == Align.center) {
            yOffset = focus.getHeight() / 2 - getHeight() / 2;
        }
        else if ((alignment & Align.bottom) == Align.bottom) {
            yOffset = 0;
        }

        setPosition(v.x + xOffset, v.y + yOffset);
        setScale(((alignment & Align.right) == Align.right) ? -1 : 1, 1);
    }
}
