


import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import core.common.Storymode;

public class SMRunner {

	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.backgroundFPS = -1;
		config.foregroundFPS = 60;
		config.width = Storymode.InternalRes[0];
		config.height = Storymode.InternalRes[1];
		//config.resizable = false;
		config.title = "StoryMode";

		new LwjglApplication(new Storymode(), config);
	}
	
}
