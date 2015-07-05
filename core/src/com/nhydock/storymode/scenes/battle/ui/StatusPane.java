package com.nhydock.storymode.scenes.battle.ui;

import github.nhydock.ssm.Inject;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.nhydock.storymode.scenes.Messages;
import com.nhydock.storymode.service.implementations.BossBattle.Effect;
import com.nhydock.storymode.service.interfaces.IBattleContainer;

public class StatusPane extends Group implements Telegraph {

    Skin skin;
    Image background;
    Entity entity;
    
    ObjectMap<String, TurnMeter> progress;
    
    @Inject public IBattleContainer battleService;
    
    public StatusPane(BattleUI ui) {
        this.skin = ui.getSkin();
        
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        /*
         * Update all the progress meters that state how many turns 
         * are left on any adjectives possessed by the entity
         */
        if (msg.message == Messages.Battle.MODIFY_UPDATE) {
            @SuppressWarnings("unchecked")
            Array<Effect> info = (Array<Effect>)msg.extraInfo;
            for (Effect e : info) {
                if (e.getTarget() == entity) {
                    String adj = e.getAdjective();
                    if (e.turns() < 0) {
                        progress.remove(adj);
                    } else {
                        if (progress.containsKey(adj)) {
                            progress.get(adj).setTurns(e.turns());
                        } else {
                            TurnMeter tm = new TurnMeter(skin, adj);
                            tm.setTurns(e.turns());
                            progress.put(adj, tm);
                            
                        }
                    }
                }
            }
        }
        return false;
    }
    
    private static class TurnMeter extends Group {
        Array<Image> progress;
        
        Drawable active;
        Drawable inactive;
        
        public TurnMeter(Skin skin, String adj) {
            
            active = skin.getDrawable("meter_active");
            inactive = skin.getDrawable("meter_inactive");
            
            for (int i = 0; i < Effect.LASTING_LENGTH; i++) {
                Image bar = new Image(active);
                bar.setPosition(i * (bar.getWidth() + 10), 0f);
                progress.add(bar);
                addActor(bar);
            }
            
            Label label = new Label(adj, skin, "prompt");
            label.setPosition(0, active.getMinHeight());
            addActor(label);
        }
        
        public void setTurns(int i) {
            for (int n = 0; n < i; n++) {
                this.progress.get(n).setDrawable(active);
            }
            for (int n = progress.size-1; n > i; n--) {
                this.progress.get(n).setDrawable(inactive);
            }
        }
        
    }
}
