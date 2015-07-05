package com.nhydock.gdx.scenes.scene2d.runnables;

import com.nhydock.storymode.scenes.Scene;

import github.nhydock.ssm.SceneManager;

public class GotoScene implements Runnable {

    String sceneName;
    Scene<?> scene;
    
    public GotoScene(String name) {
        sceneName = name;
    }
    
    public GotoScene(Scene<?> scene) {
        this.scene = scene;
    }
    
    @Override
    public void run() {
        if (scene == null) {
            SceneManager.switchToScene(sceneName);
        } else {
            SceneManager.switchToScene(scene);
        }
    }

}
