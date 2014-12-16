package scenes.dungeon;

import scenes.dungeon.ui.WanderUI;
import squidpony.squidgrid.fov.FOVSolver;
import squidpony.squidgrid.fov.ShadowFOV;
import github.nhydock.ssm.Inject;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
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

import core.components.Identifier;
import core.components.Groups;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.dungeon.Floor;
import core.service.interfaces.IColorMode;
import core.service.interfaces.IDungeonContainer;

public class RenderSystem extends EntitySystem implements EntityListener {

    public static final float MoveSpeed = .25f;

    //SquidPony's FOV System
    private float[][] wallMap;
    private float[][] fov;
    private FOVSolver fovSolver;
    
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private OrthogonalTiledMapRenderer mapRenderer;

    ComponentMapper<Position> positionMap = ComponentMapper.getFor(Position.class);
    ComponentMapper<Renderable> renderMap = ComponentMapper.getFor(Renderable.class);
    ComponentMapper<Identifier> idMap = ComponentMapper.getFor(Identifier.class);
    ComponentMapper<Stats> statMap = ComponentMapper.getFor(Stats.class);

    private float scale;
    private TextureRegion nullTile;

    private final Array<Actor> addQueue;
    private final Array<Actor> removeQueue;
    private Stage stage;
    private Group damageNumbers;
    private Skin uiSkin;

    private Table stats;
    private Label enemyName;
    private Label enemyHP;
    private boolean statsVis;

    private boolean invisible;

    // selective map layer to draw
    private TiledMap map;
    private int[] layers;

    @Inject public IColorMode color;
    @Inject public IDungeonContainer dungeonService;
    
    Family type = Family.all(Renderable.class, Position.class).get();

    Array<Entity> entities = new Array<Entity>();
    Array<Actor> shadows = new Array<Actor>();
    Group shadowLayer = new Group();
    Group entityLayer = new Group();
    Entity player;

    // temp vars used for hover effect
    static Vector2 v1 = new Vector2(0, 0);
    static Vector2 v2 = new Vector2(0, 0);

    public RenderSystem() {
        addQueue = new Array<Actor>();
        removeQueue = new Array<Actor>();
        this.layers = new int[] { 0 };
        fovSolver = new ShadowFOV();
    }

    public void setMap(TiledMap map) {
        this.mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, batch);
        scale = 32f * mapRenderer.getUnitScale();
        this.map = map;
    }

    public void setFloor(int depth) {
        this.layers[0] = depth - 1;
        
        TiledMapTileLayer layer = (TiledMapTileLayer)this.map.getLayers().get(layers[0]);
        wallMap = new float[layer.getWidth()][layer.getHeight()];
        
        //set shadow mapping
        Floor f = dungeonService.getDungeon().getFloor(depth);
        this.wallMap = f.getShadowMap();
        
        shadowLayer.clear();
        shadows.clear();
        for (int x = 0, rx = 0; x < wallMap.length; x++, rx += scale) {
            for (int y = 0, ry = 0; y < wallMap.length; y++, ry += scale) {
                Image image = new Image(uiSkin, "fill");
                image.setSize(scale, scale);
                image.setPosition(rx, ry);
                shadowLayer.addActor(image);
                shadows.add(image);
            }
        }
    }

    @Override
    public void entityAdded(final Entity e) {
        if (!type.matches(e)) {
            return;
        }
        entities.add(e);
        Renderable r = renderMap.get(e);
        Position p = positionMap.get(e);
        
        if (Groups.playerType.matches(e)) {
            player = e;
            fov = fovSolver.calculateFOV(wallMap, p.getX(), p.getY(), 8.0f);
            calcFov();
            System.out.print("Player added\n");
        }

        r.loadImage(uiSkin);
        final Image sprite = (Image) r.getActor();
        sprite.setSize(scale, scale);
        sprite.setPosition(p.getX() * scale, p.getY() * scale);
        if (Groups.monsterType.matches(e)) {
            //Gdx.app.log("[Entity]", "Entity is monster, adding hover controls");
            sprite.addListener(new InputListener() {

                @Override
                public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                    Stats s = statMap.get(e);
                    Identifier id = idMap.get(e);
                    if (id.hidden())
                        return;

                    v1.set(0, 0);
                    v2.set(0, sprite.getHeight() + 6);

                    Vector2 hv = sprite.localToStageCoordinates(v1);
                    Vector2 hv2 = sprite.localToStageCoordinates(v2);

                    // Gdx.app.log("[Input]", id.toString() +
                    // " has been hovered over. " + v.x + "," + v.y);
                    if (s.hidden) {
                        showStats(hv, hv2, id.toString(), "HP: ??? / ???");
                    }
                    else {
                        showStats(hv, hv2, id.toString(), String.format("HP: %3d / %3d", s.hp, s.maxhp));
                    }
                }

                @Override
                public void exit(InputEvent evt, float x, float y, int pointer, Actor toActor) {
                    hideStats();
                }
            });
        }

        addQueue.add(sprite);
    }

    protected void process(Entity e) {
        Position p = positionMap.get(e);
        Renderable r = renderMap.get(e);

        // adjust position to be aligned with tiles
        if (p.changed()) {
            r.getActor().addAction(Actions.moveTo(p.getX() * scale, p.getY() * scale, MoveSpeed));
            p.update();
            //update fov on move
            if (e == player) {
                fov = fovSolver.calculateFOV(wallMap, p.getX(), p.getY(), 8.0f);
                calcFov();
            }
        }
    }
    
    private void calcFov() {
        for (int x = 0, i = 0; x < fov.length; x++) {
            for (int y = 0; y < fov.length; y++, i++) {
                shadows.get(i).addAction(Actions.alpha(1.0f-fov[x][y]));
            }
        }
    }

    @Override
    public void update(float delta) {
        for (Actor r : removeQueue) {
            entityLayer.removeActor(r);
            r.clear();
        }

        for (Actor a : addQueue) {
            entityLayer.addActor(a);
        }

        removeQueue.clear();
        addQueue.clear();

        if (!invisible) {
            mapRenderer.setView(camera);
            mapRenderer.render(layers);
        }

        // process the entities
        for (Entity e : entities) {
            process(e);
        }

        stage.act(delta);

        if (player != null) {
            Renderable r = renderMap.get(player);
            camera.position.x = r.getActor().getX();
            camera.position.y = r.getActor().getY();
        }

        if (invisible) {
            invisible = false;
            return;
        }

        stage.draw();

        batch.begin();
        stats.draw(batch, stats.getColor().a);
        damageNumbers.draw(batch, damageNumbers.getColor().a);
        batch.end();
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void setView(WanderUI view, Skin skin) {
        Viewport v = view.getViewport();
        this.stage = new Stage(new ScalingViewport(Scaling.fit, v.getWorldWidth(), v.getWorldHeight(),
                new OrthographicCamera()));

        this.batch = (SpriteBatch) this.stage.getBatch();
        ShaderProgram shader = color.getShader();
        this.batch.setShader(shader);
        this.camera = (OrthographicCamera) this.stage.getCamera();

        uiSkin = skin;

        // enemy stats
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

        // dmg popups
        {
            damageNumbers = new Group();
            damageNumbers.setVisible(false);
            stage.addActor(damageNumbers);
        }
        
        entityLayer = new Group();
        shadowLayer = new Group();
        stage.addActor(entityLayer);
        stage.addActor(shadowLayer);
    }

    protected float getScale() {
        return scale;
    }

    public void setNull(Texture texture) {
        if (texture != null) {
            texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
            this.nullTile = new TextureRegion(texture);
        }
        else {
            this.nullTile = null;
        }

    }

    public Stage getStage() {
        return stage;
    }

    private void showStats(Vector2 v, Vector2 v2, String name, String hp) {
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
                Actions.parallel(Actions.alpha(0f), Actions.moveTo(v.x - (stats.getPrefWidth() / 2f), v.y)),
                Actions.parallel(Actions.alpha(1f, .2f), Actions.moveTo(v2.x - (stats.getPrefWidth() / 2f), v2.y, .2f))));
    }

    /**
     * Shows damage pop up over any entities when they take damage Call this
     * from the movement system only
     * 
     * @param e
     * @param dmg
     */
    protected void hit(Entity e, String dmg) {
        Position p = positionMap.get(e);

        float x = p.getX() * scale + (scale * .5f);
        float y = p.getY() * scale + (scale * .5f);

        final Label popup = new Label(dmg, uiSkin, "dmg");
        popup.setPosition(x - (popup.getPrefWidth() * .5f), y - (popup.getPrefHeight()));
        popup.addAction(Actions.sequence(Actions.alpha(0f),
                Actions.parallel(Actions.fadeIn(.2f), Actions.moveBy(0, scale, .3f, Interpolation.sineOut)),
                Actions.fadeOut(.2f), Actions.run(new Runnable() {

                    @Override
                    public void run() {
                        popup.remove();
                    }

                })));

        damageNumbers.addActor(popup);
    }

    public void hideStats() {
        statsVis = false;
        stats.clearActions();
        stats.addAction(Actions.alpha(0f, .3f));
    }

    @Override
    public void entityRemoved(Entity entity) {
        Renderable r = renderMap.get(entity);
        removeQueue.add(r.getActor());

        if (Groups.playerType.matches(entity)) {
            player = null;
        }
        entities.removeValue(entity, true);
    }

    @Override
    public void addedToEngine(Engine engine) {
        for (Entity e : entities) {
            Renderable r = renderMap.get(e);
            if (r != null) {
                removeQueue.add(r.getActor());
            }
        }
        entities.clear();
        for (Entity e : engine.getEntitiesFor(type)) {
            this.entityAdded(e);
        }
    }

    @Override
    public void removedFromEngine(Engine engine) {
        batch = null;
        camera = null;
        mapRenderer.dispose();
        stage.clear();
        stage.dispose();
        uiSkin.dispose();
        stage = null;
        mapRenderer = null;
        nullTile = null;
    }

    public void resize(int width, int height) {
        this.stage.getViewport().update(width, height, true);
    }
}
