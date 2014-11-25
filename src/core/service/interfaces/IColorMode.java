package core.service.interfaces;

import github.nhydock.ssm.Service;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import core.Palette;

public interface IColorMode extends Service {

    public Palette getPalette();

    public void setPalette(Palette p);

    public float getContrast();

    public float brighten();

    public float darken();

    public void invert();

    public boolean isInverted();

    public ShaderProgram getShader();
}
