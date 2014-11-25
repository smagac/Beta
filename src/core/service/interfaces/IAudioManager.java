package core.service.interfaces;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import github.nhydock.ssm.Service;

/**
 * Service for controlling system wide audio effects
 * 
 * @author nhydock
 *
 */
public interface IAudioManager extends Service {
    /**
     * @return true if bgm is currently loaded and playing
     */
    public boolean isPlayingBgm();

    /**
     * Sets a new looping bgm for the manager to control, but does not initially
     * start playing it
     * 
     * @param bgm
     */
    public void setBgm(Music bgm);

    /**
     * Sets a new bgm for the manager to control, but does not initially start
     * playing it
     * 
     * @param bgm
     * @param loop
     *            - whether the song should loop or not
     */
    public void setBgm(Music bgm, boolean loop);

    /**
     * Plays the currently set bgm of the manager
     */
    public void playBgm();

    /**
     * Plays a new bgm that loops by default
     * 
     * @param bgm
     *            - bgm to set to be controlled by the manager
     */
    public void playBgm(Music bgm);

    /**
     * Plays a new bgm
     * 
     * @param bgm
     * @param loop
     *            - whether the song loops or not
     */
    public void playBgm(Music bgm, boolean loop);

    /**
     * Pauses the currently playing bgm
     */
    public void pauseBgm();

    /**
     * Stops the currently possessed bgm
     */
    public void stopBgm();

    /**
     * Sets the global volume of all bgms that are played by this manager
     * 
     * @param vol
     */
    public void setBgmVol(float vol);

    /**
     * Sets the global volume of all sfx played by this manager
     * 
     * @param vol
     */
    public void setSfxVol(float vol);

    /**
     * Plays a sfx
     * 
     * @param clip
     */
    public void playSfx(Sound clip);

    /**
     * @return true if a bgm has been set/loaded
     */
    public boolean hasBgm();

    /**
     * Sets there to be no currently managed/playing bgm
     */
    public void clearBgm();
}
