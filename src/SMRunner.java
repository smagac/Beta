
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import core.common.Storymode;

public class SMRunner {

	public static void main (String[] args) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.backgroundFPS = -1;
		config.foregroundFPS = 60;
		config.width = Storymode.InternalRes[0];
		config.height = Storymode.InternalRes[1];
		//config.resizable = false;
		config.title = "StoryMode";
		
		for (String arg : args)
		{
			if (arg.equals("-fullscreen"))
			{
				com.badlogic.gdx.Graphics.DisplayMode dm = LwjglApplicationConfiguration.getDesktopDisplayMode();
				config.width = dm.width;
				config.height = dm.height;
				config.fullscreen = true;
			}
		}

		new LwjglApplication(new Storymode(), config);
	}
	
}
