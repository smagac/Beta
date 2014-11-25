package scenes.town.ui;

import com.badlogic.gdx.ai.fsm.State;

/**
 * Wrapped state for town ui, forcing define buttons
 * 
 * @author nhydock
 *
 */
interface UIState extends State<TownUI> {
    public String[] defineButtons();
}