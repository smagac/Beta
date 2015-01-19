package scenes.dungeon.ui;

import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;
import scene2d.InputDisabler;
import scenes.GameUI;
import scenes.Messages;
import scenes.dungeon.Direction;
import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import core.DataDirs;
import core.components.Stats;
import core.datatypes.Item;
import core.datatypes.dungeon.Progress;
import core.datatypes.quests.Quest;
import core.service.implementations.ScoreTracker;
import core.service.implementations.ScoreTracker.NumberValues;

/**
 * Handles state based ui menu logic and switching
 * 
 * @author nhydock
 *
 */
public enum WanderState implements UIState {
    Wander("Request Assistance") {

        private float walkTimer = -1f;
        private final Vector2 mousePos = new Vector2();

        @Override
        public void enter(WanderUI entity) {
            entity.hideGoddess();
        }

        @Override
        public void update(WanderUI entity) {
            if (walkTimer >= 0f) {
                MovementSystem ms = entity.dungeonService.getEngine().getSystem(MovementSystem.class);
                float delta = Gdx.graphics.getDeltaTime();
                walkTimer += delta;
                if (walkTimer > RenderSystem.MoveSpeed * 2f) {
                    Direction d = Direction.valueOf(Gdx.input);
                    if (d == null && Gdx.input.isButtonPressed(Buttons.LEFT)) {
                        mousePos.set(Gdx.input.getX(), Gdx.input.getY());
                        entity.getDisplay().screenToLocalCoordinates(mousePos);
                        d = Direction.valueOf(mousePos, entity.getDisplayWidth(), entity.getDisplayHeight());
                    }
                    if (d != null) {
                        ms.movePlayer(d);
                        walkTimer = 0f;
                    }
                    else {
                        walkTimer = -1f;
                    }
                }
            }
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {

            if (telegram.message == Messages.Dungeon.Movement) {
                Direction direction = (Direction) telegram.extraInfo;
                if (direction == null) {
                    walkTimer = -1f;
                }
                else {
                    final MovementSystem ms = entity.dungeonService.getEngine().getSystem(MovementSystem.class);
                    if (ms.movePlayer(direction)) {
                        /*
                         * Disable input when moving, move a full step, then let enemies move,
                         * then restore the input.
                         */
                        entity.addAction(
                            Actions.sequence(
                                //Actions.run(InputDisabler.instance),
                                Actions.delay(RenderSystem.MoveSpeed),
                                Actions.run(new Runnable(){
                                    @Override
                                    public void run(){
                                        ms.process();
                                    }
                                }),
                                Actions.delay(RenderSystem.MoveSpeed)
                                //Actions.run(InputDisabler.instance)
                            )
                        );
                    }
                    walkTimer = 0f;
                }
                return true;
            }
            else if (telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Dungeon.Assist) {
                entity.changeState(Assist);
                return true;
            }
            else if (telegram.message == Quest.Actions.Notify) {
                String notification = telegram.extraInfo.toString();
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, notification);
            }
            else if (telegram.message == Messages.Dungeon.Dead && telegram.extraInfo == entity.playerService.getPlayer()) {
                entity.changeState(WanderState.Dead);
            }
            else if (telegram.message == Messages.Dungeon.Exit) {
                entity.changeState(WanderState.Exit);
            }
            return false;
        }

    },
    Assist("Return", "Heal Me", "Go Home") {

        @Override
        public void enter(WanderUI entity) {
            entity.showGoddess("Hello there, what is it that you need?");
            entity.refreshButtons();
            entity.setFocus(entity.getButtonList());
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Dungeon.Heal) {
                entity.changeState(Sacrifice_Heal);
                return true;
            }
            else if (telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Dungeon.Leave) {
                entity.changeState(Sacrifice_Leave);
                return true;
            }
            entity.changeState(Wander);
            return false;
        }

    },
    Sacrifice_Heal("I've changed my mind", "Sacrifice Your Loot") {

        @Override
        public void enter(WanderUI entity) {
            int healCost = entity.dungeonService.getProgress().healed + 1;
            
            entity.showGoddess("So you'd like me to heal you?\nThat'll cost you " + healCost + " loot.");
            entity.showLoot();
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Dungeon.Sacrifice) {
                
                int healCost = entity.dungeonService.getProgress().healed + 1;
                if (entity.playerService.getInventory().sacrifice(entity.sacrifices, healCost)) {
                    for (Item i : entity.sacrifices.keys()) {
                        ServiceManager.getService(ScoreTracker.class).increment(NumberValues.Loot_Sacrificed, entity.sacrifices.get(i, 0));
                    }
                    entity.playerService.recover();
                    entity.dungeonService.getProgress().healed = healCost;
                    entity.changeState(Wander);
                    return true;
                }
                else {
                    entity.showGoddess("That's not enough!\nYou need to sacrifice " + healCost + " items");
                }
            }
            else {
                entity.changeState(Wander);
            }
            return false;
        }

    },
    Sacrifice_Leave("I've changed my mind", "Sacrifice Your Loot") {

        @Override
        public void enter(WanderUI entity) {
            int fleeCost = entity.dungeonService.getProgress().depth;
            entity.showGoddess("Each floor deep you are costs another piece of loot.\nYou're currently "
                    + fleeCost + " floors deep.");
            entity.showLoot();
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {

            if (telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Dungeon.Sacrifice) {
                int fleeCost = entity.dungeonService.getProgress().depth;
                if (entity.playerService.getInventory().sacrifice(entity.sacrifices, fleeCost)) {
                    entity.changeState(Exit);
                    return true;
                }
                else {
                    entity.showGoddess("That's not enough!\nYou need to sacrifice " + fleeCost + " items");
                }
            }
            else {
                entity.changeState(Wander);
            }
            return false;
        }

    },
    /**
     * Player is dead. drop loot and make fun of him
     */
    Dead("Return Home") {
        @Override
        public void enter(WanderUI entity) {
            entity.hideGoddess();
            entity.message.setText("You are dead.\n\nYou have dropped all your new loot.\nSucks to be you.");
            entity.dialog.clearListeners();
            entity.dialog.setVisible(true);
            entity.dialog.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(1f, .5f)));
            entity.fader.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(.5f, .5f)));

            entity.refreshButtons();
            entity.setFocus(entity.getButtonList());
        }
        
        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if ((telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Dungeon.Close) ||
                telegram.message == Messages.Interface.Close){
                entity.dialog.addAction(Actions.alpha(0f, 1f));
                entity.fader.addAction(Actions.sequence(Actions.alpha(1f, 2f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        SceneManager.switchToScene("town");
                    }
                })));
                return true;
            }
            return false;
        }
    },
    /**
     * Player strategically left. Don't drop loot but still make fun of him
     */
    Exit( "Return Home" ) {
        @Override
        public void enter(WanderUI entity) {
            entity.hideGoddess();
            entity.message
                    .setText("You decide to leave the dungeon.\nWhether that was smart of you or not, you got some sweet loot, and that's what matters.");
            entity.dialog.clearListeners();

            entity.dialog.setVisible(true);
            entity.dialog.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(1f, .5f)));
            entity.fader.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(.5f, .5f)));

            entity.refreshButtons();
            entity.setFocus(entity.getButtonList());
        }

        @Override
        public boolean onMessage(final WanderUI entity, Telegram telegram) {
            if ((telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Dungeon.Close) ||
                telegram.message == Messages.Interface.Close) {
                entity.dialog.addAction(Actions.alpha(0f, 1f));
                entity.fader.addAction(Actions.sequence(Actions.alpha(1f, 2f), Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        SceneManager.switchToScene("town");
                        entity.dungeonService.clear();
                    }
                })));
                return true;
            }
            return false;
        }

    };

    private final String[] buttons;
    
    WanderState(String... buttons) {
        this.buttons = buttons;
    }
    
    @Override
    public String[] defineButtons() {
        return buttons;
    }
    
    @Override
    public void enter(WanderUI entity) {
    }

    @Override
    public void update(WanderUI entity) {
    }

    @Override
    public void exit(WanderUI entity) {
    }

    @Override
    public boolean onMessage(WanderUI entity, Telegram telegram) {
        return false;
    }
}