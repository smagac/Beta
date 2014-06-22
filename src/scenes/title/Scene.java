package scenes.title;

import scenes.SceneManager;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.common.Storymode;

public class Scene implements Screen {
	
	AssetManager manager;
	Stage stage;
	
	boolean loaded;
	
	Music bgm;
	
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
		manager.load("assets/title.json", Skin.class);
		manager.load("assets/audio/title.mp3", Music.class);
	}

	/**
	 * Initialize the ui and load all assets
	 */
	private void init()
	{
		loaded = true;
		
		//TODO fetch assets
		final Skin skin = manager.get("assets/title.json", Skin.class);
		
		//TODO create title sequence
		
		//initial text
		{
			Table textGrid = new Table();
			textGrid.setFillParent(true);
			textGrid.pad(40f);
			
			Label text = new Label("Less than a week before TooManyGames, think I can crap out a roguelike in that time?", skin);
			text.setWrap(true);
			text.setAlignment(Align.center);
			text.addAction(
				Actions.sequence(
					Actions.alpha(0f),
					Actions.alpha(1f, 1f)
				)
			);
			textGrid.add(text).expandX().fillX();
			text = new Label("~Nick", skin);
			text.setWrap(true);
			text.setAlignment(Align.right);
			text.addAction(
				Actions.sequence(
					Actions.alpha(0f),
					Actions.delay(3f),
					Actions.alpha(1f, 1f)
				)
			);
			textGrid.row();
			textGrid.add(text).expandX().fillX().padRight(60f);
			textGrid.addAction(
				Actions.sequence(
					Actions.alpha(1f),
					Actions.delay(8f),
					Actions.alpha(0f, 2f),
					Actions.run(new Runnable(){

						@Override
						public void run() {
							bgm = manager.get("assets/audio/title.mp3", Music.class);
							bgm.play();
						}
						
					})
				)
			);
			stage.addActor(textGrid);
		}
		
		//credits animation
		{
			Table textGrid = new Table();
			textGrid.setFillParent(true);
			textGrid.pad(40f);
			
			Label text = new Label("Graphics & Programming", skin);
			text.setAlignment(Align.center);
			textGrid.add(text).expandX().fillX();
			textGrid.row();
			text = new Label("Nicholas Hydock", skin);
			text.setAlignment(Align.center);
			textGrid.add(text).expandX().fillX();
			textGrid.row();
			text = new Label(" ", skin);
			text.setAlignment(Align.center);
			textGrid.add(text).expandX().fillX();
			textGrid.row();
			text = new Label("Ideas, Suggestions, Emotional Support, & Bros4Lyfe", skin);
			text.setAlignment(Align.center);
			textGrid.add(text).expandX().fillX();
			textGrid.row();
			text = new Label("Patrick Flanagan", skin);
			text.setAlignment(Align.center);
			textGrid.add(text).expandX().fillX();
			textGrid.row();
			text = new Label("Matthew Hydock", skin);
			text.setAlignment(Align.center);
			textGrid.add(text).expandX().fillX();
			textGrid.row();
			text = new Label("Andrew Hoffman", skin);
			text.setAlignment(Align.center);
			textGrid.add(text).expandX().fillX();
			
			textGrid.addAction(
				Actions.sequence(
					Actions.alpha(0f),
					Actions.delay(10f),
					Actions.alpha(1f, .75f),
					Actions.delay(4f),
					Actions.alpha(0f, .75f)
				)
			);
			stage.addActor(textGrid);
		}
		
		//display tool logos
		{
			Group group = new Group();
			Image tools = new Image(skin.getDrawable("tools"));
			tools.setPosition(stage.getWidth()/2-tools.getWidth()/2, stage.getHeight()/2-tools.getHeight()/2);
			Table table = new Table();
			Label label = new Label("Title theme", skin);
			table.add(label).expandX().fillX();
			table.row();
			label = new Label("Anamanaguchi -  Helix Nebula", skin);
			table.add(label).expandX().fillX();
			table.row();
			label = new Label("available on FreeMusicArchive.org", skin);
			table.add(label).expandX().fillX();
			table.row();
			
			table.setPosition(stage.getWidth()/2 - table.getPrefWidth()/2, table.getPrefHeight());
			group.addActor(tools);
			group.addActor(table);
			group.addAction(
				Actions.sequence(
					Actions.alpha(0f), 
					Actions.delay(16f), 
					Actions.alpha(1f, .75f), 
					Actions.delay(4f), 
					Actions.alpha(0f, .75f)
				)
			);
			stage.addActor(group);
		}
		
		//cool animation
		{
			Group cool = new Group();
			cool.setSize(stage.getWidth(), stage.getHeight());
			Group group = new Group();
			Image cliff = new Image(skin.getRegion("cliff"));
			group.addAction(
				Actions.sequence(
					Actions.moveTo(0, -cliff.getHeight()),
					Actions.delay(24f),
					Actions.moveTo(0, 0, 4f)
				)
			);
			group.addActor(cliff);
			final Image character = new Image(skin.getRegion("back"));
			character.setSize(96f, 96f);
			group.addActor(character);
			character.addAction(
				Actions.sequence( 
					Actions.moveTo(200f, 200f),
					Actions.delay(31f),
					Actions.run(new Runnable(){

						@Override
						public void run() {
							character.setDrawable(skin.getDrawable("character"));
						}
						
					}),
					Actions.moveTo(-character.getWidth()/2, -character.getHeight(), 1f),
					Actions.delay(10f),
					Actions.run(new Runnable(){

						@Override
						public void run() {
							character.setDrawable(skin.getDrawable("back"));
						}
						
					}),
					Actions.moveTo(200f, 200f, 1f),
					Actions.delay(.2f),
					Actions.run(new Runnable(){

						@Override
						public void run() {
							character.setDrawable(skin.getDrawable("character"));
						}
						
					})
				)	
			);
			
			Image castle = new Image(skin.getRegion("castle"));
			castle.addAction(
				Actions.sequence(
					Actions.moveTo(0f, stage.getHeight()-castle.getHeight()),
					Actions.delay(24f),
					Actions.moveTo(stage.getWidth()-castle.getWidth(), stage.getHeight()-castle.getHeight(), 4f)
				)
			);
			Image lightning = new Image(skin.getRegion("lightning"));
			lightning.setPosition(stage.getWidth()-castle.getWidth(), stage.getHeight()-castle.getHeight());
			lightning.addAction(
				Actions.sequence(
					Actions.alpha(0f),
					Actions.delay(29f),
					Actions.alpha(1f, .1f),
					Actions.delay(.3f),
					Actions.alpha(.3f, .3f),
					Actions.alpha(1f, .1f),
					Actions.delay(.3f),
					Actions.alpha(0f, 1f)
				)
			);
			
			Image logo = new Image(skin.getRegion("logo"));
			logo.addAction(
				Actions.sequence(
					Actions.alpha(0f),
					Actions.moveTo(0, stage.getHeight() - logo.getHeight() + 5f),
					Actions.delay(34f),
					Actions.alpha(1f, 1f),
					Actions.forever(
						Actions.sequence(
							Actions.moveTo(0, stage.getHeight() - logo.getHeight() + 5f, .75f),
							Actions.moveTo(0, stage.getHeight() - logo.getHeight() - 5f, .75f)
						)
					)
				)
			);
			
			cool.addActor(castle);
			cool.addActor(lightning);
			cool.addActor(group);
			cool.addActor(logo);
			
			cool.addAction(
				Actions.sequence(
					Actions.alpha(0f), 
					Actions.delay(24f), 
					Actions.alpha(1f), 
					Actions.delay(30f), 
					Actions.alpha(0f, 1f)
				)
			);
			stage.addActor(cool);
		}
		
		//show title
		
		stage.addAction(Actions.sequence(
			Actions.alpha(1f),
			Actions.delay(55f),
			Actions.alpha(0f),
			Actions.run(new Runnable(){

				@Override
				public void run() {
					bgm.stop();
					SceneManager.switchToScene("town");
				}
				
			})
		));
		
		//make sure all initial steps are set
		stage.act();
	}
	
	@Override
	public void hide() {
	
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
		// TODO Auto-generated method stub
	}

}
