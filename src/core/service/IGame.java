package core.service;

public interface IGame extends Service {

	public void startGame(int difficulty);
	public void fastStart();
	public void softReset();
	public void toggleFullscreen();
}
