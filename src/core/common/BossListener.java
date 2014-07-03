package core.common;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Input.Keys;

import core.Palette;
import core.service.IColorMode;

public class BossListener implements InputProcessor {

	private IColorMode service;
	
	public BossListener(IColorMode service)
	{
		this.service = service;
	}
	
	public IColorMode getService()
	{
		return service;
	}
	
	@Override
	public boolean keyDown(int keycode) {

		//control hues
		Palette nextCol = null;
		
		if (keycode == Keys.NUM_1) { nextCol = Palette.Original; }
		if (keycode == Keys.NUM_2) { nextCol = Palette.Gameboy; }
		if (keycode == Keys.MINUS) { getService().darken(); }
		if (keycode == Keys.EQUALS) { getService().brighten(); }
		
		if (nextCol != null)
		{
			getService().setPalette(nextCol);
			return true;
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
