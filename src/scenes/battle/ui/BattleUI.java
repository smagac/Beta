package scenes.battle.ui;

import java.util.Iterator;

import github.nhydock.CollectionUtils;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
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
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectIntMap.Keys;

import core.DataDirs;
import core.common.Input;
import core.components.Combat;
import core.components.Identifier;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Item;
import core.datatypes.StatModifier;
import core.factories.AdjectiveFactory;
import core.service.interfaces.IBattleContainer;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;
import core.util.dungeon.TsxTileSet;
import scene2d.ChangeText;
import scene2d.InputDisabler;
import scene2d.PlaySound;
import scene2d.ui.ScrollOnChange;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ParticleActor;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.TabbedPane;
import scene2d.ui.extras.TableUtils;
import scene2d.ui.extras.ParticleActor.ResetParticle;
import scenes.GameUI;
import scenes.Messages;
import scenes.Messages.Battle.VictoryResults;
import scenes.battle.ui.CombatHandler.Combatant;
import scenes.battle.ui.CombatHandler.Turn;

public class BattleUI extends GameUI
{
    static final String[] stats = {"%d%% HP", "%d%% Str", "%d%% Def", "%d%% Spd"};
    
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
    Image glow;
    Image cloudsPan1;
    Image cloudsPan2;
    Image blessingLight;
    Group sacrificeAnimation;
    
    /*
     * Sacrifice menu
     */
    Label sacrificePrompt;
    Group sacrificePromptWindow;
    Group effectWindow;
    TextButton sacrificeButton;
    Table itemStatPane;
    Array<Label> itemStatLabels;
    
    /*
     * Heal Sacrifice menu 
     */
    private Table lootList;
    private ObjectIntMap<Item> loot;
    ButtonGroup<Button> lootButtons;
    private Table sacrificeList;
    private ObjectIntMap<Item> sacrifices;
    private ButtonGroup<Button> sacrificeButtons;
    private Array<Item> lootRows;
    private Array<Item> sacrificeRows;
    
    Item selectedItem;
    TabbedPane lootPane;
    TabbedPane sacrificePane;
    private FocusGroup sacrificeGroup;
    private ScrollOnChange lootPaneScroller;
    private ScrollOnChange sacrificePaneScroller;
    private ChangeListener updateStatWindow = new ChangeListener(){
        @Override
        public void changed(ChangeEvent event, Actor actor) {
            if (menu.getCurrentState() != CombatStates.MODIFY)
                return;
            
            if (!(actor instanceof Button)) {
                return;
            }
            
            Button self = (Button)actor;
            if (!lootButtons.getButtons().contains(self, true) || !self.isChecked()){
                return;
            }
            
            
            // update the modifier pane with info of the adjective associated with the item
            int index = lootButtons.getCheckedIndex();
            selectedItem = lootRows.get(index);
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
            for (int i = 1, n = 0; i <= 4; i++, n++) {
                l = itemStatLabels.get(i);
                l.clearActions();
                l.addAction(
                    Actions.sequence(
                        Actions.delay(.05f + (n * .05f)),
                        Actions.parallel(
                            Actions.alpha(0f, .2f)
                        ),
                        Actions.run(new ChangeText(l, String.format(stats[n], (int)(sm.hp*100)-100))),
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
    private Image bg;
    private Image hpBar;
    private Group attackGroup;
    
    @Override
    protected void listenTo(IntSet messages)
    {
        super.listenTo(messages);
        messages.addAll(Messages.Battle.VICTORY, Messages.Battle.DEFEAT, Messages.Battle.Stats);
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
	    
	    
        fader = new Image(skin, "fill");
        fader.setSize(getDisplayWidth(), getDisplayHeight());
        fader.setPosition(-getDisplayWidth(), 0);
        display.addActor(fader);
        fader.setTouchable(Touchable.disabled);
	    
	    makeSacrificeMenu();
	    makeHealSacrificeMenu();
	    makeSacrificeScene();
	    makeAttackElements();
	    makeBossStats();
        
	    mainmenu = new CrossMenu(skin, this);
        mainmenu.setPosition(getDisplayWidth(), getDisplayHeight()/2f);
        
        display.addActor(mainmenu);
        
        mainFocus = new FocusGroup(mainmenu);
        manualFocus = new FocusGroup(timeline);
        itemFocus = new FocusGroup(lootPane);
        sacrificeGroup = new FocusGroup(lootPane, sacrificePane, sacrificeButton);
        sacrificeGroup.addListener(focusListener);
        
        
        lootPane.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (Input.CANCEL.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                    return true;
                }
                else if (Input.UP.match(keycode)) {
                    int index = lootButtons.getCheckedIndex();
                    Array<Button> buttons = lootButtons.getButtons();
                    Button b = buttons.get(Math.max(0, index-1));
                    b.setChecked(true);
                    return true;
                }
                else if (Input.DOWN.match(keycode)) {
                    int index = lootButtons.getCheckedIndex();
                    Array<Button> buttons = lootButtons.getButtons();
                    Button b = buttons.get(Math.min(buttons.size-1, index+1));
                    b.setChecked(true);
                    return true;
                }
                else if (Input.LEFT.match(keycode)) {
                    int index = lootButtons.getCheckedIndex();
                    Array<Button> buttons = lootButtons.getButtons();
                    Button b = buttons.get(Math.max(0, index-10));
                    b.setChecked(true);
                    return true;
                }
                else if (Input.RIGHT.match(keycode)) {
                    int index = lootButtons.getCheckedIndex();
                    Array<Button> buttons = lootButtons.getButtons();
                    Button b = buttons.get(Math.min(buttons.size-1, index+10));
                    b.setChecked(true);
                    return true;
                }
                return false;
            }
        });
        
        lootPane.addListener(new InputListener(){
           @Override
            public boolean keyDown(InputEvent event, int keycode) {
                   if (loot.size <= 0) {
                       return false;
                   }
    
                   if (menu.getCurrentState() == CombatStates.MODIFY){
                       if (Input.ACCEPT.match(keycode)) {
                           MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button);
                           return true;
                       }
                   }
                   else if (menu.getCurrentState() == CombatStates.Heal) {
                       if (Input.ACCEPT.match(keycode)) {
                           Item item = (Item) lootButtons.getChecked().getUserObject();
                           swapItem(item, true);
                           return true;
                       }
                       
                   }
                   
                   return false;
            } 
        });
	}
	
    private void makeHealSacrificeMenu() {
        // loot List and buttons
        lootList = new Table();
        lootList.top();
        lootList.pad(0f);
        lootList.setTouchable(Touchable.childrenOnly);
        
        ScrollPane p = new ScrollPane(lootList, skin);
        p.setScrollingDisabled(true, false);
        p.setScrollbarsOnTop(false);
        p.setScrollBarPositions(true, false);
        p.setFadeScrollBars(false);
        p.addListener(new ScrollFocuser(p));
        lootPaneScroller = new ScrollOnChange(p);
        
        ButtonGroup<Button> tabs = new ButtonGroup<Button>();
        TextButton tab = new TextButton("Inventory", skin);
        tab.setUserObject(p);
        tabs.add(tab);
        lootPane = new TabbedPane(tabs, false);
        lootPane.setSize(280f, getDisplayHeight() - 80f);
        lootPane.setPosition(getDisplayWidth(), getDisplayHeight(), Align.topLeft);
        
        display.addActor(lootPane);
        
        sacrificeList = new Table();
        sacrificeList.top();
        sacrificeList.pad(4f).padRight(10f);
        sacrificeList.setTouchable(Touchable.childrenOnly);
        
        p = new ScrollPane(sacrificeList, skin);
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

        sacrificePane.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (sacrifices.size <= 0) {
                    return false;
                }

                if (Input.ACCEPT.match(keycode)) {
                    Button button = sacrificeButtons.getChecked();
                    if (button != null) {
                        Item item = (Item) sacrificeButtons.getChecked().getUserObject();
                        swapItem(item, false);
                    }
                    return true;
                }
                if (Input.UP.match(keycode)) {
                    int index = sacrificeButtons.getCheckedIndex();
                    Array<Button> buttons = sacrificeButtons.getButtons();
                    Button b = buttons.get(Math.max(0, index-1));
                    b.setChecked(true);
                    return true;
                }
                else if (Input.DOWN.match(keycode)) {
                    int index = sacrificeButtons.getCheckedIndex();
                    Array<Button> buttons = sacrificeButtons.getButtons();
                    Button b = buttons.get(Math.min(buttons.size-1, index+1));
                    b.setChecked(true);
                    return true;
                }
                else if (Input.LEFT.match(keycode)) {
                    int index = sacrificeButtons.getCheckedIndex();
                    Array<Button> buttons = lootButtons.getButtons();
                    Button b = buttons.get(Math.max(0, index-10));
                    b.setChecked(true);
                    return true;
                }
                else if (Input.RIGHT.match(keycode)) {
                    int index = sacrificeButtons.getCheckedIndex();
                    Array<Button> buttons = sacrificeButtons.getButtons();
                    Button b = buttons.get(Math.min(buttons.size-1, index+10));
                    b.setChecked(true);
                    return true;
                }


                return false;
            }
        });

        lootButtons = new ButtonGroup<Button>();
        sacrificeButtons = new ButtonGroup<Button>();
        
        display.addActor(lootPane);
        display.addActor(sacrificePane);
        
        loot = new ObjectIntMap<Item>(playerService.getInventory().getLoot());
        lootRows = new Array<Item>();
        Keys<Item> keys = loot.keys();
        for (Item item : keys) {
            addItem(item, loot.get(item, 1));
        }
        
        sacrifices = new ObjectIntMap<Item>();
        sacrificeRows = new Array<Item>();
    }

    /**
     * Adds a new item row to the loot list
     * @param item
     * @param amount
     */
    private void addItem(final Item item, int amount){
        final TextButton l = new TextButton(item.toString(), skin);
        l.setName(item.toString());
        l.setUserObject(item);
        l.setDisabled(true);
        lootList.add(l).width(230f);
        lootButtons.add(l);
        Label i = new Label(String.valueOf(amount), getSkin(), "smaller");
        i.setAlignment(Align.right);
        lootList.add(i).width(30f).expandX();
        lootList.row();
        lootRows.add(item);
        
        l.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                
                if (button == Buttons.LEFT) {
                    if (l.isChecked() && menu.getCurrentState() == CombatStates.MODIFY) {
                        swapItem(item, true);
                    } else {
                        l.setChecked(true);
                    }
                    return true;
                }
                return false;
            }
        });
        l.addListener(updateStatWindow);
        l.addListener(lootPaneScroller);
        
        lootList.pack();
        
        loot.put(item, amount);
    }
    
    void resetSacrifices(){
        for (Item item : sacrifices.keys()) {
            int amount = sacrifices.get(item, 0);
            int k = loot.get(item, 0);
            modifyItem(item, k+amount);
        }
        clearSacrifices();
    }
    
    void clearSacrifices(){
        sacrifices.clear();
        sacrificeRows.clear();
        sacrificeList.clear();
        sacrificeButtons.clear();
        
        sacrificeList.row();
    }
    
    /**
     * Swaps an item between the two lists
     * @param item - Item to swap over
     * @param sacrifice - true if we are adding this item to the sacrifice list
     */
    void swapItem(Item item, boolean sacrifice) {
        int k = loot.get(item, 0);
        int s = sacrifices.get(item, 0);
        
        if (sacrifice) {
            if (k-1 >= 0) {
                modifyItem(item, --k);
                modifySacrifice(item, ++s);
            }
        } else {
            if (s-1 >= 0) {
                modifyItem(item, ++k);
                modifySacrifice(item, --s);
            }
        }
    }
    
    /**
     * Modifies a single row in the sacrifice list
     * @param item
     * @param amount
     */
    private void modifySacrifice(final Item item, int amount) {
        int existing = sacrifices.get(item, 0);
        if (existing == 0 && amount > 0) {
            sacrifices.put(item, amount);
            //add button
            
            final TextButton l = new TextButton(item.toString(), skin);
            l.setName(item.toString());
            l.setUserObject(item);
            l.setDisabled(true);
            sacrificeList.add(l).width(250f);
            Label i = new Label(String.valueOf(amount), getSkin(), "smaller");
            i.setAlignment(Align.right);
            sacrificeList.add(i).width(30f);
            sacrificeList.row();
            sacrificeButtons.add(l);
            
            l.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        if (l.isChecked()) {
                            swapItem(item, false);
                        } else {
                            l.setChecked(true);
                        }
                        return true;
                    }
                    return false;
                }
            });
            l.addListener(sacrificePaneScroller);
            
            sacrificeList.pack();
            
            sacrificeRows.add(item);
        }
        else if (existing != 0 && amount == 0) 
        {
            sacrifices.remove(item, 0);
            //remove buttons
            sacrificeButtons.setChecked(item.fullname());
            sacrificeButtons.remove(sacrificeButtons.getChecked());
            //remove row
            int index = sacrificeRows.indexOf(item, true);
            TableUtils.removeTableRow(sacrificeList, index, 2);
            sacrificeRows.removeIndex(index);
        }
        else 
        {
            //inplace modify button
            int index = sacrificeRows.indexOf(item, true);
            Label label = (Label)TableUtils.getActorFromRow(sacrificeList, index, 2, 1);
            label.setText(String.valueOf(amount));
            
            sacrifices.put(item, amount);
        }
    }   
    
    /**
     * Modifies a single row in the loot list
     * @param item
     * @param amount
     */
    private void modifyItem(Item item, int amount) {
        int existing = loot.get(item, 0);
        if (existing == 0 && amount > 0) {
            addItem(item, amount);
        } 
        else if (amount > 0) {
            int index = lootRows.indexOf(item, true);
            Label label = (Label)TableUtils.getActorFromRow(lootList, index, 2, 1);
            label.setText(String.valueOf(amount));
            loot.put(item, amount);
        } else {
            int row = lootRows.indexOf(item, true);
            TableUtils.removeTableRow(lootList, row, 2);
            lootRows.removeIndex(row);
            lootButtons.setChecked(item.fullname());
            lootButtons.remove(lootButtons.getChecked());
            loot.remove(item, 0);
        }
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
        Label name = new Label(battleService.getBoss().getComponent(Identifier.class).toString(), skin, "promptsm");
        name.setPosition(5, 24);
        messageWindow.addActor(name);
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
	    bg = new Image(new TiledDrawable(tex));
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
	    
	    attackGroup.addActor(timeline);
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
	                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button);
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
                    
                    addAction(Actions.sequence(Actions.delay(.2f), Actions.run(after)));
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
	protected void triggerAction(int index)
	{
	    //shouldn't happen since we don't use the default buttons in this scene
	}
	
	@Override
	public boolean handleMessage(Telegram msg) {
	    if (msg.message == Messages.Battle.VICTORY) {
	        changeState(CombatStates.VICTORY);
	        
	        VictoryResults results = (VictoryResults)msg.extraInfo;
	        
	        //TODO show victory message
            playVictoryAnimation(results);
            return true;
	    } else if (msg.message == Messages.Battle.DEFEAT){
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
	        return true;
	    }
	    return super.handleMessage(msg);
	}

	@Override
	protected FocusGroup focusList()
	{
	    //all focusable things in this ui don't have the pointer on them
	    if (menu.getCurrentState() == CombatStates.Heal) {
            return sacrificeGroup;
        }
        hidePointer();
	    if (menu.getCurrentState() == CombatStates.MANUAL) {
	        return manualFocus;
	    }
	    if (menu.getCurrentState() == CombatStates.MODIFY) {
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

	private static class HitBox {
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
                            head.hit.clearActions();
                            head.holder.clearActions();
                            if (head.hit(time) && 
                                ((Input.ACCEPT.match(keycode) && head.who == Combatant.Player) ||
                                 (Input.CANCEL.match(keycode) && head.who == Combatant.Enemy))) {
                                audio.playSfx(DataDirs.Sounds.accept);
                                if (head.who == Combatant.Player) {
                                    playerAttackAnimation();
                                    playerHits++;
                                } else {
                                    player.addAction(hitAnimation(DataDirs.Sounds.deflect));
                                }
                                head.hit.setRotation(0);
                                head.hit.setScale(1);
                                head.hit.setColor(1,1,1,1);
                            } else {
                                audio.playSfx(DataDirs.Sounds.tick);
                                if (head.who == Combatant.Enemy) {
                                    bossAttackAnimation();
                                    bossHits++;
                                }
                            }
                            head.hit.addAction(
                                Actions.sequence(
                                    Actions.alpha(0f, .2f),
                                    Actions.removeActor()
                                )
                            );
                            head.holder.addAction(
                                Actions.sequence(
                                    Actions.parallel(
                                        Actions.scaleTo(.4f, .4f, .2f),
                                        Actions.alpha(0f, .2f)
                                    ),
                                    Actions.delay(.2f),
                                    Actions.removeActor()
                                )
                            );
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
