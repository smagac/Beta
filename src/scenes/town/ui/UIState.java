package scenes.town.ui;

import scenes.GameUI;

import com.badlogic.gdx.ai.fsm.State;

/**
 * Wrapped state for town ui, forcing define buttons
 * 
 * @author nhydock
 *
 */
interface UIState<E extends GameUI> extends State<E> {
    public String[] defineButtons();
}