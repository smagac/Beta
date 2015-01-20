package scene2d.runnables;

import github.nhydock.ssm.SceneManager;
import scenes.Scene;

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
