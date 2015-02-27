package scenes.town.ui;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;

import scene2d.ui.extras.TabbedPane;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

import core.datatypes.FileType;

/**
 * FileBrowser component for the TownUI.
 * Also manages browser history
 * @author nhydock
 *
 */
public class FileBrowser extends Group {

    private static final String FONTSIZE = "smaller";
    
    Label.LabelStyle listStyle;
    Label.LabelStyle hoverStyle;
    
    Label url;
    private static FileHandle currentFolder;
    private static ObjectSet<FileHandle> history = new ObjectSet<FileHandle>();
    ObjectMap<FileHandle, Label[]> rows;
    Table fileList;
    Table historyList;
    
    
    Skin skin;
    
    private FileHandle currentFile;
    final ListInputEmulator lie = new ListInputEmulator();
    
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
        rows = new ObjectMap<FileHandle, Label[]>();
        listStyle = skin.get(FONTSIZE, Label.LabelStyle.class);
        hoverStyle = skin.get("hover", Label.LabelStyle.class);
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
        url = new Label("", skin, "field");
        browseView.left().top().pad(5f).row();
        browseView.add(url).align(Align.left).colspan(3).expandX().fillX().padBottom(8f);
        browseView.row().padBottom(5f);
        browseView.add("File Name", FONTSIZE).width(COL[0]).expandX().align(Align.left);
        browseView.add("Type", FONTSIZE).width(COL[1]).expandX().align(Align.left);
        browseView.add("Size (Kb)", FONTSIZE).width(COL[2]).expandX().align(Align.left);
        browseView.row();
        
        fileList = new Table(skin);
        fileList.top().left().pad(5f);
        fileList.columnDefaults(0).align(Align.left).width(COL[0]).expandX().fillX();
        fileList.columnDefaults(1).align(Align.center).width(COL[1]).padLeft(8f).padRight(8f).expandX().fill();
        fileList.columnDefaults(2).align(Align.right).width(COL[2]).expandX().fill();
        
        ScrollPane filePane = new ScrollPane(fileList, skin, "files");
        filePane.setFadeScrollBars(false);
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
        
        historyList = new Table(skin);
        historyList.top().left().pad(5f);
        historyList.columnDefaults(0).align(Align.left).width(COL[0]).expandX().fillX();
        historyList.columnDefaults(1).align(Align.center).width(COL[1]).padLeft(8f).padRight(8f).expandX().fill();
        historyList.columnDefaults(2).align(Align.right).width(COL[2]).expandX().fill();
        
        for (FileHandle file : history) {
            makeLabel(file, historyList);
        }
        
        ScrollPane historyPane = new ScrollPane(historyList, skin, "files");
        historyPane.setFadeScrollBars(false);
        historyView.add(historyPane).colspan(3).fillX();
        historyWindow.addActor(historyView);
        historyWindow.setWidth(getWidth());
        historyWindow.setHeight(getHeight());
        
        TextButton browseTab = new TextButton("Browse", skin);
        browseTab.setUserObject(browseWindow);
        TextButton historyTab = new TextButton("Recent Files", skin);
        historyTab.setUserObject(historyWindow);
        ButtonGroup<Button> tabs = new ButtonGroup<Button>(browseTab, historyTab);
        browseTab.setChecked(true);
        
        TabbedPane frame = new TabbedPane(tabs, false);
        frame.setWidth(getWidth());
        frame.setHeight(getHeight());
        

        changeFolder(currentFolder);
        
        addActor(frame);
    }
    
    /**
     * Make a row for a file object
     * @param file
     */
    private void makeLabel(FileHandle file, Table list) {
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
    
    private void makeParentLabel() {
        Label[] row = new Label[3];
        fileList.row();
        row[0] = fileList.add("..", FONTSIZE).getActor();
        row[1] = fileList.add("", FONTSIZE).getActor();
        row[2] = fileList.add("", FONTSIZE).getActor();
        for (Label l : row) {
            l.addListener(lie);
            l.setUserObject(currentFolder.parent());
        }
        rows.put(currentFolder.parent(), row);
    }
    
    /**
     * Changes the current folder to the one provided
     * @param folder
     */
    private void changeFolder(FileHandle folder) {
        currentFolder = folder;
        currentFile = null;
    
        fileList.clear();
        rows.clear();
        
        makeParentLabel();
        Array<FileHandle> children = new Array<FileHandle>();
        children.addAll(folder.list(NoHidden.instance));
        children.sort(FileComparator.instance);
        
        for (FileHandle file : children) {
            makeLabel(file, fileList);
        }
        
        url.setText(folder.file().getAbsolutePath());
        
    }

    /**
     * Get the currently selected file from whatever the active pane is
     * @return the currently selected file
     */
    public FileHandle getSelectedFile() {
        if (!currentFile.isDirectory()) {
            return currentFile;
        }
        return null;
    }

    /**
     * Light up the row of the selected file, and mark it as the current
     * file of the file browser
     * @param file
     */
    protected void selectFile(FileHandle file) {
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
    }
    
    /**
     * 
     * @param file
     */
    public void addToHistory(FileHandle file) {
        history.add(file);
    }

    public static void clearHistory() {
        history.clear();
    }
    
    /**
     * Emulate list input by allowing
     * @author nhydock
     *
     */
    class ListInputEmulator extends InputListener {
        
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            FileHandle file = (FileHandle)event.getTarget().getUserObject();
            
            if (currentFile == file && file.isDirectory()) {
                changeFolder(file);
                return true;
            }
            
            
            selectFile(file);
            
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
