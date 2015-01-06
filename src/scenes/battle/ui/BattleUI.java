package scenes.battle.ui;

import github.nhydock.ssm.Inject;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

import core.DataDirs;
import core.components.Renderable;
import core.service.interfaces.IBattleContainer;
import core.service.interfaces.IPlayerContainer;
import core.util.dungeon.TsxTileSet;
import scene2d.ui.extras.FocusGroup;
import scenes.GameUI;

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
    List<String> inventory;
    Label sacrificePrompt;
    Group sacrificePromptWindow;
    Group effectWindow;
    TextButton sacrificeButton;
    
    /*
     * Dice Roll
     */
    Image playerDie;
    Image bossDie;
    Label attack;
    
    @Inject public IBattleContainer battleService;
    @Inject public IPlayerContainer playerService;
    
	public BattleUI(AssetManager manager, TiledMapTileSet environment)
	{
		super(manager);
		menu = new DefaultStateMachine<BattleUI>(this);
		this.environment = environment;
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
        
        player = new Image(skin, playerService.getGender());
        player.setSize(64f, 64f);
        player.setPosition(getDisplayWidth(), 24f, Align.bottom);
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
        boss.setPosition(0, getDisplayHeight()-64f, Align.top);
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
	    attack = new Label("", skin, "prompt");
	    
	    bossDie.setPosition(0, 0, Align.topRight);
	    playerDie.setPosition(0, 0, Align.topRight);
	    
	    display.addActor(bossDie);
	    display.addActor(playerDie);
	    display.addActor(attack);
	}
	
	/**
	 * Construct the view for sacrificing items
	 */
	private void makeSacrificeMenu(){
	    
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
                Actions.delay(.7f),
                Actions.addAction(Actions.moveToAligned(getDisplayWidth()/2f, getDisplayHeight(), Align.top, .5f, Interpolation.circleOut), goddess),
                Actions.parallel(
                    Actions.moveToAligned(getDisplayWidth()/2f, getDisplayHeight() - 96f, Align.bottom, .5f, Interpolation.circleOut),
                    Actions.scaleTo(.5f, .5f, .4f)
                ),
                Actions.delay(.4f),
                Actions.moveBy(0f, -500f, .15f),
                Actions.addAction(Actions.moveBy(0, getDisplayHeight(), .5f, Interpolation.circleOut), goddess),
                Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .5f, Interpolation.circleOut), fader),
                Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .5f, Interpolation.circleOut), cloudsPan1),
                Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .5f, Interpolation.circleOut), cloudsPan2),       
                Actions.delay(2f),
                Actions.addAction(
                    Actions.sequence(
                        Actions.sizeTo(128f, 0f),
                        Actions.moveTo(boss.getX(Align.center)-64f, getDisplayHeight()),
                        Actions.parallel(
                            Actions.sizeBy(0, getDisplayHeight() - boss.getY(), .4f, Interpolation.circleOut),
                            Actions.moveBy(0, -(getDisplayHeight() - boss.getY()), .4f, Interpolation.circleOut)
                        ),
                        Actions.delay(.9f),
                        Actions.sizeTo(128f, 0, .4f, Interpolation.circleOut),
                        Actions.run(after)
                        
                    ), 
                    blessingLight
                
                )
            )
        );
	}
	
	protected void playFightAnimation(final boolean playerPhase, int playerRoll, int bossRoll, final Runnable after) {
	    bossDie.setDrawable(getSkin(), "d"+bossRoll);
        playerDie.setDrawable(getSkin(), "d"+playerRoll);
        int hits;
                
        if (playerPhase) {
            playerRoll++;
            hits = playerRoll - bossRoll;
            attack.setText((hits > 0)?(hits > 1)?String.format("You land %d hits!", hits):"You attack for 1 hit":"Your attack is Blocked!");
        } else {
            hits = bossRoll - playerRoll;
            attack.setText((hits > 0)?String.format("The boss strikes with all its might"):"Blocked!");
        }
        attack.setColor(1, 1, 1, 0);
        
        bossDie.setPosition(getDisplayWidth(), boss.getY(Align.top), Align.topLeft);
        playerDie.setPosition(-playerDie.getWidth(), player.getY(Align.top), Align.bottomLeft);
        attack.setPosition(getDisplayWidth()/2f, getDisplayHeight()/2f - 100f, Align.center);
        attack.setAlignment(Align.center);
        
        fader.addAction(
            Actions.sequence(
                Actions.alpha(0f),
                Actions.moveTo(0, 0),
                Actions.alpha(.8f, .3f),
                Actions.addAction(
                    Actions.sequence(
                        Actions.moveTo(getDisplayWidth()*.25f, bossDie.getY(), .3f, Interpolation.circleOut),
                        Actions.moveBy(-20f, 0f, 1.4f),
                        Actions.moveTo(-bossDie.getWidth(), bossDie.getY(), .2f, Interpolation.circleIn)
                    ), 
                    bossDie
                ),
                Actions.addAction(
                    Actions.sequence(
                        Actions.moveToAligned(getDisplayWidth()*.75f, playerDie.getY(), Align.bottomRight, .3f, Interpolation.circleOut),
                        Actions.moveBy(20f, 0f, 1.4f),
                        Actions.moveTo(getDisplayWidth(), playerDie.getY(), .2f, Interpolation.circleIn)
                    ), 
                    playerDie
                ),
                Actions.delay(2f),
                Actions.addAction(
                    Actions.sequence(
                        Actions.alpha(0f),
                        Actions.parallel(
                            Actions.moveBy(0, -30, .2f, Interpolation.sineOut),
                            Actions.alpha(1, .3f)
                        ),
                        Actions.moveBy(0, 130, .3f, Interpolation.sineIn),
                        Actions.delay(.3f),
                        Actions.alpha(0f, .2f),
                        Actions.delay(.2f),
                        Actions.addAction(
                            Actions.sequence(
                                Actions.alpha(0f, .2f),
                                Actions.run(new Runnable(){

                                    @Override
                                    public void run() {
                                        if (playerPhase) {
                                            player.addAction(Actions.sequence(playerAttackAnimation(), Actions.run(after)));
                                        } else {
                                            boss.addAction(Actions.sequence(bossAttackAnimation(), Actions.run(after)));
                                        }
                                    }
                                    
                                })
                            ), fader
                        )
                    ), attack
                )
            )
        );
	}
	
	protected void fight(boolean phase, int hits){
	    //TODO deal damage to entities
	}
	
	protected Action playerAttackAnimation() {
	    return Actions.sequence(
                Actions.scaleTo(-1f, 1f, .25f, Interpolation.sineIn),
                Actions.scaleTo(1f, 1f, .25f, Interpolation.sineIn),
                Actions.addAction(
                    Actions.sequence(
                        Actions.run(new Runnable(){

                            @Override
                            public void run() {
                                audio.playSfx(shared.getResource(DataDirs.hit, Sound.class));
                            }
                            
                        }),
                        Actions.alpha(0f, .1f),
                        Actions.alpha(1f, .1f)
                    ), 
                    boss
                ),
                Actions.delay(.2f)
            );
	}
	
	protected Action bossAttackAnimation() {
	    return Actions.sequence(
                Actions.scaleTo(-1f, 1f, .25f, Interpolation.sineIn),
                Actions.scaleTo(1f, 1f, .25f, Interpolation.sineIn),
                Actions.addAction(
                    Actions.sequence(
                        Actions.run(new Runnable(){

                            @Override
                            public void run() {
                                audio.playSfx(shared.getResource(DataDirs.hit, Sound.class));
                            }
                            
                        }),
                        Actions.alpha(0f, .1f),
                        Actions.alpha(1f, .1f)
                    ), 
                    player
                ),
                Actions.delay(.2f)
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
}
