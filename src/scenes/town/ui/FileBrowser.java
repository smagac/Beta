package scenes.town.ui;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;

import scene2d.ui.extras.TabbedPane;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pools;

import core.common.Input;
import core.datatypes.FileType;

/**
 * FileBrowser component for the TownUI.
 * Also manages browser history
 * @author nhydock
 *
 */
public class FileBrowser extends Group {

    private static final String FONTSIZE = "list";
    
    Label url;
    private static FileHandle currentFolder;
    
    private static Array<FileHandle> history = new Array<FileHandle>();
    ScrollPane filePane, historyPane;
    TabbedPane frame;
    Skin skin;
    
    private FileList currentFolderContents;
    private FileList historyContents;
    
    /**
     * Constructs the actor, but does not lay anything out within it.
     * 
     * @param skin
     */
    public FileBrowser(Skin skin) {
        
        if (currentFolder == null) {
            //get the home folder as an absolute path handle
            currentFolder = Gdx.files.absolute(Gdx.files.external(".").file().getAbsolutePath()).parent();
        }
        
        this.skin = skin;

    }
    
    /**
     * Call this after you size the actor properly
     */
    void init() {
        Window browseWindow = new Window("", skin, "pane");
        
        final Value[] COL = {
                new Value.Fixed(getWidth() * .5f), 
                new Value.Fixed(getWidth() * .2f), 
                new Value.Fixed(getWidth() * .25f)
            };
        
        Table browseView = new Table(skin);
        browseView.setFillParent(true);
        url = new Label("", skin, "tag");
        browseView.left().top().pad(5f).row();
        browseView.add(url).align(Align.left).colspan(3).expandX().fillX().padBottom(8f);
        browseView.row().padBottom(5f);
        browseView.add("File Name", FONTSIZE).width(COL[0]).expandX().align(Align.left);
        browseView.add("Type", FONTSIZE).width(COL[1]).expandX().align(Align.left);
        browseView.add("Size (Kb)", FONTSIZE).width(COL[2]).expandX().align(Align.left);
        browseView.row();
        
        currentFolderContents = new FileList(skin, COL, this);
        
        filePane = new ScrollPane(currentFolderContents.list, skin, "files");
        filePane.setFadeScrollBars(false);
        filePane.setScrollingDisabled(true, false);
        browseView.add(filePane).colspan(3).fillX();
        browseWindow.addActor(browseView);
        browseWindow.setWidth(getWidth());
        browseWindow.setHeight(getHeight());
        
        
        Window historyWindow = new Window("", skin, "pane");
        
        Table historyView = new Table(skin);
        historyView.setFillParent(true);
        historyView.left().top().pad(5f).row();
        historyView.add("File Name", FONTSIZE).width(COL[0]).expandX().align(Align.left);
        historyView.add("Type", FONTSIZE).width(COL[1]).expandX().align(Align.left);
        historyView.add("Size (Kb)", FONTSIZE).width(COL[2]).expandX().align(Align.left);
        historyView.row();
        
        historyContents = new FileList(skin, COL, this);
        historyContents.setFiles((FileHandle[])history.toArray(FileHandle.class), null);
        
        historyPane = new ScrollPane(historyContents.list, skin, "files");
        historyPane.setFadeScrollBars(false);
        historyPane.setScrollingDisabled(false, true);
        historyView.add(historyPane).colspan(3).fillX();
        historyWindow.addActor(historyView);
        historyWindow.setWidth(getWidth());
        historyWindow.setHeight(getHeight());
        historyWindow.setUserObject(historyContents);
        
        TextButton browseTab = new TextButton("Browse", skin, "tab");
        browseTab.setUserObject(browseWindow);
        TextButton historyTab = new TextButton("Recent Files", skin, "tab");
        historyTab.setUserObject(historyWindow);
        ButtonGroup<Button> tabs = new ButtonGroup<Button>(browseTab, historyTab);
        browseTab.setChecked(true);
        
        frame = new TabbedPane(tabs, false);
        frame.setWidth(getWidth());
        frame.setHeight(getHeight());
        
        changeFolder(currentFolder);
        
        addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (frame.getOpenTabIndex() == 0) {
                    if (Input.DOWN.match(keycode)) {
                        currentFolderContents.nextFile();
                    }
                    else if (Input.UP.match(keycode)) {
                        currentFolderContents.prevFile();
                    }
                    else if (Input.ACCEPT.match(keycode)) {
                        changeFolder(currentFolderContents.currentFile);
                    }
                } else {
                    if (Input.DOWN.match(keycode)) {
                        historyContents.nextFile();
                    }
                    else if (Input.UP.match(keycode)) {
                        historyContents.prevFile();
                    }
                }
                
                if (Input.LEFT.match(keycode)) {
                    frame.showTab(0);
                }
                else if (Input.RIGHT.match(keycode)) {
                    frame.showTab(1);
                }
                
                return super.keyDown(event, keycode);
            }
        });
        
        addListener(new ChangeListener() {
            
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (frame.getOpenTabIndex() == 0) {
                    filePane.scrollTo(0, actor.getY(), 1, actor.getHeight());
                } else {
                    historyPane.scrollTo(0, actor.getY(), 1, actor.getHeight());
                }
            }
        });
        
        addActor(frame);
    }
    
    /**
     * Changes the current folder to the one provided
     * @param folder
     */
    private void changeFolder(FileHandle folder) {
        if (!folder.isDirectory()) {
            return;
        }
        
        currentFolder = folder;
    
        currentFolderContents.setFiles(folder.list(NoHidden.instance), currentFolder);
        
        url.setText(folder.file().getAbsolutePath());
    }

    /**
     * Get the currently selected file from whatever the active pane is
     * @return the currently selected file
     */
    public FileHandle getSelectedFile() {
        if (frame.getOpenTabIndex() == 0) {
            if (!currentFolderContents.currentFile.isDirectory()) {
                return currentFolderContents.currentFile;
            }
            return null;
        } else {
            return historyContents.currentFile;
        }
    }

    /**
     * 
     * @param file
     */
    public void addToHistory(FileHandle file) {
        if (!history.contains(file, false)) {
            history.add(file);
        }
    }

    public static void clearHistory() {
        history.clear();
    }
    
    /**
     * Simple wrapper object for managing collections of files in our view
     * @author nhydock
     *
     */
    static class FileList {

        Label.LabelStyle listStyle;
        Label.LabelStyle hoverStyle;
        
        Table list;
        Array<FileHandle> files;
        ObjectMap<FileHandle, Label[]> rows;
        
        int currentIndex;
        FileHandle currentFile;
        
        final ListInputEmulator lie;
        FileBrowser browser;
        
        FileList(Skin skin, final Value[] COL, FileBrowser browser){
            list = new Table(skin);
            list.top().left().pad(5f);
            list.columnDefaults(0).align(Align.left).width(COL[0]).expandX().fillX();
            list.columnDefaults(1).align(Align.center).width(COL[1]).padLeft(8f).padRight(8f).expandX().fill();
            list.columnDefaults(2).align(Align.right).width(COL[2]).expandX().fill();
            
            listStyle = skin.get(FONTSIZE, Label.LabelStyle.class);
            hoverStyle = skin.get("selected", Label.LabelStyle.class);
            
            lie = new ListInputEmulator();
            lie.browser = browser;
            lie.list = this;
            
            files = new Array<FileHandle>();
            rows = new ObjectMap<FileHandle, Label[]>();
            this.browser = browser;
        }
        
        
        
        public void nextFile() {
            if (files.size > 0) {
                currentIndex++;
                if (currentIndex > files.size - 1) {
                    currentIndex = 0;
                }
                selectFile(files.get(currentIndex));
            }
        }
        
        public void prevFile() {
            if (files.size > 0) {
                currentIndex--;
                if (currentIndex < 0) {
                    currentIndex = files.size - 1;
                }
                selectFile(files.get(currentIndex));
            }
        }

        /**
         * Light up the row of the selected file, and mark it as the current
         * file of the file browser
         * @param file
         */
        public void selectFile(FileHandle file) {
            Label[] row;
            
            if (currentFile != null) {
                row = rows.get(currentFile);
                if (row != null) {
                    for (Label l : row) {
                        l.setStyle(listStyle);
                    }
                }
            }
            

            row = rows.get(file);
            if (row != null) {
                for (Label l : row) {
                    l.setStyle(hoverStyle);
                }
            }
            
            currentFile = file;
            currentIndex = files.indexOf(file, true);
            
            if (row != null) {
                ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
                row[0].fire(changeEvent);
                Pools.free(changeEvent);
            }
        }

        public void setFiles(FileHandle[] files, FileHandle parent) {
            this.files.clear();
            this.files.addAll(files);
            this.files.sort(FileComparator.instance);
            
            rows.clear();
            list.clear();
            
            if (parent != null) {
                makeParentLabel(parent);
            }
            
            for (FileHandle file : this.files) {
                makeLabel(file);
            }
            
            if (parent != null) {
                this.files.insert(0, parent.parent());
            }
            
            if (files.length > 0) {
                selectFile(this.files.first());
            }
        }
        
        /**
         * Make a row for a file object
         * @param file
         */
        private void makeLabel(FileHandle file) {
            list.row();
            Label[] row;
            
            row = new Label[3];
            //name of the file
            String name = file.name();
            row[0] = list.add(name, FONTSIZE).getActor();
        
            if (!file.isDirectory()) {
                //file type
                String type = FileType.getType(file.extension()).toString();
                row[1] = list.add(type, FONTSIZE).getActor();
                //size in kb
                String size = String.format("%.02f", file.length() / 1000f);
                row[2] = list.add(size, FONTSIZE).getActor();            
            } else {
                row[1] = list.add("Folder", FONTSIZE).getActor();
                row[2] = list.add("", FONTSIZE).getActor();
            }
            
            for (Label l : row) {
                l.addListener(lie);
                l.setUserObject(file);
            }
            rows.put(file, row);
        }

        private void makeParentLabel(FileHandle currentFolder) {
            Label[] row = new Label[3];
            list.row();
            row[0] = list.add("..", FONTSIZE).getActor();
            row[1] = list.add("", FONTSIZE).getActor();
            row[2] = list.add("", FONTSIZE).getActor();
            for (Label l : row) {
                l.addListener(lie);
                l.setUserObject(currentFolder.parent());
            }
            rows.put(currentFolder.parent(), row);
        }
    }
    
    /**
     * Emulate list input by allowing
     * @author nhydock
     *
     */
    static class ListInputEmulator extends InputListener {
        
        FileList list;
        FileBrowser browser;
        
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            FileHandle file = (FileHandle)event.getTarget().getUserObject();
            
            if (file == list.currentFile) {
                browser.changeFolder(file);
            } else {
                list.selectFile(file);
            }
            return true;
        }
        
    }
    
    static class NoHidden implements FileFilter {

        static final NoHidden instance = new NoHidden();
        
        @Override
        public boolean accept(File pathname) {
            boolean hidden = pathname.isHidden();
            //hide hidden directories and empty directories
            if (!hidden && pathname.isDirectory() && pathname.list() != null && pathname.list().length == 0) {
                hidden = true;
            }
            return !hidden;
        }
        
    }
    
    static class FileComparator implements Comparator<FileHandle> {

        public static final FileComparator instance = new FileComparator(); 
        
        @Override
        public int compare(FileHandle o1, FileHandle o2) {
            if (o1 == o2) {
                return 0;
            }
            
            if (o1 == null) {
                return 1;
            }
            
            if (o2 == null) {
                return -1;
            }
            
            if (o1.isDirectory() && !o2.isDirectory())
            {
                return -1;
            }
            
            if (o2.isDirectory() && !o1.isDirectory()) {
                return 1;
            }
            
            return o1.name().toLowerCase().compareTo(o2.name().toLowerCase());
        }
        
    }
}
