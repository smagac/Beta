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
}
