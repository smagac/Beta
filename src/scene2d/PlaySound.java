package scene2d;

import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

import core.service.interfaces.IAudioManager;
import core.service.interfaces.ISharedResources;

/**
 * Simple runnable that'll play a sound using the system registered audio manager
 * @author nhydock
 *
 */
public class PlaySound implements Runnable {

    private static Pool<PlaySound> soundPool = Pools.get(PlaySound.class);
    
    /**
     * Creates a new runnable that plays the input sound effect object
     * @param sfx
     */
    public static PlaySound create(Sound sfx) {
        PlaySound play = soundPool.obtain();
        play.setSfx(sfx);
        return play;
    }
    
    /**
     * Create a new runnable where the sfx is referenced with a path name.
     * Files specified this way are loaded using the system's Shared Resource manager
     * @param path
     */
    public static PlaySound create(String path) {
        Sound sfx = ServiceManager.getService(ISharedResources.class).getResource(path, Sound.class);
        PlaySound play = soundPool.obtain();
        play.setSfx(sfx);
        return play;
    }
    

    private Sound sfx;
    
    private PlaySound(){
        
    }
    
    private void setSfx(Sound sfx) {
        this.sfx = sfx;
    }
    
    @Override
    public void run() {
        IAudioManager audio = ServiceManager.getService(IAudioManager.class);
        audio.playSfx(sfx);
        audio = null;
        sfx = null;
        soundPool.free(this);
    }
}
