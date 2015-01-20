package scene2d;

import github.nhydock.ssm.SceneManager;

public class GotoScene implements Runnable {

    String sceneName;
    
    public GotoScene(String name) {
        sceneName = name;
    }
    
    @Override
    public void run() {
        SceneManager.switchToScene(sceneName);
    }

}
