package core.datatypes.dungeon;

import scene2d.ui.extras.ParticleActor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Array;

import core.DataDirs;

/**
 * Abstract definition for weather systems.  Weather provides a visual effect, typically
 * distorting the user's vision, while just looking cool.  Weather systems are defined
 * within tilesets
 * @author nhydock
 *
 */
public abstract class Weather {

    public static Weather load(String... args){
        if (args[1].endsWith(".particle")) {
            return new ParticleWeather(args);
        } else if (args[1].endsWith(".png")) {
            return new ScrollingWeather(args);
        }
        return null;        
    }
    
    protected Group weatherLayer;
    protected float chance;
    
    protected Weather(float chance) {
        weatherLayer = new Group();
        weatherLayer.setTouchable(Touchable.disabled);
        this.chance = chance;
    }
    
    abstract public void init();
    
    public float getChance(){
        return chance;
    }
    
    /**
     * Get the actor representation of the weather system to add to a stage
     * @return
     */
    public Actor getActor(){
        return weatherLayer;
    }
    
    public abstract void update(float delta);
    
    /**
     * Simple weather system that uses a repeating texture that scrolls across the screen
     * @author nhydock
     *
     */
    public static class ScrollingWeather extends Weather {
        
        FileHandle textureFile;
        TextureRegion texture;
        float scrollX, scrollY;
        
        public ScrollingWeather(String... args) {
            this(Float.parseFloat(args[0]), Gdx.files.internal(DataDirs.Weather + args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]));
        }
        
        public ScrollingWeather(float chance, FileHandle file, float scrollX, float scrollY) {
            super(chance);
            this.chance = chance;
            this.scrollX = scrollX;
            this.scrollY = scrollY;
            this.textureFile = file;
        }
        
        public void init(){
            Texture weather = new Texture(textureFile);
            texture = new TextureRegion(weather);
            weather.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
            Image image = new Image(new TiledDrawable(texture));
            image.setFillParent(true);
            weatherLayer.addActor(image);
        }

        @Override
        public void update(float delta) {
            texture.scroll(scrollX*delta, scrollY*delta);
        }
    }
    
    /**
     * Simple weather system that uses a repeating texture that scrolls across the screen
     * @author nhydock
     *
     */
    public static class ParticleWeather extends Weather {
        
        float[][] actorLocations;
        FileHandle particleFile;
        
        public ParticleWeather(String... args) {
            super(Float.parseFloat(args[0]));
            particleFile = Gdx.files.internal(DataDirs.Weather + args[1]);
            
            Array<float[]> locations = new Array<float[]>(float[].class);
            for (int i = 2; i < args.length - 1; i += 2) {
                locations.add(new float[]{Float.parseFloat(args[i]), Float.parseFloat(args[i+1])});
            }
            
            actorLocations = locations.toArray();
            init();
        }
        
        public ParticleWeather(float chance, FileHandle file, float[]... locations) {
            super(chance);
            
            actorLocations = locations;
            
            init();
        }
        
        @Override
        public void init(){
            for (float[] pos : actorLocations) {
                ParticleEffect effect = new ParticleEffect();
                effect.load(particleFile, Gdx.files.internal(DataDirs.Home));
                
                ParticleActor actor = new ParticleActor(effect);
                actor.setPosition(pos[0] * weatherLayer.getWidth(), pos[1] * weatherLayer.getHeight());
                actor.start();
                
                weatherLayer.addActor(actor);
            }
        }

        @Override
        public void update(float delta) {
            //do nothing
        }
    }
}
