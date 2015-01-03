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
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;

import core.DataDirs;
import core.components.Renderable;
import core.service.interfaces.IBattleContainer;
import scene2d.ui.extras.FocusGroup;
import scenes.GameUI;

public class BattleUI extends GameUI
{

    Image player;
    Image boss;
    Texture environment;
    
    CrossMenu mainmenu;
    
    @Inject public IBattleContainer battleService;
    
    Action bossAttackAnimation;
    Action playerAttackAnimation;
    
    StateMachine<BattleUI> sm;
     
	public BattleUI(AssetManager manager, Texture environment)
	{
		super(manager);
		sm = new DefaultStateMachine<BattleUI>(this);
		this.environment = environment;
		
	}
	
	@Override
	public void load() {
	    super.load();
	    
	    manager.load(DataDirs.Home + "battle.json", Skin.class);
	}

	@Override
	protected void extend()
	{
	    Skin battleSkin = manager.get(DataDirs.Home + "battle.json", Skin.class);
	    
	    Image bg = new Image(new TiledDrawable(new TextureRegion(this.environment)));
	    bg.setWidth(getDisplayWidth());
	    bg.setPosition(0, 0);
	    display.addActor(bg);
	    
	    player = new Image(skin, playerService.getGender());
	    Entity bossEntity = battleService.getBoss();
	    Renderable rC = bossEntity.getComponent(Renderable.class);
	    String sName = rC.getSpriteName();
	    boss = new Image(skin, sName);
	    
	    player.setSize(32f, 32f);
	    player.setPosition(getDisplayWidth() + 32f, 0f);
	    player.addAction(Actions.moveTo(getDisplayCenterX() - 16f, player.getY(), 1f));
        
	    boss.setSize(128f, 128f);
	    boss.setPosition(-128f, getDisplayHeight()-128f);
	    boss.addAction(Actions.moveTo(getDisplayCenterX(), boss.getY()));
	    
	    player.addAction(Actions.delay(.5f));
	    player.addAction(Actions.run(new Runnable(){

            @Override
            public void run() {
                sm.changeState(CombatStates.MAIN);
            }
	        
	    }));
	    
	    display.addActor(player);
	    display.addActor(boss);
	    
	    mainmenu = new CrossMenu(battleSkin, this);
        mainmenu.setPosition(getDisplayWidth()/2f, getDisplayHeight()/2f);
        
	    display.addActor(mainmenu);
        
	    playerAttackAnimation = 
	            Actions.sequence(
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
	    bossAttackAnimation = 
                Actions.sequence(
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
	public void preRender() {
	    SpriteBatch batch = (SpriteBatch) getBatch();
	    
	    batch.begin();
	    batch.draw(environment, display.getX(), display.getY(), 32f, 128f);
	    batch.end();
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

	@Override
	public boolean handleMessage(Telegram msg) {
	    return sm.handleMessage(msg);
	}
}
