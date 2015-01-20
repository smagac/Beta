package core.common;

import scene2d.InputDisabler;
import scenes.Messages;
import github.nhydock.ssm.SceneManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;

import core.Palette;
import core.service.interfaces.IColorMode;
import core.service.interfaces.IGame;

public class BossListener {

    private static BossListener instance;

    public static BossListener getInstance() {
        return instance;
    }

    private IColorMode color;
    private IGame game;

    protected BossListener(IColorMode colorService, IGame gameService) {
        this.color = colorService;
        this.game = gameService;
        instance = this;
    }

    public IColorMode getColorService() {
        return color;
    }

    public IGame getGameService() {
        return game;
    }

    public boolean run() {

        // control hues
        Palette nextCol = null;

        if (Gdx.input.isKeyJustPressed(Keys.NUM_1)) {
            nextCol = Palette.Original;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_2)) {
            nextCol = Palette.Gameboy;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_3)) {
            nextCol = Palette.VirtualBoy;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_4)) {
            nextCol = Palette.Orange;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_5)) {
            nextCol = Palette.Tandy;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_6)) {
            nextCol = Palette.Sepia;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_7)) {
            nextCol = Palette.Vintage;
        }
        if (Gdx.input.isKeyJustPressed(Keys.NUM_8)) {
            nextCol = Palette.Pen;
        }

        if (Gdx.input.isKeyJustPressed(Keys.MINUS)) {
            getColorService().darken();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Keys.EQUALS) ||
            Gdx.input.isKeyJustPressed(Keys.PLUS)) {
            getColorService().brighten();
            return true;
        }

        if (nextCol != null) {
            getColorService().setPalette(nextCol);
            return true;
        }

        if (Gdx.input.isKeyJustPressed(Keys.F5)) {
            getGameService().softReset();
            InputDisabler.clear();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Keys.F6)) {
            getGameService().fastStart();
            InputDisabler.clear();
            return true;
        }
        if (getGameService().hasStarted() && getGameService().debug()) {
            if (Gdx.input.isKeyJustPressed(Keys.F7)) {
                SceneManager.switchToScene("lore");
                InputDisabler.clear();
                return true;
            }
            if (Gdx.input.isKeyJustPressed(Keys.F2)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.LevelUp);
                InputDisabler.clear();
                return true;
            }
            if (Gdx.input.isKeyJustPressed(Keys.END)) {
                getGameService().endGame();
                InputDisabler.clear();
                return true;
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.F9)) {
            // EXIT LIKE A BITCH
            Gdx.app.exit();
            return true;
        }

        return false;
    }

}
