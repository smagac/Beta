package core.service.implementations;

import github.nhydock.ssm.Inject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.DataDirs;
import core.Palette;
import core.service.interfaces.IColorMode;
import core.service.interfaces.ILoader;
import core.service.interfaces.ISharedResources;
import static core.common.Storymode.InternalRes;

public class LoadScreen implements ILoader {

    SpriteBatch batch;
    BitmapFont font;
    TextureRegion fill;
    
    @Inject public ISharedResources shared;
    @Inject public IColorMode colorMode;
    private boolean loading;
    private String loadingMessage;

    // loading screen data
    private Viewport standardViewport;

    @Override
    public void onRegister() {
        Skin skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);
        font = skin.getFont("loading");
        fill = skin.getRegion("fill");
        batch = new SpriteBatch();

        standardViewport = new ScalingViewport(Scaling.fit, InternalRes[0], InternalRes[1]);
    }

    @Override
    public void onUnregister() {
        batch.dispose();
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
    public void setLoadingMessage(String message) {
        if (message == null || message.trim().length() == 0)
            message = "Loading...";
        this.loadingMessage = message;
    }
    
    public void resize(int width, int height) {
        standardViewport.update(width, height, true);
    }
    
    public void draw() {
        Color clear = colorMode.getClear();
        ShaderProgram hueify = colorMode.getShader();
        
        Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // draw load screen
        batch.setShader(hueify);
        batch.setProjectionMatrix(standardViewport.getCamera().combined);
        batch.begin();
        batch.setColor(clear);
        batch.draw(fill, 0, 0, InternalRes[0], InternalRes[1]);
        batch.setColor(Color.WHITE);
        font.draw(batch, loadingMessage, InternalRes[0] / 2 - font.getBounds(loadingMessage).width / 2, 35f);
        batch.end();
    }
}
