package scenes.dungeon.ui;

import github.nhydock.ssm.ServiceManager;
import scene2d.runnables.GotoScene;
import scene2d.ui.extras.LabeledTicker;
import scenes.Messages;
import scenes.dungeon.Direction;
import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;

import core.common.Input;
import core.components.Position;
import core.components.Stats;
import core.datatypes.Inventory;
import core.datatypes.npc.Trainer;
import core.datatypes.quests.Quest;
import core.service.interfaces.IPlayerContainer;

/**
 * Handles state based ui menu logic and switching
 * 
 * @author nhydock
 *
 */
public enum WanderState implements UIState {
    Global(){

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Quest.Actions.Notify) {
                String notification = telegram.extraInfo.toString();
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, notification);
            }
            else if (telegram.message == Messages.Dungeon.Dead && telegram.extraInfo == entity.playerService.getPlayer()) {
                entity.changeState(WanderState.Dead);
                return true;
            }
            else if (telegram.message == Messages.Dungeon.Exit) {
                entity.changeState(WanderState.Exit);
                return true;
            }
            return false;
        }
    },
    Wander("Request Assistance") {

        private float walkTimer = -1f;
        private final Vector2 mousePos = new Vector2();

        @Override
        public void enter(WanderUI entity) {
            entity.display.addAction(Actions.alpha(1f, .2f));
            entity.setKeyboardFocus(entity.getRoot());
            entity.getPointer().setVisible(false);
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
                    //Gdx.app.log("Wander", "Stopping player");
                    if (Direction.valueOf(Gdx.input) != null) {
                        walkTimer = -1f;
                    }
                }
                else {
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
            else if (telegram.message == Messages.Dungeon.Target) {
                entity.changeState(Targeting);
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
            else if (telegram.message == Messages.NPC.TRAINER) {
                Trainer trainer = (Trainer)telegram.extraInfo;
                entity.trainingMenu.setTrainer(trainer);
                entity.changeState(Train);
                return true;
            }
            
            return false;
        }
        
        @Override
        public boolean keyDown(WanderUI entity, int keycode) {

            if (Input.ACCEPT.match(keycode)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Assist);
                return true;
            }
            
            if (Input.CANCEL.match(keycode)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Target);
                return true;
            }
            
            if (Input.ACTION.match(keycode)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action);
                return true;
            }
            
            Direction to = Direction.valueOf(keycode);
            if (to != null) {
                if (Input.ACTION.isPressed()){
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action, to);
                } else {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
                }
                return true;
            }
            
            return false;
        }

        @Override
        public boolean keyUp(WanderUI entity, int keycode) {
            Direction to = Direction.valueOf(keycode);
            if (to != null) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement);
            }
            return true;
        }
        
        @Override
        public boolean touchDown(WanderUI entity, float x, float y, int button) {
            if (button == Buttons.LEFT) {
                Direction to = Direction.valueOf(x, y, entity.getWidth(), entity.getHeight());
                if (Gdx.input.isButtonPressed(Buttons.RIGHT)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action, to);
                } else {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
                }
            } else if (button == Buttons.RIGHT){
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action);
            } else if (button == Buttons.MIDDLE){
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Target);
            }
            return true;
        }
        
        @Override
        public void touchUp(WanderUI entity, float x, float y, int button){
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement);
        }
    },
    Targeting() {

        private final Vector2 mousePos = new Vector2();
        
        //keeps track if the cursor has moved
        private boolean moved = false;

        @Override
        public void enter(WanderUI entity) {
            entity.display.addAction(Actions.alpha(1f, .2f));
            entity.assistMenu.hide();
            entity.setKeyboardFocus(entity.getRoot());
            
            final RenderSystem rs = entity.dungeonService.getEngine().getSystem(RenderSystem.class);
            rs.toggleCursor();
        }
        
        @Override
        public void exit(WanderUI entity) {
            final RenderSystem rs = entity.dungeonService.getEngine().getSystem(RenderSystem.class);
            rs.toggleCursor();
        }


        @Override
        public void update(WanderUI entity) {
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {

            if (telegram.message == Messages.Dungeon.Movement) {
                Direction direction = (Direction) telegram.extraInfo;
                if (direction == null) {
                    return false;
                }
                else {
                    final RenderSystem rs = entity.dungeonService.getEngine().getSystem(RenderSystem.class);
                    rs.moveCursor(direction);
                    moved = true;
                    return true;
                }
            }
            else if (telegram.message == Messages.Dungeon.Target) {
                final RenderSystem rs = entity.dungeonService.getEngine().getSystem(RenderSystem.class);
                IPlayerContainer playerService = ServiceManager.getService(IPlayerContainer.class);
                Position p = Position.Map.get(playerService.getPlayer());
                int[] cursorLoc = rs.getCursorLocation();
                //lock to axis if moved
                if (moved) {
                    int xDistance = Math.abs(p.getX() - cursorLoc[0]);
                    int yDistance = Math.abs(p.getX() - cursorLoc[1]);
                
                    //if already locked to an axis, just ignore and allow firing the spell
                    if (xDistance == 0 || yDistance == 0) {
                        moved = false;
                    } 
                    //lock to the nearest axis
                    else {
                        if (cursorLoc[0] < p.getX() && xDistance > yDistance) {
                            cursorLoc[1] = p.getY();
                            rs.moveCursor(cursorLoc);
                        }
                        else if (cursorLoc[0] > p.getX() && xDistance > yDistance) {
                            cursorLoc[1] = p.getY();
                            rs.moveCursor(cursorLoc);
                        }
                        else if (cursorLoc[1] < p.getY() && xDistance <= yDistance) {
                            cursorLoc[0] = p.getX();
                            rs.moveCursor(cursorLoc);
                        }
                        else if (cursorLoc[1] > p.getY() && xDistance <= yDistance) {
                            cursorLoc[0] = p.getX();
                            rs.moveCursor(cursorLoc);
                        }
                        moved = false;
                        return true;
                    }
                }
                
                //do cool attacky stuff
                final MovementSystem ms = entity.dungeonService.getEngine().getSystem(MovementSystem.class);
                int[][] path = ms.fireSpell(cursorLoc);
                if (path != null) {
                    rs.fireSpell(path);
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
                    return true;
                }
                return false;
            }
            else if (telegram.message == Messages.Interface.Close) {
                entity.changeState(Wander);
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
        
        @Override
        public boolean keyDown(WanderUI entity, int keycode) {

            if (Input.ACCEPT.match(keycode)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Target);
                return true;
            }
            
            if (Input.CANCEL.match(keycode)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                return true;
            }
            
            Direction to = Direction.valueOf(keycode);
            if (to != null) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
                return true;
            }
            
            return false;
        }

        @Override
        public boolean keyUp(WanderUI entity, int keycode) {
            return false;
        }
        
        @Override
        public boolean touchDown(WanderUI entity, float x, float y, int button) {
            if (button == Buttons.LEFT) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Target);
                return true;
            } else if (button == Buttons.MIDDLE){
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
            }
            return true;
        }
        
        @Override
        public void touchUp(WanderUI entity, float x, float y, int button){
            
        }
         
        @Override
        public void mouseMoved(WanderUI entity, Vector2 mousePos) {
            Engine e = entity.dungeonService.getEngine();
            RenderSystem rs = e.getSystem(RenderSystem.class);
            
            rs.moveCursor(mousePos);
        }
    },
    Assist() {

        int cost;
        boolean heal;
        
        @Override
        public void enter(WanderUI entity) {
            entity.assistMenu.show();
        }

        @Override
        public void exit(WanderUI entity) {
            entity.assistMenu.hide();
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Dungeon.Heal) {
                cost = entity.dungeonService.getProgress().healed + 1;
                heal = true;
                entity.assistMenu.showHeal(cost);
                entity.setKeyboardFocus(entity.sacrificeMenu.getGroup());
                entity.getPointer().setPosition(entity.sacrificeMenu.getFocus(), Align.topLeft);
                entity.getPointer().setVisible(true);
                return true;
            }
            else if (telegram.message == Messages.Dungeon.Leave) {
                cost = entity.dungeonService.getProgress().depth;
                heal = false;
                entity.assistMenu.showEscape(cost);
                entity.setKeyboardFocus(entity.sacrificeMenu.getGroup());
                entity.getPointer().setPosition(entity.sacrificeMenu.getFocus(), Align.topLeft);
                entity.getPointer().setVisible(true);
                return true;
            } 
            else if (telegram.message == Messages.Dungeon.Sacrifice) {
                if (entity.playerService.getInventory().sacrifice(entity.sacrificeMenu.getSacrifice(), cost)) {
                    if (heal){
                        if (Inventory.getSumOfItems(entity.sacrificeMenu.getSacrifice()) >= cost * 2) {
                            entity.playerService.getAilments().reset();
                        }
                        entity.sacrificeMenu.sacrifice();
                        entity.playerService.recover();
                        entity.dungeonService.getProgress().healed = cost;
                        entity.changeState(Wander);
                    } else {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Exit);
                        entity.changeState(Exit);
                    }
                    return true;
                }
            } 
            else if (telegram.message == Messages.Interface.Close) {
                entity.changeState(Wander);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyDown(WanderUI entity, int keycode){
            if (!entity.sacrificeMenu.getGroup().isVisible()) {
                if (Input.LEFT.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Heal);
                    return true;
                }
                if (Input.RIGHT.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Leave);
                    return true;
                }
            }
            if (Input.CANCEL.match(keycode)){
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                return true;
            }
            return false;
        }
        
    }, 
    Train() {

        @Override
        public void enter(WanderUI entity) {
            entity.trainingMenu.show();
            entity.setKeyboardFocus(entity.sacrificeMenu.getGroup());
            entity.getPointer().setPosition(entity.sacrificeMenu.getFocus(), Align.topLeft);
            entity.getPointer().setVisible(true);
        }

        @Override
        public void exit(WanderUI entity) {
            entity.trainingMenu.hide();
        }

        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Dungeon.Sacrifice) {
                Stats stats = entity.playerService.getPlayer().getComponent(Stats.class);
                if (entity.trainingMenu.getTrainer().sacrifice(entity.sacrificeMenu.getSacrifice(), stats)) {
                    entity.playerService.getInventory().sacrifice(entity.sacrificeMenu.getSacrifice(), 0);
                    entity.trainingMenu.getTrainer().train(stats);
                    entity.sacrificeMenu.sacrifice();
                    entity.changeState(Wander);
                    return true;
                }
            } 
            return false;
        }

        @Override
        public boolean keyDown(WanderUI entity, int keycode){
            if (Input.CANCEL.match(keycode)){
                entity.changeState(Wander);
                return true;
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
            entity.messageWindow.setTouchable(Touchable.childrenOnly);
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
        
        @Override
        public boolean keyDown(WanderUI entity, int keycode) {
            if (Input.ACCEPT.match(keycode) || Input.CANCEL.match(keycode)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                return true;
            }
            return false;
        }
        
        @Override
        public boolean touchDown(WanderUI entity, float x, float y, int button) {
            if (button == Buttons.LEFT || button == Buttons.RIGHT) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
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
            entity.messageWindow.setTouchable(Touchable.childrenOnly);
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

        @Override
        public boolean keyDown(WanderUI entity, int keycode) {
            if (Input.ACCEPT.match(keycode) || Input.CANCEL.match(keycode)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                return true;
            }
            return false;
        }
        
        @Override
        public boolean touchDown(WanderUI entity, float x, float y, int button) {
            if (button == Buttons.LEFT || button == Buttons.RIGHT) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                return true;
            }
            return false;
        }
    },
    SelectFloor(){
        @Override
        public void enter(WanderUI entity) {
            entity.display.addAction(Actions.alpha(0f));
            entity.floorSelect.addAction(
                Actions.sequence(
                    Actions.run(entity.getScene().getInput().disableMe),
                    Actions.alpha(0f),
                    Actions.scaleTo(.5f, .5f),
                    Actions.parallel(
                        Actions.alpha(1f, .15f),
                        Actions.scaleTo(1, 1, .2f, Interpolation.circleOut)
                    ),
                    Actions.run(entity.getScene().getInput().enableMe)
                )
            );
            
            LabeledTicker<Integer> ticker = entity.floorSelect.findActor("ticker");
            entity.setKeyboardFocus(ticker);
            entity.floorSelect.setTouchable(Touchable.enabled);
        }
        
        @Override
        public void exit(WanderUI entity) {
            entity.floorSelect.addAction(
                Actions.sequence(
                    Actions.run(entity.getScene().getInput().disableMe),
                    Actions.alpha(1f),
                    Actions.scaleTo(1f, 1f),
                    Actions.parallel(
                        Actions.alpha(0f, .15f),
                        Actions.scaleTo(4, 4, .2f, Interpolation.circleOut)
                    ),
                    Actions.run(entity.getScene().getInput().enableMe)
                )
            );
            entity.floorSelect.setTouchable(Touchable.enabled);
        }
        
        @Override
        public boolean keyDown(WanderUI entity, int keycode) {
            if (Input.ACCEPT.match(keycode) || Input.CANCEL.match(keycode)) {
                LabeledTicker<Integer> ticker = entity.floorSelect.findActor("ticker");
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Warp, ticker.getValue());
                return true;
            }
            return false;
        }
        
        
        @Override
        public boolean onMessage(WanderUI entity, Telegram telegram) {
            if (telegram.message == Messages.Dungeon.Warp) {
                entity.changeState(Wander);
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
    
    public boolean touchDown(WanderUI entity, float x, float y, int button) {
        if (button == Buttons.RIGHT){
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
            return true;
        };
        return false;
    }
    
    public void touchUp(WanderUI entity, float x, float y, int button) {
    }
    
    public boolean keyDown(WanderUI entity, int keycode) {
        if (Input.CANCEL.match(keycode)) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
            return true;
        }
        return false;
    }
    
    public boolean keyUp(WanderUI entity, int keycode) {
        return false;
    }
    
    public void mouseMoved(WanderUI entity, Vector2 mousePos) {
        
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