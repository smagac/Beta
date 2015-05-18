package scene2d.ui.extras;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

public class Card extends Group {
    
    public Card(Skin skin, String titleText, String contentText, String iconName) {
   
        Image background = new Image(skin, "window4");
        background.setSize(200, 400);
        addActor(background);
        
        Image icon = new Image(skin, iconName);
        icon.setSize(96, 96);
        icon.setPosition(100, 280, Align.center);
        addActor(icon);
        
        Label title = new Label(titleText, skin, "prompt");
        title.setPosition(100, 200, Align.center);
        addActor(title);
        
        Label content = new Label(contentText, skin, "promptsm");
        content.setAlignment(Align.center);
        content.setPosition(20, 0);
        content.setSize(150, 260);
        content.setWrap(true);
        addActor(content);
        
        setSize(200, 400);
    }
    
}
