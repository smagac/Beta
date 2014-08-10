package scenes.newgame;

import java.util.Iterator;
import java.util.Scanner;

import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.LabeledTicker;
import scenes.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.service.IPlayerContainer;
import core.service.IPlayerContainer.SaveSummary;

public class NewUI extends UI {

	private Table textTable;
	private Label text;
	private Image goddess;
	private Image you;
	
	private Group createFrame;
	private FocusGroup createFocus;
	
	private Group slotFrame;
	private FocusGroup slotFocus;
	
	Scene parent;
	private ButtonGroup gender;
	private LabeledTicker<Integer> number;
	
	private IPlayerContainer player;
	
	private StateMachine<NewUI> sm;

	
	public NewUI(Scene scene, AssetManager manager, IPlayerContainer p) {
		super(manager);
		parent = scene;
		player = p;
		
		sm = new DefaultStateMachine<NewUI>(this);
	}
	
	@Override
	protected void load() {
		manager.load("data/uiskin.json", Skin.class);	
	}

	@Override
	public void init() {
		skin = manager.get("data/uiskin.json", Skin.class);
		
		final NewUI ui = this;
		
		//create load data dialog
		{
			Group window = slotFrame = super.makeWindow(skin, 600, 300, true);
			Table table = new Table();
			
			FocusGroup focus = slotFocus = new FocusGroup();
			table.pad(32f);
			table.setFillParent(true);
			
			Table row = new Table();
			row.pad(0f);
			row.setBackground(skin.getDrawable("button_up"));
			row.add(new Label("New Game", skin, "prompt")).expandX().center();
			row.addListener(new InputListener(){

				@Override
				public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
				{
					if (button == Buttons.LEFT)
					{
						MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, slotFocus.getFocusedIndex());
						return true;
					}
					return false;
				}
			});
			row.setTouchable(Touchable.enabled);
			focus.add(row);
			table.add(row).expandX().fillX().height(60);
			table.row();
			for (int i = 1; i <= player.slots(); i++)
			{
				final int index = i;
				final SaveSummary s = player.summary(i);
				
				row = new Table();
				row.pad(0f);
				row.setBackground(skin.getDrawable("button_up"));
				if (s == null)
				{
					row.add(new Label("No Data", skin, "prompt")).expandX().center();
				}
				else
				{
					Image icon = new Image(skin, s.gender);
					row.add(icon).expand().center().colspan(1).size(32f, 32f);
					
					row.add(new Label(s.date, skin, "prompt")).expand().colspan(1).center();
					
					Table info = new Table();
					info.add(new Label("Crafting Completed: " + s.progress, skin, "smaller")).expand().colspan(1).right().row();
					info.add(new Label("Time: " + s.time, skin, "smaller")).expand().colspan(1).right().row();
					info.add(new Label(new String(new char[s.diff]).replace('\0', '*') + " difficulty", skin, "smaller")).expand().colspan(1).right();
					
					row.add(info).colspan(1).expand().right();
				}
				
				row.addListener(new InputListener(){

					@Override
					public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
					{
						if (button == Buttons.LEFT)
						{
							MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, index, s);
							return true;
						}
						return false;
					}
				});
				row.setTouchable(Touchable.enabled);
				
				focus.add(row);
				table.add(row).expandX().fillX().height(60);
				table.row();
			}
			
			window.addActor(table);
			window.setPosition(getWidth()/2-window.getWidth()/2, getHeight()/2 - window.getHeight()/2);
			addActor(window);
		}
		
		createFrame = UI.makeWindow(skin, 580, 300);
		createFrame.setPosition(getWidth()/2-createFrame.getWidth()/2, getHeight()/2-createFrame.getHeight()/2);
		
		final Table window = new Table(skin);
		window.setFillParent(true);
		window.center().top().pad(40f).pack();
		
		Label prompt = new Label("Please create a character", skin, "prompt");
		prompt.setAlignment(Align.center);
		
		window.add(prompt).expandX().fillX().padBottom(20);
		window.row();
		
		createFocus = new FocusGroup();
		//Difficulty
		{
			Integer[] values = {1, 2, 3, 4, 5};
			number = new LabeledTicker<Integer>("Difficulty", values, skin);
			number.setLeftAction(new Runnable(){

				@Override
				public void run() {
					manager.get(DataDirs.tick, Sound.class).play();
					number.defaultLeftClick.run();
				}
				
			});
			
			number.setRightAction(new Runnable(){
				
				@Override
				public void run() {
					manager.get(DataDirs.tick, Sound.class).play();
					number.defaultRightClick.run();
				}
				
			});
			window.add(number).expandX().fillX().pad(0, 50f, 10f, 50f);
			createFocus.add(number);
		}
		window.row();
		
		//Gender
		{
			Table table = new Table();
			prompt = new Label("Gender", skin, "prompt");
			prompt.setAlignment(Align.left);
			table.add(prompt).expandX().fillX();
			
			final TextButton left = new TextButton("Male", skin, "big");
			left.pad(10);
			left.setChecked(true);
			
			final TextButton right = new TextButton("Female", skin, "big");
			right.pad(10);
			
			gender = new ButtonGroup(left, right);
			
			window.center();
			table.add(left).width(80f).right().padRight(10f);
			table.add(right).width(80f).right();
			window.add(table).expandX().fillX().pad(0, 50f, 10f, 50f);
			createFocus.add(table);
			
			table.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					boolean hit = false;
					
					if (keycode == Keys.LEFT || keycode == Keys.A)
					{
						hit = true;
						left.setChecked(true);
					}
					if (keycode == Keys.RIGHT || keycode == Keys.D)
					{
						hit = true;
						right.setChecked(true);
					}
					return hit;
				}
			});
		}
		
		
		final TextButton accept = new TextButton("START", skin);
		accept.align(Align.center);
		accept.setSize(80, 32);
		accept.pad(5);
		accept.setPosition(createFrame.getWidth()/2-accept.getWidth()/2, 10f);
		accept.addListener(new ChangeListener(){

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				
				createFrame.addAction(
					Actions.sequence(
						Actions.run(new Runnable(){

							@Override
							public void run() {
								number.clearListeners();
								createFrame.clearListeners();
								accept.clearListeners();
								createFocus.clearListeners();
								manager.get(DataDirs.accept, Sound.class).play();
								hidePointer();
							}
							
						}),
						Actions.alpha(0f, .5f),
						Actions.run(new Runnable(){
							
							@Override
							public void run() {
								createFrame.clear();
								createFrame.remove();
								parent.prepareStory();
								sm.changeState(UIState.Story);
							}
						})
					)
				);
			}
		});
		createFrame.addActor(accept);
		
		createFrame.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				boolean hit = false;
				
				if (keycode == Keys.ENTER || keycode == Keys.SPACE)
				{
					hit = true;
					accept.setChecked(true);
				}
				if (keycode == Keys.DOWN || keycode == Keys.S)
				{
					hit = true;
					createFocus.next();
				}
				if (keycode == Keys.UP || keycode == Keys.W)
				{
					hit = true;
					createFocus.prev();
				}
				return hit;
			}
		});
		
		createFrame.addActor(window);
		createFrame.setColor(1.0f, 1.0f, 1.0f, 0.0f);
		addActor(createFrame);
		
		goddess = new Image();
		goddess.setScaling(Scaling.stretch);
		goddess.setSize(128f, 128f);
		goddess.setPosition(getWidth() * .6f, 48f);
		goddess.setOrigin(64f, 64f);
		
		you = new Image();
		you.setScaling(Scaling.stretch);
		you.setSize(64f, 64f);
		you.setPosition(getWidth()*.4f, 48f);
		
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
		textTable.addAction(Actions.alpha(0f));
		
		act();
		
		pointer = new Image(skin.getDrawable("pointer"));
		hidePointer();
		addActor(pointer);
		
		sm.changeState(UIState.Choose);
	}

	public int getDifficulty() {
		return number.getValue();
	}

	public boolean isDone() {
		return sm.isInState(UIState.Over);
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
	}

	public boolean getGender() {
		return gender.getButtons().get(0).isChecked();
	}
	
	@Override
	public void unhook() {
		player = null;
	}

	@Override
	public void update(float delta) { }

	@Override
	public boolean handleMessage(Telegram msg) {
		return sm.handleMessage(msg);
	}
	
	private static enum UIState implements State<NewUI>
	{
		Choose(){

			@Override
			public void enter(final NewUI entity) {
				entity.slotFrame.addAction(
						Actions.sequence(
							Actions.alpha(0f),
							Actions.delay(1.5f),
							Actions.alpha(1f, .3f),
							Actions.run(new Runnable(){
								@Override
								public void run() {
									entity.slotFrame.addActor(entity.slotFocus);
									entity.slotFrame.addListener(new InputListener(){
										@Override
										public boolean keyDown(InputEvent evt, int keycode)
										{
											if (keycode == Keys.DOWN || keycode == Keys.S)
											{
												entity.slotFocus.next(true);
											}
											if (keycode == Keys.UP || keycode == Keys.W)
											{
												entity.slotFocus.prev(true);
											}
											if (keycode == Keys.SPACE || keycode == Keys.ENTER)
											{
												MessageDispatcher.getInstance().dispatchMessage(0f, entity, entity, entity.slotFocus.getFocusedIndex(), entity.slotFocus.getFocused().getUserObject());
											}

											return false;
										}
									});
									entity.slotFocus.addListener(new ChangeListener(){
										@Override
										public void changed(ChangeEvent event, Actor actor) {
											Actor a = entity.slotFocus.getFocused();
											entity.setKeyboardFocus(a);
											entity.showPointer(a, Align.left, Align.center);
										}
									});
									
									Actor a = entity.slotFocus.getActors().first();
									entity.slotFocus.setFocus(a);

								}
							})
						)
					);
				
			}

			@Override
			public void update(NewUI entity) {}

			@Override
			public void exit(NewUI entity) {
				
			}

			@Override
			public boolean onMessage(final NewUI entity, Telegram telegram) {
				final int index = telegram.message;
				if (index == 0)
				{
					entity.manager.get(DataDirs.accept, Sound.class).play();
					entity.slotFrame.addAction(Actions.sequence(
						Actions.sequence(
							Actions.run(new Runnable(){
								@Override
								public void run(){
									entity.slotFocus.clearListeners();
									entity.slotFrame.clearListeners();
									entity.hidePointer();
								}
							}),
							Actions.alpha(0f, .4f),
							Actions.run(new Runnable(){

								@Override
								public void run() {
									
									entity.sm.changeState(Create);
								}
								
							})
						)
					));
					return true;
				}
				else
				{
					if (telegram.extraInfo == null)
					{
						entity.manager.get(DataDirs.tick, Sound.class).play();
						return false;
					}
					
					entity.manager.get(DataDirs.accept, Sound.class).play();
					entity.slotFrame.addAction(Actions.sequence(
						Actions.sequence(
							Actions.run(new Runnable(){
								@Override
								public void run(){
									entity.slotFocus.clearListeners();
									entity.slotFrame.clearListeners();
									entity.hidePointer();
								}
							}),
							Actions.alpha(0f, .4f),
							Actions.run(new Runnable(){

								@Override
								public void run() {
									entity.player.load(index);
									entity.sm.changeState(Over);
								}
								
							})
						)
					));
					return true;
				}
			}
			
		},
		Create(){

			@Override
			public void enter(final NewUI entity) {
				entity.createFrame.addAction(
					Actions.sequence(
						Actions.alpha(0f),
						Actions.delay(1.5f),
						Actions.alpha(1f, .3f),
						Actions.run(new Runnable(){
							@Override
							public void run() {
								entity.createFrame.addActor(entity.createFocus);
								entity.createFocus.addListener(new ChangeListener(){
									@Override
									public void changed(ChangeEvent event, Actor actor) {
										Actor a = entity.createFocus.getFocused();
										entity.setKeyboardFocus(a);
										
										entity.showPointer(a, Align.left, Align.center);
									}
								});
								entity.createFocus.setFocus(entity.number);
							}
						})
					)
				);
			}

			@Override
			public void update(NewUI entity) { /* Do Nothing */ }

			@Override
			public void exit(NewUI entity) {
			}

			@Override
			public boolean onMessage(NewUI entity, Telegram telegram) { return false; }
			
		},
		Story(){

			Iterator<String> story;
			
			@Override
			public void enter(final NewUI entity) {
				entity.goddess.setDrawable(entity.skin, entity.player.getWorship());
				entity.you.setDrawable(entity.skin, entity.player.getGender());
				
				Array<String> data = new Array<String>();
				entity.setKeyboardFocus(null);
				entity.addListener(new InputListener(){
					@Override
					public boolean keyDown(InputEvent evt, int keycode)
					{
						if (keycode == Keys.ESCAPE || keycode == Keys.BACKSPACE)
						{
							entity.sm.changeState(Over);
							return true;
						}
						return false;
					}
				});
				Scanner s = new Scanner(Gdx.files.classpath("core/data/title_"+entity.player.getGender()+".txt").read());
				while (s.hasNextLine())
				{
					data.add(s.nextLine());
				}
				s.close();
				story = data.iterator();

				entity.you.addAction(Actions.sequence(Actions.alpha(0f),Actions.alpha(1f, 1f)));
				entity.addActor(entity.you);
				
				//goddess
				entity.goddess.addAction(Actions.sequence(
						Actions.alpha(0f),
						Actions.delay(4f),
						Actions.alpha(1f, 3f),
						Actions.run(new Runnable(){

							@Override
							public void run() {
								update(entity);
								entity.addListener(new InputListener(){
									@Override
									public boolean keyDown(InputEvent evt, int keycode)
									{
										if (keycode == Keys.ENTER || keycode == Keys.SPACE)
										{
											entity.sm.update();
											return true;
										}						
										return false;
									}
									
									@Override
									public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
									{
										if (button == Buttons.LEFT)
										{
											entity.sm.update();
											return true;
										}
										return false;
									}
								});
								entity.setKeyboardFocus(null);
							}
							
						}),
						Actions.forever(
							Actions.sequence(
								Actions.moveTo(entity.getWidth() * .6f, 58f, 5f),
								Actions.moveTo(entity.getWidth() * .6f, 48f, 5f)
							)
						)
					)
				);
				
				entity.addActor(entity.goddess);
				entity.addActor(entity.textTable);
				
				entity.act();
			}

			@Override
			public void update(final NewUI entity) {
				if (story.hasNext())
				{
					entity.textTable.addAction(Actions.alpha(1f));
					entity.text.addAction(
						Actions.sequence(
							Actions.alpha(0f, .1f),
							Actions.run(new Runnable(){
	
								@Override
								public void run() {
									String dialog = story.next();
									entity.text.setText(dialog);
									entity.textTable.pack();
								}
								
							}),
							Actions.alpha(1f, .1f)
						)
					);
				}
				else
				{
					entity.textTable.addAction(Actions.alpha(0f, 1f));
					entity.goddess.clearActions();
					entity.goddess.addAction(Actions.sequence(
						Actions.run(new Runnable(){

							@Override
							public void run() {
								entity.getRoot().clearListeners();
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
											entity.manager.get(DataDirs.shimmer, Sound.class).play();
										}
										
									}),
									Actions.moveTo(entity.goddess.getX(), entity.getHeight()+128f, .4f)
								)
							)
						)
					);
					entity.you.addAction(
						Actions.sequence(
							Actions.delay(5f),
							Actions.moveTo(entity.getWidth()/2f - entity.you.getWidth()/2f, 48f, 2f),
							Actions.delay(2f),
							Actions.alpha(0f, 2f),
							Actions.run(new Runnable(){

								@Override
								public void run() {
									entity.sm.changeState(Over);
								}
								
							})
					));
				}
			}

			@Override
			public void exit(final NewUI entity) { }

			@Override
			public boolean onMessage(NewUI entity, Telegram telegram) {
				return false;
			}
		},
		Over(){

			@Override
			public void enter(NewUI entity) { }

			@Override
			public void update(NewUI entity) { }

			@Override
			public void exit(NewUI entity) { }

			@Override
			public boolean onMessage(NewUI entity, Telegram telegram) { return false; }
			
		};
	}
}
