package core.service.interfaces;

import github.nhydock.ssm.Service;

public interface IGame extends Service {

    public interface GamePreferences {};
    
    public void startGame(GamePreferences preferences);

    public void fastStart();

    public void softReset();

    public void endGame();

    public boolean hasStarted();
    
    public boolean hardcore();
    
    public boolean debug();
}
