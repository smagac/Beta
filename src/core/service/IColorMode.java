package core.service;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import core.Palette;

public interface IColorMode {

	public Palette getPalette();
	public void setPalette(Palette p);
	
	public float getContrast();
	public float brighten();
	public float darken();
	
	public void invert();
	public boolean isInverted();
	
	public ShaderProgram getShader();
}
