package scenes.dungeon.ui;

import scene2d.runnables.GotoScene;
import scenes.Messages;
import scenes.dungeon.Direction;
import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import core.datatypes.Inventory;
import core.datatypes.quests.Quest;

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
            entity.sacrificeMenu.hide();
            entity.setKeyboardFocus(entity.getRoot());
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
                        entity.getRoot().screenToLocalCoordinates(mousePos);
                        d = Direction.valueOf(mousePos, entity.getWidth(), entity.getHeight());
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
                    Gdx.app.log("Wander", "Stopping player");
                    if (Direction.valueOf(Gdx.input) != null) {
                        walkTimer = -1f;
                    }
                }
                else {
                    Gdx.app.log("Wander", "Moving player");
                    final MovementSystem ms = entity.dungeonService.getEngine().getSystem(MovementSystem.class);
                    if (ms.movePlayer(direction)) {
                        ms.process();
                        /*
                         * Disable input when moving, move a full step, then let enemies move,
                         * then restore the input.
                         */
                        entity.addAction(
                            Actions.sequence(
                                Actions.delay(RenderSystem.MoveSpeed)
                            )
                        );
                    }
                    walkTimer = 0f;
                }
                return true;
            }
            else if (telegram.message == Messages.Dungeon.Zoom) {
                RenderSystem rs = entity.dungeonService.getEngine().getSystem(RenderSystem.class);
                rs.toggleZoom();
                return true;
            }
            else if (telegram.message == Messages.Dungeon.Action) {
                final MovementSystem ms = entity.dungeonService.getEngine().getSystem(MovementSystem.class);
                if (telegram.extraInfo != null) {
                    if (ms.openAction((Direction)telegram.extraInfo)) {
                        ms.process();
                        /*
                         * Disable input when moving, move a full step, then let enemies move,
                         * then restore the input.
                         */
                        entity.addAction(
                            Actions.sequence(
                                Actions.delay(RenderSystem.MoveSpeed)
                            )
                        );
                    }
                } else {
                    int change = ms.changeFloor();
                    
                    if (change != -1) {
                        MessageDispatcher.getInstance().dispatchMessage(null, change);
                    }
                }
                return true;
            }
            else if (telegram.message == Messages.Dungeon.Assist) {
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
            entity.sacrificeMenu.show();
            entity.setKeyboardFocus(entity.sacrificeMenu.getGroup());
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Dungeon.Heal) {
                entity.changeState(Sacrifice_Heal);
                return true;
            }
            else if (telegram.message == Messages.Dungeon.Leave) {
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
            
            entity.sacrificeMenu.showHeal(healCost);
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Dungeon.Sacrifice) {
                
                int healCost = entity.dungeonService.getProgress().healed + 1;
                if (entity.playerService.getInventory().sacrifice(entity.sacrificeMenu.getSacrifice(), healCost)) {
                    if (Inventory.getSumOfItems(entity.sacrificeMenu.getSacrifice()) >= healCost * 2) {
                        entity.playerService.getAilments().reset();
                    }
                    entity.sacrificeMenu.sacrifice();
                    entity.playerService.recover();
                    entity.dungeonService.getProgress().healed = healCost;
                    entity.changeState(Wander);
                    return true;
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
            
            entity.sacrificeMenu.showEscape(fleeCost);
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {

            if (telegram.message == Messages.Dungeon.Sacrifice) {
                int fleeCost = entity.dungeonService.getProgress().depth;
                if (entity.playerService.getInventory().sacrifice(entity.sacrificeMenu.getSacrifice(), fleeCost)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Exit);
                    entity.changeState(Exit);
                    return true;
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
            entity.message.setText("You are dead.\n\nYou have dropped all your new loot.\nSucks to be you.");
            entity.messageWindow.addAction(
                Actions.sequence(
                    Actions.scaleTo(.5f, .5f),
                    Actions.moveBy(0, -20),
                    Actions.alpha(0f),
                    Actions.parallel(
                        Actions.scaleTo(1, 1, .2f, Interpolation.circleOut),
                        Actions.moveBy(0, 20, .2f, Interpolation.circleOut),
                        Actions.alpha(1f, .15f)
                    )    
                )
            );
            entity.fader.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(.5f, .5f)));
        }
        
        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Close){
                entity.fader.addAction(
                    Actions.sequence(
                        Actions.addAction(Actions.alpha(0f, .3f), entity.messageWindow),
                        Actions.alpha(1f, 2f), 
                        Actions.run(new GotoScene("town"))
                    )
                );
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
            entity.message.setText("You decide to leave the dungeon.\n \nWhether that was smart of you or not, you got some sweet loot, and that's what matters.");
            entity.messageWindow.addAction(
                Actions.sequence(
                    Actions.scaleTo(.5f, .5f),
                    Actions.moveBy(0, -20),
                    Actions.alpha(0f),
                    Actions.parallel(
                        Actions.scaleTo(1, 1, .2f, Interpolation.circleOut),
                        Actions.moveBy(0, 20, .2f, Interpolation.circleOut),
                        Actions.alpha(1f, .15f)
                    )    
                )
            );
            entity.fader.addAction(Actions.sequence(Actions.alpha(0f), Actions.alpha(.5f, .5f)));

        }

        @Override
        public boolean onMessage(final WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Close) {
                entity.fader.addAction(
                    Actions.sequence(
                        Actions.addAction(Actions.alpha(0f, .3f), entity.messageWindow),
                        Actions.alpha(1f, 2f),
                        Actions.run(new GotoScene("town"))
                    )
                );
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