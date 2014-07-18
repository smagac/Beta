package scenes.endgame;

import java.util.Scanner;

import scenes.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.common.Tracker;
import core.common.Tracker.NumberValues;
import core.common.Tracker.StringValues;
import core.service.IPlayerContainer;

public class EndUI extends UI {

	private Scanner story;
	private Table textTable;
	private Label text;
	
	private Image goddess;
	private Image you;
	private Image bg;
	
	private boolean over;
	
	Scene parent;
	
	IPlayerContainer player;

	private Group stats;
	
	public EndUI(Scene scene, AssetManager manager, IPlayerContainer player) {
		super(manager);
		parent = scene;
		this.player = player;
		manager.load("data/end.json", Skin.class);
	}

	@Override
	public void init() {
		skin = manager.get("data/end.json", Skin.class);
		story = new Scanner(Gdx.files.classpath("core/data/end.txt").read());
		
		clear();
		
		//explore icon
		{
			bg = new Image(skin.getRegion("bg"));
			bg.setPosition(0f, 0f);
			bg.setFillParent(true);
			addActor(bg);
		}
		
		you = new Image(skin.getRegion("character"));
		you.setScaling(Scaling.stretch);
		you.setSize(64f, 64f);
		you.setPosition(getWidth()*.4f, 48f);
		addActor(you);
		
		//goddess
		goddess = new Image(skin.getRegion("goddess"));
		goddess.setScaling(Scaling.stretch);
		goddess.setSize(128f, 128f);
		goddess.setPosition(getWidth() * .6f, getHeight());
		goddess.setOrigin(64f, 64f);
		goddess.addAction(Actions.sequence(
				Actions.delay(8f),
				Actions.run(new Runnable(){
					@Override
					public void run() {
						manager.get(DataDirs.shimmer, Sound.class).play(.4f);
					}
				}),
				Actions.parallel(
					Actions.sizeTo(128f, 128f),
					Actions.repeat(3, Actions.rotateBy(360f, .2f)),
					Actions.moveTo(getWidth()*.6f, 48f, .6f)			
				),
				Actions.run(new Runnable(){
					@Override
					public void run() {
						manager.get(DataDirs.hit, Sound.class).play(1, .5f, 0);
					}
				}),
				Actions.delay(1f),
				Actions.run(new Runnable(){
					@Override
					public void run() {
						next();
						addListener(new InputListener(){
							@Override
							public boolean keyDown(InputEvent evt, int keycode)
							{
								boolean hit = false;
								if (keycode == Keys.ESCAPE || keycode == Keys.BACKSPACE)
								{
									hit = true;
									end();
								}
								else if (keycode == Keys.ENTER || keycode == Keys.SPACE)
								{
									hit = true;
									next();
								}						
								return hit;
							}
							
							@Override
							public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
							{
								if (button == Buttons.LEFT)
								{
									next();
								}
								return false;
							}
						});
					}
				}),
				Actions.forever(
					Actions.sequence(
						Actions.moveTo(getWidth() * .6f, 58f, 5f),
						Actions.moveTo(getWidth() * .6f, 48f, 5f)
					)
				)
			)
		);
		
		addActor(goddess);
		
		textTable = new Table();
		textTable.center();
		textTable.setWidth(getWidth());
		textTable.setFillParent(true);
		textTable.addAction(Actions.sequence(Actions.alpha(0f)));
		
		//text
		text = new Label("", skin, "prompt");
		text.setAlignment(Align.center);
		text.setWrap(true);
		
		textTable.add(text).center().expandX().fillX().pad(60f).padBottom(0f);
		
		TextButton down = new TextButton("More", skin);
		textTable.row();
		down.pad(5, 30, 5, 30);
		textTable.add(down).right().padRight(60f);
		textTable.pack();
		addActor(textTable);
		
		fader = new Image(skin.getDrawable("fader"));
		fader.setFillParent(true);
		fader.addAction(Actions.sequence(
			Actions.alpha(1f),
			Actions.alpha(0f, 8f)
		));
		addActor(fader);
		
		stats = UI.makeWindow(skin, 400, 320, true);
		stats.setPosition(getWidth()/2-stats.getWidth()/2, getHeight()/2-stats.getHeight()/2);
		{
			Table view = new Table();
			view.setFillParent(true);
			view.pad(32f);
			
			Label score = new Label(String.format("Score: %09d", Tracker.score()), skin, "prompt2");
			score.setAlignment(Align.center);
			view.top();
			view.add(score).expandX().fillX();
			view.row();
			Label rank = new Label(Tracker.rank(), skin);
			rank.setAlignment(Align.center);
			view.add(rank).expandX().fillX();
			view.row();
			
			Table t = new Table();
			t.setFillParent(true);
			t.pad(16f);
			
			for (NumberValues val : Tracker.NumberValues.values())
			{
				Label title = new Label(val.toString(), skin);
				Label value = new Label(""+val.value(), skin);
				
				title.setAlignment(Align.left);
				value.setAlignment(Align.right);
				t.top();
				t.add(title).expandX().fillX();
				t.add(value).expandX().fillX();
				t.row();
			}
			
			t.add();
			t.row();
			
			for (StringValues val : Tracker.StringValues.values())
			{
				Label title = new Label(val.toString(), skin);
				Label value = new Label(""+val.max(), skin);
				
				title.setAlignment(Align.left);
				value.setAlignment(Align.right);
				t.top();
				t.add(title).expandX().fillX();
				t.add(value).expandX().fillX();
				t.row();
			}
			
			ScrollPane p = new ScrollPane(t, skin);
			p.setFillParent(false);
			p.setFadeScrollBars(false);
			view.add(p).expand().fill();
			
			stats.addActor(view);
		}
		stats.setVisible(false);
		addActor(stats);
		
		
		act();
	}

	private void next()
	{
		if (story.hasNextLine())
		{
			advanceStory();
		}
		else
		{
			end();
		}
	}
	

	/**
	 * Shows the progress/score of the player after completing the story
	 */
	private void showStats() {
		textTable.setVisible(false);
		stats.setVisible(true);
		stats.addAction(Actions.sequence(
			Actions.alpha(0),
			Actions.alpha(1f, .2f)
		));
	}

	protected void hideStats() {
		textTable.setVisible(true);
		stats.setVisible(false);
	}

	private void end()
	{
		textTable.addAction(Actions.alpha(0f, 1f));
		goddess.clearActions();
		goddess.addAction(Actions.sequence(
			Actions.run(new Runnable(){

				@Override
				public void run() {
					getRoot().clearListeners();
				}
				
			}),
			Actions.rotateBy(360f, 1f),
			Actions.rotateBy(360f, .75f),
			Actions.rotateBy(360f, .5f),
			Actions.rotateBy(360f, .25f),
			Actions.parallel(
					Actions.repeat(10, Actions.rotateBy(360f, .25f)),
					Actions.sequence(
						Actions.delay(1f),
						Actions.run(new Runnable(){

							@Override
							public void run() {
								manager.get(DataDirs.shimmer, Sound.class).play();
							}
							
						}),
						Actions.moveTo(goddess.getX(), getHeight()+128f, .4f)
					)
				)
			)
		);
		you.addAction(
			Actions.sequence(
				Actions.delay(5f),
				Actions.moveTo(getWidth()/2f - you.getWidth()/2f, 48f, 3f)
		));
		fader.addAction(
			Actions.sequence(
				Actions.delay(8f),
				Actions.alpha(1f, 3f)
			)
		);
		addAction(
			Actions.sequence(
				Actions.delay(13f),
				Actions.alpha(0f),
				Actions.run(new Runnable(){

					@Override
					public void run() {
						over = true;
					}
					
				})
			)
		);
	}

	private void advanceStory()
	{
		textTable.addAction(Actions.alpha(0f));
		textTable.addAction(
			Actions.sequence(
				Actions.alpha(0f, .1f),
				Actions.run(new Runnable(){

					@Override
					public void run() {
						String dialog = story.nextLine();
						if (dialog.equals("#"))
						{
							text.setText("");
							showStats();
						}
						else
						{
							hideStats();
							text.setText(String.format(dialog, player.getFullTime()));
							textTable.pack();
						}
					}
					
				}),
				Actions.alpha(1f, .1f)
			)
		);
	}


	public boolean isDone() {
		return over;
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		if (story != null) {
			story.close();
		}
	}
}
