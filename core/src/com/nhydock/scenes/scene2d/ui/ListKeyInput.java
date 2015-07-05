package com.nhydock.scenes.scene2d.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.nhydock.storymode.common.Input;

public class ListKeyInput extends InputListener {

    @Override
    public boolean keyDown(InputEvent evt, int keycode) {
        if (evt.getListenerActor().getClass() == List.class){
            List list = (List)evt.getListenerActor();
        
            if (Input.DOWN.match(keycode)) {
                list.setSelectedIndex(Math.min(list.getItems().size - 1, list.getSelectedIndex() + 1));
                return true;
            }
            if (Input.UP.match(keycode)) {
                list.setSelectedIndex(Math.max(0, list.getSelectedIndex() - 1));
                return true;
            }
        }
        return false;
    }
    
}
