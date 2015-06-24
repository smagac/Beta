package scenes.town.ui;

import scene2d.ui.ScrollOnChange;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ItemList;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.TabbedPane;
import scenes.Messages;
import scenes.UI;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
class SacrificeSubmenu {
    
    private Group window;
    
    ItemList lootList;
    private ScrollPane lootPane;
    private ItemList sacrificeList;
    private ScrollPane sacrificePane;
    
    FocusGroup focus;
    private Label prompt;
    TextButton sacrificeButton;
    
    private Image pointer;
    private Runnable resetFocus;
    
    public SacrificeSubmenu(Skin skin, IPlayerContainer playerService, final UI parent) {
        window = new Group();
        window.setWidth(500);
        window.setHeight(350);
        
        // loot List
        {
            lootList = new ItemList(skin);
            lootList.getList().setTouchable(Touchable.childrenOnly);
            if (playerService.isHardcore()) {
                lootList.setItems(playerService.getInventory().getTmpLoot());
            } else {
                lootList.setItems(playerService.getInventory().getLoot());    
            }
            lootPane = new ScrollPane(lootList.getList(), skin);
            lootPane.setWidth(240f);
            lootPane.setHeight(350f);
            lootPane.setScrollingDisabled(true, false);
            lootPane.setScrollbarsOnTop(false);
            lootPane.setScrollBarPositions(true, false);
            lootPane.setFadeScrollBars(false);
            lootPane.addListener(new ScrollFocuser(lootPane));
            
            TextButton label = new TextButton("My Loot", skin, "tab");
            label.setUserObject(lootPane);
            TabbedPane pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(240f);
            pane.setHeight(350f);
            pane.setPosition(0f, window.getHeight(), Align.topLeft);
            window.addActor(pane);
            
            sacrificeList = new ItemList(skin);
            sacrificeList.getList().setTouchable(Touchable.childrenOnly);
            
            lootList.setSwapList(sacrificeList);
            sacrificeList.setSwapList(lootList);
            
            sacrificePane = new ScrollPane(sacrificeList.getList(), skin);
            sacrificePane.setWidth(240f);
            sacrificePane.setHeight(170f);
            sacrificePane.setScrollingDisabled(true, false);
            sacrificePane.setScrollbarsOnTop(false);
            sacrificePane.setScrollBarPositions(true, false);
            sacrificePane.setFadeScrollBars(false);
            sacrificePane.addListener(new ScrollFocuser(sacrificePane));

            label = new TextButton("Sacrifice", skin, "tab");
            label.setUserObject(sacrificePane);
            pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(240f);
            pane.setHeight(170f);
            pane.setPosition(260f, window.getHeight(), Align.topLeft);
            window.addActor(pane);
            
            
            window.addAction(Actions.alpha(0f));
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
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                    sacrificeButton.setChecked(true);
                    focus.setFocus(sacrificeButton);
                }
            });
            sacrificeButton.setTouchable(Touchable.enabled);
            
            window.addActor(sacrificeButton);
        }
        
        //Goddess prompt
        {
            Group promptGroup = new Group();
            promptGroup.setSize(240f, 130f);
            promptGroup.setPosition(260f, 50f);
            
            Window pane = new Window("", skin, "pane");
            pane.setSize(240f, 130f);
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
            
            window.addActor(promptGroup);
        }
        
        
        focus = new FocusGroup(lootList.getList(), sacrificeList.getList(), sacrificeButton);
        focus.setFocus(lootList.getList());
        window.addActor(focus);
        
        ScrollOnChange lootPaneScroller = new ScrollOnChange(lootPane);
        ScrollOnChange sacrificePaneScroller = new ScrollOnChange(sacrificePane);
        lootList.getList().addListener(lootPaneScroller);
        sacrificeList.getList().addListener(sacrificePaneScroller);
        
        pointer = new Image(skin, "pointer");
        pointer.setVisible(false);
        window.addActor(pointer);
        
        //start view as invisible
        window.setColor(1f, 1f, 1f, 0f);
        window.setOrigin(Align.center);
        window.setTouchable(Touchable.disabled);
        window.setVisible(false);
        
        window.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (window.isVisible()) {
                    if (Input.ACTION.match(keycode)) {
                        focus.next(true);
                        focus.getFocused().getStage().setKeyboardFocus(focus.getFocused());
                        if (focus.getFocused() != sacrificeButton){
                            parent.getPointer().setPosition(focus.getFocused(), Align.topLeft);
                            parent.getPointer().setVisible(true);
                            sacrificeButton.setChecked(false);
                        } else {
                            parent.getPointer().setVisible(false);
                            sacrificeButton.setChecked(true);
                        }
                        return true;
                    }
                }
                return false;
            }
        });
        
        resetFocus = new Runnable(){

            @Override
            public void run() {
                focus.setFocus(focus.getActors().first());
                focus.getFocused().getStage().setKeyboardFocus(focus.getFocused());
                parent.getPointer().setPosition(focus.getFocused(), Align.topLeft);
                parent.getPointer().setVisible(true);
            }
            
        };
    }

    /**
     * Get the stage2D representation of the menu
     * @return a group actor
     */
    public Group getGroup(){
        return window;
    }
    
    public void setPrompt(String msg){
        this.prompt.setText(msg);
    }
    
    /**
     * Show the item lists
     */
    public Action show() {
        sacrificeButton.setTouchable(Touchable.enabled);
        window.setTouchable(Touchable.enabled);
        focus.setFocus(focus.getActors().first());
        
        return Actions.addAction(
                Actions.sequence(
                    Actions.visible(true),
                    Actions.parallel(
                        Actions.moveBy(20f, 0, .2f, Interpolation.circleOut),
                        Actions.alpha(1f, .15f)
                    ),
                    Actions.run(resetFocus)
                )
                , window
            );
    }
    
    public void reset(){
        //any items still in the sacrifice pool will be returned to the item list
        for (Item i : sacrificeList.getItems().keys()) {
            int amount = sacrificeList.getItems().get(i, 0);
            lootList.updateLabel(i, lootList.getItems().get(i, 0) + amount);
        }
        sacrificeList.clear();
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
        window.clearActions();
        sacrificeButton.setTouchable(Touchable.disabled);
        window.setTouchable(Touchable.disabled);
    }

    public Actor getFocus() {
        return focus.getFocused();
    }

    public void updateLabel(Item item, int amount) {
        lootList.updateLabel(item, amount);
    }
}
