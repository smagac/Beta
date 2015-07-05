package com.nhydock.gdx.scenes.scene2d.runnables;

import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.audio.Sound;
import com.nhydock.storymode.service.interfaces.IAudioManager;
import com.nhydock.storymode.service.interfaces.ISharedResources;

/**
 * Simple runnable that'll play a sound using the system registered audio manager
 * @author nhydock
 *
 */
public class PlaySound implements Runnable {

    private Sound sfx;
    
    /**
     * Creates a new runnable that plays the input sound effect object
     * @param sfx
     */
    public PlaySound(Sound fx){
        sfx = fx;
    }
    
    /**
     * Create a new runnable where the sfx is referenced with a path name.
     * Files specified this way are loaded using the system's Shared Resource manager
     * @param path
     */
    public PlaySound(String file) {
        sfx = ServiceManager.getService(ISharedResources.class).getResource(file, Sound.class);
    }
    
    @Override
    public void run() {
        IAudioManager audio = ServiceManager.getService(IAudioManager.class);
        audio.playSfx(sfx);
        audio = null;
    }
}
