package core.service.implementations;

import scenes.battle.ui.BattleMessages;
import scenes.dungeon.GameState;

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
        if (msg.message == GameState.Messages.FIGHT)
        {
            setBoss((Entity)msg.extraInfo);
            return true;
        }
        //sets the target for the next effect
        if (msg.message == BattleMessages.TARGET) {
            target = (Entity)msg.extraInfo;
            return true;
        }
        //damage an entity
        if (msg.message == BattleMessages.DAMAGE) {
            if (target == null) {
                throw new NullPointerException("Target must be defined before we may attack it");
            }
            
            Stats s = target.getComponent(Stats.class);
            s.hp -= (Integer)msg.extraInfo;
            target = null;
            return true;
        }
        //apply a modifier onto an entity
        if (msg.message == BattleMessages.MODIFY) {
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
        if (msg.message == BattleMessages.ADVANCE) {
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
    
    /**
     * Temporary modifier affect to attach to entities
     * @author nhydock
     *
     */
    private class Effect {
        Entity target;
        StatModifier mod;
        String adjective;
        int turns;
        
        private Effect(Entity target, String adj) {
            this.target = target;
            this.adjective = adj;
            this.mod = AdjectiveFactory.getModifier(adjective);
            this.turns = 3;
        }
        
        private void apply() {
            target.getComponent(Identifier.class).addModifier(adjective);
            target.getComponent(Stats.class).addModifier(mod);
        }
        
        private void advance() {
            turns--;
            if (turns <= 0) {
                expire();
            }
        }
        
        private void expire() {
            target.getComponent(Identifier.class).removeModifier(adjective);
            target.getComponent(Stats.class).removeModifier(mod);
        }
    }

    @Override
    public void onRegister() {
        MessageDispatcher.getInstance().addListener(this, GameState.Messages.FIGHT);
    }

    @Override
    public void onUnregister() {
        MessageDispatcher.getInstance().removeListener(this, GameState.Messages.FIGHT);
    }
}
