package scenes.title;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

import core.common.SceneManager;
import scenes.UI;

public class TitleSequence extends UI {

	scenes.title.Scene parent;
	
	public TitleSequence(scenes.title.Scene parent, AssetManager manager) {
		super( manager);
		this.parent = parent;
		manager.load("data/title.json", Skin.class);
	}

	@Override
	public void init() {
		//create title sequence
		final Skin skin = manager.get("data/title.json", Skin.class);

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
			text = new Label("June 20, 2014", skin, "small");
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
							parent.startMusic();
						}
						
					})
				)
			);
			addActor(textGrid);
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
			addActor(textGrid);
		}
		
		//display tool logos
		{
			Group group = new Group();
			Image tools = new Image(skin.getDrawable("tools"));
			tools.setPosition(getWidth()/2-tools.getWidth()/2, getHeight()/2-tools.getHeight()/2);
			Table table = new Table();
			Label label = new Label("All music available on FreeMusicArchive.org", skin);
			table.add(label).expandX().fillX();
			table.row();
			
			table.setPosition(getWidth()/2 - table.getPrefWidth()/2, table.getPrefHeight());
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
			addActor(group);
		}
		
		//cool animation
		{
			Group cool = new Group();
			cool.setSize(getWidth(), getHeight());
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
					Actions.moveTo(0f, getHeight()-castle.getHeight()),
					Actions.delay(24f),
					Actions.moveTo(getWidth()-castle.getWidth(), getHeight()-castle.getHeight(), 4f)
				)
			);
			Image lightning = new Image(skin.getRegion("lightning"));
			lightning.setPosition(getWidth()-castle.getWidth(), getHeight()-castle.getHeight());
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
					Actions.moveTo(0, getHeight() - logo.getHeight() + 5f),
					Actions.delay(34f),
					Actions.alpha(1f, 1f),
					Actions.forever(
						Actions.sequence(
							Actions.moveTo(0, getHeight() - logo.getHeight() + 5f, 2f),
							Actions.moveTo(0, getHeight() - logo.getHeight() - 5f, 2f)
						)
					)
				)
			);
			
			Table table = new Table();
			
			Label label = new Label("Title theme", skin, "small");
			label.setAlignment(Align.right);
			table.add(label).expandX().fillX();
			table.row();
			label = new Label("Anamanaguchi - Helix Nebula", skin, "small");
			label.setAlignment(Align.right);
			table.add(label).expandX().fillX();
			table.row();
			table.pad(10f);
			table.pack();
			table.addAction(
				Actions.sequence(
					Actions.alpha(0f),
					Actions.moveTo(getWidth()-table.getWidth(), 0),
					Actions.delay(40f),
					Actions.alpha(1f, 1f)
				)
			);
		
			cool.addActor(castle);
			cool.addActor(lightning);
			cool.addActor(group);
			cool.addActor(logo);
			cool.addActor(table);
			
			cool.addAction(
				Actions.sequence(
					Actions.alpha(0f), 
					Actions.delay(24f), 
					Actions.alpha(1f), 
					Actions.delay(40f), 
					Actions.alpha(0f, 1f),
					Actions.delay(1f),
					Actions.run(new Runnable(){

						@Override
						public void run() {
							SceneManager.switchToScene("title");
						}
						
					}),
					Actions.delay(1.5f),
					Actions.run(new Runnable(){

						@Override
						public void run() {
						}
						
					})				
				)
			);
			addActor(cool);
			

			Label startLabel = new Label("Press Start", skin);
			startLabel.setPosition(getWidth()-360f, 120f);
			startLabel.addAction(Actions.sequence(
				Actions.alpha(0f),
				Actions.delay(38f),
				Actions.alpha(1f, .3f),
				Actions.delay(24f),
				Actions.alpha(0f, 1f)
			));
			addActor(startLabel);
			
		}

		//make sure all initial steps are set
		act();
	}

}
