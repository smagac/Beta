package core.service.implementations;

import github.nhydock.ssm.Inject;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;

import scenes.Messages;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import core.DataDirs;
import core.components.Equipment;
import core.components.Groups;
import core.components.Identifier;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Inventory;
import core.datatypes.QuestTracker;
import core.datatypes.StatModifier;
import core.service.implementations.ScoreTracker.NumberValues;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.ISharedResources;

public class PlayerManager implements IPlayerContainer {
    public static final int SaveSlots = 3;
    private static final String SaveFormat = "slot%03d.sv";
    private static boolean noSave = false;

    private static final String timeFormat = "%03d:%02d:%02d";
    private float time;
    private int[] formattedTime = new int[3];
    private int difficulty;

    private Entity player;
    private Inventory inventory;
    private QuestTracker quests;
    private String goddess;
    private String character;

    private boolean made;
    
    @Inject public ISharedResources shared;
    @Inject public ScoreTracker tracker;

    public PlayerManager() {
        for (int i = 1; i <= SaveSlots; i++) {

            FileHandle save = getSaveFile(i);
            if (!save.exists()) {
                try {
                    save.parent().mkdirs();
                    save.file().createNewFile();
                }
                catch (IOException e) {
                    noSave = true;
                }
            }
        }
        
        player = new Entity();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public QuestTracker getQuestTracker() {
        return quests;
    }

    @Override
    public Entity getPlayer() {
        return player;
    }

    /**
     * Fully heal the player
     */
    @Override
    public void rest() {
        recover();
        tracker.increment(NumberValues.Times_Slept);

        // new quests each day
        getInventory().refreshCrafts();
    }

    @Override
    public void recover() {
        Stats stats = player.getComponent(Stats.class);
        stats.hp = stats.maxhp;
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
    }

    @Override
    public int[] getTimeElapsed() {
        formattedTime[0] = (int) (time / 3600f);
        formattedTime[1] = (int) (time / 60f) % 60;
        formattedTime[2] = (int) (time % 60);
        return formattedTime;
    }
    
    
    private int[] formatTime(float time) {
        int[] formattedTime = new int[3];
        formattedTime[0] = (int) (time / 3600f);
        formattedTime[1] = (int) (time / 60f) % 60;
        formattedTime[2] = (int) (time % 60);
        return formattedTime;
    }
    
    @Override
    public String getFullTime() {
        
        return String.format("%d hours %d minutes and %d seconds", (int) (time / 3600f), (int) (time / 60f) % 60,
                (int) (time % 60));
    }

    @Override
    public String getGender() {
        return character;
    }

    @Override
    public String getWorship() {
        return goddess;
    }

    @Override
    public SaveSummary summary(int slot) {

        JsonReader json = new JsonReader();
        try {
            JsonValue jv = json.parse(getAppData().child(String.format(SaveFormat, slot)));

            
            SaveSummary s = new SaveSummary();
            s.gender = jv.getString("gender");
            s.progress = jv.getInt("made") + "/" + jv.getInt("required");
            
            int[] fTime = formatTime(jv.getFloat("time"));
            
            s.time = String.format(timeFormat, fTime[0], fTime[1], fTime[2]);
            s.date = jv.getString("date");
            s.diff = jv.getInt("difficulty");
            return s;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public int slots() {
        return 3;
    }

    @Override
    public void save(int slot) {
        if (noSave)
            return;

        try {
            JsonWriter js = new JsonWriter(new FileWriter(getSaveFile(slot).file()));

            Json json = new Json();
            json.setWriter(js);

            json.writeObjectStart();
            json.writeValue("gender", getGender());
            json.writeValue("made", inventory.getProgress());
            json.writeValue("required", inventory.getRequiredCrafts().size);
            json.writeValue("time", time);
            json.writeValue("difficulty", difficulty);

            DateFormat df = DateFormat.getDateInstance();
            json.writeValue("date", df.format(Calendar.getInstance().getTime()));
            json.writeValue("inventory", inventory, Inventory.class);
            json.writeValue("stats", player.getComponent(Stats.class), Stats.class);
            json.writeValue("tracker", tracker, ScoreTracker.class);
            json.writeValue("quests", quests, QuestTracker.class);
            json.writeObjectEnd();
            js.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load(int slot) {

        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(getSaveFile(slot));
        Json json = new Json();

        this.character = root.getString("gender");
        this.goddess = (character.equals("male") ? "goddess" : "god");
        this.inventory = new Inventory();
        this.inventory.read(json, root.get("inventory"));
        this.time = root.getFloat("time");
        this.difficulty = root.getInt("difficulty");
        this.player = new Entity();
        Stats stats = new Stats();
        stats.read(json, root.get("stats"));
        this.player.add(stats);
        this.player.add(new Identifier("Adventurer", null, new String[0]));
        Renderable r = new Renderable(this.character);
        r.loadImage(shared.getResource(DataDirs.Home + "uiskin.json", Skin.class));
        this.player.add(r);
        this.player.add(new Groups.Player());
        this.player.add(new Position(0,0));
        
        this.quests = json.readValue(QuestTracker.class, root.get("quests"));
        tracker.read(json, root.get("tracker"));
        made = true;
    }

    private FileHandle getSaveFile(int slot) {
        return getAppData().child(String.format(SaveFormat, slot));
    }

    /**
     * Provides location of where to save persistent storymode game files to
     * 
     * @return
     */
    private FileHandle getAppData() {
        String OS = System.getProperty("os.name").toUpperCase();
        FileHandle h;
        if (OS.contains("WIN"))
            h = Gdx.files.absolute(System.getenv("APPDATA") + "/StoryMode");
        else if (OS.contains("MAC"))
            h = Gdx.files.absolute(System.getProperty("user.home") + "/Library/Application Support/StoryMode");
        else
            h = Gdx.files.absolute(System.getProperty("user.home") + "/.local/share/StoryMode");

        if (!h.exists()) {
            h.mkdirs();
        }
        return h;
    }

    @Override
    public void init(int difficulty, boolean gender) {
        if (made) {
            throw new GdxRuntimeException(
                    "Player Manager has already created a character.  The service must be disposed of before init can be called again");
        }

        this.difficulty = difficulty;

        // make a player
        player = new Entity();
        player.add(new Stats(new int[]{10, 5, 5, 10, 0}, new StatModifier[0]));
        player.add(new Identifier("Adventurer", null, new String[0]));
        player.add(new Groups.Player());
        player.add(new Position(0,0));
        player.add(new Equipment());
        
        // make crafting requirements
        inventory = new Inventory(difficulty);

        // make quest tracker
        quests = new QuestTracker();

        // reset game clock
        time = 0f;

        character = (gender) ? "male" : "female";
        goddess = (gender) ? "goddess" : "god";
        
        Renderable r = new Renderable(character);
        r.loadImage(shared.getResource(DataDirs.Home + "uiskin.json", Skin.class));
        player.add(r);
        
        made = true;
    }

    @Override
    public boolean isPrepared() {
        return made;
    }

    @Override
    public void updateTime(float delta) {
        time += delta;
    }

    @Override
    public void onRegister() {
        // Do nothing   
    }

    @Override
    public void onUnregister() {
        // Do nothing
    }
}
