package com.nhydock.storymode.scenes.newgame;

import java.util.Iterator;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Array;
import com.nhydock.gdx.scenes.scene2d.runnables.PlaySound;
import com.nhydock.scenes.scene2d.ui.extras.Card;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.scenes.Messages;

enum UIState implements State<NewUI> {
        Choose() {

            @Override
            public void enter(final NewUI entity) {
                entity.slots.addAction(
                    Actions.sequence(
                        Actions.run(entity.getScene().getInput().disableMe),
                        Actions.alpha(0f), 
                        Actions.moveBy(0, 20f),
                        Actions.delay(1.5f),
                        Actions.parallel(
                            Actions.alpha(1, .3f),
                            Actions.moveBy(0, -20f, .3f, Interpolation.circleOut)
                        ),
                        Actions.run(entity.getScene().getInput().enableMe)
                    )
                );
                entity.slots.setTouchable(Touchable.enabled);
                entity.setKeyboardFocus(entity.slots);
            }

            @Override
            public void update(NewUI entity) {
            }

            @Override
            public void exit(NewUI entity) {
                entity.slots.addAction(
                    Actions.sequence(
                        Actions.touchable(Touchable.disabled),
                        Actions.alpha(0f, .5f)
                    )
                );
            }

            @Override
            public boolean onMessage(final NewUI entity, Telegram telegram) {
                
                if (telegram.message == Messages.Interface.Selected) {
                    entity.audio.playSfx(DataDirs.Sounds.accept);
                    int index = (Integer)telegram.extraInfo;
                    Card card = entity.slots.findActor("slot " + index);
                    if (card == null){
                        entity.sm.changeState(Create);
                        return true;
                    }
                    Object summary = card.getUserObject();
                    
                    if (summary == null) {
                        entity.sm.changeState(Create);
                    } else {
                        entity.player.load(index);
                        entity.sm.changeState(Over);
                    }
                    return true;
                }
                return false;
            }

        },
        Create() {

            @Override
            public void enter(final NewUI entity) {
                entity.getPointer().setVisible(false);
                entity.createFrame.addAction(
                    Actions.sequence(
                        Actions.run(entity.getScene().getInput().disableMe),
                        Actions.alpha(0f),
                        Actions.delay(.8f),
                        Actions.alpha(1f, .3f), 
                        Actions.run(new Runnable() {
                            @Override
                            public void run() {
                                entity.createFocus.setFocus(entity.number);
                                entity.createFrame.setTouchable(Touchable.childrenOnly);
                            }
                        }),
                        Actions.run(entity.getScene().getInput().enableMe)
                    )
                );
                
            }

            @Override
            public void update(NewUI entity) { /* Do Nothing */
                
            }

            @Override
            public void exit(NewUI entity) {
                entity.getPointer().setVisible(false);;
                entity.createFrame.setTouchable(Touchable.disabled);
                entity.createFrame.addAction(
                    Actions.sequence(
                        Actions.run(new PlaySound(DataDirs.Sounds.accept)),
                        Actions.alpha(0f, .2f) 
                    )
                );
            }

            @Override
            public boolean onMessage(NewUI entity, Telegram telegram) {
                if (telegram.message == Messages.Interface.Button) {
                    entity.sm.changeState(Story);
                }
                return false;
            }

        },
        Story() {

            Iterator<String> story;

            @Override
            public void enter(final NewUI entity) {
                entity.parent.prepareStory();
                
                entity.goddess.setDrawable(entity.getSkin(), entity.player.getWorship());
                entity.you.setDrawable(entity.getSkin(), entity.player.getGender());

                Array<String> data = new Array<String>();
                entity.setKeyboardFocus(entity.textTable);
                
                Scanner s = new Scanner(Gdx.files.classpath(
                        DataDirs.GameData + "title_" + entity.player.getGender() + ".txt").read());
                while (s.hasNextLine()) {
                    data.add(s.nextLine());
                }
                s.close();
                story = data.iterator();

                entity.goddess.addAction(
                    Actions.sequence(
                        Actions.alpha(0f),
                        Actions.delay(4f),
                        Actions.alpha(1f, 3f),
                        Actions.forever(
                            Actions.sequence(
                                Actions.moveTo(entity.getWidth() * .6f, 58f, 2f),
                                Actions.moveTo(entity.getWidth() * .6f, 48f, 2f)
                            )
                        )
                    )
                );
                entity.you.addAction(
                    Actions.sequence(
                        Actions.run(entity.getScene().getInput().disableMe),
                        Actions.alpha(0f), 
                        Actions.alpha(1f, 1f),
                        Actions.delay(7f),
                        Actions.addAction(Actions.alpha(1f, .5f), entity.textTable),
                        Actions.run(entity.getScene().getInput().enableMe)
                    )
                );
                entity.textTable.addAction(
                    Actions.alpha(0f)
                );

                entity.addActor(entity.you);
                entity.addActor(entity.goddess);
                entity.addActor(entity.textTable);

                entity.act();
                entity.setKeyboardFocus(entity.textTable);
                entity.textTable.setTouchable(Touchable.enabled);
            }

            @Override
            public void update(final NewUI entity) {
                if (story.hasNext()) {
                    entity.text.addAction(
                        Actions.sequence(
                            Actions.alpha(0f, .1f), 
                            Actions.run(new Runnable() {

                                @Override
                                public void run() {
                                    String dialog = story.next();
                                    entity.text.setText(dialog);
                                    entity.textTable.pack();
                                }

                            }), 
                            Actions.alpha(1f, .1f)
                        )
                    );
                }
                else {
                    entity.textTable.addAction(Actions.alpha(0f, 1f));
                    entity.goddess.clearActions();
                    entity.goddess.addAction(
                        Actions.sequence(
                            Actions.run(entity.getScene().getInput().disableMe),
                            Actions.rotateBy(360f, 1f),
                            Actions.rotateBy(360f, .75f),
                            Actions.rotateBy(360f, .5f),
                            Actions.rotateBy(360f, .25f),
                            Actions.parallel(
                                Actions.repeat(10, Actions.rotateBy(360f, .25f)),
                                Actions.sequence(
                                    Actions.delay(1f), 
                                    Actions.run(new PlaySound(DataDirs.Sounds.shimmer)), 
                                    Actions.moveTo(entity.goddess.getX(), entity.getHeight() + 128f, .4f)
                                )
                            ),
                            Actions.addAction(
                                Actions.sequence(
                                    Actions.moveTo(entity.getWidth() / 2f - entity.you.getWidth() / 2f, 48f, 2f),
                                    Actions.delay(2f), 
                                    Actions.alpha(0f, 2f), 
                                    Actions.run(new Runnable() {

                                        @Override
                                        public void run() {
                                            entity.sm.changeState(Over);
                                        }

                                    })
                                ), 
                                entity.you
                            )
                        )
                    );
                }
            }

            @Override
            public void exit(final NewUI entity) {
            }

            @Override
            public boolean onMessage(NewUI entity, Telegram telegram) {
                if (telegram.message == Messages.Interface.Close) {
                    entity.sm.changeState(Over);
                    return true;
                } 
                else if (telegram.message == Messages.Interface.Notify) {
                    update(entity);
                    return true;
                }
                return false;
            }
        },
        Over() {

            @Override
            public void enter(NewUI entity) {
            }

            @Override
            public void update(NewUI entity) {
            }

            @Override
            public void exit(NewUI entity) {
            }

            @Override
            public boolean onMessage(NewUI entity, Telegram telegram) {
                return false;
            }

        };
    }