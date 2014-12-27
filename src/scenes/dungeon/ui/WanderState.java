package scenes.dungeon.ui;

import github.nhydock.ssm.SceneManager;
import scenes.GameUI;
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
import core.common.Tracker;
import core.components.Stats;
import core.datatypes.dungeon.Progress;
import core.datatypes.quests.Quest;

/**
 * Handles state based ui menu logic and switching
 * 
 * @author nhydock
 *
 */
public enum WanderState implements UIState {
    Wander() {

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

            if (telegram.message == MenuMessage.Movement) {
                Direction direction = (Direction) telegram.extraInfo;
                if (direction == null) {
                    walkTimer = -1f;
                }
                else {
                    entity.dungeonService.getEngine().getSystem(MovementSystem.class).movePlayer(direction);
                    walkTimer = 0f;
                }
                return true;
            }
            else if (telegram.message == MenuMessage.Assist) {
                entity.changeState(Assist);
                return true;
            }
            else if (telegram.message == Quest.Actions.Notify) {
                String notification = telegram.extraInfo.toString();
                MessageDispatcher.getInstance().dispatchMessage(0, null, null, GameUI.Messages.Notify, notification);
            }
            else if (telegram.message == MenuMessage.Dead) {
                entity.changeState(WanderState.Dead);
            }
            else if (telegram.message == MenuMessage.Exit) {
                entity.changeState(WanderState.Exit);
            }
            else if (telegram.message == MenuMessage.Refresh) {
                entity.refresh((Progress) telegram.extraInfo);
            }
            else if (telegram.message == MenuMessage.LevelUp) {
                entity.changeState(WanderState.LevelUp);
            }
            return false;
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "Request Assistance" };
        }

    },
    Assist() {

        @Override
        public void enter(WanderUI entity) {
            entity.showGoddess("Hello there, what is it that you need?");
            entity.refreshButtons();
            entity.setFocus(entity.getButtonList());
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == MenuMessage.Heal) {
                entity.changeState(Sacrifice_Heal);
                return true;
            }
            else if (telegram.message == MenuMessage.Leave) {
                entity.changeState(Sacrifice_Leave);
                return true;
            }
            entity.changeState(Wander);
            return false;
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "Return", "Heal Me", "Go Home" };
        }

    },
    Sacrifice_Heal() {

        @Override
        public void enter(WanderUI entity) {
            int healCost = entity.dungeonService.getProgress().healed + 1;
            
            entity.showGoddess("So you'd like me to heal you?\nThat'll cost you " + healCost + " loot.");
            entity.showLoot();
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "I've changed my mind", "Sacrifice Your Loot" };
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == MenuMessage.Sacrifice) {
                
                int healCost = entity.dungeonService.getProgress().healed + 1;
                if (entity.playerService.getInventory().sacrifice(entity.sacrifices, healCost)) {
                    for (int i = 0; i < entity.sacrifices.size; i++) {
                        Tracker.NumberValues.Loot_Sacrificed.increment();
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
    Sacrifice_Leave() {

        @Override
        public void enter(WanderUI entity) {
            int fleeCost = entity.dungeonService.getProgress().depth;
            entity.showGoddess("Each floor deep you are costs another piece of loot.\nYou're currently "
                    + fleeCost + " floors deep.");
            entity.showLoot();
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "I've changed my mind", "Sacrifice Your Loot" };
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {

            if (telegram.message == MenuMessage.Sacrifice) {
                int fleeCost = entity.dungeonService.getProgress().depth;
                if (entity.playerService.getInventory().sacrifice(entity.sacrifices, fleeCost)) {
                    for (int i = 0; i < entity.sacrifices.size; i++) {
                        Tracker.NumberValues.Loot_Sacrificed.increment();
                    }
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
    Dead() {
        @Override
        public void enter(WanderUI entity) {
            entity.hideGoddess();
            entity.message.setText("You are dead.\n\nYou have dropped all your new loot.\nSucks to be you.");
            entity.dialog.clearListeners();
            entity.dialog.setVisible(true);
            entity.dialog.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(1f, .5f)));
            entity.getFader().addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(.5f, .5f)));

            entity.refreshButtons();
            entity.setFocus(entity.getButtonList());
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "Return Home" };
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == GameUI.Messages.Close) {
                entity.dialog.addAction(Actions.alpha(0f, 1f));
                entity.getFader().addAction(Actions.sequence(Actions.alpha(1f, 2f), Actions.run(new Runnable() {
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
    Exit() {
        @Override
        public void enter(WanderUI entity) {
            entity.hideGoddess();
            entity.message
                    .setText("You decide to leave the dungeon.\nWhether that was smart of you or not, you got some sweet loot, and that's what matters.");
            entity.dialog.clearListeners();

            entity.dialog.setVisible(true);
            entity.dialog.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(1f, .5f)));
            entity.getFader().addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(.5f, .5f)));

            entity.refreshButtons();
            entity.setFocus(entity.getButtonList());
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "Return Home" };
        }

        @Override
        public boolean onMessage(final WanderUI entity, Telegram telegram) {
            if (telegram.message == GameUI.Messages.Close) {
                entity.dialog.addAction(Actions.alpha(0f, 1f));
                entity.getFader().addAction(Actions.sequence(Actions.alpha(1f, 2f), Actions.run(new Runnable() {
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

    },
    LevelUp() {

        private static final int POINTS_REWARDED = 5;

        @Override
        public void enter(WanderUI entity) {
            entity.hideGoddess();
            entity.levelUpGroup.setFocus(entity.levelUpGroup.getActors().first());

            entity.setPoints(POINTS_REWARDED);

            Stats s = entity.playerService.getPlayer();
            Integer[] str = new Integer[POINTS_REWARDED + 1];
            Integer[] def = new Integer[POINTS_REWARDED + 1];
            Integer[] spd = new Integer[POINTS_REWARDED + 1];
            Integer[] vit = new Integer[POINTS_REWARDED + 1];
            for (int i = 0; i < POINTS_REWARDED + 1; i++) {
                str[i] = s.getStrength() + i;
                def[i] = s.getDefense() + i;
                spd[i] = s.getEvasion() + i;
                vit[i] = s.getVitality() + i;
            }
            ;

            entity.strTicker.changeValues(str);
            entity.defTicker.changeValues(def);
            entity.spdTicker.changeValues(spd);
            entity.vitTicker.changeValues(vit);

            entity.levelUpDialog.setVisible(true);
            entity.levelUpGroup.setVisible(true);
            entity.levelUpDialog.addAction(Actions.moveTo(entity.levelUpDialog.getX(), entity.getHeight() / 2
                    - entity.levelUpDialog.getHeight() / 2, .3f));
            entity.levelUpDialog.setTouchable(Touchable.enabled);

            entity.setFocus(entity.levelUpDialog);
        }

        @Override
        public void exit(final WanderUI entity) {
            entity.levelUpDialog.addAction(Actions.sequence(Actions.run(new Runnable() {

                @Override
                public void run() {
                    entity.changeState(WanderState.Wander);
                    entity.hidePointer();
                    entity.setFocus(null);
                }

            }), Actions.moveTo(entity.levelUpDialog.getX(), entity.getHeight(), .3f), Actions.run(new Runnable() {

                @Override
                public void run() {
                    entity.levelUpDialog.setVisible(false);
                }

            })));
            entity.levelUpDialog.setTouchable(Touchable.disabled);
            entity.playerService.getPlayer().levelUp(
                    new int[] { entity.strTicker.getValue(), entity.defTicker.getValue(), entity.spdTicker.getValue(),
                            entity.vitTicker.getValue() });
            entity.strTicker.setValue(0);
            entity.defTicker.setValue(0);
            entity.spdTicker.setValue(0);
            entity.vitTicker.setValue(0);
            entity.points = 0;
            entity.getManager().get(DataDirs.accept, Sound.class).play();
        }

        @Override
        public String[] defineButtons() {
            return null;
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == GameUI.Messages.Close) {
                if (entity.points > 0) {
                    entity.getManager().get(DataDirs.tick, Sound.class).play();
                    return false;
                }
                else {
                    entity.changeState(Wander);
                    return true;
                }
            }
            return false;
        }

    };

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