package scenes.dungeon;

import github.nhydock.ssm.Inject;
import scenes.Messages;
import scenes.Messages.Dungeon.CombatNotify;
import scenes.dungeon.ui.WanderUI;
import squidpony.squidgrid.fov.FOVSolver;
import squidpony.squidgrid.fov.RippleFOV;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.components.Groups;
import core.components.Identifier;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.dungeon.Floor;
import core.service.interfaces.IColorMode;
import core.service.interfaces.IDungeonContainer;

public class RenderSystem extends EntitySystem implements EntityListener, Telegraph {

    public static final float MoveSpeed = .15f;

    //SquidPony's FOV System
    private int width, height;
    private float[][] wallMap;  //fov density of walls
    private float[][] actorMap; //fov density of actors
    private float[][] actorFOV; //calculated fov from actors
    private float[][] wallFOV;  //calculated fov from walls
    private FOVSolver fovSolver;
    
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private OrthogonalTiledMapRenderer mapRenderer;

    ComponentMapper<Renderable> renderMap = ComponentMapper.getFor(Renderable.class);
    ComponentMapper<Stats> statMap = ComponentMapper.getFor(Stats.class);

    private float scale;

    private final Array<Actor> addQueue;
    private final Array<Actor> removeQueue;
    private Stage stage;
    private Group damageNumbers;
    private Skin uiSkin;

    private Table stats;
    private Label enemyName;
    private Label enemyHP;
    private Entity statsVis;

    private boolean invisible;

    // selective map layer to draw
    private int[] layers;

    @Inject public IColorMode color;
    @Inject public IDungeonContainer dungeonService;
    
    Family type = Family.all(Renderable.class, Position.class).get();

    Array<Entity> entities = new Array<Entity>();
    Array<Image> shadows = new Array<Image>();
    Group shadowLayer = new Group();
    Group entityLayer = new Group();
    Pool<Image> shadowPool = Pools.get(Image.class, 4000);
    Entity player;

    // temp vars used for hover effect
    static Vector2 v1 = new Vector2(0, 0);
    static Vector2 v2 = new Vector2(0, 0);
    boolean moved;
    
    public RenderSystem() {
        addQueue = new Array<Actor>();
        removeQueue = new Array<Actor>();
        this.layers = new int[] { 0 };
        fovSolver = new RippleFOV();
    }

    public void setMap(TiledMap map) {
        this.mapRenderer = new OrthogonalTiledMapRenderer(map, 1f, batch);
        scale = 32f * mapRenderer.getUnitScale();
    }
    
    /**
     * Combines wall and entity density to create an accurate FOV blocking space
     */
    private void calculateDensity() {
        for (int i = 0; i < actorMap.length; i++) {
            for (int n = 0; n < actorMap[0].length; n++) {
                actorMap[i][n] = 0;
            }
        }
        
        for (int i = 0; i < entities.size; i++) {
            Entity e  = entities.get(i);
            
            Renderable r = Renderable.Map.get(e);
            Position p = Position.Map.get(e);
            
            actorMap[p.getX()][p.getY()] = r.getDensity();
        }
    }
    
    /**
     * Updates all the actors on screen with the calculated FOV
     */
    private void updateFOV(){
        Position p = Position.Map.get(player);
        
        calculateDensity();
        fovSolver.calculateFOV(wallMap, p.getX(), p.getY(), 8.0f, wallFOV);
        fovSolver.calculateFOV(actorMap, p.getX(), p.getY(), 8.0f, actorFOV);
        
        for (int x = 0, i = 0; x < width; x++) {
            for (int y = 0; y < height; y++, i++) {
                Actor a = shadows.get(i);
                a.clearActions();
                float strength = 0;
                strength = Math.min(wallFOV[x][y], actorFOV[x][y]);
                a.addAction(Actions.alpha(1.0f-strength));
                //block hover over enemies when they're in the shadows
                if (strength <= .5f) {
                    a.setTouchable(Touchable.enabled);
                } 
                else {
                    a.setTouchable(Touchable.disabled);
                }
            }
        }
    }

    public void setFloor(int depth) {
        this.layers[0] = depth - 1;
        
        //set shadow mapping
        Floor f = dungeonService.getDungeon().getFloor(depth);
        width = f.getBooleanMap().length;
        height = f.getBooleanMap()[0].length;
        
        wallMap = f.getShadowMap();
        actorMap = new float[width][height];
        wallFOV = new float[width][height];
        actorFOV = new float[width][height];
        
        shadowLayer.clear();
        for (Image s : shadows)
        {
            shadowPool.free(s);
        }
        shadows.clear();
        Drawable d = uiSkin.getDrawable("fill");
        for (int x = 0, rx = 0; x < wallMap.length; x++, rx += scale) {
            for (int y = 0, ry = 0; y < wallMap[0].length; y++, ry += scale) {
                Image image = shadowPool.obtain();
                image.setDrawable(d);
                image.setSize(scale, scale);
                image.setPosition(rx, ry);
                shadowLayer.addActor(image);
                shadows.add(image);
            }
        }
        
        if (player != null){
            updateFOV();
        }
        
    }

    @Override
    public void entityAdded(final Entity e) {
        if (!type.matches(e)) {
            return;
        }
        entities.add(e);
        Renderable r = renderMap.get(e);
        Position p = Position.Map.get(e);
        
        if (Groups.playerType.matches(e)) {
            player = e;
            if (wallMap != null){
                updateFOV();
            }
        }

        if (r.getActor() == null) {
            r.loadImage(uiSkin);
        }
        final Image sprite = (Image) r.getActor();
        sprite.setSize(scale, scale);
        sprite.setPosition(p.getX() * scale, p.getY() * scale);
        if (Groups.monsterType.matches(e)) {
            //Gdx.app.log("[Entity]", "Entity is monster, adding hover controls");
            sprite.addListener(new InputListener() {

                @Override
                public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                    refreshStats(e);
                    showStats(e);
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
        Position p = Position.Map.get(e);
        Renderable r = renderMap.get(e);

        // adjust position to be aligned with tiles
        if (p.changed()) {
            r.getActor().clearActions();
            r.getActor().addAction(Actions.moveTo(p.getX() * scale, p.getY() * scale, MoveSpeed));
            p.update();
            
            if (e == player) {
                moved = true;
            }
        }
        
    }
    
    @Override
    public void update(float delta) {
        for (Actor r : removeQueue) {
            entityLayer.removeActor(r);
            r.clear();
        }
        removeQueue.clear();
        
        for (Actor a : addQueue) {
            entityLayer.addActor(a);
        }
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
            Actor a = r.getActor();
            camera.position.x = a.getX(Align.center);
            camera.position.y = a.getY() - a.getHeight()/2f;
        }

        if (invisible) {
            invisible = false;
            return;
        }

        if (moved) {
            updateFOV();
            moved = false;
        }
        
        stage.draw();
        
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

        entityLayer = new Group();
        shadowLayer = new Group();
        damageNumbers = new Group();
        entityLayer.setTouchable(Touchable.childrenOnly);
        shadowLayer.setTouchable(Touchable.childrenOnly);
        damageNumbers.setTouchable(Touchable.disabled);
        stage.addActor(entityLayer);
        stage.addActor(shadowLayer);
        stage.addActor(damageNumbers);
        
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
            stats.setColor(1, 1, 1, 0);
            stats.setBackground(skin.getDrawable("button_up"));
            stats.setTouchable(Touchable.disabled);
            stage.addActor(stats);
        }

    }

    protected float getScale() {
        return scale;
    }

    public Stage getStage() {
        return stage;
    }
    
    void refreshStats(Entity e) {
        Stats s = statMap.get(e);
        Identifier id = Identifier.Map.get(e);
        
        enemyName.setText(id.toString());
        
        if (s.hidden) {
            enemyHP.setText("HP: ??? / ???");
        }
        else {
            enemyHP.setText( String.format("HP: %3d / %3d", s.hp, s.maxhp));
        }

        stats.pack();
        float width = Math.max(enemyName.getPrefWidth(), enemyHP.getPrefWidth()) + 40;
        stats.setWidth(width);
    }

    /**
     * Pops up the message box with the actor's stats in it
     * @param e
     */
    void showStats(Entity e) {
        if (statsVis != null || Identifier.Map.get(e).hidden()) {
            return;
        }
        
        statsVis = e;
        
        Actor sprite = Renderable.Map.get(e).getActor();
        v1.set(sprite.getWidth()/2f, sprite.getHeight());

        Vector2 hv = sprite.localToStageCoordinates(v1);
        stats.setPosition(hv.x, hv.y, Align.bottom);
        stats.setColor(1, 1, 1, 0);
        
        stats.clearActions();
        stats.addAction(
            Actions.sequence(
                Actions.parallel(
                    Actions.alpha(1f, .2f),
                    Actions.moveBy(0, 6, .2f)
                )
            )
        );
    }

    /**
     * Shows damage pop up over any entities when they take damage Call this
     * from the movement system only
     * 
     * @param e
     * @param dmg
     */
    protected void hit(Entity e, String dmg) {
        Position p = Position.Map.get(e);

        float x = p.getX() * scale + (scale * .5f);
        float y = p.getY() * scale + (scale * .5f);

        final Label popup = new Label(dmg, uiSkin, "dmg");
        popup.setPosition(x - (popup.getPrefWidth() * .5f), y - (popup.getPrefHeight()));
        popup.addAction(
            Actions.sequence(
                Actions.alpha(0f),
                Actions.parallel(Actions.fadeIn(.2f), Actions.moveBy(0, scale, .3f, Interpolation.sineOut)),
                Actions.fadeOut(.2f), 
                Actions.removeActor()
            )
        );

        damageNumbers.addActor(popup);
    }

    public void hideStats() {
        statsVis = null;
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
        if (statsVis == entity) {
            hideStats();
        }
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
        
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.Notify);
    }

    @Override
    public void removedFromEngine(Engine engine) {
        batch = null;
        camera = null;
        
        mapRenderer.dispose();
        stage.dispose();
        stage = null;
        mapRenderer = null;
        
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.Notify);
        
    }

    public void resize(int width, int height) {
        this.stage.getViewport().update(width, height, true);
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        if (msg.message == Messages.Dungeon.Notify) {
            CombatNotify notification = (CombatNotify)msg.extraInfo;
            int dmg = notification.dmg;
            Entity attacker = notification.attacker;
            Entity opponent = notification.opponent;
            if (dmg == -1) {
                hit(opponent, "Miss");
            } 
            else if (attacker == player) {
                if (dmg == 0) {
                    hit(opponent, "Block");
                }
                else {
                    hit(opponent, String.valueOf(dmg));
                }
            }
            else {
                if (dmg == 0) {
                    hit(opponent, "Block");
                }
                else {
                    hit(opponent, String.valueOf(dmg));
                }
            }
            if (statsVis == opponent) {
                refreshStats(opponent);
            }
        }
        return false;
    }
}
