package com.nhydock.gdx.scenes.scene2d.runnables;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegraph;

public class SendMessage implements Runnable {

    Telegraph from;
    Telegraph to;
    int message;
    Object extra;
    
    public SendMessage(int m, Object e) {
        from = null;
        to = null;
        this.message = m;
        this.extra = e;
    }
    
    public SendMessage(Telegraph to, int m, Object e) {
        from = null;
        this.to = to;
        this.message = m;
        this.extra = e;
    }
    
    @Override
    public void run() {
        MessageManager.getInstance().dispatchMessage(from, to, message, extra);
    }

}
