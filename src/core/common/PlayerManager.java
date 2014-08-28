package core.common;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import components.Stats;
import core.datatypes.Inventory;
import core.service.interfaces.IPlayerContainer;

public class PlayerManager implements IPlayerContainer {
	public static final int SaveSlots = 3;
	private static final String SaveFormat = "slot%03d.sv";
	private static boolean noSave = false;
	
	private static final String timeFormat = "%03d:%02d:%02d";
	private float time;
	private int difficulty;
	
	private Stats player;
	private Inventory inventory;
	private String goddess;
	private String character;
	
	private boolean made;
	
	public PlayerManager()
	{
		for (int i = 1; i <= SaveSlots; i++)
		{
			
			FileHandle save = getSaveFile(i);
			if (!save.exists())
			{
				try {
					save.parent().mkdirs();
					save.file().createNewFile();
				} catch (IOException e) {
					noSave = true;
				}
			}
		}
	}

	@Override
	public Inventory getInventory()
	{
		return inventory;
	}
	
	@Override
	public Stats getPlayer()
	{
		return player;
	}

	/**
	 * Fully heal the player
	 */
	@Override
	public void rest()
	{
		player.hp = player.maxhp;
		Tracker.NumberValues.Times_Slept.increment();
	}

	@Override
	public String getTimeElapsed()
	{
		return formatTime(this.time);
	}
	
	public static String formatTime(float time)
	{
		return String.format(timeFormat, (int)(time/3600f), (int)(time/60f)%60, (int)(time % 60));
	}

	@Override
	public String getFullTime() {
		return String.format("%d hours %d minutes and %d seconds", (int)(time/3600f), (int)(time/60f)%60, (int)(time % 60));
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
		try
		{
			JsonValue jv = json.parse(getAppData().child(String.format(SaveFormat, slot)));
			
			SaveSummary s = new SaveSummary();
			s.gender = jv.getString("gender");
			s.progress = jv.getInt("made") + "/" + jv.getInt("required");
			s.time = formatTime(jv.getFloat("time"));
			s.date = jv.getString("date");
			s.diff = jv.getInt("difficulty");
			return s;
		}
		catch (Exception e)
		{
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
			json.writeValue("stats", player, Stats.class);
			json.writeValue("tracker", Tracker._instance, Tracker.class);
			
			json.writeObjectEnd();
			js.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void load(int slot) {
		
		JsonReader reader = new JsonReader();
		JsonValue root = reader.parse(getSaveFile(slot));
		Json json = new Json();
		
		this.character = root.getString("gender");
		this.goddess = (character.equals("male")?"goddess":"god");
		this.inventory = new Inventory();
		this.inventory.read(json, root.get("inventory"));
		this.time = root.getFloat("time");
		this.difficulty = root.getInt("difficulty");
		this.player = new Stats();
		this.player.read(json, root.get("stats"));
		Tracker._instance.read(json, root.get("tracker"));
	}

	private FileHandle getSaveFile(int slot)
	{
		return getAppData().child(String.format(SaveFormat, slot));
	}

	/**
	 * Provides location of where to save persistent storymode game files to
	 * @return
	 */
	private FileHandle getAppData()
	{
	    String OS = System.getProperty("os.name").toUpperCase();
	    FileHandle h;
	    if (OS.contains("WIN"))
	        h = Gdx.files.absolute(System.getenv("APPDATA")+"/StoryMode");
	    else if (OS.contains("MAC"))
	        h = Gdx.files.absolute(System.getProperty("user.home") + "/Library/Application Support/StoryMode");
	    else
	        h = Gdx.files.absolute(System.getProperty("user.home") + "/.config/StoryMode");
	    
	    if (!h.exists())
	    {
	    	h.mkdirs();
	    }
	    return h;
	}

	@Override
	public void init(int difficulty, boolean gender) {
		if (made)
		{
			throw new GdxRuntimeException("Player Manager has already created a character.  The service must be disposed of before init can be called again");
		}
		
		this.difficulty = difficulty;
		
		//make a player
		player = new Stats(10, 5, 5, 10, 0);

		//make crafting requirements
		inventory = new Inventory(difficulty);
		
		//reset game clock
		time = 0f;
		
		character = (gender)?"male":"female";
		goddess = (gender)?"goddess":"god";
		
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
}
