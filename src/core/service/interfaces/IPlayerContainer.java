package core.service.interfaces;

import github.nhydock.ssm.Service;
import core.components.Stats;
import core.datatypes.Inventory;

public interface IPlayerContainer extends Service {

	public Inventory getInventory();
	public Stats getPlayer();
	public void rest();
	public void recover();
	public String getTimeElapsed();
	public String getFullTime();
	public String getGender();
	public String getWorship();
	public SaveSummary summary(int slot);
	public int slots();
	public void save(int slot);
	public void load(int slot);
	
	public static class SaveSummary
	{
		public String gender;
		public String time;
		public String progress;
		public String date;
		public int diff;
	}

	public void init(int difficulty, boolean gender);
	public boolean isPrepared();
	public void updateTime(float delta);
}
