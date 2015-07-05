package com.nhydock.scenes.scene2d.ui.extras;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

public class Card extends Group {
    
    private Image background;
    private Label content;
    private Label title;

    private Skin skin;
    private Image icon;
    
    public Card(Skin skin, String text) {
        this(skin, text, null, null);
    }
    
    public Card(Skin skin, String titleText, String contentText, String iconName) {
        this.skin = skin;
        background = new Image(skin, "window4");
        background.setSize(200, 400);
        background.setTouchable(Touchable.disabled);
        addActor(background);
        
        if (iconName != null) {
            icon = new Image(skin, iconName);
            icon.setSize(96, 96);
            icon.setPosition(100, 280, Align.center);
            icon.setTouchable(Touchable.disabled);
            addActor(icon);
        }
        
        title = new Label(titleText, skin, "prompt");
        title.setPosition(100, 200, Align.center);
        title.setAlignment(Align.center);
        title.setTouchable(Touchable.disabled);
        addActor(title);
        
        if (contentText != null) {
            content = new Label(contentText, skin, "promptsm");
            content.setAlignment(Align.center);
            content.setPosition(20, 0);
            content.setSize(150, 260);
            content.setWrap(true);
            content.setTouchable(Touchable.disabled);
            addActor(content);
        }
        
        setSize(200, 400);
        this.setTouchable(Touchable.enabled);
    }
    
    public void setTitle(String text){
        title.setText(text);
    }
    
    public void setTitlePosition(int align) {
        if (align == Align.top){
            title.setPosition(100, 375, Align.top);
        } else if (align == Align.center){
            title.setPosition(100, 200, Align.center);
        }
    }
    
    public void setDescription(String text){
        if (content == null){
            content = new Label(text, skin, "promptsm");
            content.setAlignment(Align.center);
            content.setPosition(20, 0);
            content.setSize(150, 260);
            content.setWrap(true);
            addActor(content);
        } else {
            content.setText(text);
        }
    }
    
    public void setIcon(String iconName){
        if (icon == null) {
            icon = new Image(skin, iconName);
            icon.setSize(96, 96);
            icon.setPosition(100, 280, Align.center);
            addActor(icon);
        } else {
            icon.setDrawable(skin, iconName);
        }
    }
    
    @Override
    public void setHeight(float size){
        super.setHeight(size);
        background.setHeight(size);
    }
}
