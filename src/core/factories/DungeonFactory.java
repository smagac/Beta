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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
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
    public static Dungeon create(DungeonParams params, float[] progress) {
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
                Gdx.app.log("DungeonFactory", "could not create cache file for " + params.getFilename());
                e.printStackTrace();
            }
        }

        Random seededRandom = new Random(params.getSeed());
        
        // please don't ask about my numbers, they're so randomly picked out from my head
        // I don't even know what the curve looks like on a TI calculator ;_;
        int s = 5 * params.getDifficulty();
        int e = 10 * params.getDifficulty() + (params.getDifficulty() - 1) * 10;
        int depth = s + seededRandom.nextInt(e - s + 1);
        
        // to stress test, uncomment next line
        // depth = 99;
        final Array<FloorData> floors = new Array<FloorData>();
        floors.addAll(new FloorData[depth]);
        long[] seeds = new long[depth];
        for (int i = 0; i < depth; i++) {
            seeds[i] = seededRandom.nextLong();
        }
        
        int[] volDepth = {0};
        Array<Thread> threads = new Array<Thread>();
        //create threads
        for (int i = 0; i < 4; i++) {
            FloorLoader fl = new FloorLoader();
            fl.depth = volDepth;
            fl.seeds = seeds;
            fl.floors = floors;
            fl.progress = progress;
            fl.increment = (int)((1.0f / (float)depth) * 100);
            Thread t = new Thread(fl);
            threads.add(t);
        }
        //start threads
        for (int d = 0; d < threads.size; d++) {
            Thread t = threads.get(d);
            t.start();
        }
        //wait for threads
        boolean done = false;
        while (!done) {
            done = true;
            for (int i = 0; i < threads.size && done; i++) {
                Thread t = threads.get(i);
                if (t.getState() != Thread.State.TERMINATED) {
                    done = false;
                }
            }
        }
        
        Dungeon d = new Dungeon(params, floors);
        return d;
    }
    
    private static class FloorLoader implements Runnable {
        int difficulty;
        int increment;
        float[] progress;
        Array<FloorData> floors;
        int[] depth;
        long[] seeds;
        
        @Override
        public void run(){
            Gdx.app.log("Dungeon Generation", "Building " + floors.size + " floors");
            
            while (true) {
                int d;
                long seed;
                synchronized (depth) {
                    if (depth[0] >= floors.size) {
                        break;
                    }
                    d = depth[0];
                    seed = seeds[depth[0]];
                    depth[0] = depth[0] + 1;   
                }
                
                int width = 30 + (int)(5 * (d / 5f));
                int height = 30 + (int)(5 * (d / 5f));
                
                FloorData data = new RandomFloorData(seed, difficulty, d + 1, width, height);
                PathMaker.run(data);
                floors.set(d, data);
            
                progress[0] += increment;
            }
        }
    }
}
