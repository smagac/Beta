package core;

import java.util.Scanner;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

public final class DataDirs {

    public static final String Home = "data/";
    public static final String Audio = Home + "audio/";
    public static final String sfx = Audio + "fx/";
    public static final String GameData = Home + "content/";

    public static interface Sounds {
        public static final String tick = sfx + "tick.wav";
        public static final String hit = sfx + "hit.wav";
        public static final String hit2 = sfx + "hit2.wav";
        public static final String critical = sfx + "critical.wav";
        public static final String deflect = sfx + "deflect.wav";
        public static final String shimmer = sfx + "shimmer.wav";
        public static final String accept = sfx + "accept.wav";
        public static final String cancel = sfx + "cancel.wav";
        public static final String dead = sfx + "dead.wav";
        public static final String charge = sfx + "charge.wav";
        public static final String blast = sfx + "blast.wav";
        public static final String transition = sfx + "transition.wav";
        public static final String explode = sfx + "explode.wav";
        public static final String open = sfx + "open.wav";
        
        public static final String[] allSounds = {tick, hit, hit2, critical, deflect, shimmer, accept, cancel, dead, charge, blast, transition, explode, open};
        public static final String Footsteps = sfx + "footsteps/";
        
    }
    
    public static final String Tilesets = Home + "tilesets/";
    public static final String Lore = GameData + "lore/";
    public static final String Particles = Home + "particles/";
    public static final String Backgrounds = Home + "backgrounds/";
    public static final String Weather = Home + "weather/";
    
    /**
     * Allows getting a specified list of children in an internal directory
     * by reading a list of files in the directory from a text file.
     * @param directory - directory we want the children from.  
     *      Must have a "list" file inside it to read.
     * @return an array of file paths as strings
     */
    public static Array<String> getChildren(FileHandle directory) {
        Array<String> files = new Array<String>();
        FileHandle listFile = directory.child("list");
        try (Scanner s = new Scanner(listFile.read())) {
            while (s.hasNextLine()) {
                String file = s.nextLine().trim();
                files.add(directory.child(file).path());
            }
        }
        return files;
    }
    
    /**
     * Allows getting a specified list of children in an internal directory
     * by reading a list of files in the directory from a text file.
     * @param directory - directory we want the children from.  
     *      Must have a "list" file inside it to read.
     * @return an array of file handles
     */
    public static Array<FileHandle> getChildrenHandles(FileHandle directory) {
        Array<FileHandle> files = new Array<FileHandle>();
        FileHandle listFile = directory.child("list");
        try (Scanner s = new Scanner(listFile.read())) {
            while (s.hasNextLine()) {
                String file = s.nextLine().trim();
                files.add(directory.child(file));
            }
        }
        return files;
    }
}
