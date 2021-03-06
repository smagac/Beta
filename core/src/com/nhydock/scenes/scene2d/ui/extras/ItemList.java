package com.nhydock.scenes.scene2d.ui.extras;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pools;
import com.nhydock.storymode.common.Input;
import com.nhydock.storymode.datatypes.Item;

/**
 * Wraps a table of items
 * @author nhydock
 *
 */
public class ItemList {
    Label.LabelStyle hoverStyle;
    Label.LabelStyle normalStyle;
    
    Table list;
    ObjectIntMap<Item> items;
    ObjectMap<Item, Label[]> rows;
    Array<Item> order;
    
    int index;
    
    /**
     * Shared listener used for mouse input
     */
    InputListener hoverListener = new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            Item item = (Item)event.getTarget().getUserObject();
            if (item == getSelectedItem()){
                swap(item);
            } else {
                selectItem(item);
            }
            return true;
        };
    };
    private ItemList swap;
    
    /**
     * Constructs a new UI list for an item container
     * @param skin
     */
    public ItemList(Skin skin) {
        list = new Table(skin);
        items = new ObjectIntMap<Item>();
        rows = new ObjectMap<Item, Label[]>();
        order = new Array<Item>();
        index = 0;
        
        hoverStyle = skin.get("selected", Label.LabelStyle.class);
        normalStyle = skin.get("list", Label.LabelStyle.class);
        
        list.top();
        list.setWidth(230f);
        list.pad(5f);
        list.row();
        
        list.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (order.size <= 0) {
                    return false;
                }
                if (Input.DOWN.match(keycode)) {
                    selectNextItem();
                    return true;
                }
                else if (Input.UP.match(keycode)) {
                    selectPreviousItem();
                    return true;
                }
                else if (Input.ACCEPT.match(keycode)) {
                    swap();
                    return true;
                }
                return false;
            }
        });
    }
    
    /**
     * Replace the items in this list
     * @param i
     */
    public void setItems(ObjectIntMap<Item> i) {
        items.clear();
        rows.clear();
        list.clearChildren();
        
        items.putAll(i);
        for (Item item : items.keys()) {
            makeLabel(item, items.get(item, 0));
        }
        
        list.pack();
        index = -1;
    }
    
    /**
     * Creates a new label for an item in this list
     * @param i
     *  Item to create a label for
     * @param x
     *  The amount of the item in the inventory stack
     */
    public void makeLabel(Item i, int x) {
        Label[] labels = new Label[2];
        list.row();
        
        Label itemLabel = new Label(i.toString(), normalStyle);
        list.add(itemLabel).width(200f).expandX().align(Align.left).padRight(0f);
        labels[0] = itemLabel;
        itemLabel.setUserObject(i);
        itemLabel.addListener(hoverListener);
        
        Label countLabel = new Label(""+x, normalStyle);
        countLabel.setAlignment(Align.right);
        list.add(countLabel).width(20f).expandX().padLeft(0f);
        labels[1] = countLabel;
        countLabel.setUserObject(i);
        countLabel.addListener(hoverListener);
        
        rows.put(i, labels);
        order.add(i);
    }
    
    /**
     * Updates the value of an Item's label.  If the item doesn't exist,
     * a new label is made, and if the amount set is 0 then the item will
     * be removed from the list.
     * @param i
     *  Item to update
     * @param x
     *  Amount of the item that is in the inventory stack
     */
    public void updateLabel(Item i, int x) {
        if (!items.containsKey(i)) {
            makeLabel(i, x);
            items.put(i, x);
        } else {
            if (x == 0) {
                int loc = order.indexOf(i, true);
                if (index == loc) {
                    index = -1;
                    selectItem(null);
                }
                items.remove(i, 0);
                rows.remove(i);
                TableUtils.removeTableRow(list, loc, 2);
                order.removeIndex(loc);
            } else {
                Label[] labels = rows.get(i);
                labels[1].setText(""+x);
                items.put(i, x);
            }
        }
        list.pack();
    }
    
    /**
     * Selects a specific item, lighting it up in the interface
     * @param i
     */
    public void selectItem(Item i) {
        for (Item item : rows.keys()) {
            Label[] labels = rows.get(item);
            labels[0].setStyle(normalStyle);
            labels[1].setStyle(normalStyle);
        }
        if (i != null) {
            Label[] labels = rows.get(i);
            labels[0].setStyle(hoverStyle);
            labels[1].setStyle(hoverStyle);
            index = order.indexOf(i, true);;
            
            ChangeEvent event = Pools.obtain(ChangeEvent.class);
            labels[0].fire(event);
            Pools.free(event);
        }
    }
    
    /**
     * Set a list that this list is paired with for swapping
     * @param swap
     */
    public void setSwapList(ItemList swap) {
        this.swap = swap;
    }
    
    /**
     * Swaps an item over between lists
     * @param item
     */
    public void swap(Item item) {
        if (swap != null) {
            int amount = items.get(item, 0);
            if (amount > 0) {
                updateLabel(item, amount - 1);
                swap.updateLabel(item, swap.items.get(item, 0) + 1);
            }
        }
    }
    
    /**
     * Swaps the currently selected item over between lists
     * @param item
     */
    public void swap() {
        if (swap != null) {
            Item item = getSelectedItem();
            int amount = items.get(item, 0);
            if (amount > 0) {
                updateLabel(item, amount - 1);
                swap.updateLabel(item, swap.items.get(item, 0) + 1);
            }
        }
    }
    
    /**
     * Swaps all the items in this list into the other that it's attached to
     * @param item
     */
    public void swapAll() {
        if (swap != null) {
            for (int i = 0; i < order.size; i++){
                Item item = order.get(i);
                int amount = items.get(item, 0);
                swap.updateLabel(item, swap.items.get(item, 0) + 1);
            }
        }
        clear();
    }
    
    public Table getList() {
        return list;
    }
    
    public ObjectIntMap<Item> getItems() {
        return items;
    }
    
    /**
     * Clears this list completely
     */
    public void clear() {
        items.clear();
        list.clear();
        rows.clear();
        order.clear();
    }

    public Item getSelectedItem() {
        if (index >= 0) {
            return order.get(index);
        }
        return null;
    }

    public void selectPreviousItem() {
        index--;
        if (index < 0) {
            index = 0;
        }
        selectItem(order.get(index));
    }
    
    public void selectNextItem() {
        index++;
        if (index > order.size - 1) {
            index = 0;
        }
        
        selectItem(order.get(index));
    }

    public boolean isEmpty() {
        return order.size <= 0;
    }
}
