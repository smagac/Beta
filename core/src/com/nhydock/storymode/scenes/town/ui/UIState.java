package com.nhydock.storymode.scenes.town.ui;

import com.badlogic.gdx.ai.fsm.State;
import com.nhydock.storymode.scenes.GameUI;

/**
 * Wrapped state for town ui, forcing define buttons
 * 
 * @author nhydock
 *
 */
interface UIState<E extends GameUI> extends State<E> {
    public String[] defineButtons();
}