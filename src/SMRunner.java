


import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import core.common.Storymode;

public class SMRunner {

	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.backgroundFPS = -1;
		config.foregroundFPS = 60;
		config.width = (int)Storymode.InternalRes[0];
		config.height = (int)Storymode.InternalRes[1];
		new LwjglApplication(new Storymode(), config);
	}
	
}
