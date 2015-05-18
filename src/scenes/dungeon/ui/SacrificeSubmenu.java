package scenes.dungeon.ui;

import scene2d.InputDisabler;
import scene2d.ui.ScrollOnChange;
import scene2d.ui.extras.Card;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.TabbedPane;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
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
    
    private Card healCard;
    private Card leaveCard;
    
    public SacrificeSubmenu(Skin skin, IPlayerContainer playerService) {
        menu = new Group();
        menu.setWidth(600);
        menu.setHeight(400);
        
        // loot List and buttons
        {
            itemSubmenu = new Group();
            itemSubmenu.setWidth(500f);
            itemSubmenu.setHeight(400f);

            lootList = new ItemList(skin);
            lootList.list.setTouchable(Touchable.childrenOnly);
            if (playerService.isHardcore()) {
                lootList.setItems(playerService.getInventory().getTmpLoot());
            } else {
                lootList.setItems(playerService.getInventory().getLoot());    
            }
            lootPane = new ScrollPane(lootList.list, skin);
            lootPane.setWidth(240f);
            lootPane.setHeight(380f);
            lootPane.setScrollingDisabled(true, false);
            lootPane.setScrollbarsOnTop(false);
            lootPane.setScrollBarPositions(true, false);
            lootPane.setFadeScrollBars(false);
            lootPane.addListener(new ScrollFocuser(lootPane));
            
            TextButton label = new TextButton("My Loot", skin);
            label.setUserObject(lootPane);
            TabbedPane pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(240f);
            pane.setHeight(380f);
            pane.setPosition(0f, itemSubmenu.getHeight(), Align.topLeft);
            itemSubmenu.addActor(pane);
            
            sacrificeList = new ItemList(skin);
            sacrificeList.list.setTouchable(Touchable.childrenOnly);
            
            lootList.setSwapList(sacrificeList);
            sacrificeList.setSwapList(lootList);
            
            sacrificePane = new ScrollPane(sacrificeList.list, skin);
            sacrificePane.setWidth(240f);
            sacrificePane.setHeight(180f);
            sacrificePane.setScrollingDisabled(true, false);
            sacrificePane.setScrollbarsOnTop(false);
            sacrificePane.setScrollBarPositions(true, false);
            sacrificePane.setFadeScrollBars(false);
            sacrificePane.addListener(new ScrollFocuser(sacrificePane));

            label = new TextButton("Sacrifice", skin);
            label.setUserObject(sacrificePane);
            pane = new TabbedPane(new ButtonGroup<Button>(label), false);
            pane.setWidth(240f);
            pane.setHeight(180f);
            pane.setPosition(260f, itemSubmenu.getHeight(), Align.topLeft);
            itemSubmenu.addActor(pane);
            
            
            itemSubmenu.addAction(Actions.alpha(0f));
            menu.addActor(itemSubmenu);
        }

        //cards for choosing menu
        {
            healCard = new Card(skin, "Heal", "Sacrifice items to recover all of your hp and/or status ailments", "god");
            healCard.setPosition(getWidth()/2f - 20, getHeight()/2f, Align.right);
            menu.addActor(healCard);
            
            leaveCard = new Card(skin, "Escape", "Sacrifice items to instantly escape from this dungeon with all of your loot", "up");
            leaveCard.setPosition(getWidth()/2f + 20, getHeight()/2f, Align.left);
            menu.addActor(leaveCard);
        }
        
        ScrollOnChange lootPaneScroller = new ScrollOnChange(lootPane);
        ScrollOnChange sacrificePaneScroller = new ScrollOnChange(sacrificePane);
        lootList.list.addListener(lootPaneScroller);
        sacrificeList.list.addListener(sacrificePaneScroller);
        
        //start view as invisible
        menu.setColor(1f, 1f, 1f, 0f);
        menu.setOrigin(Align.center);
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
                            Actions.moveToAligned(getWidth()/2f - 220, getHeight()/2f, Align.left),
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
    }
    
    /**
     * Show all elements and prompt associated with healing
     */
    public void showHeal() {
        clearActions();
        
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.moveToAligned(getWidth()/2f - 200, getHeight()/2f, Align.right, .3f, Interpolation.circleOut),
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
    }
    
    /**
     * Show all elements and prompt associated with escaping
     */
    public void showEscape() {
        clearActions();
        
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.moveToAligned(getWidth()/2f - 200, getHeight()/2f, Align.right, .3f, Interpolation.circleOut),
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
        for (Item i : sacrificeList.items.keys()) {
            int amount = sacrificeList.items.get(i, 0);
            lootList.updateLabel(i, lootList.items.get(i, 0) + amount);
        }
        sacrificeList.clear();
        
        menu.addAction(
            Actions.sequence(
                Actions.parallel(
                        Actions.alpha(0f, .2f),
                        Actions.scaleTo(2f, 2f, .3f, Interpolation.circleOut)
                )
            )
        );
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
    
    private void clearActions(){
        healCard.clearActions();
        leaveCard.clearActions();
        itemSubmenu.clearActions();
        menu.clearActions();
    }
}
