package core.service.implementations;

import github.nhydock.ssm.Inject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.DataDirs;
import core.service.interfaces.IColorMode;
import core.service.interfaces.ILoader;
import core.service.interfaces.ISharedResources;
import static core.common.Storymode.InternalRes;

public class LoadScreen implements ILoader {

    Stage stage;
    TextureRegion fill;
    Label message;
    
    @Inject public ISharedResources shared;
    @Inject public IColorMode colorMode;
    private boolean loading;

    // loading screen data
    private Viewport standardViewport;

    @Override
    public void onRegister() {
        Skin skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);
        fill = skin.getRegion("fill");
        
        message = new Label("Hello", skin, "loading");
        
        standardViewport = new ScalingViewport(Scaling.fit, InternalRes[0], InternalRes[1]);
        stage = new Stage(standardViewport);
        
        message.setPosition(-message.getPrefWidth(), 35f);
        stage.addActor(message);
    }

    @Override
    public void onUnregister() {
        stage.dispose();
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoadingMessage(String msg) {
        if (msg == null || msg.trim().length() == 0) {
            msg = "Loading...";
        }
        message.setText(msg);
        message.validate();
        message.setPosition(10f, 10f);
        //message.addAction(Actions.sequence(Actions.moveTo(-message.getPrefWidth(), 10f), Actions.moveTo(10f, 10f, .2f, Interpolation.sine)));
    }
    
    public void resize(int width, int height) {
        standardViewport.update(width, height, true);
    }
    
    public void draw(float delta) {
        Color clear = colorMode.getClear();
        ShaderProgram hueify = colorMode.getShader();
        
        Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // draw load screen
        stage.getBatch().setShader(hueify);
        stage.act(delta);
        stage.draw();
    }
}
