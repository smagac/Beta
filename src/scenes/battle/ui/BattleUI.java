package scenes.battle.ui;

import github.nhydock.CollectionUtils;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectMap;

import core.DataDirs;
import core.components.Identifier;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Item;
import core.service.interfaces.IBattleContainer;
import core.service.interfaces.IPlayerContainer;
import core.util.dungeon.TsxTileSet;
import scene2d.PlaySound;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ParticleActor;
import scene2d.ui.extras.ParticleActor.ResetParticle;
import scenes.GameUI;
import scenes.Messages;
import scenes.battle.ui.CombatHandler.Combatant;
import scenes.battle.ui.CombatHandler.Turn;

public class BattleUI extends GameUI
{
    /*
     * Primary view elements
     */
    Image player;
    Image boss;
    TiledMapTileSet environment;
    
    /*
     * Main menu
     */
    CrossMenu mainmenu;
    FocusGroup mainFocus;
    
    /*
     * Goddess animation
     */
    Image goddess;
    Image fader;
    Image sacrifice;
    Image cloudsPan1;
    Image cloudsPan2;
    Image blessingLight;
    
    /*
     * Sacrifice menu
     */
    List<Item> inventory;
    ScrollPane itemPane;
    Label sacrificePrompt;
    Group sacrificePromptWindow;
    Group effectWindow;
    TextButton sacrificeButton;
    
    /*
     * Dice Roll
     */
    Image playerDie;
    Image bossDie;
    
    @Inject public IBattleContainer battleService;
    @Inject public IPlayerContainer playerService;
    
    CombatHandler combat;
    
    ParticleActor hitSplash;
    
    @Override
    protected void listenTo(IntSet messages)
    {
        super.listenTo(messages);
        messages.addAll(Messages.Battle.VICTORY, Messages.Battle.DEFEAT);
    }
	
    public BattleUI(AssetManager manager, TiledMapTileSet environment)
	{
		super(manager);
		menu = new DefaultStateMachine<BattleUI>(this);
		this.environment = environment;
		combat = new CombatHandler();
		
	}
	
	@Override
	protected void extend()
	{
	    makeField();
	    makeSacrificeMenu();
	    makeSacrificeScene();
	    makeAttackElements();
        
	    mainmenu = new CrossMenu(skin, this);
        mainmenu.setPosition(getDisplayWidth(), getDisplayHeight()/2f);
        
        mainFocus = new FocusGroup(mainmenu);
        
        display.addActor(mainmenu);
        setFocus(mainmenu);
	}
	
	/**
	 * Constructs the primary view of the battle (player, boss, and background)
	 */
	private void makeField(){
	    /*
         * Construct the background
         */
	    /**
        {
            Image floor = new Image(new TiledDrawable(environment.getTile(TsxTileSet.FLOOR).getTextureRegion()));
            floor.setWidth(getDisplayWidth());
            floor.setHeight(128f);
            floor.setPosition(0, getDisplayHeight(), Align.topLeft);
            
            Image wall = new Image(new TiledDrawable(environment.getTile(TsxTileSet.WALL).getTextureRegion()));
            wall.setWidth(getDisplayWidth());
            wall.setHeight(32f);
            wall.setPosition(0, getDisplayHeight() - 64, Align.topLeft);
            
            Image none = new Image(new TiledDrawable(environment.getTile(TsxTileSet.NULL).getTextureRegion()));
            none.setWidth(getDisplayWidth());
            none.setHeight(64f);
            none.setPosition(0, getDisplayHeight(), Align.topLeft);
            
            display.addActor(floor);
            display.addActor(none);
            display.addActor(wall);
        }
        */
	    
	    TextureRegion tex = new TextureRegion(new Texture(Gdx.files.internal("data/backgrounds/dungeon.png")));
	    Image bg = new Image(new TiledDrawable(tex));
	    bg.setWidth(getDisplayWidth());
	    bg.setHeight(128f);
	    bg.setPosition(0, getDisplayHeight(), Align.topLeft);
	    display.addActor(bg);
	    
        player = new Image(skin, playerService.getGender());
        player.setSize(64f, 64f);
        player.setPosition(getDisplayWidth(), 16f, Align.bottom);
        player.setOrigin(Align.center);
        player.addAction(
            Actions.sequence(
                Actions.moveBy(-getDisplayWidth()/2f, 0, 1f),
                Actions.delay(.5f),
                Actions.run(new Runnable(){
        
                    @Override
                    public void run() {
                        changeState(CombatStates.MAIN);
                    }
                    
                })
            )
        );
        
        Entity bossEntity = battleService.getBoss();
        Renderable rC = bossEntity.getComponent(Renderable.class);
        String sName = rC.getSpriteName();
        boss = new Image(skin, sName);
        boss.setSize(192f, 192f);
        boss.setOrigin(Align.center);
        boss.setPosition(0, getDisplayHeight()-96f, Align.top);
        boss.addAction(Actions.moveBy(getDisplayWidth()/2f, 0f, 1f));
        
        display.addActor(player);
        display.addActor(boss);
        
        fader = new Image(skin, "fill");
        fader.setSize(getDisplayWidth(), getDisplayHeight());
        fader.setPosition(-getDisplayWidth(), 0);
        display.addActor(fader);
	}

	/**
	 * Constructs the elements required for the attack phase of the battle
	 */
	private void makeAttackElements() {
	    bossDie = new Image(skin, "d1");
	    playerDie = new Image(skin, "d2");
	    
	    bossDie.setPosition(0, 0, Align.topRight);
	    playerDie.setPosition(0, 0, Align.topRight);
	    
	    display.addActor(bossDie);
	    display.addActor(playerDie);
	    
	    ParticleEffect pe = new ParticleEffect();
	    pe.load(Gdx.files.internal(DataDirs.Particles + "splash.particle"), Gdx.files.internal(DataDirs.Home));
	    hitSplash = new ParticleActor(pe);
	    
	    display.addActor(hitSplash);
	}
	
	/**
	 * Construct the view for sacrificing items
	 */
	private void makeSacrificeMenu(){
	    inventory = new List<Item>(skin);
	    
	    ObjectMap<Item, Integer> items = playerService.getInventory().getLoot();
	    inventory.setItems(items.keys().toArray());
	    
	    itemPane = new ScrollPane(inventory, skin);
	    itemPane.setSize(280f, getDisplayHeight() - 80f);
	    itemPane.setPosition(getDisplayWidth(), getDisplayHeight(), Align.topLeft);
	    display.addActor(itemPane);
	    
	    
	    sacrificePrompt = new Label("", skin, "promptsm");
	    sacrificePrompt.setWidth(304f);
	    sacrificePrompt.setPosition(48f, 64f);
	    sacrificePrompt.setWrap(true);
	    
	    sacrificePromptWindow = makeWindow(skin, 400, 130, true);
        sacrificePromptWindow.setColor(1,1,1,0);
        sacrificePromptWindow.setPosition(40f, 200f);
        sacrificePromptWindow.addActor(sacrificePrompt);
	    
	    display.addActor(sacrificePromptWindow);
	    
	    sacrificeButton = new TextButton("Sacrifice", skin);
	    sacrificeButton.setSize(280f, 32f);
	    sacrificeButton.setPosition(getDisplayWidth(), 20);
	    sacrificeButton.addListener(
            new InputListener(){
                @Override
                public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                    sacrificeButton.setChecked(true);
                }
                
                @Override
                public void exit(InputEvent evt, float x, float y, int pointer, Actor toActor) {
                    sacrificeButton.setChecked(false);
                }
                
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button ) {
                    if (button == Buttons.LEFT) {
	                    Item i = inventory.getSelected();
	                    MessageDispatcher.getInstance().dispatchMessage(null, null, Messages.Battle.MODIFY, i.descriptor());
	                    return true;
                    }
                    return false;
                }
            }
        );
	    display.addActor(sacrificeButton);
	}
	
	/**
	 * Construct the elements needed for the sacrifice animation
	 */
	private void makeSacrificeScene(){
	    
	    cloudsPan1 = new Image(new TiledDrawable(skin.getRegion("clouds")));
	    cloudsPan2 = new Image(new TiledDrawable(skin.getRegion("clouds")));
        cloudsPan1.setWidth(getDisplayWidth()*2);
        cloudsPan2.setWidth(getDisplayWidth()*2);
        
        cloudsPan1.setPosition(0, 0, Align.topLeft);
        cloudsPan2.setPosition(0, 0, Align.topLeft);
        display.addActor(cloudsPan1);
        display.addActor(cloudsPan2);
	    
	    goddess = new Image(skin, playerService.getWorship());
	    goddess.setSize(128, 128);
	    goddess.setPosition(-256f, -256f);
	    goddess.setOrigin(Align.center);
	    display.addActor(goddess);
	    
	    sacrifice = new Image(skin, "sacrifice");
	    sacrifice.setSize(64f, 64f);
	    sacrifice.setPosition(-64, -64);
	    sacrifice.setOrigin(Align.center);
	    display.addActor(sacrifice);
	    
	    blessingLight = new Image(new TiledDrawable(skin.getRegion("blessing")));
	    blessingLight.setSize(128f, 0f);
	    display.addActorBefore(boss, blessingLight);
	}
	
	/**
	 * Execute the animation sequence for sacrificing an item
	 */
	protected void playSacrificeAnimation(Runnable after) {
	    
	    fader.clearActions();
	    cloudsPan1.clearActions();
	    cloudsPan2.clearActions();
	    goddess.clearActions();
	    sacrifice.clearActions();
	    
	    fader.addAction(Actions.sequence(
	            Actions.alpha(1f),
	            Actions.moveToAligned(-getDisplayWidth(), 0, Align.bottomLeft),
	            Actions.moveToAligned(0, 0, Align.bottomLeft, .5f, Interpolation.linear)
	    ));
	    
	    cloudsPan1.addAction(Actions.sequence(
	            Actions.moveToAligned(getDisplayWidth(), 0, Align.topRight),
	            Actions.delay(.8f),
	            Actions.parallel(
	                    Actions.moveBy(0, 150f, .8f),
	                    Actions.forever(
	                            Actions.sequence(
    	                            Actions.moveBy(getDisplayWidth(), 0, 5f),
    	                            Actions.moveBy(-getDisplayWidth(), 0)
	                            )
	                    )
	            )
	    ));
	    
	    cloudsPan2.addAction(Actions.sequence(
                Actions.moveToAligned(getDisplayWidth(), 0, Align.topRight),
                Actions.delay(.8f),
                Actions.parallel(
                        Actions.moveBy(0, 80f, .8f),
                        Actions.forever(
                                Actions.sequence(
                                    Actions.moveBy(getDisplayWidth(), 0, 3f),
                                    Actions.moveBy(-getDisplayWidth(), 0)
                                )
                        )
                )
        ));
	    
	    goddess.addAction(Actions.sequence(
	            Actions.moveToAligned(getDisplayWidth() - 70f, 0, Align.top),
	            Actions.delay(2f),
	            Actions.parallel(
	                    Actions.rotateBy(360*4, 1.4f),
	                    Actions.sequence(
	                            Actions.moveTo(getDisplayWidth()-230f, getDisplayHeight() - 100f, .7f, Interpolation.circleOut),
	                            Actions.moveTo(getDisplayWidth()-230f, getDisplayHeight()/2f - 40f, .7f, Interpolation.bounceOut)
	                    )
	            )
	    ));
	    
	    sacrifice.addAction(
            Actions.sequence(
	            Actions.scaleTo(1f, 1f),
	            Actions.moveToAligned(200f, -32f, Align.top),
	            Actions.alpha(1f),
	            Actions.delay(4f),
	            Actions.moveToAligned(200f, getDisplayHeight()/2f, Align.center, .2f, Interpolation.circleOut),
	            Actions.run(new PlaySound(DataDirs.Sounds.charge)),
                Actions.scaleTo(1.5f, 1.5f, .1f, Interpolation.circleOut),
                Actions.scaleTo(1.0f, 1.0f, .1f, Interpolation.circleOut),
                Actions.scaleTo(1.5f, 1.5f, .1f, Interpolation.circleOut),
                Actions.scaleTo(1.0f, 1.0f, .1f, Interpolation.circleOut),
                Actions.scaleTo(2.0f, 2.0f, .1f, Interpolation.circleOut),
                Actions.scaleTo(1.5f, 1.5f, .1f, Interpolation.circleOut),
                Actions.scaleTo(2.0f, 2.0f, .1f, Interpolation.circleOut),
                Actions.scaleTo(1.5f, 1.5f, .1f, Interpolation.circleOut),
                Actions.scaleTo(3.0f, 3.0f, .1f, Interpolation.circleOut),
                Actions.scaleTo(2.4f, 2.4f, .1f, Interpolation.circleOut),
                Actions.scaleTo(3.0f, 3.0f, .1f, Interpolation.circleOut),
                Actions.scaleTo(2.4f, 2.4f, .1f, Interpolation.circleOut),
                Actions.scaleTo(3.0f, 3.0f, .1f, Interpolation.circleOut),    
                Actions.delay(.5f),
                Actions.addAction(Actions.moveToAligned(getDisplayWidth()/2f, getDisplayHeight(), Align.top, .5f, Interpolation.circleOut), goddess),
                Actions.parallel(
                    Actions.moveToAligned(getDisplayWidth()/2f, getDisplayHeight() - 96f, Align.bottom, .5f, Interpolation.circleOut),
                    Actions.scaleTo(.5f, .5f, .4f)
                ),
                Actions.delay(.3f),
                Actions.moveBy(0f, -500f, .15f),
                Actions.addAction(Actions.moveBy(0, getDisplayHeight(), .3f, Interpolation.circleOut), goddess),
                Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .3f, Interpolation.circleOut), fader),
                Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .3f, Interpolation.circleOut), cloudsPan1),
                Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .3f, Interpolation.circleOut), cloudsPan2),       
                Actions.delay(.4f),
                Actions.addAction(
                    Actions.sequence(
                        Actions.sizeTo(192f, 0f),
                        Actions.run(new PlaySound(DataDirs.Sounds.blast)),
                        Actions.moveTo(boss.getX(Align.center)-64f, getDisplayHeight()),
                        Actions.parallel(
                            Actions.sizeBy(0, getDisplayHeight() - boss.getY(), .4f, Interpolation.circleOut),
                            Actions.moveBy(0, -(getDisplayHeight() - boss.getY()), .4f, Interpolation.circleOut)
                        ),
                        Actions.delay(.9f),
                        Actions.sizeTo(192f, 0, .4f, Interpolation.circleOut),
                        Actions.run(after)
                        
                    ), 
                    blessingLight
                
                )
            )
        );
	}
	
	protected void playFightAnimation(final Turn turn, final Runnable after) {
	    playRollAnimation(turn,
            Actions.sequence(
                Actions.run(new Runnable(){

                    @Override
                    public void run() {
                        if (turn.phase == Combatant.Player) {
                            playerAttackAnimation();
                        } else {
                            bossAttackAnimation();
                        }
                    }
                    
                }),
                Actions.delay(1f),
                Actions.run(after)
            )
        );
	}
	
	protected Action hitAnimation(){
	    return hitAnimation(CollectionUtils.randomChoice(DataDirs.Sounds.hit, DataDirs.Sounds.hit2));
	}
	
	protected Action hitAnimation(String fx) {
	    return Actions.sequence(
                    Actions.run(new PlaySound(fx)),
                    Actions.alpha(0f, .1f),
                    Actions.run(new ResetParticle(hitSplash)),
                    Actions.alpha(1f, .1f)
                );
	}
	
	private void playerAttackAnimation() {
	    hitSplash.setPosition(boss.getX(Align.center), boss.getY(Align.center));
	    player.addAction(
            Actions.sequence(
                Actions.scaleTo(-1f, 1f, .25f, Interpolation.sineIn),
                Actions.scaleTo(1f, 1f, .25f, Interpolation.sineIn)
        ));
	    boss.addAction(
            Actions.sequence(
	            Actions.delay(.25f),
	            hitAnimation()
        ));
	}
	
	private void bossAttackAnimation() {
	    hitSplash.setPosition(player.getX(Align.center), player.getY(Align.center));
        boss.addAction(
            Actions.sequence(
                Actions.scaleTo(-1f, 1f, .25f, Interpolation.sineIn),
                Actions.scaleTo(1f, 1f, .25f, Interpolation.sineIn)
        ));
        player.addAction(
            Actions.sequence(
                Actions.delay(.25f),
                hitAnimation()
        ));
	}
	
	/**
	 * Plays the defending animation sequence
	 * @param t
	 * @param after
	 */
	protected void playDefenseAnimation(final Turn t, final Runnable after) {
	    playRollAnimation(t, 
            Actions.run(new Runnable(){
                @Override
                public void run(){
                    if (t.hits < 0) {
                        hitSplash.setPosition(player.getX(Align.center), player.getY(Align.center));
                        
                        player.addAction(hitAnimation());
                    } else {
                        hitSplash.setPosition(boss.getX(Align.center), boss.getY(Align.center));
                        
                        boss.addAction(hitAnimation(DataDirs.Sounds.deflect));
                    }
                    
                    addAction(Actions.sequence(Actions.delay(.2f), Actions.run(after)));
                }
            })
        );
    }
	
	/**
	 * Show a dice roll
	 * @param turn
	 */
	private void playRollAnimation(Turn turn, Action after){
	    fader.clearActions();
	    bossDie.clearActions();
	    playerDie.clearActions();
	    
	    bossDie.setDrawable(getSkin(), "d"+turn.bossRoll);
        playerDie.setDrawable(getSkin(), "d"+turn.playerRoll);
        
        bossDie.setPosition(getDisplayWidth(), boss.getY(Align.top), Align.topLeft);
        playerDie.setPosition(-playerDie.getWidth(), player.getY(Align.top), Align.bottomLeft);
        
        fader.addAction(
            Actions.sequence(
                Actions.alpha(0f),
                Actions.moveTo(0, 0),
                Actions.alpha(.8f, .3f),
                Actions.addAction(
                    Actions.sequence(
                        Actions.moveTo(getDisplayWidth()*.25f, bossDie.getY(), .3f, Interpolation.circleOut),
                        Actions.moveBy(-20f, 0f, 1f),
                        Actions.moveTo(-bossDie.getWidth(), bossDie.getY(), .2f, Interpolation.circleIn)
                    ), 
                    bossDie
                ),
                Actions.addAction(
                    Actions.sequence(
                        Actions.moveToAligned(getDisplayWidth()*.75f, playerDie.getY(), Align.bottomRight, .3f, Interpolation.circleOut),
                        Actions.moveBy(20f, 0f, 1f),
                        Actions.moveTo(getDisplayWidth(), playerDie.getY(), .2f, Interpolation.circleIn)
                    ), 
                    playerDie
                ),
                Actions.delay(2f),
                Actions.alpha(0f, .2f),
                after
            )
        );
	}
	
	
	@Override
	protected void triggerAction(int index)
	{
	    //shouldn't happen since we don't use the default buttons in this scene
	}

	@Override
	protected FocusGroup focusList()
	{
		return mainFocus;
	}

	@Override
	public String[] defineButtons()
	{
	    return null;
	}
	
	/**
     * Adds a new notification bubble into the bottom left corner of the display
     * 
     * @param s
     */
	@Override
    public void pushNotification(String msg) {
        final Group notification = new Group();
        String[] lines = msg.split("\n");
        LabelStyle style = skin.get("promptsm", LabelStyle.class);
        float delay = 0;
        for (int i = lines.length-1, y = 10, x = 10; i >= 0; i--, y += style.font.getLineHeight(), x += 10){
            Label line = new Label(lines[i], style);
            line.setPosition(0, y, Align.bottomRight);
            line.setColor(1, 1, 1, 0);
            
            line.addAction(
                Actions.sequence(
                    Actions.delay(delay),
                    Actions.parallel(
                        Actions.alpha(1f, .1f),
                        Actions.moveToAligned(x, y, Align.bottomLeft, .1f)
                    )
                )
            );
            
            notification.addActor(line);
            
            delay += .1f;
        }
        
        delay += 1.4f;
        
        notification.addAction(
            Actions.sequence(
               Actions.delay(delay),
               Actions.parallel(
                   Actions.alpha(0f, .1f),
                   Actions.moveBy(-30, 0f, .1f)
               ),
               Actions.run(new Runnable() {
                
                @Override
                public void run() {
                    notification.remove();
                }
            })
            )
        );
        
        display.addActor(notification);
    }

    
}
