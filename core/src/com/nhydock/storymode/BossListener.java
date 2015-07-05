package com.nhydock.storymode;

import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.nhydock.storymode.datatypes.Ailment;
import com.nhydock.storymode.datatypes.Health;
import com.nhydock.storymode.scenes.Messages;
import com.nhydock.storymode.service.interfaces.IColorMode;
import com.nhydock.storymode.service.interfaces.IGame;
import com.nhydock.storymode.service.interfaces.IPlayerContainer;

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
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Keys.F6)) {
            getGameService().fastStart();
            return true;
        }
        if (getGameService().hasStarted() && getGameService().debug()) {
            if (Gdx.input.isKeyJustPressed(Keys.F7)) {
                SceneManager.switchToScene("lore");
                return true;
            }
            if (Gdx.input.isKeyJustPressed(Keys.F2)) {
                MessageManager.getInstance().dispatchMessage(null, Messages.Player.LevelUp);
                return true;
            }
            if (Gdx.input.isKeyPressed(Keys.ALT_LEFT) && Gdx.input.isKeyJustPressed(Keys.END)) {
                getGameService().endGame();
                return true;
            }
            
            //status debugging
            if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) {
                {
                    Health h = ServiceManager.getService(IPlayerContainer.class).getAilments();
                    Ailment ailment = null;
                    if (Gdx.input.isKeyJustPressed(Keys.INSERT)) {
                        ailment = Ailment.POISON;
                    }
                    if (Gdx.input.isKeyJustPressed(Keys.DEL)) {
                        ailment = Ailment.TOXIC;
                    }
                    if (Gdx.input.isKeyJustPressed(Keys.HOME)) {
                        ailment = Ailment.SPRAIN;
                    }
                    if (Gdx.input.isKeyJustPressed(Keys.END)) {
                        ailment = Ailment.ARTHRITIS;
                    }
                    if (Gdx.input.isKeyJustPressed(Keys.PAGE_UP)) {
                        ailment = Ailment.BLIND;
                    }
                    if (Gdx.input.isKeyJustPressed(Keys.PAGE_DOWN)) {
                        ailment = Ailment.CONFUSE;
                    }
                    if (Gdx.input.isKeyJustPressed(Keys.NUM)) {
                        h.reset();
                        return true;
                    }
                    
                    if (ailment != null){
                        h.addAilment(ailment);
                        return true;
                    }
                }
                
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
