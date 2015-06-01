package scenes;

import scene2d.InputDisabler;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.LabeledTicker;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;

import core.DataDirs;
import core.common.Input;
import core.components.Stats;
import core.service.interfaces.IAudioManager;
import core.service.interfaces.IPlayerContainer;
import github.nhydock.ssm.ServiceManager;

/**
 * Encapsulates the Level Up Dialog's actions and styling.
 * The dialog provides a way to distribute skill points to a player's stats
 * upon level up.  It displays a series of tickers corresponding with each
 * status category.
 * @author nhydock
 *
 */
public class LevelUpDialog {
    
    private Group window;
    private Label pointLabel;
    private LabeledTicker<Integer> strTicker;
    private LabeledTicker<Integer> defTicker;
    private LabeledTicker<Integer> spdTicker;
    private LabeledTicker<Integer> vitTicker;
    private FocusGroup focusGroup;
    
    private int points;
    
    public LevelUpDialog(Skin skin) {

        focusGroup = new FocusGroup();
        focusGroup.setVisible(false);

        window = new Window("", skin, "square");
        window.setSize(500, 450);
        window.setOrigin(Align.center);
        
        final Table table = new Table();
        table.setFillParent(true);
        table.center().top().pack();

        Label prompt = new Label("You've Leveled Up!", skin, "prompt");
        prompt.setAlignment(Align.center);
        table.add(prompt).expandX().fillX().padBottom(20).colspan(3);
        table.row();

        pointLabel = new Label("Points 0", skin, "prompt");
        pointLabel.setAlignment(Align.center);

        LabeledTicker<Integer>[] tickers = new LabeledTicker[4];
        tickers[0] = strTicker = new LabeledTicker<Integer>("Strength", new Integer[] { 0 }, skin);
        tickers[1] = defTicker = new LabeledTicker<Integer>("Defense", new Integer[] { 0 }, skin);
        tickers[2] = spdTicker = new LabeledTicker<Integer>("Speed", new Integer[] { 0 }, skin);
        tickers[3] = vitTicker = new LabeledTicker<Integer>("Vitality", new Integer[] { 0 }, skin);
        
        for (final LabeledTicker<Integer> ticker : tickers) {
            ticker.setLeftAction(new Runnable() {

                @Override
                public void run() {
                    final IAudioManager audio = ServiceManager.getService(IAudioManager.class);
                    audio.playSfx(DataDirs.Sounds.tick);
                    if (ticker.getValueIndex() > 0) {
                        ticker.defaultLeftClick.run();
                        setPoints(points + 1);
                    }
                }

            });
            ticker.setRightAction(new Runnable() {

                @Override
                public void run() {
                    final IAudioManager audio = ServiceManager.getService(IAudioManager.class);
                    audio.playSfx(DataDirs.Sounds.tick);
                    if (ticker.getValueIndex() < ticker.length() && points > 0) {
                        ticker.defaultRightClick.run();
                        setPoints(points - 1);
                    }
                }

            });

            table.center();
            table.add(ticker).expandX().fillX().pad(0, 50f, 10f, 50f).colspan(3);
            table.row();

            focusGroup.add(ticker);
        }

        table.add(pointLabel).expandX().fillX().colspan(3);
        window.addActor(table);

        final TextButton accept = new TextButton("START", skin);
        accept.align(Align.center);
        accept.setSize(80, 32);
        accept.pad(5);
        accept.setPosition(window.getWidth() / 2f, 20f, Align.bottom);

        accept.addListener(new InputListener() {
            @Override
            public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                accept.setChecked(true);
            }

            @Override
            public void exit(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                accept.setChecked(false);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (points > 0) {
                    final IAudioManager audio = ServiceManager.getService(IAudioManager.class);
                    audio.playSfx(DataDirs.Sounds.accept);
                    return false;
                }

                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                return true;
            }
        });
        window.addActor(accept);
        window.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (Input.DOWN.match(keycode)) {
                    focusGroup.next();
                    return true;
                }
                if (Input.UP.match(keycode)) {
                    focusGroup.prev();
                    return true;
                }
                if (Input.ACCEPT.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                    return true;
                }
                return false;
            }
        });
        window.setColor(1, 1, 1, 0);
        window.setTouchable(Touchable.disabled);
    }
    
    public Group getGroup(){
        return window;
    }
    
    public FocusGroup getFocusGroup(){
        return focusGroup;
    }
    
    protected void setPoints(int points) {
        this.points = points;
        this.pointLabel.setText(String.format("Points %d", points));
    }
    
    public boolean isVisible(){
        return window.isTouchable();
    }
    
    /**
     * Private level up state for GameUI. Provides a shared way of for any GameUI to
     * be capable of providing the level up dialog
     * 
     * @author nhydock
     *
     */
    public static class LevelUpState {
        private static final int POINTS_REWARDED = 5;

        public static void enter(final LevelUpDialog entity) {
            entity.setPoints(POINTS_REWARDED);

            IPlayerContainer playerService = ServiceManager.getService(IPlayerContainer.class);
            Entity player = playerService.getPlayer();
            Stats s = Stats.Map.get(player);
            Integer[] str = new Integer[POINTS_REWARDED + 1];
            Integer[] def = new Integer[POINTS_REWARDED + 1];
            Integer[] spd = new Integer[POINTS_REWARDED + 1];
            Integer[] vit = new Integer[POINTS_REWARDED + 1];
            for (int i = 0; i < POINTS_REWARDED + 1; i++) {
                str[i] = s.getStrength() + i;
                def[i] = s.getDefense() + i;
                spd[i] = s.getEvasion() + i;
                vit[i] = s.getVitality() + i;
            }
            ;

            entity.strTicker.changeValues(str);
            entity.defTicker.changeValues(def);
            entity.spdTicker.changeValues(spd);
            entity.vitTicker.changeValues(vit);

            entity.window.addAction(
                Actions.sequence(
                    Actions.scaleTo(.4f, .4f),
                    Actions.alpha(0f),
                    Actions.parallel(
                            Actions.scaleTo(1, 1, .2f, Interpolation.circleOut),
                            Actions.alpha(1f, .15f, Interpolation.circleOut)
                    )
                )
            );
            entity.window.setTouchable(Touchable.enabled);
        }

        public static void exit(final LevelUpDialog entity) {
            entity.window.addAction(
                Actions.sequence(
                    Actions.alpha(1f),
                    Actions.scaleTo(1f,1f),
                    Actions.alpha(0f, .15f, Interpolation.circleOut)
                )
            );
            entity.window.setTouchable(Touchable.disabled);
            
            IPlayerContainer playerService = ServiceManager.getService(IPlayerContainer.class);
            Entity player = playerService.getPlayer();
            Stats s = Stats.Map.get(player);

            s.levelUp(new int[] { entity.strTicker.getValue(), entity.defTicker.getValue(),
                                  entity.spdTicker.getValue(), entity.vitTicker.getValue() });
            entity.strTicker.setValue(0);
            entity.defTicker.setValue(0);
            entity.spdTicker.setValue(0);
            entity.vitTicker.setValue(0);
            entity.points = 0;
            
            final IAudioManager audio = ServiceManager.getService(IAudioManager.class);
            audio.playSfx(DataDirs.Sounds.accept);
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
        }

        public static boolean onMessage(LevelUpDialog entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Close) {
                if (entity.points > 0) {
                    final IAudioManager audio = ServiceManager.getService(IAudioManager.class);
                    audio.playSfx(DataDirs.Sounds.tick);
                    return false;
                }
                else {
                    exit(entity);
                    return true;
                }
            }
            return false;
        }
    }
}
