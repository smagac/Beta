package core.factories;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.DungeonParams;
import core.datatypes.dungeon.FloorData;
import core.datatypes.dungeon.RandomFloorData;
import core.util.dungeon.PathMaker;

/**
 * Generates tiled maps and populates them for you to explore
 * 
 * @author nhydock
 *
 */
public class DungeonFactory {

    /**
     * Load a file from its cachefile
     * 
     * @param params
     * @return
     */
    public static Dungeon loadFromCache(DungeonParams params) {
        if (params.isCached()) {
            FileHandle hashFile = params.getCacheFile();

            String outStr = "";
            try {
                FileInputStream in = new FileInputStream(hashFile.file());
                GZIPInputStream unzipper = new GZIPInputStream(in);
                InputStreamReader zipRead = new InputStreamReader(unzipper);
                BufferedReader bf = new BufferedReader(zipRead);

                String line;
                while ((line = bf.readLine()) != null) {
                    outStr += line;
                }

                bf.close();
                zipRead.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            Json json = new Json();
            Dungeon d = json.fromJson(Dungeon.class, outStr);
            return d;
        }
        return null;
    }

    /**
     * Writes a created dungeon to the cache file indicated by the params used
     * to create the dungeon.
     * 
     * @param params
     * @param dungeon
     * @return
     */
    public static void writeCacheFile(DungeonParams params, Dungeon dungeon) {
        // ignore randomly created dungeons for caching because
        // they don't have a cache file
        // also don't overwrite files that have already been cached
        if (params.getCacheFile() != null && !params.isCached()) {
            try (FileOutputStream out = new FileOutputStream(params.getCacheFile().file());
                    GZIPOutputStream gzip = new GZIPOutputStream(out);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip, "UTF-8"));) {

                Json json = new Json();
                json.toJson(dungeon, Dungeon.class, writer);
            }
            catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    /**
     * Generate a new dungeon and save all its floors into a temp directory
     * 
     * @param difficulty
     *            - defines how large and how many monsters the dungeon will
     *            have, but also more loot
     * @throws IOException
     * @return an array of the serial ids of the floors on the file system
     */
    public static Dungeon create(DungeonParams params, int[] progress) {
        // make sure dungeon dir exists
        // Gdx.files.absolute(tmpDir).mkdirs();
        // make sure to create cache files one first create
        if (params.getCacheFile() != null) {
            try {
                FileHandle hashFile = params.getCacheFile();
                hashFile.parent().mkdirs();
                hashFile.file().createNewFile();
                hashFile.file().deleteOnExit();
            }
            catch (IOException e) {
                System.err.println("could not create cache file for " + params.getFilename());
                e.printStackTrace();
            }
        }

        Random oldRandom = MathUtils.random;
        Random seededRandom = new Random(params.getSeed());
        MathUtils.random = seededRandom;

        // please don't ask about my numbers, they're so randomly picked out
        // from my head
        // I don't even know what the curve looks like on a TI calculator ;_;
        int depth = MathUtils.random(5 * params.getDifficulty(), 10 * params.getDifficulty()
                + (params.getDifficulty() - 1) * 10);
        // to stress test, uncomment next line
        //depth = 99;
        final Array<FloorData> floors = new Array<FloorData>();
        floors.addAll(new FloorData[depth]);
        // final Thread[] makerThreads = new Thread[Math.min(Math.max(1,
        // depth/8), 4)];

        for (int i = depth-1, made=1; i >= 0; i--, made++) {
            Runnable run = new FloorMaker(params.getDifficulty(), i, floors);
            run.run();
            progress[0] = (int)((made / (float)depth) * 100);
        }

        Dungeon d = new Dungeon(params, floors);
        MathUtils.random = oldRandom;
        return d;
    }

    /**
     * Simple runnable for making a floor of a dungeon and updating a loader's
     * progress
     * 
     * @author nhydock
     *
     */
    private static class FloorMaker implements Runnable {

        final int difficulty;
        private int depth;
        private int width;
        private int height;
        final Array<FloorData> dungeon;

        private FloorMaker(int difficulty, int depth, Array<FloorData> dungeon) {
            this.difficulty = difficulty;
            this.dungeon = dungeon;
            this.depth = depth;
        }

        @Override
        public void run() {
            FloorData floor;
            width = 50 + (5 * (depth / 5));
            height = 50 + (5 * (depth / 5));
            floor = new RandomFloorData(difficulty, depth + 1, width, height);

            int roomCount = MathUtils.random(Math.max(5, ((3 * depth) / 10) + depth),
                    Math.max(5, ((5 * depth) / 10) + depth));
            PathMaker.run(floor, roomCount);
            dungeon.set(depth, floor);
        }
    }
}
