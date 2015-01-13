package scenes.battle.ui;

import scenes.Messages;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.MathUtils;

import core.components.Identifier;
import core.components.Stats;
import core.service.interfaces.IBattleContainer;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

public class CombatHandler implements Telegraph {

    private static ComponentMapper<Stats> statsMap = ComponentMapper.getFor(Stats.class);
    
    /**
     * Identifier enum for stating which combatant is in charge of 
     * a turn of combat
     * @author nhydock
     *
     */
    public static enum Combatant {
        Player, Enemy;
    }
    
    /**
     * Container class for turning out the results of a combat turn
     * 
     * @author nhydock
     */
    public static class Turn {
        final public int bossRoll, playerRoll, hits;
        final public Combatant phase;
        
        private Turn(int b, int p, Combatant phase) {
            bossRoll = b;
            playerRoll = p;
            hits = (phase == Combatant.Player) ? p - b : b - p;
            this.phase = phase;
        }
    }
    
    /**
     * Value used for indicating how many more turns the foe is stunned for
     */
    private int stun;
    
    @Inject
    public IBattleContainer battleService;
    
    CombatHandler(){
        MessageDispatcher.getInstance().addListener(this, Messages.Battle.ADVANCE);
        ServiceManager.inject(this);
    }
    
    /**
     * Roll for during the attack action if the player is performing a manual attack
     * @param phase
     * @return
     */
    public Turn manualFightRoll(Combatant phase) {
        int eRoll = MathUtils.random(1, 6); 
        int pRoll = MathUtils.random(1, 6);
        
        if (isFoeStunned()) {
            eRoll = 1;
        }
        
        return new Turn(eRoll, pRoll, phase);
    }
    
    /**
     * Roll for during the attack action
     * @param phase
     * @return
     */
    public Turn fightRoll(Combatant phase){
        int eRoll = MathUtils.random(1, 3); 
        int pRoll = MathUtils.random(1, 4);
        
        if (isFoeStunned()) {
            eRoll = 1;
        }
        
        return new Turn(eRoll, pRoll, phase);
    }
    
    /**
     * Roll for during the defense action
     * @return
     */
    public Turn defendRoll() {
        int eRoll = MathUtils.random(1, 3);
        int pRoll = MathUtils.random(1, 3);
        
        return new Turn(eRoll, pRoll, Combatant.Player);
    }
    
    public Turn truePenetration(Combatant c) {
        int eRoll = (c == Combatant.Player) ? 1 : MathUtils.random(1, 6);
        int pRoll = (c == Combatant.Enemy) ? 1 : MathUtils.random(1, 6);
        
        return new Turn(eRoll, pRoll, c);
    }
    
    /**
     * When a foe is stunned, it may not attack for a turn
     * @return a foe is stunned if they have 1 or more turns where they can not do anything
     */
    public boolean isFoeStunned() {
        return stun > 0;
    }
    
    private int calcDamage(Turn turn) {
        Entity target = (turn.phase == Combatant.Player) ? battleService.getBoss() : battleService.getPlayer();
        Entity attacker = (turn.phase == Combatant.Player) ? battleService.getPlayer() : battleService.getBoss();
        
        int dmg = 0;
        float attackerStr = statsMap.get(target).getStrength();
        float targetDef = statsMap.get(attacker).getDefense();
        float crit = (turn.phase == Combatant.Player)?2f:1.25f;
        for (int i = 0; i < turn.hits; i++) {
            float chance = MathUtils.random(0.8f, crit);
            dmg += Math.max(0, (int) (chance * attackerStr) - targetDef);
        }
        return dmg;
    }
    
    /**
     * Handles fighting between two entities
     * @return
     */
    protected void fight(Turn turn) {
        //deal damage to entities
        Entity target = (turn.phase == Combatant.Player) ? battleService.getBoss() : battleService.getPlayer();
        
        int dmg = calcDamage(turn);
        
        String id = target.getComponent(Identifier.class).toString();
        
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.TARGET, target);
        if (turn.hits <= 0) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, String.format("%s\nBlocked the attack", id));
            
        } else {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, String.format("%s\n%d damage\n%d hits", id, dmg, turn.hits));
                
        }
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.DAMAGE, dmg);
    }
    
    protected void fightManual(int playerHits, int bossHits) {
        //deal damage to entities
        int pdmg = calcDamage(new Turn(0, playerHits, Combatant.Player));
        int bdmg = calcDamage(new Turn(bossHits, 0, Combatant.Enemy));
        
        String pid = battleService.getPlayer().getComponent(Identifier.class).toString();
        String bid = battleService.getBoss().getComponent(Identifier.class).toString();
        
        String notification;
        notification = String.format("%s\n%s\n%s\n%s",
                    pid,
                    (bossHits <= 0) ? "Blocked the attack" : pdmg + " damage\n" + bossHits + " hits",
                    bid,
                    (playerHits <= 0) ? "Blocked the attack" : bdmg + " damage\n" + playerHits + " hits"
                );
        
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, notification);
        
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.TARGET, battleService.getPlayer());
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.DAMAGE, bdmg);
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.TARGET, battleService.getBoss());
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.DAMAGE, pdmg);
    }
    
    /**
     * Player defends
     * @return true if player successfully defended against an enemy attack
     */
    protected void defend(Turn t) {
        Entity target = battleService.getBoss();
        String id = target.getComponent(Identifier.class).toString();
        if ( t.hits >= 0 ) {
            /*
             * Stun foe for 2 turns if parried
             */
            if ( t.hits == 0 ) 
            {
                stun = 3;
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, String.format("%s's attack has been parried!\nThey have been stunned for 2 turns", id));
            }
            /*
             * Stun foe for a turn on successful defense
             */
            else {
                stun = 2;
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, String.format("%s's attack has been blocked!\nThey have been stunned for 1 turn", id));
            }            
        } 
        else
        {
            id = battleService.getPlayer().getComponent(Identifier.class).toString();
            int dmg = calcDamage(new Turn(3, 1, Combatant.Enemy));
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.TARGET, battleService.getPlayer());
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, String.format("%s's guard was penetrated!\n%d damage\n2 hits", id, dmg));
        }
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        /*
         * Decrement stun length each turn 
         */
        if (msg.message == Messages.Battle.ADVANCE) {
            stun--;
            return true;
        }
        return false;
    }

}
