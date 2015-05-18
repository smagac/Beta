package scenes.dungeon.ui;

import scene2d.ui.ScrollOnChange;
import scene2d.ui.extras.Card;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.TabbedPane;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectIntMap;

import core.datatypes.Item;
import core.service.interfaces.IPlayerContainer;

public class SacrificeSubmenu {
    
    private Group menu;
    private Group itemSubmenu;
    
    ItemList lootList;
    private ScrollPane lootPane;
    ItemList sacrificeList;
    private ScrollPane sacrificePane;
    
    private FocusGroup sacrificeGroup;
    
    public SacrificeSubmenu(Skin skin, IPlayerContainer playerService) {
        menu = new Group();
        menu.setWidth(600);
        menu.setHeight(400);
        
     // loot List and buttons
        {
            itemSubmenu = new Group();
            itemSubmenu.setWidth(200f);
            itemSubmenu.setHeight(500f);
            itemSubmenu.setPosition(-200f, 100f);


            lootList = new ItemList(skin);
            lootList.list.setTouchable(Touchable.childrenOnly);
            if (playerService.isHardcore()) {
                lootList.setItems(playerService.getInventory().getTmpLoot());
            } else {
                lootList.setItems(playerService.getInventory().getLoot());    
            }
            
            lootPane = new ScrollPane(lootList.list, skin);
            lootPane.setWidth(200f);
            lootPane.setHeight(240f);
            lootPane.setScrollingDisabled(true, false);
            lootPane.setScrollbarsOnTop(false);
            lootPane.setScrollBarPositions(true, false);
            lootPane.setFadeScrollBars(false);
            lootPane.addListener(new ScrollFocuser(lootPane));
            
            TextButton label = new TextButton("My Loot", skin);
            label.setUserObject(lootPane);
            TabbedPane pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(230f);
            itemSubmenu.addActor(pane);
            
            sacrificeList = new ItemList(skin);
            sacrificeList.list.setTouchable(Touchable.childrenOnly);
            
            lootList.setSwapList(sacrificeList);
            sacrificeList.setSwapList(lootList);
            
            sacrificePane = new ScrollPane(sacrificeList.list, skin);
            sacrificePane.setWidth(200f);
            sacrificePane.setHeight(240f);
            sacrificePane.setScrollingDisabled(true, false);
            sacrificePane.setScrollbarsOnTop(false);
            sacrificePane.setScrollBarPositions(true, false);
            sacrificePane.setFadeScrollBars(false);
            sacrificePane.addListener(new ScrollFocuser(sacrificePane));

            label = new TextButton("My Loot", skin);
            label.setUserObject(lootPane);
            pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(230f);
            pane.setPosition(0f, 260f);
            itemSubmenu.addActor(pane);
            
            
            itemSubmenu.addAction(Actions.alpha(0f));
            menu.addActor(itemSubmenu);
        }

        //cards for choosing menu
        {
            Card healCard = new Card(skin, "Heal", "Sacrifice items to recover all of your hp and/or status ailments", "god");
            healCard.setPosition(getWidth()/2f - 20, getHeight(), Align.bottomRight);
            healCard.addAction(Actions.moveToAligned(getWidth()/2f - 20, getHeight()/2f, Align.right, 1f));
            menu.addActor(healCard);
            
            Card leaveCard = new Card(skin, "Escape", "Sacrifice items to instantly escape from this dungeon with all of your loot", "up");
            leaveCard.setPosition(getWidth()/2f + 20, getHeight(), Align.bottomLeft);
            leaveCard.addAction(Actions.moveToAligned(getWidth()/2f + 20, getHeight()/2f, Align.left, 1f));
            menu.addActor(leaveCard);
            
        }
        
        ScrollOnChange lootPaneScroller = new ScrollOnChange(lootPane);
        ScrollOnChange sacrificePaneScroller = new ScrollOnChange(sacrificePane);
        lootList.list.addListener(lootPaneScroller);
        sacrificeList.list.addListener(sacrificePaneScroller);
        
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
        
    }
    
    /**
     * Show all elements and prompt associated with healing
     */
    public void showHeal() {
        
    }
    
    /**
     * Show all elements and prompt associated with escaping
     */
    public void showEscape() {
        
    }
    
    /**
     * Show the item lists
     */
    private void showItems() {
        
    }
    
    /**
     * Hide all elements
     */
    public void hide() {

        //any items still in the sacrifice pool will be returned to the item list
        for (Item i : sacrificeList.items.keys()) {
            int amount = sacrificeList.items.get(i, 0);
            lootList.updateLabel(i, lootList.items.get(i, 0) + amount);
        }
        sacrificeList.clear();
        
        
    }

    /**
     * Gets the items currently selected for sacrificing
     * @return ObjectIntMap indicating how many of each item is to be sacrificed
     */
    public ObjectIntMap<Item> getSacrifice() {
        return sacrificeList.items;
    }

    /**
     * Clears the sacrifice list after having sacrificed items so they are not added
     * back into the loot list when the ui is hidden
     */
    public void sacrifice() {
        sacrificeList.clear();
        lootList.selectItem(null);
    }
    
    
}
