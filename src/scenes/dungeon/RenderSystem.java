package scenes.dungeon;

import java.util.Arrays;

import github.nhydock.ssm.Inject;
import scene2d.runnables.PlaySound;
import scene2d.ui.extras.ParticleActor;
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
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.DataDirs;
import core.components.Groups;
import core.components.Identifier;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Ailment;
import core.datatypes.Health;
import core.datatypes.dungeon.Floor;
import core.service.interfaces.IColorMode;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;

public class RenderSystem extends EntitySystem implements EntityListener, Telegraph {

    public static final float MoveSpeed = .15f;
    public static final float MaxZoom = 0f;
    private static final float SCALE = 32f;
    private static final int[] SHADOW_RANGE = {41, 21};
    private static final float FOV_RANGE = 8.0f;
    private static final float BLIND_RANGE = 3.0f;
    
    //SquidPony's FOV System
    private int width, height;
    private float[][] wallMap;  //base fov density of walls
    private float[][] mergeMap; //combined fov density of walls and actors
    private float[][] fov;
    private FOVSolver fovSolver;
    
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private OrthogonalTiledMapRenderer mapRenderer;

    ComponentMapper<Renderable> renderMap = ComponentMapper.getFor(Renderable.class);
    ComponentMapper<Stats> statMap = ComponentMapper.getFor(Stats.class);

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
    @Inject public IPlayerContainer playerService;
    
    Family type = Family.all(Renderable.class, Position.class).get();

    boolean zoomb;
    Actor zoom = new Actor();
    Array<Entity> entities = new Array<Entity>();
    Image[][] shadows;
    Group shadowLayer;
    Group entityLayer;
    Entity player;

    boolean moved;
    
    ParticleActor dustParticle;
    private static final float DUST_LIMIT = 1f;
    private float dustTimer = DUST_LIMIT;
    
    private Array<ParticleActor> weatherSystem;
    private Group weatherLayer;
    
    private Image targetCursor;
    private int[] cursorLocation;
    
    private Group spellLayer;
    
    public RenderSystem() {
        addQueue = new Array<Actor>();
        removeQueue = new Array<Actor>();
        this.layers = new int[] { 0 };
        fovSolver = new RippleFOV();
        cursorLocation = new int[2];
    }

    public void setMap(TiledMap map) {
        this.mapRenderer = new OrthogonalTiledMapRenderer(map, (SCALE)/32f, batch);
    }
    
    /**
     * Combines wall and entity density to create an accurate FOV blocking space
     */
    private void calculateDensity() {
        for (int i = 0; i < wallMap.length; i++) {
            for (int n = 0; n < wallMap[0].length; n++) {
                mergeMap[i][n] = wallMap[i][n];
            }
        }
        
        for (int i = 0; i < entities.size; i++) {
            Entity e  = entities.get(i);
            
            Renderable r = Renderable.Map.get(e);
            Position p = Position.Map.get(e);
            
            mergeMap[p.getX()][p.getY()] = r.getDensity();
        }
    }
    
    /**
     * Updates all the actors on screen with the calculated FOV
     */
    public void updateFOV(){
        Position p = Position.Map.get(player);
        
        calculateDensity();
        Health health = playerService.getAilments();
        float range = FOV_RANGE;
        if (health.getAilments().contains(Ailment.BLIND, true)) {
            range = BLIND_RANGE;
        }
        fovSolver.calculateFOV(mergeMap, p.getX(), p.getY(), range, fov);
        
        final int WBOUND = SHADOW_RANGE[0]/2;
        final int HBOUND = SHADOW_RANGE[1]/2;
        final int LEFT = p.getX() - WBOUND;
        final int RIGHT = p.getX() + WBOUND;
        final int BOTTOM = p.getY() + HBOUND;
        final int TOP = p.getY() - HBOUND;
        for (int x = LEFT, i = 0; x < RIGHT; x++, i++) {
            for (int y = TOP, n = 0; y < BOTTOM; y++, n++) {
                Actor a = shadows[i][n];
                a.clearActions();
                float strength = 0;
                if (x >= 0 && x < fov.length && y >= 0 && y < fov[0].length) {
                    strength = fov[x][y];
                }
                a.getColor().a = 1.0f-strength;
                //block hover over enemies when they're in the shadows
                if (strength <= .5f) {
                    a.setTouchable(Touchable.enabled);
                } 
                else {
                    a.setTouchable(Touchable.disabled);
                }
            }
        }
        shadowLayer.setPosition(p.getX() * SCALE + (SCALE/2), p.getY() * SCALE + (SCALE/2), Align.center);
    }

    public void setFloor(int depth) {
        this.layers[0] = depth - 1;
        
        //set shadow mapping
        Floor f = dungeonService.getDungeon().getFloor(depth);
        width = f.getBooleanMap().length;
        height = f.getBooleanMap()[0].length;
        
        wallMap = f.getShadowMap();
        mergeMap = new float[width][height];
        fov = new float[width][height];
        
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
        sprite.setSize(SCALE, SCALE);
        sprite.setPosition(p.getX() * SCALE, p.getY() * SCALE);
        sprite.setUserObject(e);
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

    /**
     * Handles movement animations of actors
     * @param e
     */
    protected void process(Entity e) {
        Position p = Position.Map.get(e);
        Renderable r = renderMap.get(e);

        //update image
        if (r.hasChanged()) {
            r.loadImage(uiSkin);
        }
        
        if (p.isFighting()) {
            float shiftX, shiftY, x, y;

            x = p.getX() * SCALE;
            y = p.getY() * SCALE;
            shiftX = (p.getDestinationX() * SCALE) - x;
            shiftY = (p.getDestinationY() * SCALE) - y;
            
            r.getActor().clearActions();
            r.getActor().addAction(
                Actions.sequence(
                    Actions.moveTo(x, y),
                    Actions.moveTo(x + shiftX / 4f, y + shiftY / 4f, RenderSystem.MoveSpeed / 2f),
                    Actions.moveTo(x, y, RenderSystem.MoveSpeed / 2f)
                )
            );
            
            p.update();
            
            if (e == player) {
                moved = true;
            }
        }
        // adjust position to be aligned with tiles
        else if (p.hasChanged()) {
            if (e == player) {
                moved = true;
                
                if (dustTimer >= DUST_LIMIT) {
                    dustParticle.setPosition((p.getCurrentX() + .5f) * SCALE, p.getCurrentY() * SCALE);
                    dustParticle.addAction(Actions.run(new ParticleActor.ResetParticle(dustParticle)));
                }
                dustTimer = 0f;
            }
            
            r.getActor().clearActions();
            r.getActor().addAction(Actions.moveTo(p.getDestinationX() * SCALE, p.getDestinationY() * SCALE, MoveSpeed));
            p.update();
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
        dustTimer += delta;
        
        if (player != null) {
            Renderable r = renderMap.get(player);
            Actor a = r.getActor();
            camera.position.x = a.getX(Align.center);
            camera.position.y = a.getY(Align.center);
            camera.zoom = 1f + (zoom.getColor().a);
            camera.update();
            
            weatherLayer.setPosition(a.getX(Align.center), a.getY(Align.center), Align.center);
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
        zoom.addAction(Actions.alpha(0f));
        stage.addActor(zoom);
        
        //setup dust effect
        {
            ParticleEffect pe = new ParticleEffect();
            pe.load(Gdx.files.internal(DataDirs.Particles + "dust.particle"), Gdx.files.internal(DataDirs.Home));
            dustParticle = new ParticleActor(pe);
            dustParticle.setPosition(v.getWorldWidth()/2f, v.getWorldHeight()/2f - 8f);
        }
        
        //setup targeting cursor
        {
            targetCursor = new Image(skin, "cursor");
            targetCursor.setSize(SCALE, SCALE);
            targetCursor.setOrigin(Align.center);
            targetCursor.setScale(1);
            targetCursor.setVisible(false);
            targetCursor.addAction(
                    Actions.forever(
                        Actions.sequence(
                            Actions.scaleTo(1, 1), 
                            Actions.scaleTo(.7f, .7f, .2f, Interpolation.circleIn), 
                            Actions.scaleTo(1f, 1f, .2f, Interpolation.circleOut)
                        )
                    )
                );
            targetCursor.setTouchable(Touchable.disabled);
        }
        
        entityLayer = new Group();
        shadowLayer = new Group();
        spellLayer = new Group();
        damageNumbers = new Group();
        weatherLayer = new Group();
        weatherLayer.setSize(this.stage.getWidth(), this.stage.getHeight());
        spellLayer.setSize(this.stage.getWidth(), this.stage.getHeight());
        
        entityLayer.setTouchable(Touchable.childrenOnly);
        spellLayer.setTouchable(Touchable.disabled);
        weatherLayer.setTouchable(Touchable.disabled);
        shadowLayer.setTouchable(Touchable.childrenOnly);
        damageNumbers.setTouchable(Touchable.disabled);
        stage.addActor(dustParticle);
        stage.addActor(entityLayer);
        stage.addActor(spellLayer);
        stage.addActor(shadowLayer);
        stage.addActor(weatherLayer);
        stage.addActor(targetCursor);
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

        //handle weather
        {
            weatherSystem = new Array<ParticleActor>();
            for (float i = 0, x = 0; i < 6; i++, x += v.getWorldWidth()/5f){
                ParticleEffect pe = new ParticleEffect();
                pe.load(Gdx.files.internal(DataDirs.Particles + "sandstorm.particle"), Gdx.files.internal(DataDirs.Home));
                
                ParticleActor pa = new ParticleActor(pe);
                pa.setPosition(x, v.getWorldHeight());
                weatherSystem.add(pa);
                weatherLayer.addActor(pa);
                pa.start();
            }
        }

        shadows = new Image[SHADOW_RANGE[0]][SHADOW_RANGE[1]];
        for (int x = 0, rx = 0; x < SHADOW_RANGE[0]; x++, rx += SCALE) {
            for (int y = 0, ry = 0; y < SHADOW_RANGE[1]; y++, ry += SCALE) {
                Image image = new Image(skin, "fill");
                image.setSize(SCALE, SCALE);
                image.setPosition(rx, ry);
                shadowLayer.addActor(image);
                shadows[x][y] = image;
            }
        }
        shadowLayer.setSize(SHADOW_RANGE[0] * SCALE, SHADOW_RANGE[1] * SCALE);
        
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
        Vector2 v = new Vector2(sprite.getWidth()/2f, sprite.getHeight());
        sprite.localToStageCoordinates(v);
        stats.clearActions();
        stats.addAction(
            Actions.sequence(
                Actions.moveToAligned(v.x, v.y, Align.bottom),
                Actions.alpha(0f),
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

        float x = p.getX() * SCALE + (SCALE * .5f);
        float y = p.getY() * SCALE + (SCALE * .5f);

        final Label popup = new Label(dmg, uiSkin, "dmg");
        popup.setPosition(x - (popup.getPrefWidth() * .5f), y - (popup.getPrefHeight()));
        popup.addAction(
            Actions.sequence(
                Actions.alpha(0f),
                Actions.parallel(Actions.fadeIn(.2f), Actions.moveBy(0, SCALE, .3f, Interpolation.sineOut)),
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
    
    public void toggleZoom() {
        
        zoom.clearActions();
        if (zoomb) {
            zoom.addAction(Actions.sequence(Actions.alpha(MaxZoom), Actions.alpha(0f, .3f, Interpolation.circleOut)));
            zoomb = false;
        } else {
            zoom.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(MaxZoom, .3f, Interpolation.circleOut)));
            zoomb = true;
        }
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
        if (msg.message == Messages.Dungeon.Notify && msg.extraInfo instanceof CombatNotify) {
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
    
    /**
     * Toggles the visibility of the cursor and positions it on the player
     */
    public void toggleCursor() {
        Position p = Position.Map.get(player);
        cursorLocation[0] = p.getX();
        cursorLocation[1] = p.getY();
        targetCursor.setPosition(cursorLocation[0]*SCALE, cursorLocation[1]*SCALE);
        targetCursor.setVisible(!targetCursor.isVisible());
    }
    
    /**
     * shift the position of the cursor
     * @param d
     */
    public void moveCursor(Direction d) {
        int[] cursorLocation = d.move(getCursorLocation());
        moveCursor(cursorLocation);
    }
    
    /**
     * Checks if the cursor is over a target, and if it is then show its stats
     */
    private void cursorTargetHover(){
        //show stats when cursor hovers over entity
        Entity hover = null;
        for (int i = 0; i < entities.size && hover == null; i++) {
            Entity e = entities.get(i);
            Position p = Position.Map.get(e);
            if (p.getX() == cursorLocation[0] && p.getY() == cursorLocation[1]) {
                hover = e;
            }
        }
        
        if (hover == null || hover == player) {
            hideStats();
        } else {
            refreshStats(hover);
            showStats(hover);
        }
    }

    /**
     * Get the location of the cursor  
     * Used in determining which direction to fire a spell in
     * @return a copy of the location point
     */
    public int[] getCursorLocation() {
        return Arrays.copyOf(cursorLocation, 2);
    }

    /**
     * Invokes particle system explosion along path of the spell
     */
    public void fireSpell(int[][] path) {
        for (int i = 0; i < path.length && i <= 10; i++) {
            int[] pos = path[i];
            final ParticleEffect pe = new ParticleEffect();
            pe.load(Gdx.files.internal(DataDirs.Particles + "spell.particle"), Gdx.files.internal(DataDirs.Home));
            ParticleActor actor = new ParticleActor(pe);
            actor.setPosition(pos[0] * SCALE, pos[1] * SCALE);
            actor.start();
            actor.addAction(Actions.sequence(Actions.delay(1f), Actions.run(new Runnable(){
                @Override
                public void run(){
                    Gdx.app.log("Spell", "Explosion particle disposed");
                    pe.dispose();
                }
            }), Actions.removeActor()));
            spellLayer.addActor(actor);
        }
        (new PlaySound(DataDirs.Sounds.shimmer)).run();
    }

    /**
     * Shifts the cursor to a new tile location as long as that tile is visible
     * @param cursorLocation
     */
    public void moveCursor(int[] cursorLocation) {
        Vector2 loc = new Vector2(cursorLocation[0]*SCALE, cursorLocation[1]*SCALE);
        shadowLayer.stageToLocalCoordinates(loc);
        if (shadowLayer.hit(loc.x, loc.y, true) != null){
            return;
        }
        
        this.cursorLocation[0] = cursorLocation[0];
        this.cursorLocation[1] = cursorLocation[1];
        targetCursor.setPosition(cursorLocation[0]*SCALE, cursorLocation[1]*SCALE);

        cursorTargetHover();
    }
}
