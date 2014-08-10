package core.service;

import github.nhydock.ssm.Service;
import components.Stats;
import core.datatypes.Inventory;

public interface IPlayerContainer extends Service {

	public Inventory getInventory();
	public Stats getPlayer();
	public void rest();
	public String getTimeElapsed();
	public String getFullTime();
	public String getGender();
	public String getWorship();
	public void save(int slot);
	public void load(int slot);
	public SaveSummary summary(int slot);
	public int slots();
	
	public static class SaveSummary
	{
		public String gender;
		public String time;
		public String progress;
		public String date;
		public int diff;
	}
}
