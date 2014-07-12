package scenes.dungeon;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Mapper;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import components.Identifier;
import components.Monster;
import components.Position;
import components.Renderable;
import components.Stats;

public class RenderSystem extends EntityProcessingSystem {

	public static final float MoveSpeed = .25f;
	
	WanderUI parentScene;
	
	private SpriteBatch batch;
	private OrthographicCamera camera;
	private OrthogonalTiledMapRenderer mapRenderer;
	
	@Mapper ComponentMapper<Position> positionMap;
	@Mapper ComponentMapper<Renderable> renderMap;
	@Mapper ComponentMapper<Monster> monsterMap;
	@Mapper ComponentMapper<Identifier> idMap;
	@Mapper ComponentMapper<Stats> statMap;
	
	float scale;
	float height;
	private TiledMap map;
	private TextureRegion nullTile;
	
	private final Array<Actor> addQueue;
	private final Array<Actor> removeQueue;
	private Stage stage;
	
	Table stats;
	Label enemyName;
	Label enemyHP;
	private boolean statsVis;
	
	@SuppressWarnings("unchecked")
	public RenderSystem()
	{
		super(Aspect.getAspectForAll(Renderable.class, Position.class));
		addQueue = new Array<Actor>();
		removeQueue = new Array<Actor>();
	}
	
	@Override
	protected void inserted(final Entity e)
	{
		super.inserted(e);
		
		Renderable r = renderMap.get(e);
		final Image sprite = new Image(r.getSprite());
		sprite.setSize(scale, scale);
		if (monsterMap.has(e))
		{
			//Gdx.app.log("[Entity]", "Entity is monster, adding hover controls");
			sprite.addListener(new InputListener(){
				@Override
				public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
				{
					Stats s = statMap.get(e);
					Identifier id = idMap.get(e);
					if (id.hidden())
						return;
					
					Vector2 v = sprite.localToStageCoordinates(new Vector2(0, 0));
					Vector2 v2 = sprite.localToStageCoordinates(new Vector2(0, sprite.getHeight()+6));
					
					//Gdx.app.log("[Input]", id.toString() + " has been hovered over. " + v.x + "," + v.y);
					if (s.hidden)
					{
						showStats( v, v2, id.toString(), "HP: ??? / ???");		
					}
					else
					{
						showStats(
							v, v2, id.toString(), 
							String.format("HP: %3d / %3d", s.hp, s.maxhp)
						);
					}
				}
				@Override
				public void exit(InputEvent evt, float x, float y, int pointer, Actor toActor)
				{
					hideStats();
				}
			});
		}
		r.setActor(sprite);
		
		addQueue.add(sprite);
	}
	
	@Override
	protected void removed(final Entity e)
	{
		Renderable r = renderMap.get(e);
		removeQueue.add(r.getActor());
		
		super.removed(e);
	}
	
	@Override
	protected void process(Entity e) {
		Position p = positionMap.get(e);
		Renderable r = renderMap.get(e);
		
		//adjust position to be aligned with tiles
		if (r.getActor().getX() == 0 && r.getActor().getY() == 0)
		{
			r.getActor().addAction(Actions.moveTo(p.getX()*scale, p.getY()*scale));
		}
		
		if (p.changed())
		{
			r.getActor().addAction(Actions.moveTo(p.getX()*scale, p.getY()*scale, MoveSpeed));
			p.update();	
		}
	}
	
	public void setMap(TiledMap map)
	{
		this.map = map;
		this.height = ((TiledMapTileLayer)map.getLayers().get(0)).getHeight();
	}
	
	public void setView(WanderUI view, Skin skin)
	{
		parentScene = view;
		Viewport v = view.getViewport();
		stage = new Stage(new ScalingViewport(Scaling.fit, v.getWorldWidth(), v.getWorldHeight(), new OrthographicCamera()));
		stage.addListener(new InputListener(){
			@Override
			public boolean mouseMoved(InputEvent evt, float x, float y)
			{
				//Gdx.app.log("[Input]", "Mouse is moving");
				return true;
			}
		});
		this.batch = (SpriteBatch) this.stage.getBatch();
		this.camera = (OrthographicCamera) this.stage.getCamera();
		
		mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, batch);
		scale = 32f * mapRenderer.getUnitScale();
		
		//enemy stats
		{
			stats = new Table();
			enemyName = new Label("", skin, "promptsm");
			enemyName.setAlignment(Align.center);
			enemyHP = new Label("HP: 0/0", skin, "smaller");
			enemyHP.setAlignment(Align.center);
			
			float width = Math.max(enemyName.getPrefWidth(), enemyHP.getPrefWidth()) + 40;
			stats.setWidth(width);
			stats.add(enemyName).expandX().fillX().align(Align.center);
			stats.row();
			stats.add(enemyHP).expandX().fillX().align(Align.center);
			stats.addAction(Actions.alpha(0f));
			stats.setBackground(skin.getDrawable("button_up"));
			stats.setVisible(false);
			stage.addActor(stats);

		}
	}
	
	@Override
	protected void begin()
	{
		for (Actor a : addQueue)
		{
			stage.addActor(a);
		}
		
		for (Actor r : removeQueue)
		{
			//Gdx.app.log("[Entity]", "removing an actor");
			r.remove();
		}
		
		float x = camera.position.x;
		float y = camera.position.y;
		
		camera.position.set(camera.viewportWidth/2, camera.viewportHeight/2, 0);
		camera.update();
		//fill background
		if (nullTile != null)
		{
			nullTile.setRegionWidth((int)camera.viewportWidth);
			nullTile.setRegionHeight((int)camera.viewportHeight);
			batch.setProjectionMatrix(camera.combined);
			batch.begin();
			batch.draw(nullTile, 0, 0, camera.viewportWidth, camera.viewportHeight);
			batch.end();
		}
		Entity player = world.getManager(TagManager.class).getEntity("player");
		Renderable r = renderMap.get(player);
		
		if (r.getActor() == null) return;
		
		camera.position.set(x, y, 0);
		camera.update();
		mapRenderer.setView(camera);
		mapRenderer.render();
	}
	
	@Override
	protected void end()
	{
		stage.act(world.getDelta());
		stage.draw();
		
		stage.getBatch().begin();
		stats.draw(stage.getBatch(), stats.getColor().a);
		stage.getBatch().end();
		stage.getBatch().setColor(1f, 1f, 1f, 1f);
		
		Entity player = world.getManager(TagManager.class).getEntity("player");
		Renderable r = renderMap.get(player);
		if (r.getActor() == null) return;
		
		camera.position.x = r.getActor().getX();
		camera.position.y = r.getActor().getY();
	}
	
	public void dispose()
	{
		batch = null;
		camera = null;
		stage.clear();
		stage = null;
	}

	@Override
	protected boolean checkProcessing() {
		return true;
	}
	
	protected float getScale() {
		return scale;
	}

	public void setNull(Texture texture) {
		if (texture != null)
		{
			texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
			this.nullTile = new TextureRegion(texture);
		}
		else
		{
			this.nullTile = null;
		}
		
	}
	
	public Stage getStage()
	{
		return stage;
	}
	
	public void showStats(Vector2 v, Vector2 v2, String name, String hp)
	{
		if (statsVis && name.equals(enemyName.getText().toString())) {
			return;
		}
		
		statsVis = true;
		
		enemyName.setText(name);
		enemyHP.setText(hp);
		
		stats.pack();
		float width = Math.max(enemyName.getPrefWidth(), enemyHP.getPrefWidth()) + 40;
		stats.setWidth(width);
		
		stats.addAction(Actions.sequence(
			Actions.alpha(0f),
			Actions.moveTo(v.x - stats.getPrefWidth()/2f, v.y),
			Actions.parallel(
				Actions.alpha(1f, .2f),
				Actions.moveTo(v2.x - stats.getPrefWidth()/2f, v2.y, .2f)
				)
			)
		);
	}
	
	public void hideStats()
	{
		statsVis = false;
		stats.clearActions();
		stats.addAction(Actions.alpha(0f, .3f));
	}
}
