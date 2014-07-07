package scenes.dungeon;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;

import components.Position;
import components.Renderable;
import core.common.Storymode;

public class RenderSystem extends EntityProcessingSystem {

	private SpriteBatch batch;
	private OrthographicCamera camera;
	private OrthogonalTiledMapRenderer mapRenderer;
	
	@Mapper ComponentMapper<Position> positionMap;
	@Mapper ComponentMapper<Renderable> renderMap;
	
	float scale;
	float height;
	private TiledMap map;
	private TextureRegion nullTile;
	
	@SuppressWarnings("unchecked")
	public RenderSystem()
	{
		super(Aspect.getAspectForAll(Renderable.class, Position.class));
	}
	
	@Override
	protected void process(Entity e) {
		Position p = positionMap.get(e);
		Renderable r = renderMap.get(e);
		
		//adjust position to be aligned with tiles
		batch.draw(r.getSprite(), p.getX()*scale, p.getY()*scale, scale, scale);
	}
	
	/**
	 * Get if touching an enemy with mouse cursor at x, y
	 * @param x
	 * @param y
	 */
	protected Vector2 unproject(float x, float y, float f, float g)
	{
		x -= f/2f;
		y -= g/2f;
		x += camera.position.x;
		y += camera.position.y;
		x -= x % scale;
		y -= y % scale;
		x /= scale;
		y /= scale;
		return new Vector2((int)(x), (int)(y+1));
	}
	
	protected Vector2 project(float x, float y, float f, float g)
	{
		y -= 1;
		x *= scale;
		y *= scale;
		x -= camera.position.x;
		y -= camera.position.y;
		x += f/2f;
		y += g/2f;
		return new Vector2((int)(x), (int)(y));
	}
	
	public void setMap(TiledMap map)
	{
		this.map = map;
		this.height = ((TiledMapTileLayer)map.getLayers().get(0)).getHeight();
		mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, batch);
		scale = 32f * mapRenderer.getUnitScale();
	}
	
	public void setView(Batch batch, Camera camera)
	{
		this.batch = (SpriteBatch) batch;
		if (this.camera == null)
		{
			this.camera = new OrthographicCamera();
		}
		mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, batch);
	}
	
	@Override
	protected void begin()
	{
		camera.setToOrtho(false, Storymode.InternalRes[0], Storymode.InternalRes[1]);
		Entity player = world.getManager(TagManager.class).getEntity("player");
		Position pos = positionMap.get(player);
		camera.position.x = pos.getX()*scale;
		camera.position.y = pos.getY()*scale;
		camera.update();
		
		//fill background
		if (nullTile != null)
		{
			nullTile.setRegionWidth((int)camera.viewportWidth);
			nullTile.setRegionHeight((int)camera.viewportHeight);
			batch.begin();
			batch.draw(nullTile, 0, 0, camera.viewportWidth, camera.viewportHeight);
			batch.end();
		}
		mapRenderer.setView(camera);
		mapRenderer.render();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
	}
	
	@Override
	protected void end()
	{
		batch.end();
	}
	
	public void dispose()
	{
		batch = null;
		camera = null;
	}

	@Override
	protected boolean checkProcessing() {
		return true;
	}
	
	protected float getScale() {
		return scale;
	}

	public void setNull(Texture texture) {
		texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
		this.nullTile = new TextureRegion(texture);
		
	}
}
