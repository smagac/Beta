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
    
    /*
     * Goddess animation
     */
    Image goddess;
    Image fader;
    Image sacrifice;
    Image cloudsPan1;
    Image cloudsPan2;
    
    /*
     * Sacrifice menu
     */
    List<String> inventory;
    Label sacrificePrompt;
    Group sacrificePromptWindow;
    Group effectWindow;
    TextButton sacrificeButton;
    
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
	    
	    mainmenu = new CrossMenu(skin, this);
        mainmenu.setPosition(getDisplayWidth()/2f, getDisplayHeight()/2f - 48f);
        
	    display.addActor(mainmenu);
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
            floor.setHeight(getDisplayHeight());
            
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
        player.setPosition(getDisplayWidth(), 32f, Align.bottom);
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
        boss.setSize(128f, 128f);
        boss.setPosition(-64f, getDisplayHeight()-64, Align.top);
        boss.addAction(Actions.moveBy(getDisplayWidth()/2f, 0f, 1f));
        
        
        
        display.addActor(player);
        display.addActor(boss);
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
	    fader = new Image(skin, "fill");
	    fader.setSize(getDisplayWidth(), getDisplayHeight());
	    fader.setPosition(-getDisplayWidth(), 0);
	    display.addActor(fader);
	    
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
	                            Actions.moveTo(getDisplayWidth()-230f, getDisplayHeight()/2f - 64f, .7f, Interpolation.bounceOut)
	                    )
	            )
	    ));
	    
	    sacrifice.addAction(Actions.sequence(
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
                        Actions.moveToAligned(getDisplayWidth()/2f, getDisplayHeight() - 64f, Align.center, .5f, Interpolation.circleOut),
                        Actions.scaleTo(.5f, .5f, .4f)
                ),
                Actions.delay(.4f),
                Actions.moveBy(0f, -500f, .15f),
                        Actions.addAction(Actions.moveBy(0, getDisplayHeight(), .5f, Interpolation.circleOut), goddess),
                        Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .5f, Interpolation.circleOut), fader),
                        Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .5f, Interpolation.circleOut), cloudsPan1),
                        Actions.addAction(Actions.moveTo(0, getDisplayHeight(), .5f, Interpolation.circleOut), cloudsPan2),       
                Actions.delay(2f),
                Actions.scaleTo(3, 3),
                Actions.moveToAligned(getDisplayWidth()/2f, getDisplayHeight() + 300f, Align.bottom),
                Actions.parallel(
                    Actions.moveToAligned(getDisplayWidth()/2f, getDisplayHeight()/2f, Align.center, .6f),
                    Actions.scaleTo(.0f, .0f, .6f)
	            ),
                Actions.delay(.2f),
                Actions.scaleTo(100f, 100f, .5f),
                Actions.alpha(0f, 2f),
                Actions.run(after)
	    ));
	}
	
	protected Action playerAttackAnimation() {
	    return Actions.sequence(
                Actions.scaleTo(-1f, 1f, .18f, Interpolation.sineIn),
                Actions.scaleTo(1f, 1f, .18f, Interpolation.sineIn),
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
                )
            );
	}
	
	protected Action bossAttackAnimation() {
	    return Actions.sequence(
                Actions.scaleTo(-1f, 1f, .18f, Interpolation.sineIn),
                Actions.scaleTo(1f, 1f, .18f, Interpolation.sineIn),
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] defineButtons()
	{
		return null;
	}
}
