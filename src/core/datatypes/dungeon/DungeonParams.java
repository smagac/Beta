package core.datatypes.dungeon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

import core.datatypes.FileType;

/**
 * Params used for defining the core specifications of a dungeon.
 * 
 * @author nhydock
 *
 */
public class DungeonParams {
    private boolean cached;
    private long seed;
    private FileType ext;
    private int difficulty;
    private FileHandle hashFile;
    private String fileName;

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
    }

    private DungeonParams(long seed, FileType ext, int difficulty) {
        this.seed = seed;
        this.ext = ext;
        this.difficulty = Math.max(1, Math.min(5, difficulty));
        this.cached = false;
        this.hashFile = null;
        this.fileName = null;
    }

    /**
     * 
     * @param file
     * @param ext
     * @param difficulty
     */
    private DungeonParams(FileHandle file, String hashName) {
        try {
            this.seed = getFileHash(file.file());
            this.cached = false;
            this.ext = FileType.getType(file.name());
            this.difficulty = ext.difficulty(file.length());

            String tmpDir = System.getProperty("java.io.tmpdir");
            this.hashFile = Gdx.files.absolute(tmpDir + "/storymode/" + hashName + ".tmp");
            if (this.hashFile.exists()) {
                this.cached = true;
            }

            this.fileName = file.path();
        }
        catch (IOException e) {
            this.seed = -1;
            this.ext = FileType.Other;
            this.difficulty = 0;
            this.hashFile = null;
            this.cached = false;
            this.fileName = file.path();
        }
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

    public String getFileName() {
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
        XXHash64 hasher = XXHashFactory.nativeInstance().hash64();

        return hasher.hash(data, 0, data.length, 0L);
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

    public static DungeonParams loadFromSimpleData(String data) {
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

}
