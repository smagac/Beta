package core;

import java.util.Scanner;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

public class DataDirs {

    public static final String Home = "data/";
    public static final String Audio = Home + "audio/";
    public static final String sfx = Audio + "fx/";
    public static final String GameData = Home + "game/";

    public static final String tick = sfx + "tick.wav";
    public static final String hit = sfx + "hit.wav";
    public static final String shimmer = sfx + "shimmer.wav";
    public static final String accept = sfx + "accept.wav";
    public static final String dead = sfx + "dead.wav";

    public static final String Tilesets = Home + "tilesets/";
    public static final String Lore = Home + "lore/";
    
    /**
     * Allows getting a specified list of children in an internal directory
     * by reading a list of files in the directory from a text file.
     * @param directory - directory we want the children from.  
     *      Must have a "list" file inside it to read.
     * @return an array of file handles
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
}
