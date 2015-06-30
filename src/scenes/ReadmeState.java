package scenes;

import scene2d.runnables.SendMessage;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Align;

import core.service.implementations.InputHandler;

public class ReadmeState implements State<UI> {

    public static final ReadmeState instance = new ReadmeState();
    
    private boolean showing = false;
    
    @Override
    public void enter(UI entity) {
        InputHandler input = ServiceManager.getService(InputHandler.class);
        entity.getReadme().addAction(
            Actions.sequence(
                Actions.run(input.disableMe),
                Actions.moveToAligned(entity.getWidth()/2f, entity.getHeight(), Align.bottom),
                Actions.moveToAligned(entity.getWidth()/2f, entity.getHeight()/2f, Align.center, .25f, Interpolation.circleOut),
                Actions.run(new SendMessage(Messages.Interface.Focus, entity.getReadme())),
                Actions.run(input.enableMe)
            )
        );
    }

    @Override
    public void update(UI entity) {
        if (!showing) {
            enter(entity);
            showing = true;
        }
    }

    @Override
    public void exit(UI entity) {
        InputHandler input = ServiceManager.getService(InputHandler.class);
        entity.getReadme().addAction(
            Actions.sequence(
                Actions.run(input.disableMe),
                Actions.moveToAligned(entity.getWidth()/2f, entity.getHeight()/2f, Align.center),
                Actions.moveToAligned(entity.getWidth()/2f, entity.getHeight(), Align.bottom, .25f, Interpolation.circleOut),Actions.run(new SendMessage(Messages.Interface.Focus, entity.getReadme())),
                Actions.run(input.enableMe)
            )
        );
        showing = false;
    }

    @Override
    public boolean onMessage(UI entity, Telegram telegram) {
        if (telegram.message == Messages.Readme.Close) {
            entity.getStateMachine().setGlobalState(null);
            exit(entity);
            return true;
        }
        return false;
    }

}
