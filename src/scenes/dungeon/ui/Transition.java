package scenes.dungeon.ui;

import github.nhydock.ssm.Inject;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import core.DataDirs;
import core.components.Identifier;
import core.components.Renderable;
import core.service.interfaces.IPlayerContainer;
import scenes.UI;

/**
 * Simple UI/Stage for doing a transition effect to introduce
 * the boss for the battle
 * @author nhydock
 *
 */
public class Transition extends UI {

    private Image hero;
    private Image foe;
    private Label foeLabel;
    private Label heroLabel;
    
    @Inject public IPlayerContainer playerService;
    
    public Transition(AssetManager manager) {
        super(manager);
    }

    @Override
    protected void load() {
        this.manager.load(DataDirs.Home + "dungeon.atlas", TextureAtlas.class);
        this.manager.load(DataDirs.Home + "uiskin.json", Skin.class);
    }

    @Override
    public void init() {
        this.skin = manager.get(DataDirs.Home + "uiskin.json", Skin.class);
        hero = new Image(skin, playerService.getGender());
        hero.setSize(128f, 128f);
        foe = new Image(skin, "goddess");
        foe.setSize(128f, 128f);
        
        heroLabel = new Label("Hero", skin, "prompt");
        heroLabel.setAlignment(Align.left);
        foeLabel = new Label("", skin, "prompt");
        foeLabel.setAlignment(Align.left);
        
        this.addActor(hero);
        this.addActor(foe);
        
        this.addActor(heroLabel);
        this.addActor(foeLabel);
    }

    public void setEnemy(Entity boss) {

        Renderable r = boss.getComponent(Renderable.class);
        Identifier id = boss.getComponent(Identifier.class);        
        TextureAtlas monsters = manager.get(DataDirs.Home + "dungeon.atlas", TextureAtlas.class);
        TextureRegion foeSprite = monsters.findRegion(r.getSpriteName());
        foe.setDrawable(new TextureRegionDrawable(foeSprite));
        
        foeLabel.setText(id.toString());
        foeLabel.setAlignment(Align.left);
        
        foeLabel.pack();
    }
    
    public void playAnimation(Runnable after) {
        getRoot().clearActions();
        hero.clearActions();
        foe.clearActions();
        foeLabel.clearActions();
        heroLabel.clearActions();
        
        hero.addAction(Actions.sequence(
                Actions.sizeTo(512, 512),
                Actions.moveTo(-512, getHeight()/2f - 256f),
                Actions.moveTo(getWidth()*.8f, getHeight()/2f - 256f, .5f, Interpolation.circleOut),
                Actions.moveTo(getWidth()*.9f, getHeight()/2f - 256f, .8f, Interpolation.linear),
                Actions.moveTo(getWidth()+512, getHeight()/2f - 256f, .5f, Interpolation.circleIn),
                Actions.delay(1f)
            ));
        foe.addAction(Actions.sequence(
                Actions.delay(1f),
                Actions.sizeTo(512, 512),
                Actions.moveTo(getWidth()+512, getHeight()/2f - 256f),
                Actions.moveTo(getWidth()*.2f, getHeight()/2f - 256f, .5f, Interpolation.circleOut),
                Actions.moveTo(getWidth()*.1f, getHeight()/2f - 256f, .8f, Interpolation.linear),
                Actions.moveTo(-512, getHeight()/2f - 256f, .5f, Interpolation.circleIn)
            ));
            
        foeLabel.addAction(Actions.sequence(
            Actions.moveTo(getWidth(), 64),
            Actions.delay(1f),
            Actions.moveTo(getWidth()*.9f - foeLabel.getPrefWidth(), 64, .4f, Interpolation.circleOut),
            Actions.delay(1f),
            Actions.moveTo( -foeLabel.getPrefWidth(), 64, .4f, Interpolation.circleIn)
        ));
            
        heroLabel.addAction(Actions.sequence(
            Actions.moveTo(-heroLabel.getPrefWidth(), getHeight() - 64),
            Actions.moveTo(getWidth()*.1f, getHeight() - 64, .4f, Interpolation.circleOut),
            Actions.delay(.1f),
            Actions.moveTo(getWidth(), getHeight() - 64, .4f, Interpolation.circleIn)
        ));
        
        addAction(Actions.sequence(Actions.delay(4f), Actions.run(after)));
    }
}
