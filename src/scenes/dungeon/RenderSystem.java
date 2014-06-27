package scenes.dungeon;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

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
	private TiledMap map;
	
	@SuppressWarnings("unchecked")
	public RenderSystem()
	{
		super(Aspect.getAspectForAll(Renderable.class, Position.class));
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
	}
	
	@Override
	protected void process(Entity e) {
		Position p = positionMap.get(e);
		Renderable r = renderMap.get(e);
		
		//adjust position to be aligned with tiles
		batch.draw(r.getSprite(), p.getX()*scale, p.getY()*scale, scale, scale);
	}
	
	public void setMap(TiledMap map)
	{
		this.map = map;
		mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, batch);
		scale = 32 * mapRenderer.getUnitScale();
	}
	
	public void setView(Batch batch, Camera camera)
	{
		this.batch = (SpriteBatch) batch;
		//this.camera = (OrthographicCamera) camera;
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
		
		//give color to non-walkable spaces outside the bounds of the map
				
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

	@Override
	protected boolean checkProcessing() {
		return true;
	}
}
