package scene2d;

import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.audio.Music;

import core.service.interfaces.IAudioManager;
import core.service.interfaces.ISharedResources;

/**
 * Simple runnable that'll play a sound using the system registered audio manager
 * @author nhydock
 *
 */
public class PlayBGM implements Runnable {

    private Music bgm;
    
    /**
     * Creates a new runnable that plays the input sound effect object
     * @param sfx
     */
    public PlayBGM(Music m){
        bgm = m;
    }
    
    /**
     * Create a new runnable where the sfx is referenced with a path name.
     * Files specified this way are loaded using the system's Shared Resource manager
     * @param path
     */
    public PlayBGM(String file) {
        bgm = ServiceManager.getService(ISharedResources.class).getResource(file, Music.class);
    }
    
    @Override
    public void run() {
        IAudioManager audio = ServiceManager.getService(IAudioManager.class);
        audio.playBgm(bgm);
        audio = null;
    }
}
