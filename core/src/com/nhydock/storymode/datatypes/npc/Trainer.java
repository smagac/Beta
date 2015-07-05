package com.nhydock.storymode.datatypes.npc;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.nhydock.storymode.components.Stats;
import com.nhydock.storymode.components.Stats.Stat;
import com.nhydock.storymode.datatypes.Item;
import com.nhydock.storymode.datatypes.npc.NPC.Behavior;
import com.nhydock.storymode.factories.ItemFactory;
import com.nhydock.storymode.scenes.Messages;

/**
 * Trainers allow you to exchange items for stat bonuses.
 * It takes a specific amount of points to raise a stat.
 * When exchanging items the trainer may offer addition points
 * if you trade him an item that he desires.
 * 
 * @author nhydock
 */
public class Trainer implements Behavior {
    
    Stat type;
    String bonusType;
    
    public Trainer(Stat stat){
        this.type = stat;
        bonusType = ItemFactory.randomType();
    }
    
    @Override
    public void run(){
        MessageManager.getInstance().dispatchMessage(null, Messages.NPC.TRAINER, this);
    }
    
    public Stat getTrainingType(){
        return type;
    }
    
    /**
     * This is the type of item that will reward bonus points for leveling up
     * @return
     */
    public String getBonus(){
        return bonusType;
    }
    
    public int calcPoints(Stats stats){
        if (type == Stat.STRENGTH){
            return stats.getStrength();
        }
        else if (type == Stat.DEFENSE){
            return stats.getDefense();
        }
        else if (type == Stat.VITALITY){
            return stats.getVitality();
        }
        else {
            return (int)Math.max(1, stats.getSpeed()/2);
        }
    }
    
    /**
     * Checks to see if the amount being sacrificed is enough to fulfill the cost of training
     * @param list
     * @param stats
     * @return
     */
    public boolean sacrifice(ObjectIntMap<Item> list, Stats stats){
        int sum = 0;
        ObjectIntMap.Entries<Item> items = list.entries();
        while (items.hasNext){
            ObjectIntMap.Entry<Item> item = items.next();
            if (item.key.type().equals(bonusType)){
                sum += item.value * 2;
            } else {
                sum += item.value;
            }
        }
        
        return (sum >= calcPoints(stats));
    }

    public void train(Stats stats) {
        stats.levelUp(type);
        MessageManager.getInstance().dispatchMessage(null, Messages.Player.Stats);
    }
}