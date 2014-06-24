package scenes.dungeon;

import scenes.SceneManager;
import scenes.UI;
import GenericSystems.RenderSystem;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;

import scenes.Scene;

public class WanderUI extends UI {

	ScrollPane logPane;
	List<String> log;
	
	Label message;
	Group dialog;
	
	RenderSystem floor;
	
	public WanderUI(Scene<WanderUI> scene, AssetManager manager) {
		super(scene, manager);
	}

	@Override
	protected void extend() {
		
		messageWindow.clear();
		
		log = new List<String>(skin);
		log.setWidth(messageWindow.getWidth());
		logPane = new ScrollPane(log, skin);
		logPane.setSize(messageWindow.getWidth(), messageWindow.getHeight());
		messageWindow.addActor(logPane);
		
		dialog = makeWindow(skin, 400, 250);
		message = new Label("", skin, "promptsm");
		message.setWrap(true);
		message.setPosition(40f, 40f);
		message.setWidth(320f);
		message.setHeight(170f);
		dialog.addActor(message);
		dialog.setVisible(false);
		
		dialog.setPosition(display.getWidth()/2-dialog.getWidth()/2, display.getHeight()/2-dialog.getHeight()/2);
		display.addActor(dialog);
	}
	
	protected void externalRender()
	{
		floor.setView(getBatch(), getCamera());
		floor.process();
	}
	
	protected void setFloor(World world)
	{
		floor = world.getSystem(RenderSystem.class);
	}

	@Override
	protected void triggerAction(int index) {
		//ignore
	}

	@Override
	public String[] defineButtons() {
		return null;
	}

	@Override
	protected Actor[] focusList() {
		return new Actor[]{dialog};
	}
	
	public void setMessage(String msg)
	{
		//update the battle log
		log.getItems().add(msg);
		log.pack();
		
		float y = Math.max(0, (log.getItems().size * log.getItemHeight()) + logPane.getHeight()/2);
		logPane.scrollTo(0, log.getHeight()-y, logPane.getWidth(), logPane.getHeight());
	}

	/**
	 * Player is dead. drop loot and make fun of him
	 */
	protected void dead() {
		message.setText("You are dead.\n\nYou have dropped all you new loot.\nSucks to be you.");
		dialog.clearListeners();
		dialog.addListener(new InputListener(){
			public boolean keyDown(InputEvent evt, int keycode)
			{
				if (keycode == Keys.ENTER || keycode == Keys.SPACE)
				{
					Gdx.input.setInputProcessor(null); //disable input
					
					//fade screen out and do stuff
					display.addAction(
						Actions.sequence(
							Actions.alpha(0f, 3f),
							Actions.run(new Runnable(){
								public void run(){
									SceneManager.switchToScene("town");
								}
							})
						)
					);
					return true;
				}
				return false;
			}
		});
	}
	
	/**
	 * Player is dead. drop loot and make fun of him
	 */
	protected void leave() {
		message.setText("You decide to leave the dungeon.\nWhether that was smart of you or not, you got some sweet loot, and that's what matters.");
		dialog.clearListeners();
		dialog.addListener(new InputListener(){
			public boolean keyDown(InputEvent evt, int keycode)
			{
				if (keycode == Keys.ENTER || keycode == Keys.SPACE)
				{
					Gdx.input.setInputProcessor(null); //disable input
					
					//fade screen out and do stuff
					display.addAction(
						Actions.sequence(
							Actions.alpha(0f, 3f),
							Actions.run(new Runnable(){
								public void run(){
									SceneManager.switchToScene("town");
								}
							})
						)
					);
					return true;
				}
				return false;
			}
		});
	}
}
