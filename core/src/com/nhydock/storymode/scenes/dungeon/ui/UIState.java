package com.nhydock.storymode.scenes.dungeon.ui;

import com.badlogic.gdx.ai.fsm.State;

/**
 * Wrapped state for wander ui, forcing define buttons
 * 
 * @author nhydock
 *
 */
public interface UIState extends State<WanderUI> {
    public String[] defineButtons();
}