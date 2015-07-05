package com.nhydock.storymode.scenes.battle.ui;

import github.nhydock.CollectionUtils;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

import java.util.Iterator;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectIntMap.Keys;
import com.nhydock.gdx.scenes.scene2d.runnables.ChangeText;
import com.nhydock.gdx.scenes.scene2d.runnables.PlaySound;
import com.nhydock.scenes.scene2d.ui.ScrollOnChange;
import com.nhydock.scenes.scene2d.ui.extras.FocusGroup;
import com.nhydock.scenes.scene2d.ui.extras.ItemList;
import com.nhydock.scenes.scene2d.ui.extras.ParticleActor;
import com.nhydock.scenes.scene2d.ui.extras.ScrollFocuser;
import com.nhydock.scenes.scene2d.ui.extras.SimpleWindow;
import com.nhydock.scenes.scene2d.ui.extras.TabbedPane;
import com.nhydock.scenes.scene2d.ui.extras.TableUtils;
import com.nhydock.scenes.scene2d.ui.extras.ParticleActor.ResetParticle;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.common.Input;
import com.nhydock.storymode.components.Identifier;
import com.nhydock.storymode.components.Renderable;
import com.nhydock.storymode.components.Stats;
import com.nhydock.storymode.datatypes.Inventory;
import com.nhydock.storymode.datatypes.Item;
import com.nhydock.storymode.datatypes.StatModifier;
import com.nhydock.storymode.datatypes.dungeon.Dungeon;
import com.nhydock.storymode.factories.AdjectiveFactory;
import com.nhydock.storymode.scenes.GameUI;
import com.nhydock.storymode.scenes.Messages;
import com.nhydock.storymode.scenes.Scene;
import com.nhydock.storymode.scenes.Messages.Battle.VictoryResults;
import com.nhydock.storymode.scenes.battle.ui.CombatHandler.Combatant;
import com.nhydock.storymode.scenes.battle.ui.CombatHandler.Turn;
import com.nhydock.storymode.service.implementations.PageFile;
import com.nhydock.storymode.service.interfaces.IBattleContainer;
import com.nhydock.storymode.service.interfaces.IDungeonContainer;
import com.nhydock.storymode.service.interfaces.IPlayerContainer;
import com.nhydock.storymode.util.dungeon.TsxTileSet;

public class BattleUI extends GameUI
{
    static final String[] stats = {"%s%% HP", "%s%% Str", "%s%% Def", "%s%% Spd"};
    
    /*
     * Primary view elements
     */
    Image player;
    Image boss;
    private Image bg;
    
    /*
     * Main menu
     */
    CrossMenu mainmenu;
    
    /*
     * Goddess animation
     */
    Image goddess;
    Image fader;
    Image glow;
    Image cloudsPan1;
    Image cloudsPan2;
    Image blessingLight;
    Group sacrificeAnimation;
    
    /*
     * Sacrifice menu
     */
    Label sacrificePrompt;
    SimpleWindow sacrificePromptWindow;
    Window effectWindow;
    TextButton sacrificeButton;
    Table itemStatPane;
    Array<Label> itemStatLabels;
    
    /*
     * Heal Sacrifice menu 
     */
    ItemList lootList;
    ItemList sacrificeList;
    
    TabbedPane lootPane;
    TabbedPane sacrificePane;
    private FocusGroup sacrificeGroup;
    private ScrollOnChange lootPaneScroller;
    private ScrollOnChange sacrificePaneScroller;
    private ChangeListener updateStatWindow = new ChangeListener(){
        @Override
        public void changed(ChangeEvent event, Actor actor) {
            if (stateMachine.getCurrentState() != CombatStates.MODIFY)
                return;
            
            Item selectedItem = lootList.getSelectedItem();
            if (selectedItem == null) {
                return;
            }
            
            PageFile pf = ServiceManager.getService(PageFile.class);
                        
            // update the modifier pane with info of the adjective associated with the item
            String adj = selectedItem.descriptor();
            StatModifier sm = AdjectiveFactory.getModifier(adj);
            Label l = itemStatLabels.get(0);
            l.clearActions();
            l.addAction(
                Actions.sequence(
                    Actions.parallel(
                        Actions.alpha(0f, .2f)
                    ),
                    Actions.run(new ChangeText(l, adj)),
                    Actions.parallel(
                        Actions.alpha(1f, .2f)
                    )
                )
            );
            float[] mStats = {sm.hp, sm.str, sm.def, sm.spd};
            for (int i = 1, n = 0; i <= 4; i++, n++) {
                l = itemStatLabels.get(i);
                l.clearActions();
                ChangeText text;
                if (pf.hasUnlocked(adj)) {
                    text = new ChangeText(l, String.format(stats[n], String.valueOf((int)(mStats[n]*100)-100)));
                } else {
                    text = new ChangeText(l, String.format(stats[n], "???"));
                }
                l.addAction(
                    Actions.sequence(
                        Actions.delay(.05f + (n * .05f)),
                        Actions.parallel(
                            Actions.alpha(0f, .2f)
                        ),
                        Actions.run(text),
                        Actions.delay(.05f + (n * .05f)),
                        Actions.parallel(
                            Actions.alpha(1f, .2f)
                        )
                    )
                );
            }
        }
    };
    
    
    
    /*
     * Dice Roll
     */
    Image playerDie;
    Image bossDie;
    
    @Inject public IBattleContainer battleService;
    @Inject public IPlayerContainer playerService;
    
    CombatHandler combat;
    
    ParticleActor hitSplash;
    ParticleActor death;
    
    Timeline timeline;
    FocusGroup manualFocus;
    FocusGroup mainFocus;
    FocusGroup itemFocus;
    private Image hpBar;
    private Label bossName;
    private Group attackGroup;

    Group dialog;
    
    @Override
    protected void listenTo(IntSet messages)
    {
        super.listenTo(messages);
        messages.addAll(Messages.Battle.VICTORY, Messages.Battle.DEFEAT, Messages.Battle.Stats, Messages.Battle.DEBUFF);
    }
	
    public BattleUI(Scene scene, AssetManager manager)
	{
		super(scene, manager);
		stateMachine = new DefaultStateMachine<BattleUI>(this);
		combat = new CombatHandler();
	}
	
	@Override
	protected void extend()
	{
	    makeField();
	    
        fader = new Image(skin, "fill");
        fader.setSize(getDisplayWidth(), getDisplayHeight());
        fader.setPosition(-getDisplayWidth(), 0);
        fader.setTouchable(Touchable.disabled);
        display.addActor(fader);
        
        makeAttackElements();
        makeSacrificeMenu();
	    makeHealSacrificeMenu();
	    makeSacrificeScene();
	    makeBossStats();
        makeDead();
        
	    mainmenu = new CrossMenu(skin, this);
        mainmenu.setPosition(getDisplayWidth(), getDisplayHeight()/2f);
        
        display.addActor(mainmenu);
        
        mainFocus = new FocusGroup(mainmenu);
        manualFocus = new FocusGroup(timeline);
        itemFocus = new FocusGroup(lootList.getList());
        sacrificeGroup = new FocusGroup(lootList.getList(), sacrificeList.getList(), sacrificeButton);
        sacrificeGroup.addListener(focusListener);
        
        
        lootPane.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (Input.CANCEL.match(keycode)) {
                    MessageManager.getInstance().dispatchMessage(null, Messages.Interface.Close);
                    return true;
                }
                return false;
            }
        });
        
        lootPane.addListener(new InputListener(){
           @Override
            public boolean keyDown(InputEvent event, int keycode) {
                   if (!lootList.isEmpty()) {
                       return false;
                   }
    
                   if (stateMachine.getCurrentState() == CombatStates.MODIFY){
                       if (Input.ACCEPT.match(keycode)) {
                           MessageManager.getInstance().dispatchMessage(null, Messages.Interface.Button);
                           return true;
                       }
                   }
                   else if (stateMachine.getCurrentState() == CombatStates.Heal) {
                       if (Input.ACCEPT.match(keycode)) {
                           lootList.swap();
                           return true;
                       }
                       
                   }
                   
                   return false;
            } 
        });
	}
	
    private void makeDead() {
        dialog = new SimpleWindow(skin, "square");
        dialog.setSize(400, 250);
        
        Label message = new Label("You are dead.\n\nYou have dropped all your new loot.\nSucks to be you.", skin, "promptsm");
        message.setWrap(true);
        message.setAlignment(Align.center);
        message.setPosition(40f, 40f);
        message.setWidth(320f);
        message.setHeight(170f);
        dialog.addActor(message);
        dialog.setColor(1,1,1,0);
        dialog.setTouchable(Touchable.disabled);

        display.addActor(dialog);

    }

    private void makeHealSacrificeMenu() {
        // loot List and buttons
        lootList = new ItemList(skin);
        lootList.setItems(playerService.getInventory().getLoot());
        lootList.getList().addListener(updateStatWindow);
        
        ScrollPane p = new ScrollPane(lootList.getList(), skin);
        p.setScrollingDisabled(true, false);
        p.setScrollbarsOnTop(false);
        p.setScrollBarPositions(false, false);
        p.setFadeScrollBars(false);
        p.addListener(new ScrollFocuser(p));
        lootPaneScroller = new ScrollOnChange(p);
        
        ButtonGroup<Button> tabs = new ButtonGroup<Button>();
        TextButton tab = new TextButton("Inventory", skin, "tab");
        tab.setUserObject(p);
        tabs.add(tab);
        lootPane = new TabbedPane(tabs, false);
        lootPane.setSize(280f, getDisplayHeight() - 80f);
        lootPane.setPosition(getDisplayWidth(), getDisplayHeight(), Align.topLeft);
        
        display.addActor(lootPane);
        
        sacrificeList = new ItemList(skin);
        
        p = new ScrollPane(sacrificeList.getList(), skin);
        p.setScrollingDisabled(true, false);
        p.setScrollbarsOnTop(false);
        p.setScrollBarPositions(true, false);
        p.setFadeScrollBars(false);
        p.addListener(new ScrollFocuser(p));
        
        tabs = new ButtonGroup<Button>();
        tab = new TextButton("Sacrifices", skin);
        tab.setUserObject(p);
        tabs.add(tab);
        sacrificePane = new TabbedPane(tabs, false);
        sacrificePane.setSize(280f, 150f);
        sacrificePane.setPosition(-280f, 60f);
        
        display.addActor(sacrificePane);
        
        sacrificePaneScroller = new ScrollOnChange(p);
        
        display.addActor(lootPane);
        display.addActor(sacrificePane);
    }
    
    /**
	 * Make the boss stat display for the bottom right of the screen
	 */
	private void makeBossStats() {
        messageWindow.clear();

        //add the wrap around graphic
        Image decor = new Image(skin, "enemystats");
        messageWindow.addActor(decor);
        
        //add hp bar
        hpBar = new Image(skin, "wfill");
        hpBar.setPosition(34, 10);
        hpBar.setSize(258, 4);
        messageWindow.addActor(hpBar);
        
        //add name
        bossName = new Label(battleService.getBoss().getComponent(Identifier.class).toString(), skin, "promptsm");
        bossName.setPosition(5, 24);
        bossName.setWrap(false);
        bossName.setEllipsis(true);
        bossName.setWidth(messageWindow.getWidth() - 30f);
        messageWindow.addActor(bossName);
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
	    Dungeon dungeon = ServiceManager.getService(IDungeonContainer.class).getDungeon();
	    String type = dungeon.getEnvironment();
	    TextureRegion tex = new TextureRegion(new Texture(Gdx.files.internal(DataDirs.Backgrounds + type + ".png")));
	    bg = new Image(new TiledDrawable(tex));
	    bg.setWidth(getDisplayWidth());
	    bg.setHeight(128f);
	    bg.setPosition(0, getDisplayHeight(), Align.topLeft);
	    display.addActor(bg);
	    
	    for (float i = 0, op = 1f, height = 16, y = getDisplayHeight()-128f-height, rows = 3; 
	            i < rows; 
	            i++, op -= 1/rows, height *= 2f, y -= height) {
	        TiledDrawable tile = new TiledDrawable(dungeon.getTileset().getTile(TsxTileSet.FLOOR).getTextureRegion());
	        if (height < 32f) {
	            tile.setRepeat(true, false);
	        } else {
	            tile.setRepeat(true, true);
	        }
	        Image row = new Image(tile);
	        row.setWidth(getDisplayWidth() + 32f);
	        row.setX(MathUtils.random(-32f, 0));
	        row.setHeight(height);
	        row.setColor(1, 1, 1, op);
	        row.setY(y);
	        display.addActor(row);
	    }
	    
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
        

        ParticleEffect pe = new ParticleEffect();
        pe.load(Gdx.files.internal(DataDirs.Particles + "splash.particle"), Gdx.files.internal(DataDirs.Home));
        hitSplash = new ParticleActor(pe);
        
        display.addActor(hitSplash);

        pe = new ParticleEffect();
        pe.load(Gdx.files.internal(DataDirs.Particles + "fire.particle"), Gdx.files.internal(DataDirs.Home));
        death = new ParticleActor(pe);
        
        display.addActor(death);  
    }

	/**
	 * Constructs the elements required for the attack phase of the battle
	 */
	private void makeAttackElements() {
	    attackGroup = new Group();
	    bossDie = new Image(skin, "d1");
	    playerDie = new Image(skin, "d2");
	    
	    bossDie.setPosition(0, 0, Align.topRight);
	    playerDie.setPosition(0, 0, Align.topRight);
	    
	    attackGroup.addActor(bossDie);
	    attackGroup.addActor(playerDie);
	    
	    timeline = new Timeline();
	    timeline.setSize(getDisplayWidth() - 20f, getDisplayHeight() - 20f);
	    timeline.setPosition(10f, 10f);
	    display.addActor(timeline);
	    
	    attackGroup.setTouchable(Touchable.disabled);
	    display.addActor(attackGroup);
	}
	
	/**
	 * Construct the view for sacrificing items
	 */
	private void makeSacrificeMenu(){
	    
	    sacrificePrompt = new Label("", skin, "promptsm");
	    sacrificePrompt.setWidth(304f);
	    sacrificePrompt.setPosition(48f, 64f);
	    sacrificePrompt.setWrap(true);
	    
	    sacrificePromptWindow = new SimpleWindow(skin, "square");
	    sacrificePromptWindow.setSize(400, 130);
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
	                    MessageManager.getInstance().dispatchMessage(null, Messages.Interface.Button);
	                    return true;
                    }
                    return false;
                }
            }
        );
	    display.addActor(sacrificeButton);
	    
	    itemStatPane = new Table();
	    itemStatPane.setBackground(skin.getDrawable("button_up"));
	    itemStatPane.setWidth(250f);
	    itemStatPane.setHeight(120f);
	    itemStatPane.setPosition(-280f, 40f);
	    itemStatPane.pad(10f).row().top();
	    
	    itemStatLabels = new Array<Label>();
	    Label name = new Label("", skin, "promptsm");
	    itemStatPane.add(name).align(Align.left).expandX();
	    itemStatPane.row();
	    itemStatLabels.add(name);
	    for (String s : stats) {
	        Label l = new Label(String.format(s, 0), skin, "promptsm");
	        itemStatPane.add(l).align(Align.right).expandX();
	        itemStatPane.row();
	        itemStatLabels.add(l);
	        l.setColor(1, 1, 1, 0);
	    }
	    
	    display.addActor(itemStatPane);
        
	}
	
	/**
	 * Construct the elements needed for the sacrifice animation
	 */
	private void makeSacrificeScene(){
	    sacrificeAnimation = new Group();
	    
	    cloudsPan1 = new Image(new TiledDrawable(skin.getRegion("clouds")));
	    cloudsPan2 = new Image(new TiledDrawable(skin.getRegion("clouds")));
        cloudsPan1.setWidth(getDisplayWidth()*2);
        cloudsPan2.setWidth(getDisplayWidth()*2);
        
        cloudsPan1.setPosition(0, 0, Align.topLeft);
        cloudsPan2.setPosition(0, 0, Align.topLeft);
        sacrificeAnimation.addActor(cloudsPan1);
        sacrificeAnimation.addActor(cloudsPan2);
	    
	    goddess = new Image(skin, playerService.getWorship());
	    goddess.setSize(128, 128);
	    goddess.setPosition(-256f, -256f);
	    goddess.setOrigin(Align.center);
	    sacrificeAnimation.addActor(goddess);
	    
	    glow = new Image(skin, "sacrifice");
	    glow.setSize(64f, 64f);
	    glow.setPosition(-64, -64);
	    glow.setOrigin(Align.center);
	    sacrificeAnimation.addActor(glow);
	    
	    blessingLight = new Image(new TiledDrawable(skin.getRegion("blessing")));
	    blessingLight.setSize(128f, 0f);
	    display.addActorBefore(boss, blessingLight);
	    
	    sacrificeAnimation.setTouchable(Touchable.disabled);
	    display.addActor(sacrificeAnimation);
	}
	
	/**
	 * Execute the animation sequence for sacrificing an item
	 */
	protected void playSacrificeAnimation(Actor target, Runnable after) {
	    
	    fader.clearActions();
	    cloudsPan1.clearActions();
	    cloudsPan2.clearActions();
	    goddess.clearActions();
	    glow.clearActions();
	    
	    fader.addAction(Actions.sequence(
	            Actions.run(getScene().getInput().disableMe),
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
	    
	    glow.addAction(
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
                        Actions.moveTo(target.getX(Align.center)-96f, getDisplayHeight()),
                        Actions.parallel(
                            Actions.sizeBy(0, getDisplayHeight() - target.getY(), .4f, Interpolation.circleOut),
                            Actions.moveBy(0, -(getDisplayHeight() - target.getY()), .4f, Interpolation.circleOut)
                        ),
                        Actions.delay(.9f),
                        Actions.sizeTo(192f, 0, .4f, Interpolation.circleOut),
                        Actions.run(after),
                        Actions.run(getScene().getInput().enableMe)
                    ), 
                    blessingLight
                
                )
            )
        );
	}
	
	protected void playFightAnimation(final Turn turn, final Runnable after) {
	    playRollAnimation(turn,
            Actions.sequence(
                Actions.run(getScene().getInput().disableMe),
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
                Actions.run(after),
                Actions.run(getScene().getInput().enableMe)
            )
        );
	}
	
	private Action hitAnimation(){
	    return hitAnimation(CollectionUtils.randomChoice(DataDirs.Sounds.hit, DataDirs.Sounds.hit2));
	}
	
	private Action hitAnimation(String fx) {
	    return Actions.sequence(
                    Actions.run(new PlaySound(fx)),
                    Actions.alpha(0f, .1f),
                    Actions.run(new ResetParticle(hitSplash)),
                    Actions.alpha(1f, .1f)
                );
	}
	
	protected void playerAttackAnimation() {
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
	
	protected void bossAttackAnimation() {
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
                    
                    addAction(
                        Actions.sequence(
                            Actions.run(getScene().getInput().disableMe),
                            Actions.delay(.2f), 
                            Actions.run(after),
                            Actions.run(getScene().getInput().enableMe)
                        )
                    );
                }
            })
        );
    }
	
	/**
	 * Show a dice roll
	 * @param turn
	 */
	protected void playRollAnimation(Turn turn, Action after){
	    fader.clearActions();
	    bossDie.clearActions();
	    playerDie.clearActions();
	    
	    bossDie.setDrawable(getSkin(), "d"+turn.bossRoll);
        playerDie.setDrawable(getSkin(), "d"+turn.playerRoll);
        
        bossDie.setPosition(getDisplayWidth(), boss.getY(Align.top), Align.topLeft);
        playerDie.setPosition(-playerDie.getWidth(), player.getY(Align.top), Align.bottomLeft);
        
        fader.addAction(
            Actions.sequence(
                Actions.run(getScene().getInput().disableMe),
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
                Actions.run(getScene().getInput().enableMe),
                after
            )
        );
	}
	
	/**
	 * 
	 * @param t
	 */
	protected void buildTimeline(Turn turn, Runnable after)
	{
	    timeline.set(turn);
	    
	    fader.clearActions();
	    fader.addAction(
            Actions.sequence(
                Actions.alpha(0f),
                Actions.alpha(.8f, .2f),
                Actions.run(getScene().getInput().enableMe),
                Actions.run(new Runnable(){

                    @Override
                    public void run() {
                        timeline.start();
                    }
                    
                }),
                Actions.delay(timeline.duration),
                Actions.alpha(0f, .2f),
                
                Actions.run(new Runnable() {
                    
                    @Override
                    public void run() {
                        combat.fightManual(timeline.playerHits, timeline.bossHits);
                    }
                }),
                Actions.delay(1.5f),
                Actions.run(getScene().getInput().disableMe),
                Actions.run(after)
            )
        );
	}
	
	/**
	 * Show the animation that plays when you win.
	 * Should show the rewards for winning as well
	 * @param reward - bonus item rewarded for winning
	 * @param amount - amount of bonus item rewarded
	 */
	protected void playVictoryAnimation(VictoryResults results){
	    Label congrats = new Label("Victory", skin, "prompt");
        congrats.setFontScale(2f);
        congrats.setPosition(getDisplayCenterX(), getDisplayCenterY() - 20f, Align.center);
        congrats.setColor(1,1,1,0);
        congrats.setAlignment(Align.center);
        display.addActor(congrats);

        String result;
        result = String.format("Obtained 1 %s\nBonus %d %s", results.reward, results.bonusCount, results.bonus);
        Label drops = new Label(result, skin, "prompt");
        drops.setPosition(getDisplayCenterX(), getDisplayCenterY() - 70f, Align.center);
        drops.setColor(1,1,1,0);
        drops.setAlignment(Align.center);
        display.addActor(drops);
        
	    audio.fadeOut();
	    death.setPosition(boss.getX(), boss.getY());
	    death.addAction(
            Actions.sequence(
                Actions.run(new ParticleActor.ResetParticle(death)),
                Actions.delay(6f),
                Actions.run(new ParticleActor.StopParticle(death)),
                Actions.delay(1f),
                Actions.addAction(Actions.moveBy(0, 128f, 1f), bg),
                Actions.addAction(Actions.moveBy(0, 210f, 2f), player),
                Actions.addAction(
                    Actions.sequence(
                        Actions.moveBy(0f, 20f),
                        Actions.delay(2f),
                        Actions.parallel(
                            Actions.alpha(1f, .3f),
                            Actions.moveBy(0f, -20, .3f)
                        )
                    ), congrats
                ),
                Actions.addAction(
                    Actions.sequence(
                        Actions.moveBy(0f, 20f),
                        Actions.delay(3f),
                        Actions.parallel(
                            Actions.alpha(1f, .3f),
                            Actions.moveBy(0f, -20f, .3f)
                        )
                    ), drops
                )
            )
        );
	    boss.addAction(
            Actions.sequence(
                Actions.parallel(
                    Actions.repeat(30,
                        Actions.sequence(
                            Actions.moveBy(-10f, 0, .05f),
                            Actions.moveBy(20f, 0, .1f),
                            Actions.moveBy(-10f, 0, .05f)
                        )
                    ),
                    Actions.repeat(30, 
                        Actions.sequence(
                            Actions.run(new PlaySound(DataDirs.Sounds.explode)),
                            Actions.delay(.2f)
                        )
                    ),
                    Actions.alpha(0f, 6f)
                ),
                Actions.removeActor()
            )
        );
	    
	}
	
	@Override
	public boolean handleMessage(Telegram msg) {
	    if (msg.message == Messages.Battle.VICTORY) {
	        if (stateMachine.getCurrentState() == CombatStates.DEAD) {
	            return false;
	        }
	        changeState(CombatStates.VICTORY);
	        
	        VictoryResults results = (VictoryResults)msg.extraInfo;

            Inventory inv = playerService.getInventory();
            if (results.reward == Item.Placeholder) {
                results.reward = ServiceManager.getService(IDungeonContainer.class).getDungeon().getItemFactory().createItem();
            }
            
            inv.pickup(results.reward);
            inv.pickup(results.bonus, results.bonusCount);
            
	        //TODO show victory message
            playVictoryAnimation(results);
            return true;
	    } else if (msg.message == Messages.Battle.DEFEAT){
	        getRoot().clearActions();
	        changeState(CombatStates.DEAD);
	        //TODO fade out screen and return home
	        return true;
	    } else if (msg.message == Messages.Battle.Stats) {
	        Stats s = battleService.getBoss().getComponent(Stats.class);
	        int hp = s.hp;
	        int mhp = s.maxhp;
	        
	        //update enemy stats
	        hpBar.clearActions();
	        hpBar.addAction(
                Actions.sequence(
                    Actions.alpha(0f, .1f),
                    Actions.alpha(1f, .1f),
                    Actions.sizeTo(((float)hp /(float)mhp) * 258f, 6f, .5f, Interpolation.sine)
                )
            );
	        Identifier id = Identifier.Map.get(battleService.getBoss());
	        bossName.setText(id.toString());
	        
	        return true;
	    } else if (msg.message == Messages.Battle.DEBUFF) {
	        String adj = (String)msg.extraInfo;
	        pushNotification(String.format("The effects of %s have worn off", adj));
	        return true;
	    }
	    return super.handleMessage(msg);
	}

	@Override
	protected FocusGroup focusList()
	{
	    //all focusable things in this ui don't have the pointer on them
	    if (stateMachine.getCurrentState() == CombatStates.Heal) {
            return sacrificeGroup;
        }
        pointer.setVisible(false);
	    if (stateMachine.getCurrentState() == CombatStates.MANUAL) {
	        return manualFocus;
	    }
	    if (stateMachine.getCurrentState() == CombatStates.MODIFY) {
	        return itemFocus;
	    }
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

	private class HitBox {
        /**
         * Maximum amount of seconds between hits
         */
        private static final float HIT_DELAY_MAX = .7f;
        /**
         * Minimum amount of seconds between hits
         */
        private static final float HIT_DELAY_MIN = .3f;
        
        /**
         * Hit window that you have for safely hitting a box
         */
        private static final float HIT_WINDOW = .2f;
        
        private static final float ANIM_DURATION = 1.0f;
        
        final Actor hit;
	    final Actor holder;
        
        final float spawnTime;
        final Combatant who;
        final float when;
        
        HitBox(float s, Actor a, Actor b, Combatant c) {
            hit = a;
            holder = b;
            spawnTime = s;
            who = c;
            when = spawnTime + 1.0f;
        }
        
        boolean hit(float time){
            return (time >= when - HIT_WINDOW && 
                time <= when + HIT_WINDOW);
        }
        
        void success(){
            audio.playSfx(DataDirs.Sounds.accept);
            if (who == Combatant.Player) {
                playerAttackAnimation();
            } else {
                player.addAction(hitAnimation(DataDirs.Sounds.deflect));
            }
            hit.setRotation(0);
            hit.setScale(1);
            hit.setColor(1,1,1,1);
        }
        
        void fail(){
            audio.playSfx(DataDirs.Sounds.tick);
            if (who == Combatant.Enemy) {
                bossAttackAnimation();
            }
        }
        
        void end(){
            hit.clearActions();
            holder.clearActions();
            
            hit.addAction(
                Actions.sequence(
                    Actions.alpha(0f, .2f),
                    Actions.removeActor()
                )
            );
            holder.addAction(
                Actions.sequence(
                    Actions.parallel(
                        Actions.scaleTo(.4f, .4f, .2f),
                        Actions.alpha(0f, .2f)
                    ),
                    Actions.delay(.2f),
                    Actions.removeActor()
                )
            );
        }
    }

	/**
	 * timelines are used for manually attacking
	 * @author nhydock
	 *
	 */
    private class Timeline extends Group {
        
        HitBox head;
        
        Array<HitBox> hitpoints;
        Iterator<HitBox> hitpointIter;
        
        float time;
        float duration;
        
        /*
         * Accumulated dealt hits by each side
         */
        int playerHits;
        int bossHits;
        
        InputListener touchBox = new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (time < head.spawnTime){
                    return false;
                }
                
                //ignore when the head isn't clicked
                if (event.getTarget() != head.holder) {
                    return false;
                }
                
                if (head.hit(time)) {
                    head.success();
                    if (head.who == Combatant.Player){
                        playerHits++;
                    }
                } else {
                    head.fail();
                    if (head.who == Combatant.Enemy) {
                        bossHits++;
                    }
                }
                head.end();
                if (hitpointIter.hasNext()) {
                    head = hitpointIter.next();
                }
                return true;
            }
        };
        
        Timeline(){
            super();
            hitpoints = new Array<HitBox>();
            
            addListener(new InputListener(){
                
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    if (head != null) {
                        if (time < head.spawnTime){
                            return false;
                        }
                        if (Input.ACCEPT.match(keycode) || Input.CANCEL.match(keycode)) {
                            if (head.hit(time) && 
                                ((Input.ACCEPT.match(keycode) && head.who == Combatant.Player) ||
                                 (Input.CANCEL.match(keycode) && head.who == Combatant.Enemy))) {
                                head.success();
                                if (head.who == Combatant.Player){
                                    playerHits++;
                                }
                            } else {
                                head.fail();
                                if (head.who == Combatant.Enemy) {
                                    bossHits++;
                                }
                            }
                            head.end();
                            if (hitpointIter.hasNext()) {
                                head = hitpointIter.next();
                            }
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        
        public void set(Turn t) {
            hitpoints.clear();
            
            playerHits = 0;
            bossHits = 0;
            
            int eHits = t.bossRoll;
            int pHits = t.playerRoll;
            int total = eHits + pHits;
            
            float delay = 0;
            duration = 0;
            
            while (total > 0) {
                Combatant c;
                if (eHits > 0) {
                    if (pHits > 0) {
                        c = MathUtils.randomBoolean() ? Combatant.Enemy : Combatant.Player;
                    } else {
                        c = Combatant.Enemy;
                    }
                } else {
                    c = Combatant.Player;
                }
                
                if (c == Combatant.Enemy) {
                    eHits--;
                } else {
                    pHits--;
                }
                
                
                delay += MathUtils.random(HitBox.HIT_DELAY_MIN, HitBox.HIT_DELAY_MAX);
                
                Actor holder = new Image(skin, (c == Combatant.Player) ? "hitbox" : "hittri");
                holder.setPosition(MathUtils.random(64f, getWidth() - 64f), MathUtils.random(64f, getHeight()-64f));
                holder.setOrigin(Align.center);
                holder.setScale(1f);
                holder.setColor(1,1,1,0f);
                addActor(holder);
                 
                
                Actor hit = new Image(skin, (c == Combatant.Player) ? "hitbox" : "hittri");
                hit.setPosition(holder.getX(), holder.getY());
                hit.setOrigin(Align.center);
                hit.setScale(10f);
                hit.setColor(1,1,1,0f);
                hit.setRotation(360f);
                hit.setTouchable(Touchable.disabled);
                final HitBox hb = new HitBox(delay, hit, holder, c);
                
                hit.addAction(
                    Actions.sequence(
                        Actions.delay(hb.spawnTime),
                        Actions.parallel(
                            Actions.alpha(1f, HitBox.ANIM_DURATION * .5f),
                            Actions.rotateTo(0, HitBox.ANIM_DURATION),
                            Actions.scaleTo(1, 1, HitBox.ANIM_DURATION)
                        ),
                        Actions.alpha(0f, HitBox.HIT_WINDOW),
                        Actions.run(new Runnable(){

                            @Override
                            public void run() {
                                if (hb.who == Combatant.Enemy){
                                    bossHits++;
                                    bossAttackAnimation();
                                }
                            }
                            
                        }),
                        Actions.removeActor()
                    )
                );
                
                holder.addAction(
                    Actions.sequence(
                        Actions.delay(hb.spawnTime),
                        Actions.alpha(.5f, .15f),
                        Actions.delay(HitBox.ANIM_DURATION),
                        Actions.alpha(0f, HitBox.HIT_WINDOW),
                        Actions.removeActor()
                    )
                );
                holder.addListener(touchBox);
                
                addActor(holder);
                addActor(hit);
                
                hitpoints.add(hb);
                
                total--;
                
            }
            
            duration = hitpoints.peek().when + HitBox.HIT_WINDOW + .2f;
            head = null;
        }
        
        public void start() {
            
            hitpointIter = new Array.ArrayIterator<HitBox>(hitpoints);
            head = hitpointIter.next();
            time = 0;
        }
        
        @Override
        public void act(float delta) {
            super.act(delta);
            if (head != null) {
                this.time += delta;
                if (this.time > head.when + HitBox.HIT_WINDOW) {
                    if (hitpointIter.hasNext()) {
                        head = hitpointIter.next();                    
                    } else {
                        head = null;
                    }
                }
            }
        }
    }
}
