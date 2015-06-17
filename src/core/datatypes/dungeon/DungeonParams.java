package core.datatypes.dungeon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.UUID;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

import core.DataDirs;
import core.datatypes.FileType;

/**
 * Params used for defining the core specifications of a dungeon.
 * 
 * @author nhydock
 *
 */
public class DungeonParams {
    long seed;
    private boolean cached;
    private FileType ext;
    private int difficulty;
    private FileHandle hashFile;
    private String fileName;
    private String type;
    private boolean changed;

    /**
     * Generate Random Dungeon parameters
     */
    private DungeonParams() {
        this.seed = (long) (Math.random() * 1000000L);
        this.ext = FileType.values()[(int) (Math.random() * FileType.values().length)];
        this.difficulty = MathUtils.random(1, 5);
        this.cached = false;
        this.hashFile = null;
        this.fileName = null;
        this.type = DataDirs.getChildren(Gdx.files.internal(DataDirs.Tilesets)).random();
        this.type = this.type.substring(DataDirs.Tilesets.length(), this.type.length()-4);
        
        this.type = "snow";
    }

    private DungeonParams(long seed, FileType ext, int difficulty) {
        this.seed = seed;
        this.ext = ext;
        this.difficulty = Math.max(1, Math.min(5, difficulty));
        this.cached = false;
        this.hashFile = null;
        this.fileName = null;
        this.type = DataDirs.getChildren(Gdx.files.internal(DataDirs.Tilesets)).random();
        this.type = this.type.substring(DataDirs.Tilesets.length(), this.type.length()-4);
    }

    /**
     * 
     * @param file
     * @param ext
     * @param difficulty
     */
    private DungeonParams(FileHandle file, String hashName) {
        try {
            this.ext = FileType.getType(file.extension());
            this.difficulty = ext.difficulty(file.length());
            this.type = DataDirs.getChildren(Gdx.files.internal(DataDirs.Tilesets)).random();
            this.type = this.type.substring(DataDirs.Tilesets.length(), this.type.length()-4);
        
            String tmpDir = System.getProperty("java.io.tmpdir");
            this.hashFile = Gdx.files.absolute(tmpDir + "/storymode/" + hashName + ".tmp");
            if (this.hashFile.exists()) {
                this.cached = true;
                this.seed = -1; // don't need to seed generate on already loaded
                                // files
            }
            else {
                this.seed = getFileHash(file.file());
                this.cached = false;
            }

            this.fileName = file.path();
        }
        catch (IOException e) {
            Gdx.app.log("DungeonParams", "Could not figure out hash from file");
            this.seed = -1;
            this.ext = FileType.Other;
            this.difficulty = 0;
            this.hashFile = null;
            this.cached = false;
            this.fileName = file.path();
        }

        this.type = "dungeon";
    }

    public long getSeed() {
        return this.seed;
    }

    public FileType getType() {
        return this.ext;
    }

    public int getDifficulty() {
        return this.difficulty;
    }

    public String getFilename() {
        return this.fileName;
    }

    /**
     * Get the related file storying this dungeon's data in cache
     * 
     * @return a filehandle to the cache file in the temp directory, null if the
     *         dungeon is not generated from a file
     */
    public FileHandle getCacheFile() {
        return this.hashFile;
    }

    /**
     * @return true if the dungeon already exists in the cache
     */
    public boolean isCached() {
        return this.cached;
    }

    /**
     * Get the hashed filename of the dungeon you want to explore
     * 
     * @param fileName
     * @return
     */
    public static String getHashName(FileHandle fileName) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] hash = md.digest(fileName.path().getBytes());
            String name = new String(bytesToHex(hash));

            return name;
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Figures out the random seed hash value from a file. Reads in the entire
     * file, maxing the analysis array to 128 mb so we don't load an excessive
     * amount into memory.
     * 
     * @param f
     *            - the file we are analyzing
     * @return
     * @throws IOException
     */
    private static long getFileHash(File f) throws IOException {
        byte[] data = new byte[Math.min((int) f.length(), 134000000)];
        FileInputStream is = new FileInputStream(f);
        is.read(data);
        is.close();
        UUID hash = java.util.UUID.nameUUIDFromBytes(data);
        return hash.getMostSignificantBits();
    }

    /**
     * Load a file's dungeon data
     * 
     * @param f
     * @return
     */
    public static DungeonParams loadDataFromFile(FileHandle f) {
        String hashName = getHashName(f);
        DungeonParams params = new DungeonParams(f, hashName);
        return params;
    }

    public static DungeonParams loadRandomDungeon() {
        return new DungeonParams();
    }

    public static DungeonParams loadFromSimpleData(String data) throws Exception {
        DungeonParams params = null;
        try (Scanner reader = new Scanner(data)) {
            params = new DungeonParams(reader.nextLong(), FileType.valueOf(reader.next()), reader.nextInt());
        }
        return params;
    }

    /**
     * Convert byte loaded hash into a string
     * 
     * @param b
     * @return
     */
    private static String bytesToHex(byte[] b) {
        char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < b.length; j++) {
            buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
            buf.append(hexDigit[b[j] & 0x0f]);
        }
        return buf.toString();
    }

    public String getTileset() {
        return type;
    }

    public void change(){
        changed = true;
    }
    
    public boolean hasChanged() {
        // TODO Auto-generated method stub
        return changed;
    }

}
