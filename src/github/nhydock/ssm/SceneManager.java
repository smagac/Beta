package github.nhydock.ssm;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Very simple manager for storing all saved scene names. Much like a route
 * manager for web development, except it's for screens and games.
 * 
 * @author nhydock
 */
public class SceneManager {

    private static Game game;
    private static ObjectMap<String, Class<? extends Screen>> map;
    
    static {
        map = new ObjectMap<String, Class<? extends Screen>>();
    }

    /**
     * Signs a new screen into the manager
     * 
     * @param name
     * @param cls
     */
    public static void register(String name, Class<? extends Screen> cls) {
        map.put(name, cls);
    }

    /**
     * Generate a new screen from a registered name
     * 
     * @param name
     * @return
     * @throws NullPointerException
     *             if there is no scene registered with the specified name
     */
    public static Screen create(String name) throws NullPointerException {
        if (map.containsKey(name)) {
            Class<? extends Screen> c = map.get(name);
            Screen s;
            try {
                s = c.newInstance();
                return s;
            }
            catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        throw (new NullPointerException(name + " is not a registered Scene"));
    }

    public static Screen switchToScene(String name) {
        try {
            Screen s = create(name);
            switchToScene(s);
            return s;
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    /**
     * Switch to an already created scene
     * 
     * @param scene
     */
    public static void switchToScene(Screen scene) {
        game.setScreen(scene);
    }

    public static void setGame(Game g) {
        game = g;
    }
}
