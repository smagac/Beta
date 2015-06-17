package core.util.dungeon;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.ImageResolver;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

public class TsxTileSet extends TiledMapTileSet {

    public static final int WALL = 0;
    public static final int FLOOR = 1;
    public static final int NULL = 2;
    public static final int DOWN = 5;
    public static final int UP = 3;
    public static final int[] ALL = {WALL, FLOOR, NULL, DOWN, UP};
    
    private XmlReader xml = new XmlReader();
    
    public TsxTileSet(FileHandle file, AssetManager am) {
        super();
        try {
            Element tsx = xml.parse(file);
            loadTileSet(tsx, new ImageResolver.AssetManagerImageResolver(am));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Loads in a tiles property list
     * @param properties - a tile's property list
     * @param element - xml node in the tsx containing defined properties for the tile
     */
    protected void loadProperties (MapProperties properties, Element element) {
        for (Element property : element.getChildrenByName("property")) {
            String name = property.getAttribute("name", null);
            Object value = null;
            if (name.equals("block")) {
                value = property.getBooleanAttribute("value", false);
            } 
            else if (name.equals("density"))
            {
                value = property.getFloatAttribute("value", 0.0f);
            }
            else
            {
                value = property.getAttribute("value", null);
            }
            properties.put(name, value);
        }
    }
    
    /** 
     * Loads the specified tileset data given the XML element, the tsxFile and
     * an {@link ImageResolver} used to retrieve the tileset Textures.  This is a slightly modified
     * version of what's packaged with LibGDX's tmxmaploader, made abstracted so it will only load
     * the tileset.  Tiles are also only inserted into the tile set if the tiles have defined properties.
     * Animated tiles are not supported
     * 
     * <p>
     * Default tileset's property keys that are loaded by default are:
     * </p>
     * 
     * <ul>
     * <li><em>imagesource</em>, (String, defaults to empty string) the tileset source image filename</li>
     * <li><em>imagewidth</em>, (int, defaults to 0) the tileset source image width</li>
     * <li><em>imageheight</em>, (int, defaults to 0) the tileset source image height</li>
     * <li><em>tilewidth</em>, (int, defaults to 0) the tile width</li>
     * <li><em>tileheight</em>, (int, defaults to 0) the tile height</li>
     * <li><em>margin</em>, (int, defaults to 0) the tileset margin</li>
     * <li><em>spacing</em>, (int, defaults to 0) the tileset spacing</li>
     * </ul>
     * 
     * <p>
     * The values are extracted from the specified tsx file, if a value can't be found then the default is used.
     * </p>
     * @param map the Map whose tilesets collection will be populated
     * @param element the XML element identifying the tileset to load
     * @param tmxFile the Filehandle of the tmx file
     * @param imageResolver the {@link ImageResolver} */
    protected void loadTileSet (Element element, ImageResolver imageResolver) {
        String name = element.get("name", null);
        int tilewidth = element.getIntAttribute("tilewidth", 0);
        int tileheight = element.getIntAttribute("tileheight", 0);
        int spacing = element.getIntAttribute("spacing", 0);
        int margin = element.getIntAttribute("margin", 0);

        int offsetX = 0;
        int offsetY = 0;

        String imageSource = "";
        int imageWidth = 0, imageHeight = 0;

        FileHandle image = null;
        Element offset = element.getChildByName("tileoffset");
        if (offset != null) {
            offsetX = offset.getIntAttribute("x", 0);
            offsetY = offset.getIntAttribute("y", 0);
        }
        imageSource = element.getChildByName("image").getAttribute("source");
        imageWidth = element.getChildByName("image").getIntAttribute("width", 0);
        imageHeight = element.getChildByName("image").getIntAttribute("height", 0);
        image = Gdx.files.internal(imageSource);
    
        TextureRegion texture = imageResolver.getImage(image.path());

        MapProperties props = this.getProperties();
        this.setName(name);
        props.put("imagesource", imageSource);
        props.put("imagewidth", imageWidth);
        props.put("imageheight", imageHeight);
        props.put("tilewidth", tilewidth);
        props.put("tileheight", tileheight);
        props.put("margin", margin);
        props.put("spacing", spacing);
        
        int cols = imageWidth / tilewidth;

        Array<Element> tileElements = element.getChildrenByName("tile");
        for (Element tile : tileElements)
        {
            int id = tile.getIntAttribute("id");
            int y = margin + (id / cols) * (tileheight + spacing);
            int x = margin + (id % cols) * (tilewidth + spacing);
            TextureRegion tileRegion = new TextureRegion(texture, x, y, tilewidth, tileheight);
            TiledMapTile t = new StaticTiledMapTile(tileRegion);
            t.setId(id);
            t.setOffsetX(offsetX);
            t.setOffsetY(-offsetY);
            
            MapProperties tileProps = t.getProperties();
            loadProperties(tileProps, tile.getChildByName("properties"));
            
            putTile(id, t);
        }

        Element properties = element.getChildByName("properties");
        if (properties != null) {
            loadProperties(props, properties);
        }
    }
}
