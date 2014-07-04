package core.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;

import core.Palette;
import core.service.IColorMode;
import core.service.IGame;

public class BossListener implements InputProcessor {

	private static BossListener instance;
	public static BossListener getInstance()
	{
		return instance;
	}
	
	private IColorMode color;
	private IGame game;
	
	protected BossListener(IColorMode colorService, IGame gameService)
	{
		this.color = colorService;
		this.game = gameService;
		instance = this;
	}
	
	public IColorMode getColorService()
	{
		return color;
	}
	
	public IGame getGameService()
	{
		return game;
	}
	
	@Override
	public boolean keyDown(int keycode) {

		//control hues
		Palette nextCol = null;
		
		if (keycode == Keys.NUM_1) { nextCol = Palette.Original; }
		if (keycode == Keys.NUM_2) { nextCol = Palette.Gameboy; }
		if (keycode == Keys.NUM_3) { nextCol = Palette.VirtualBoy; }
		if (keycode == Keys.MINUS) { getColorService().darken(); return true; }
		if (keycode == Keys.EQUALS) { getColorService().brighten(); return true; }
		
		if (nextCol != null)
		{
			getColorService().setPalette(nextCol);
			return true;
		}
		
		if (keycode == Keys.F9)
		{
			getGameService().startGame(3);
			return true;
		}
		if (keycode == Keys.F10)
		{
			getGameService().fastStart();
		}
		
		//fullscreen toggle
		if (keycode == Keys.ENTER)
		{
			if (Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT))
			{
				getGameService().toggleFullscreen();
			}
		}
		
		return false;
	}

	@Override
	public boolean keyTyped(char arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean keyUp(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMoved(int arg0, int arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean scrolled(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDown(int arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchDragged(int arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean touchUp(int arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		return false;
	}

}
