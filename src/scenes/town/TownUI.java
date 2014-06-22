package scenes.town;

import java.io.File;

import scenes.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;

import core.datatypes.Craftable;
import core.datatypes.FileType;

public class TownUI extends UI {

	Image sleepImg;
	Group exploreImg;
	Image craftImg;
	Image character;
	
	Table exploreSubmenu;
	List<String> fileList;
	Array<FileHandle> directoryList;
	FileHandle directory;
	Table fileDetails;
	
	Table craftSubmenu;
	List<Craftable> craftList;
	Table requirementList;
	
	boolean changeDir;
	int lastIndex = -1;
	
	public TownUI(Scene scene, AssetManager manager) {
		super(scene, manager);
	}
	
	public void extend()
	{
		//explore icon
		{
			exploreImg = new Group();
			Image back = new Image(skin.getRegion("explore_back"));
			Image front = new Image(skin.getRegion("explore"));
			exploreImg.addActor(back);
			exploreImg.addActor(front);
			
			display.addActor(exploreImg);
			front.addAction(
					Actions.forever(
						Actions.sequence(
							Actions.moveTo(0, 0),
							Actions.moveTo(0, 10f, 2f),
							Actions.moveTo(0, 0, 2f)
						)
					)
				);
			exploreImg.setSize(front.getWidth(), front.getHeight());
			exploreImg.setPosition(display.getWidth()/2-exploreImg.getWidth()/2, display.getHeight()-exploreImg.getHeight());
		}

		//sleep icon
		{
			sleepImg = new Image(skin.getRegion("sleep"));
			sleepImg.setPosition(0f, 0f);
			display.addActor(sleepImg);
		}
		
		//craft icon
		{
			craftImg = new Image(skin.getRegion("craft"));
			craftImg.setPosition(display.getWidth()-craftImg.getWidth(), 0);
			display.addActor(craftImg);
		}
		
		//draw you
		{
			character = new Image(skin.getRegion("character"));
			character.setSize(96f, 96f);
			character.setPosition(display.getWidth()/2-character.getWidth()/2, 18f);
			display.addActor(character);
		}
		
		//create craft submenu layout
		{
			craftSubmenu = new Table();
			craftSubmenu.setWidth(350f);
			craftSubmenu.setHeight(display.getHeight());
			
			//list of required crafts
			craftList = new List<Craftable>(skin);
			craftList.setItems(getService().getInventory().getRequiredCrafts());
			craftList.addListener(new ChangeListener(){

				@Override
				public void changed(ChangeEvent event, Actor actor) {
					requirementList.clear();
					
					//build requirements list
					Craftable c = craftList.getSelected();
					ObjectMap<String, Integer> map = c.getRequirements();
					for (String name : map.keys())
					{
						Label l = new Label(name, skin);
						l.setAlignment(Align.left);
						requirementList.add(l);
						Label i = new Label(""+map.get(name), skin);
						i.setAlignment(Align.right);
						requirementList.add(i).expandX().fillX();
						requirementList.row();
					}
				}

			});
			
			ScrollPane pane = new ScrollPane(craftList, skin);
			pane.setHeight((display.getHeight())/2);
			craftSubmenu.add(pane).expandX().fillX().pad(10f);
			
			craftSubmenu.row();
			//current highlighted craft item requirements
			requirementList = new Table();
			requirementList.clear();
			requirementList.row();
			
			ScrollPane pane2 = new ScrollPane(requirementList, skin);
			craftSubmenu.add(pane2).expand().fill().pad(10f);
			craftSubmenu.setPosition(display.getWidth(), 0);
			display.addActor(craftSubmenu);
		}
		

		//create explore submenu layout
		{
			exploreSubmenu = new Table();
			exploreSubmenu.setWidth(300f);
			exploreSubmenu.setHeight(display.getHeight());
			exploreSubmenu.setPosition(-exploreSubmenu.getWidth(), 0);
			
			//list of required crafts
			fileList = new List<String>(skin);
			
			loadDir(Gdx.files.external("."));
			changeDir = false;
			
			fileList.addListener(new ChangeListener(){

				
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (changeDir)
					{
						changeDir = false;
						event.cancel();
						return;
					}
					
					fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
					
					String path = fileList.getSelected();
					int listIndex = fileList.getSelectedIndex();
					final FileHandle selected = directoryList.get(listIndex);
					
					if (selected == null && lastIndex == listIndex)
					{
						//go to parent directory
						loadDir(directory.parent());
					}
					else if (selected != null)
					{
						if (selected.isDirectory())
						{
							fileDetails.clearActions();
							fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
							
							if (lastIndex == listIndex)
							{
								listIndex = 0;
								changeDir = true;
								fileList.addAction(Actions.sequence(
										Actions.moveTo(-fileList.getWidth(), 0, .3f),
										Actions.run(new Runnable(){

											@Override
											public void run() {
												loadDir(selected);
											}
											
										}),
										Actions.moveTo(0, 0, .3f)
								));
							}
						}
						else
						{
							fileDetails.clear();
							
							//generate a details panel
							Table contents = new Table();
							
							FileType ext = FileType.getType(selected.extension());
							Image icon = new Image(skin.getRegion(ext.toString()));
							Label type = new Label("File Type: " + ext, skin);
							Label size = new Label("File Size: " + (selected.length()/1000f) + " kb", skin);
							Label diff = new Label("Difficulty: " + "*****", skin);
							
							icon.setAlign(Align.center);
							icon.setSize(96f, 96f);
							icon.setScaling(Scaling.fit);
							
							contents.pad(10f);
							contents.add(icon).size(96f, 96f).expandX();
							contents.row();
							contents.add(type).expandX().fillX();
							contents.row();
							contents.add(size).expandX().fillX();
							contents.row();
							contents.add(diff).expandX().fillX();
							
							ScrollPane pane = new ScrollPane(contents, skin);
							pane.setScrollingDisabled(true, true);
							fileDetails.add(pane).expand().fill();
							fileDetails.addAction(Actions.moveTo(display.getWidth()-fileDetails.getWidth(), 0, .3f));
						}
					
					}

					lastIndex = listIndex;
				}

			});
			
			ScrollPane pane = new ScrollPane(fileList, skin);
			pane.setHeight(display.getHeight());
			pane.setScrollingDisabled(true, false);
			exploreSubmenu.add(pane).expand().fill().pad(10f);
			
			fileDetails = new Table();
			fileDetails.setSize(200f, display.getHeight());
			fileDetails.setPosition(display.getWidth(), 0);
			
			display.addActor(fileDetails);
			display.addActor(exploreSubmenu);
		}
		
		setMessage("What're we doing next?");
	}
	
	private void loadDir(FileHandle external) {
		FileHandle[] handles = external.list();
		Array<FileHandle> acceptable = new Array<FileHandle>();
		Array<String> paths = new Array<String>();
		
		if (external.parent() != null || !external.path().equals(external.parent().path()))
		{
			paths.add("..");
			acceptable.add(null);
		}
		for (FileHandle handle : handles)
		{
			File f = handle.file();
			String path = handle.nameWithoutExtension();
			//make sure hidden files are not shown
			if (f.isHidden())
			{
				continue;
			}
			if (f.isDirectory())
			{
				path += "/";
			}
			
			acceptable.add(handle);
			paths.add(path);
		}
		
		this.fileList.setItems(paths);
		this.directoryList = acceptable;
		directory = external;
		
	}

	protected void triggerAction(int index)
	{
		disableMenuInput();
		//craft menu
		if (index == 2)
		{
			showCraftSubmenu();
		}
		else if (index == 1)
		{
			showExploreSubmenu();
		}
		else if (index == 0)
		{
			sleep();
		}
		else
		{
			restore();
		}
	}
	
	private void showCraftSubmenu()
	{
		//populate the submenu's data
		craftList.setItems(getService().getInventory().getRequiredCrafts());	
		requirementList.clear();
			
		sleepImg.addAction(Actions.moveTo(-sleepImg.getWidth(), 0, 1.5f));
		exploreImg.addAction(Actions.moveTo(-exploreImg.getWidth(), 118f, 1.5f));
		craftImg.addAction(Actions.sequence(
				Actions.moveTo(display.getWidth()-craftImg.getWidth(), display.getHeight() - craftImg.getHeight(), .5f),
				Actions.moveTo((display.getWidth()*.3f)-craftImg.getWidth()/2, display.getHeight() - craftImg.getHeight(), 1f)
			));
		character.addAction(Actions.moveTo((display.getWidth()*.3f)-character.getWidth()/2, 18f, 1.5f));
	
		craftSubmenu.addAction(Actions.sequence(
			Actions.moveTo(display.getWidth(), 0),
			Actions.delay(1.5f),
			Actions.moveTo(display.getWidth()-craftSubmenu.getWidth(), 0, .3f)
		));
		
		setMessage("Tink Tink");
	}
	
	private void showExploreSubmenu()
	{
		sleepImg.addAction(Actions.moveTo(-sleepImg.getWidth(), 0, 1.5f));
		craftImg.addAction(Actions.moveTo(display.getWidth(), 0, 1.5f));
	
		exploreSubmenu.addAction(Actions.sequence(
			Actions.moveTo(-exploreSubmenu.getWidth(), 0),
			Actions.delay(1.5f),
			Actions.moveTo(0, 0, .3f)
		));
		
		setMessage("Where to?");
	}
	
	private void sleep()
	{
		setMessage("Good night!");
		sleepImg.addAction(Actions.sequence(
			Actions.moveTo(32f, display.getHeight()/2 - sleepImg.getHeight()/2, .5f),
			Actions.moveTo(display.getWidth()/2 - sleepImg.getWidth()/2, display.getHeight()/2 - sleepImg.getHeight()/2, 1f)
		));
		exploreImg.addAction(Actions.moveTo(display.getWidth(), 118f, 1.5f));
		craftImg.addAction(Actions.moveTo(display.getWidth(), 0f, 1.5f));
		disableMenuInput();
		
		display.addAction(Actions.sequence(
			Actions.delay(2f),
			Actions.alpha(0f, 2f),
			Actions.delay(3f),
			Actions.run(new Runnable(){

				@Override
				public void run() {
					getService().rest();
				}
				
			}),
			Actions.alpha(1f, 2f),
			Actions.delay(.3f),
			Actions.run(new Runnable() {
				
				@Override
				public void run()
				{
					restore();
				}
				
			})
		));
	}
	
	/**
	 * restores the original positions of all the images
	 */
	private void restore()
	{
		exploreImg.addAction(Actions.moveTo(display.getWidth()/2-exploreImg.getWidth()/2, 118f, 1.5f));
		sleepImg.addAction(Actions.moveTo(0, 0, 1.5f));
		craftImg.addAction(Actions.moveTo(display.getWidth()-craftImg.getWidth(), 0, 1.5f));
		character.addAction(Actions.moveTo(display.getWidth()/2-character.getWidth()/2, 18f, 1.5f));
		craftSubmenu.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
		exploreSubmenu.addAction(Actions.moveTo(-exploreSubmenu.getWidth(), 0, .3f));
		enableMenuInput();
		setMessage("What're we doing next?");
	}

	@Override
	public String[] defineButtons() {
		return new String[]{"sleep", "explore", "craft"};
	}
}
