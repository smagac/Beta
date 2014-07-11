package core.service;

import components.Stats;
import core.datatypes.Inventory;

public interface IPlayerContainer extends Service {

	public Inventory getInventory();
	public Stats getPlayer();
	public void rest();
	public String getTimeElapsed();
	public String getFullTime();
}
