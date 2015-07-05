package com.nhydock.gdx.scenes.scene2d.runnables;

import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class ChangeText implements Runnable {
    Label label;
    String set;
    
    public ChangeText(Label l, String str) {
        label = l;
        set = str;
    }
    
    @Override
    public void run() {
        label.setText(set);
    }

}
