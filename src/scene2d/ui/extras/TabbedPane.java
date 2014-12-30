package scene2d.ui.extras;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Simple widget for making stacked "panes" that can be switched between using
 * tabs.
 * <p/>
 * Buttons defining tabs are passed into the pane. Each button must have a pane
 * assigned to its user object in order to perform proper stack switching.
 * 
 * @author nhydock
 *
 */
public class TabbedPane extends Table {

    public static class Messages {
        // used when switching tabs in a ui
        public static final int ChangeTabs = 0x2000;
    }

    ButtonGroup<Button> buttons;
    Table tabs;
    Table panes;

    boolean vertical;

    Runnable changedTab;

    public TabbedPane(final ButtonGroup<Button> buttons, final boolean vertical) {
        final TabbedPane me = this;
        this.vertical = vertical;
        this.buttons = buttons;
        buttons.setMaxCheckCount(1);
        ChangeListener li = new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {

                for (Button b : buttons.getButtons()) {
                    ((Actor) b.getUserObject()).setVisible(b.isChecked());
                }
                if (changedTab != null) {
                    changedTab.run();
                }
                MessageDispatcher.getInstance().dispatchMessage(0, null, null, Messages.ChangeTabs, me);
            }
        };

        this.tabs = new Table();
        this.tabs.left();
        this.panes = new Table();
        Stack panes = new Stack();
        for (Button b : buttons.getButtons()) {
            b.addListener(li);
            Actor pane = (Actor) b.getUserObject();
            pane.setVisible(b.isChecked());
            panes.addActor(pane);
            this.tabs.add(b);
            if (vertical) {
                this.tabs.row();
            }
        }
        this.panes.add(panes).expand().fill();

        buttons.getButtons().get(0).setChecked(true);

        if (!vertical) {
            bottom().add(this.tabs).expandX().fillX().row();
        }
        else {
            bottom().add(this.tabs).expandY().fillY().top();
        }
        add(this.panes).expand().fill();

        this.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if ((!vertical && (keycode == Keys.LEFT || keycode == Keys.A))
                        || (vertical && (keycode == Keys.UP || keycode == Keys.W))) {
                    buttons.getButtons().get(Math.max(0, buttons.getButtons().indexOf(buttons.getChecked(), true) - 1))
                            .setChecked(true);
                    return true;
                }
                if ((!vertical && (keycode == Keys.RIGHT || keycode == Keys.D))
                        || (vertical && (keycode == Keys.DOWN || keycode == Keys.S))) {
                    buttons.getButtons()
                            .get(Math.min(buttons.getButtons().size - 1,
                                    buttons.getButtons().indexOf(buttons.getChecked(), true) + 1)).setChecked(true);
                    return true;
                }
                return false;
            }
        });

        pack();
        this.setTouchable(Touchable.childrenOnly);
    }

    public void showTab(int index) {
        this.buttons.getButtons().get(index).setChecked(true);
    }

    public int getOpenTabIndex() {
        return this.buttons.getButtons().indexOf(this.buttons.getChecked(), true);
    }

    /**
     * Adds a specific action to changing tabs
     */
    public void setTabAction(Runnable action) {
        this.changedTab = action;
    }
}
