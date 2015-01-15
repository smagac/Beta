package core.service.implementations;

import github.nhydock.ssm.Inject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.GdxRuntimeException;

import core.service.interfaces.IAudioManager;
import core.service.interfaces.ISharedResources;

public class AudioManager implements IAudioManager {

    @Inject public ISharedResources shared;
    
    // currently playing bgm
    private Music bgm;
    
    Stage controller;
    Actor bgmController;
    Actor sfxController;
    
    private float bgmVolScale = 1f;
    private float sfxVolScale = 1f;
    
    public AudioManager() {
        controller = new Stage();
        bgmController = new Actor();
        bgmController.setColor(new Color(0,0,0,1));
        sfxController = new Actor();
        sfxController.setColor(new Color(0,0,0,1));
        controller.addActor(bgmController);
        controller.addActor(sfxController);
    }
    
    private float getBgmVol() {
        return bgmController.getColor().a * bgmVolScale;
    }

    private float getSFXVol() {
        return sfxController.getColor().a * sfxVolScale;
    }

    public void update(float delta) {
        controller.act(delta);
        
        if (bgm != null) {
            bgm.setVolume(getBgmVol());
        }
    }
    
    @Override
    public void onRegister() { }

    @Override
    public void onUnregister() { }

    @Override
    public boolean isPlayingBgm() {
        return bgm != null && bgm.isPlaying();
    }

    @Override
    public void setBgm(Music bgm) {
        setBgm(bgm, true);
    }

    @Override
    public void setBgm(Music bgm, boolean loop) {
        if (this.bgm != null) {
            this.bgm.stop();
            this.bgm = null;
        }
        bgmController.addAction(Actions.alpha(1f));
        this.bgm = bgm;
        this.bgm.setLooping(loop);
    }

    @Override
    public void playBgm() {
        if (this.bgm != null) {
            bgm.play();
        }
    }

    @Override
    public void playBgm(Music bgm) {
        playBgm(bgm, true);
    }

    @Override
    public void playBgm(Music bgm, boolean loop) {
        setBgm(bgm, loop);
        playBgm();
    }

    @Override
    public void pauseBgm() {
        if (this.bgm != null) {
            this.bgm.pause();
        }
    }

    @Override
    public void stopBgm() {
        if (this.bgm != null) {
            this.bgm.stop();
        }
    }

    @Override
    public void setBgmVol(float vol) {
        bgmVolScale = vol;
    }

    @Override
    public void setSfxVol(float vol) {
        sfxVolScale = vol;
    }

    @Override
    public void playSfx(Sound clip) {
        clip.play(getSFXVol());
    }

    @Override
    public boolean hasBgm() {
        return this.bgm != null;
    }

    @Override
    public void clearBgm() {
        stopBgm();
        if (this.bgm != null) {
            this.bgm.dispose();
            this.bgm = null;
        }
    }

    @Override
    public void playSfx(Sound sound, int vol, float pitch, int pan) {
        sound.play(vol * getSFXVol(), pitch, pan);
    }

    @Override
    public void playSfx(Sound sound, float vol) {
        sound.play(vol * getSFXVol());
    }

    @Override
    public void playSfx(String sound) {
        Sound fx;
        try {
            fx = shared.getResource(sound, Sound.class);
        } catch (GdxRuntimeException e) {
            fx = Gdx.audio.newSound(Gdx.files.internal(sound));
        }
        playSfx(fx);
    }

    @Override
    public void playSfx(FileHandle sound) {
        Sound fx;
        fx = Gdx.audio.newSound(sound);
        playSfx(fx);
    }

    @Override
    public void fadeOut() {
        bgmController.addAction(Actions.alpha(0f, .5f));
    }

    @Override
    public void fadeIn() {
        bgmController.setColor(0,0,0,0);
        bgmController.addAction(Actions.alpha(1f, .5f));
    }
}
