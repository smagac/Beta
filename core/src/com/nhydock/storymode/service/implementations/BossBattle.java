package com.nhydock.storymode.service.implementations;

import github.nhydock.ssm.Inject;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.nhydock.storymode.components.Drop;
import com.nhydock.storymode.components.Identifier;
import com.nhydock.storymode.components.Stats;
import com.nhydock.storymode.datatypes.Inventory;
import com.nhydock.storymode.datatypes.Item;
import com.nhydock.storymode.datatypes.StatModifier;
import com.nhydock.storymode.factories.AdjectiveFactory;
import com.nhydock.storymode.scenes.Messages;
import com.nhydock.storymode.scenes.Messages.Battle.VictoryResults;
import com.nhydock.storymode.service.interfaces.IBattleContainer;
import com.nhydock.storymode.service.interfaces.IDungeonContainer;
import com.nhydock.storymode.service.interfaces.IPlayerContainer;

public class BossBattle implements IBattleContainer {

    @Inject public IDungeonContainer dungeonService;
    @Inject public IPlayerContainer playerService;
    
    private Entity boss;
    private Entity player;
    
    /**
     * Selected target for the next attack
     */
    private Entity target;
    
    private Array<Effect> effectWatch = new Array<Effect>();

    @Override
    public boolean handleMessage(Telegram msg) {
        //set's the boss for the service
        if (msg.message == Messages.Dungeon.FIGHT)
        {
            setBoss((Entity)msg.extraInfo);
            return true;
        }
        //sets the target for the next effect
        if (msg.message == Messages.Battle.TARGET) {
            target = (Entity)msg.extraInfo;
            return true;
        }
        //damage an entity
        if (msg.message == Messages.Battle.DAMAGE) {
            if (target == null) {
                throw new NullPointerException("Target must be defined before we may attack it");
            }
            
            Stats s = target.getComponent(Stats.class);
            s.hp = Math.max(0, s.hp - (Integer)msg.extraInfo);
            if (target == player) {
                MessageManager.getInstance().dispatchMessage(null, Messages.Player.Stats);
            }
            else {
                MessageManager.getInstance().dispatchMessage(null, Messages.Battle.Stats);
            }
            if (s.hp <= 0 && target == boss)
            {
                VictoryResults results = new VictoryResults();
                
                //pick a random required material and random amount between 1-5
                Inventory inv = playerService.getInventory();
                String reward = inv.getRequiredCrafts().random().getRequirementTypes().random();
                String adjective = AdjectiveFactory.getAdjective();
                
                results.reward = (Item)Drop.Map.get(boss).reward;
                results.bonus = new Item(reward, adjective);
                results.bonusCount = MathUtils.random(1, 5); 
                results.exp = 5;
                
                dungeonService.getEngine().removeEntity(boss);
                dungeonService.getProgress().monstersKilled++;
                
                MessageManager.getInstance().dispatchMessage(null, Messages.Battle.VICTORY, results);
            } 
            else if (s.hp <= 0 && target == player)
            {
                MessageManager.getInstance().dispatchMessage(null, Messages.Battle.DEFEAT);
            }
            target = null;
            return true;
        }
        //apply a modifier onto an entity
        if (msg.message == Messages.Battle.MODIFY) {
            if (target == null) {
                throw new NullPointerException("Target must be defined before we may attack it");
            }
            
            String adj = (String)msg.extraInfo;
            
            //build a modifier effect
            Effect e = new Effect(target, adj);
            e.apply();
            effectWatch.add(e);
            MessageManager.getInstance().dispatchMessage(this, Messages.Battle.Stats);
            
            target = null;
            return true;
        }
        //advance to the next turn
        if (msg.message == Messages.Battle.ADVANCE) {
            for (Effect e : effectWatch) {
                e.advance();
            }
            return true;
        }
        return false;
    }

    @Override
    public void setBoss(Entity bossEntity) {
        boss = bossEntity;
    }


    @Override
    public Entity getBoss() {
        return boss;
    }

    @Override
    public void setPlayer(Entity playerEntity) {
        player = playerEntity;
    }

    @Override
    public Entity getPlayer() {
        return player;
    }
    
    /**
     * Temporary modifier affect to attach to entities
     * @author nhydock
     *
     */
    public static class Effect {
        public static final int LASTING_LENGTH = 3;
        
        Entity target;
        StatModifier mod;
        String adjective;
        int turns;
        
        private Effect(Entity target, String adj) {
            this.target = target;
            this.adjective = adj;
            this.mod = AdjectiveFactory.getModifier(adjective);
            this.turns = LASTING_LENGTH;
        }
        
        private void apply() {
            target.getComponent(Identifier.class).addModifier(adjective);
            target.getComponent(Stats.class).addModifier(mod);
        }
        
        private void advance() {
            turns--;
            if (turns < 0) {
                expire();
            }
        }
        
        private void expire() {
            target.getComponent(Identifier.class).removeModifier(adjective);
            target.getComponent(Stats.class).removeModifier(mod);
            MessageManager.getInstance().dispatchMessage(null, Messages.Battle.DEBUFF, adjective);
            MessageManager.getInstance().dispatchMessage(null, Messages.Battle.Stats);
        }
        
        public String getAdjective() {
            return adjective;
        }
        
        public int turns() {
            return turns;
        }
        
        public Entity getTarget() {
            return target;
        }
    }

    @Override
    public void onRegister() {
        MessageManager.getInstance().addListener(this, Messages.Dungeon.FIGHT);
        MessageManager.getInstance().addListener(this, Messages.Battle.TARGET);
        MessageManager.getInstance().addListener(this, Messages.Battle.DAMAGE);
        MessageManager.getInstance().addListener(this, Messages.Battle.MODIFY);
        
        player = playerService.getPlayer();
    }

    @Override
    public void onUnregister() {
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.FIGHT);
        MessageManager.getInstance().removeListener(this, Messages.Battle.TARGET);
        MessageManager.getInstance().removeListener(this, Messages.Battle.DAMAGE);
        MessageManager.getInstance().removeListener(this, Messages.Battle.MODIFY);
        
        player = null;
    }

    @Override
    public Entity getTarget() {
        return target;
    }
}
