package com.nhydock.scenes.scene2d.ui.extras;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;

public class SimpleWindow extends Group {

    public SimpleWindow(Skin skin, String style) {
        super();
        WindowStyle winStyle = skin.get(style, WindowStyle.class);
        
        Image image = new Image(winStyle.background);
        image.setFillParent(true);
        
        this.addActor(image);
    }
    
}
