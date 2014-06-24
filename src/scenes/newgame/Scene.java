package scenes.newgame;

import scenes.SceneManager;
import scenes.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.common.Storymode;

public class Scene implements Screen {

	AssetManager manager;
	Stage stage;
	
	boolean loaded;
	
	int difficulty = 3;
	
	@Override
	public void render(float delta) {
		//don't do anything while trying to load
		if (!manager.update()){
			return;
		}
		
		if (!loaded)
		{
			init();
		}
		
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}
	
	@Override
	public void show() {
		Viewport v = new ScalingViewport(Scaling.fit, Storymode.InternalRes[0], Storymode.InternalRes[1]);
		stage = new Stage(v);
		manager = new AssetManager();
		manager.load("data/uiskin.json", Skin.class);
	}
	
	private void init()
	{
		loaded = true;
		
		//fetch assets
		final Skin skin = manager.get("data/uiskin.json", Skin.class);
				
		stage.clear();
		
		final Group window = UI.makeWindow(skin, 500, 180);
		window.setPosition(stage.getWidth()/2-window.getWidth()/2, stage.getHeight()/2-window.getHeight()/2);
		
		Label prompt = new Label("Please choose your difficulty", skin, "prompt");
		prompt.setPosition(window.getWidth()/2-prompt.getPrefWidth()/2, window.getHeight()-(36f + prompt.getPrefHeight()));
		window.addActor(prompt);
		
		HorizontalGroup buttons = new HorizontalGroup();
		
		final Label number = new Label(""+difficulty, skin);
		
		final TextButton left = new TextButton("<", skin);
		left.pad(0,10,0,10);
		left.addListener(new InputListener() {
			
			@Override
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				if (button == Buttons.LEFT)
				{
					difficulty = Math.max(0, difficulty-1);
					number.setText(""+difficulty);
					return true;
				}
				return false;
			}

			@Override
			public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				left.setChecked(true);
			}
			
			@Override
			public void exit(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				left.setChecked(false);
			}
		});
		
		final TextButton right = new TextButton(">", skin);
		right.pad(0,10,0,10);
		right.addListener(new InputListener() {
			
			@Override
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				if (button == Buttons.LEFT)
				{
					difficulty = Math.min(5, difficulty+1);
					number.setText(""+difficulty);
					return true;
				}
				return false;
			}

			@Override
			public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				right.setChecked(true);
			}
			
			@Override
			public void exit(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				right.setChecked(false);
			}
		});
		
		final TextButton accept = new TextButton("START", skin);
		accept.align(Align.center);
		accept.setSize(80, 32);
		accept.pad(5);
		accept.setPosition(window.getWidth()/2-accept.getWidth()/2, 10f);
		accept.addListener(new ChangeListener(){

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				
				window.addAction(
					Actions.sequence(
						Actions.alpha(0f, .5f),
						Actions.run(new Runnable(){
							
							@Override
							public void run() {
								next();
							}
							
						})
					)
				);
			}
		});
		
		buttons.addActor(left);
		buttons.addActor(number);
		buttons.addActor(right);
		buttons.space(30);
		buttons.pack();
		buttons.setPosition(window.getWidth()/2-buttons.getWidth()/2, window.getHeight()/2-(buttons.getHeight()/2 + 10));
		
		window.addActor(prompt);
		window.addActor(buttons);
		window.addActor(accept);
		
		window.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.delay(1.5f),
				Actions.alpha(1f, .3f)
			)
		);
		
		stage.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				boolean hit = false;
				if (keycode == Keys.LEFT || keycode == Keys.A)
				{
					hit = true;
					difficulty = Math.max(1, difficulty-1);
					number.setText(""+difficulty);		
				}
				else if (keycode == Keys.RIGHT || keycode == Keys.D)
				{
					hit = true;
					difficulty = Math.min(5, difficulty+1);
					number.setText(""+difficulty);		
				}
				else if (keycode == Keys.ENTER || keycode == Keys.SPACE)
				{
					hit = true;
					window.addAction(
						Actions.sequence(
							Actions.alpha(0f, .5f),
							Actions.delay(1.5f),
							Actions.run(new Runnable(){
								
								@Override
								public void run() {
									next();
								}
							})
						)
					);
				}
				
				return hit;
			}
		});
		
		stage.addActor(window);
		stage.act();
		Gdx.input.setInputProcessor(stage);
		
	}

	private void next()
	{
		Storymode.startGame(difficulty);
		SceneManager.switchToScene("town");
	}
	
	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		stage.dispose();
		
	}

}
