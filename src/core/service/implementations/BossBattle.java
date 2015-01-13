package core.service.implementations;

import scenes.Messages;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.utils.Array;

import core.components.Identifier;
import core.components.Stats;
import core.datatypes.StatModifier;
import core.factories.AdjectiveFactory;
import core.service.interfaces.IBattleContainer;

public class BossBattle implements IBattleContainer {

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
            s.hp -= (Integer)msg.extraInfo;
            if (s.hp <= 0 && target == boss)
            {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.VICTORY);
            } 
            else if (s.hp <= 0 && target == player)
            {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.DEFEAT);
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
            return true;
        }
        //advance to the next turn
        if (msg.message == Messages.Battle.ADVANCE) {
            for (Effect e : effectWatch) {
                e.advance();
            }
            MessageDispatcher.getInstance().dispatchMessage(this, Messages.Battle.MODIFY_UPDATE, effectWatch);
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
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.FIGHT);
        MessageDispatcher.getInstance().addListener(this, Messages.Battle.TARGET);
        MessageDispatcher.getInstance().addListener(this, Messages.Battle.DAMAGE);
        MessageDispatcher.getInstance().addListener(this, Messages.Battle.MODIFY);
    }

    @Override
    public void onUnregister() {
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.FIGHT);
        MessageDispatcher.getInstance().removeListener(this, Messages.Battle.TARGET);
        MessageDispatcher.getInstance().removeListener(this, Messages.Battle.DAMAGE);
        MessageDispatcher.getInstance().removeListener(this, Messages.Battle.MODIFY);
    }

    @Override
    public Entity getTarget() {
        return target;
    }
}
