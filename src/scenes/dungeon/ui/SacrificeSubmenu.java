package scenes.dungeon.ui;

import scene2d.InputDisabler;
import scene2d.ui.ScrollOnChange;
import scene2d.ui.extras.Card;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ItemList;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.TabbedPane;
import scenes.Messages;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

import core.common.Input;
import core.datatypes.Item;
import core.service.interfaces.IPlayerContainer;

/**
 * Submenu element for the Dungeon UI.  Contains the mechanics
 * and UI elements required for requesting asssistance and sacrificing
 * items to the goddess.
 * @author nhydock
 *
 */
public class SacrificeSubmenu {
    
    private static final String HEALFMT = "So you'd like me to heal you?\n \n%d loot recovers hp\n%d loot cures ailments.";
    private static final String LEAVEFMT = "Each floor deep you are costs another piece of loot.\n \nYou're currently %d floors deep.";
    
    private Group menu;
    private Group itemSubmenu;
    
    ItemList lootList;
    private ScrollPane lootPane;
    ItemList sacrificeList;
    private ScrollPane sacrificePane;
    
    private Card healCard;
    private Card leaveCard;
    
    FocusGroup focus;
    private Label prompt;
    private TextButton sacrificeButton;
    
    private Image pointer;
    
    @SuppressWarnings("rawtypes")
    public SacrificeSubmenu(Skin skin, IPlayerContainer playerService, final StateMachine sm) {
        menu = new Group();
        menu.setWidth(720);
        menu.setHeight(400);
        
        // loot List
        {
            itemSubmenu = new Group();
            itemSubmenu.setWidth(500f);
            itemSubmenu.setHeight(400f);

            lootList = new ItemList(skin);
            lootList.getList().setTouchable(Touchable.childrenOnly);
            if (playerService.isHardcore()) {
                lootList.setItems(playerService.getInventory().getTmpLoot());
            } else {
                lootList.setItems(playerService.getInventory().getLoot());    
            }
            lootPane = new ScrollPane(lootList.getList(), skin);
            lootPane.setWidth(240f);
            lootPane.setHeight(400f);
            lootPane.setScrollingDisabled(true, false);
            lootPane.setScrollbarsOnTop(false);
            lootPane.setScrollBarPositions(true, false);
            lootPane.setFadeScrollBars(false);
            lootPane.addListener(new ScrollFocuser(lootPane));
            
            TextButton label = new TextButton("My Loot", skin, "tab");
            label.setUserObject(lootPane);
            TabbedPane pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(240f);
            pane.setHeight(400f);
            pane.setPosition(0f, itemSubmenu.getHeight(), Align.topLeft);
            itemSubmenu.addActor(pane);
            
            sacrificeList = new ItemList(skin);
            sacrificeList.getList().setTouchable(Touchable.childrenOnly);
            
            lootList.setSwapList(sacrificeList);
            sacrificeList.setSwapList(lootList);
            
            sacrificePane = new ScrollPane(sacrificeList.getList(), skin);
            sacrificePane.setWidth(240f);
            sacrificePane.setHeight(200f);
            sacrificePane.setScrollingDisabled(true, false);
            sacrificePane.setScrollbarsOnTop(false);
            sacrificePane.setScrollBarPositions(true, false);
            sacrificePane.setFadeScrollBars(false);
            sacrificePane.addListener(new ScrollFocuser(sacrificePane));

            label = new TextButton("Sacrifice", skin, "tab");
            label.setUserObject(sacrificePane);
            pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(240f);
            pane.setHeight(180f);
            pane.setPosition(260f, itemSubmenu.getHeight(), Align.topLeft);
            itemSubmenu.addActor(pane);
            
            
            itemSubmenu.addAction(Actions.alpha(0f));
            menu.addActor(itemSubmenu);
        }

        //buttons for sacrificing
        {
            sacrificeButton = new TextButton("Sacrifice", skin, "pop");
            sacrificeButton.setWidth(240f);
            sacrificeButton.setHeight(48f);
            sacrificeButton.setPosition(260,0);
            sacrificeButton.setChecked(false);
            sacrificeButton.addListener(new InputListener(){
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (Input.ACCEPT.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Sacrifice);
                        return true;
                    }
                    return false;
                }
                
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT){
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Sacrifice);
                        return true;
                    }
                    return false;
                }
            });
            sacrificeButton.setTouchable(Touchable.enabled);
            
            itemSubmenu.addActor(sacrificeButton);
        }
        
        //Goddess prompt
        {
            Group promptGroup = new Group();
            promptGroup.setSize(240f, 140f);
            promptGroup.setPosition(260f, 60f);
            
            Window pane = new Window("", skin, "pane");
            pane.setSize(240f, 140f);
            promptGroup.addActor(pane);
            
            prompt = new Label("", skin, "promptsm");
            prompt.setSize(150f, 120f);
            prompt.setAlignment(Align.topLeft);
            prompt.setPosition(10, 130, Align.topLeft);
            prompt.setWrap(true);
            promptGroup.addActor(prompt);
            
            Image worship = new Image(skin, playerService.getWorship());
            worship.setSize(64, 64);
            worship.setPosition(190f, 70f, Align.center);
            promptGroup.addActor(worship);
            
            itemSubmenu.addActor(promptGroup);
        }
        
        //cards for choosing menu
        {
            healCard = new Card(skin, "Heal", "Sacrifice items to recover all of your hp and/or status ailments", "god");
            healCard.setPosition(getWidth()/2f - 20, getHeight()/2f, Align.right);
            healCard.setName("healName");
            healCard.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Heal);
                    return true;
                }
            });
            menu.addActor(healCard);
            
            leaveCard = new Card(skin, "Escape", "Sacrifice items to instantly escape from this dungeon with all of your loot", "up");
            leaveCard.setPosition(getWidth()/2f + 20, getHeight()/2f, Align.left);
            leaveCard.setName("leaveName");
            leaveCard.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Leave);
                    return true;
                }
            });
            menu.addActor(leaveCard);
        }
        
        focus = new FocusGroup(lootList.getList(), sacrificeList.getList(), sacrificeButton);
        focus.setFocus(lootList.getList());
        
        ScrollOnChange lootPaneScroller = new ScrollOnChange(lootPane);
        ScrollOnChange sacrificePaneScroller = new ScrollOnChange(sacrificePane);
        lootList.getList().addListener(lootPaneScroller);
        sacrificeList.getList().addListener(sacrificePaneScroller);
        
        menu.addActor(focus);
        
        pointer = new Image(skin, "pointer");
        pointer.setVisible(false);
        menu.addActor(pointer);
        
        //start view as invisible
        menu.setColor(1f, 1f, 1f, 0f);
        menu.setOrigin(Align.center);
        menu.setTouchable(Touchable.disabled);
        
        menu.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (sm.getCurrentState() == WanderState.Assist) {
                    if (Input.LEFT.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Heal);
                        return true;
                    }
                    if (Input.RIGHT.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Leave);
                        return true;
                    }
                }
                else if (sm.getCurrentState() == WanderState.Sacrifice_Heal || sm.getCurrentState() == WanderState.Sacrifice_Leave) {
                    if (Input.ACTION.match(keycode)) {
                        focus.next(true);
                        if (focus.getFocused() == sacrificeList.getList()) {
                            pointer.setVisible(true);
                            sacrificeButton.setChecked(false);
                            Pool<Vector2> pool = Pools.get(Vector2.class);
                            Vector2 pos = pool.obtain();
                            pos.x = sacrificeList.getList().getParent().getX(Align.topLeft);
                            pos.y = sacrificeList.getList().getParent().getY(Align.topLeft);
                            sacrificeList.getList().getParent().localToAscendantCoordinates(menu, pos);
                            
                            pointer.setPosition(pos.x, pos.y, Align.right);
                            
                            pool.free(pos);
                        }
                        else if (focus.getFocused() == lootList.getList()) {
                            pointer.setVisible(true);
                            sacrificeButton.setChecked(false);
                            Pool<Vector2> pool = Pools.get(Vector2.class);
                            Vector2 pos = pool.obtain();
                            pos.x = lootList.getList().getParent().getX(Align.topLeft);
                            pos.y = lootList.getList().getParent().getY(Align.topLeft);
                            lootList.getList().getParent().localToAscendantCoordinates(menu, pos);
                            
                            pointer.setPosition(pos.x, pos.y, Align.right);
                            
                            pool.free(pos);
                        }
                        else if (focus.getFocused() == sacrificeButton) {
                            pointer.setVisible(false);
                            sacrificeButton.setChecked(true);
                        }
                    }
                    else if (event.getTarget() != focus.getFocused()) {
                        focus.getFocused().fire(event);
                    }
                    return true;
                }
                
                return false;
            }
        });
    }
    
    private float getWidth() {
        return menu.getWidth();
    }
    
    private float getHeight() {
        return menu.getHeight();
    }

    /**
     * Get the stage2D representation of the menu
     * @return a group actor
     */
    public Group getGroup(){
        return menu;
    }
    
    /**
     * Show initial state with cards
     */
    public void show() {
        clearActions();
        menu.addAction(
            Actions.sequence(
                Actions.scaleTo(.7f, .7f),
                Actions.alpha(0f),
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.parallel(
                        Actions.alpha(1f),
                        Actions.moveToAligned(getWidth()/2f - 20, getHeight()/2f, Align.right)
                    ),
                    healCard
                ),
                Actions.addAction(
                    Actions.parallel(
                        Actions.alpha(1f),
                        Actions.moveToAligned(getWidth()/2f + 20, getHeight()/2f, Align.left)
                    ), 
                    leaveCard
                ),
                Actions.addAction(
                    Actions.parallel(
                            Actions.moveToAligned(200, getHeight()/2f, Align.left),
                            Actions.alpha(0f)
                    ),
                    itemSubmenu
                ),
                Actions.parallel(
                        Actions.alpha(1f, .15f),
                        Actions.scaleTo(1f, 1f, .25f, Interpolation.circleOut)
                ),
                Actions.run(InputDisabler.instance)
            )
        );
        menu.setTouchable(Touchable.childrenOnly);
        healCard.setTouchable(Touchable.enabled);
        leaveCard.setTouchable(Touchable.enabled);
    }
    
    /**
     * Show all elements and prompt associated with healing
     */
    public void showHeal(int cost) {
        clearActions();
        
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.moveToAligned(0, getHeight()/2f, Align.left, .3f, Interpolation.circleOut),
                    healCard
                ),
                Actions.addAction(
                    Actions.alpha(0f, .15f),
                    leaveCard
                ),
                Actions.delay(.25f),
                showItems(),
                Actions.run(InputDisabler.instance)
            )
        );

        sacrificeButton.setTouchable(Touchable.enabled);
        itemSubmenu.setTouchable(Touchable.enabled);
        
        prompt.setText(String.format(HEALFMT, cost, cost * 2));
    }
    
    /**
     * Show all elements and prompt associated with escaping
     */
    public void showEscape(int cost) {
        clearActions();
        
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.moveToAligned(0, getHeight()/2f, Align.left, .3f, Interpolation.circleOut),
                    leaveCard
                ),
                Actions.addAction(
                    Actions.alpha(0f, .15f),
                    healCard
                ),
                Actions.delay(.25f),
                showItems(),
                Actions.run(InputDisabler.instance)
            )
        );
        
        sacrificeButton.setTouchable(Touchable.enabled);
        itemSubmenu.setTouchable(Touchable.enabled);
        
        prompt.setText(String.format(LEAVEFMT, cost));
    }
    
    /**
     * Show the item lists
     */
    private Action showItems() {
        return Actions.addAction(
                Actions.parallel(
                    Actions.moveBy(20f, 0, .2f, Interpolation.circleOut),
                    Actions.alpha(1f, .15f)
                ), itemSubmenu
            );
    }
    
    /**
     * Hide all elements
     */
    public void hide() {
        clearActions();
        
        //any items still in the sacrifice pool will be returned to the item list
        for (Item i : sacrificeList.getItems().keys()) {
            int amount = sacrificeList.getItems().get(i, 0);
            lootList.updateLabel(i, lootList.getItems().get(i, 0) + amount);
        }
        sacrificeList.clear();
        
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.parallel(
                        Actions.alpha(0f, .2f),
                        Actions.scaleTo(2f, 2f, .3f, Interpolation.circleOut)
                ),
                Actions.run(InputDisabler.instance)
            )
        );
        menu.setTouchable(Touchable.disabled);
    }

    /**
     * Gets the items currently selected for sacrificing
     * @return ObjectIntMap indicating how many of each item is to be sacrificed
     */
    public ObjectIntMap<Item> getSacrifice() {
        return sacrificeList.getItems();
    }

    /**
     * Clears the sacrifice list after having sacrificed items so they are not added
     * back into the loot list when the ui is hidden
     */
    public void sacrifice() {
        sacrificeList.clear();
        lootList.selectItem(null);
    }
    
    private void clearActions(){
        healCard.clearActions();
        leaveCard.clearActions();
        itemSubmenu.clearActions();
        menu.clearActions();
        
        healCard.setTouchable(Touchable.disabled);
        leaveCard.setTouchable(Touchable.disabled);
        sacrificeButton.setTouchable(Touchable.disabled);
        itemSubmenu.setTouchable(Touchable.disabled);
    }
}
