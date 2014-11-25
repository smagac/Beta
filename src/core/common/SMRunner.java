package core.common;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class SMRunner {

    public static void main(String[] args) {
        Storymode app = new Storymode();
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.backgroundFPS = -1;
        config.foregroundFPS = 60;

        config.width = Storymode.InternalRes[0];
        config.height = Storymode.InternalRes[1];

        // config.resizable = false;
        config.title = "StoryMode";
        config.fullscreen = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-fullscreen")) {
                com.badlogic.gdx.Graphics.DisplayMode dm = LwjglApplicationConfiguration.getDesktopDisplayMode();
                config.width = dm.width;
                config.height = dm.height;
                config.fullscreen = true;
            }
            else if (arg.equals("-resolution") && !config.fullscreen) {
                String val = args[++i];
                String[] param = val.split("x");
                config.width = Integer.parseInt(param[0]);
                config.height = Integer.parseInt(param[1]);
            }
            else if (arg.equals("-bgmvol")) {
                String val = args[++i];
                float vol = Float.parseFloat(val);
                app.setBgmVol(vol);
            }
            else if (arg.equals("-sfxvol")) {
                String val = args[++i];
                float vol = Float.parseFloat(val);
                app.setSfxVol(vol);
            }
        }

        new LwjglApplication(app, config);
    }

}
