package core.service.interfaces;

import github.nhydock.ssm.Service;

public interface IGame extends Service {

    public void startGame(int difficulty, boolean gender);

    public void fastStart();

    public void softReset();

    public void endGame();

    public boolean hasStarted();
}
