package scenes.newgame;

import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.service.IPlayerContainer;
import scenes.UI;

public class NewUI extends UI {
	
	private int difficulty = 3;
	private int index = -1;

	private Scanner story;
	private Table textTable;
	private Label text;
	private Image goddess;
	private Image you;
	
	private boolean over;
	
	Scene parent;
	private ButtonGroup gender;
	private Label number;
	
	private IPlayerContainer player;
	
	public NewUI(Scene scene, AssetManager manager, IPlayerContainer p) {
		super(manager);
		parent = scene;
		manager.load("data/uiskin.json", Skin.class);
		player = p;
	}

	@Override
	public void init() {
		skin = manager.get("data/uiskin.json", Skin.class);
		
		clear();
		

		final Group frame = UI.makeWindow(skin, 580, 300);
		frame.setPosition(getWidth()/2-frame.getWidth()/2, getHeight()/2-frame.getHeight()/2);
		
		final Table window = new Table();
		window.setFillParent(true);
		window.pad(32f);
		
		//Difficulty
		{
			Label prompt = new Label("Please choose your difficulty", skin, "prompt");
			prompt.setAlignment(Align.center);
			window.add(prompt).expandX().fillX();
			window.row();
			
			HorizontalGroup buttons = new HorizontalGroup();
			
			number = new Label(""+difficulty, skin);
			
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
						manager.get(DataDirs.tick, Sound.class).play();
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
						manager.get(DataDirs.tick, Sound.class).play();
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
			
			
			
			buttons.addActor(left);
			buttons.addActor(number);
			buttons.addActor(right);
			buttons.space(30);
			buttons.pack();
			
			window.add(buttons).expandX();
			window.row();
		}
		//Gender
		{
			Label prompt = new Label("Choose your gender", skin, "prompt");
			prompt.setAlignment(Align.center);
			window.add(prompt).expandX().fillX();
			window.row();
			
			
			TextButton left = new TextButton("Male", skin);
			left.pad(0,10,0,10);
			left.setChecked(true);
			
			TextButton right = new TextButton("Female", skin);
			right.pad(0,10,0,10);
			
			gender = new ButtonGroup(left, right);
			
			window.add(left).fillX().pad(10f);
			window.add(right).fillX().pad(10f);
			
			window.row();
		}
		window.pack();
		
		final TextButton accept = new TextButton("START", skin);
		accept.align(Align.center);
		accept.setSize(80, 32);
		accept.pad(5);
		accept.setPosition(frame.getWidth()/2-accept.getWidth()/2, 10f);
		accept.addListener(new ChangeListener(){

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				
				frame.addAction(
					Actions.sequence(
						Actions.run(new Runnable(){

							@Override
							public void run() {
								manager.get(DataDirs.accept, Sound.class).play();
							}
							
						}),
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
		frame.addActor(accept);
		
		frame.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.delay(1.5f),
				Actions.alpha(1f, .3f)
			)
		);
		
		addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				boolean hit = false;
				if (keycode == Keys.LEFT || keycode == Keys.A)
				{
					hit = true;
					
					difficulty = Math.max(1, difficulty-1);
					manager.get(DataDirs.tick, Sound.class).play();
					number.setText(""+difficulty);		
				}
				else if (keycode == Keys.RIGHT || keycode == Keys.D)
				{
					hit = true;
					difficulty = Math.min(5, difficulty+1);
					manager.get(DataDirs.tick, Sound.class).play();
					number.setText(""+difficulty);		
				}
				else if (keycode == Keys.ENTER || keycode == Keys.SPACE)
				{
					hit = true;
					accept.setChecked(true);
				}
				
				return hit;
			}
		});
		
		frame.addActor(window);
		
		addActor(frame);
		
		act();
	}

	public int getDifficulty() {
		return difficulty;
	}
	
	private void next()
	{
		if (index == -1)
		{
			parent.prepareStory();
		}
		else if (story.hasNextLine())
		{
			advanceStory();
		}
		else
		{
			end();
		}
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
				Actions.moveTo(getWidth()/2f - you.getWidth()/2f, 48f, 2f),
				Actions.delay(2f),
				Actions.alpha(0f, 2f),
				Actions.run(new Runnable(){

					@Override
					public void run() {
						over = true;
					}
					
				})
		));
	}

	private void advanceStory()
	{
		textTable.addAction(Actions.alpha(1f));
		text.addAction(
			Actions.sequence(
				Actions.alpha(0f, .1f),
				Actions.run(new Runnable(){

					@Override
					public void run() {
						String dialog = story.nextLine();
						text.setText(dialog);
						textTable.pack();
					}
					
				}),
				Actions.alpha(1f, .1f)
			)
		);
		index++;
	}

	public boolean isDone() {
		return over;
	}
	
	public void prepareStory() {
		story = new Scanner(Gdx.files.classpath("core/data/title_"+player.getGender()+".txt").read());
		
		you = new Image(skin.getRegion(player.getGender()));
		you.setScaling(Scaling.stretch);
		you.setSize(64f, 64f);
		you.setPosition(getWidth()*.4f, 48f);
		you.addAction(Actions.sequence(Actions.alpha(0f),Actions.alpha(1f, 1f)));
		addActor(you);
		
		//goddess
		goddess = new Image(skin.getRegion(player.getWorship()));
		goddess.setScaling(Scaling.stretch);
		goddess.setSize(128f, 128f);
		goddess.setPosition(getWidth() * .6f, 48f);
		goddess.setOrigin(64f, 64f);
		goddess.addAction(Actions.sequence(
				Actions.alpha(0f),
				Actions.delay(4f),
				Actions.alpha(1f, 3f),
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
		
		//text
		text = new Label("", skin, "prompt");
		text.setAlignment(Align.center);
		text.setWrap(true);
		
		textTable.add(text).center().expandX().fillX().pad(60f);
		
		Label down = new Label("More", skin, "promptsm");
		textTable.row();
		textTable.add(down).right().padRight(80f);
		textTable.pack();
		textTable.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.delay(8f),
				Actions.run(new Runnable(){
	
					@Override
					public void run() {
						
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
						advanceStory();
					}
				})
			)
		);
		addActor(textTable);
		// TODO Auto-generated method stub
		
		act();
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		if (story != null) {
			story.close();
		}
	}

	public boolean getGender() {
		return gender.getButtons().get(0).isChecked();
	}
	
	@Override
	public void unhook() {
		player = null;
	}
}
