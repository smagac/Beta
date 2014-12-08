package core.service.implementations;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

import core.service.interfaces.IAudioManager;

public class AudioManager implements IAudioManager {

    // currently playing bgm
    private Music bgm;
    
    Stage controller;
    Actor bgmController;
    Actor sfxController;
    
    public AudioManager() {
        controller = new Stage();
        bgmController = new Actor();
        bgmController.setColor(new Color(0,0,0,1));
        sfxController = new Actor();
        sfxController.setColor(new Color(0,0,0,1));
    }
    
    public void update(float delta) {
        controller.act(delta);
        
        if (bgm != null) {
            bgm.setVolume(bgmController.getColor().a);
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
        this.bgm = bgm;
        this.bgm.setLooping(loop);
    }

    @Override
    public void playBgm() {
        bgm.play();
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
        this.bgmController.getColor().a = vol;
    }

    @Override
    public void setSfxVol(float vol) {
        sfxController.getColor().a = vol;
    }

    @Override
    public void playSfx(Sound clip) {
        if (clip != null) {
            clip.play(getSFXVol());
        }
    }

    @Override
    public boolean hasBgm() {
        return this.bgm != null;
    }

    @Override
    public void clearBgm() {
        
    }

    private float getSFXVol() {
        return sfxController.getColor().a;
    }
}
