package com.nhydock.storymode.service.implementations;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.nhydock.storymode.Palette;
import com.nhydock.storymode.service.interfaces.IColorMode;

public class ColorManager implements IColorMode {
    // COOL RENDERING
    private Palette currentHue = Palette.Original;
    private float contrast = .5f;
    private ShaderProgram hueify;
    private boolean invert;
    private Color clear = new Color();

    @Override
    public void onRegister() {
        hueify = new ShaderProgram(Gdx.files.classpath("shaders/bg.vertex.glsl"),
                Gdx.files.classpath("shaders/bg.fragment.glsl"));
        if (!hueify.isCompiled()) {
            (new GdxRuntimeException(hueify.getLog())).printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void onUnregister() {
        hueify.dispose();
    }
    
    @Override
    public Palette getPalette() {
        return currentHue;
    }

    @Override
    public void setPalette(Palette p) {
        if (p.equals(currentHue)) {
            invert();
        }
        else {
            currentHue = p;
            invert = false;
        }
    }

    @Override
    public float getContrast() {
        return (invert) ? 1f - contrast : contrast;
    }

    @Override
    public float brighten() {
        return contrast = Math.min(.9f, contrast + .1f);
    }

    @Override
    public float darken() {
        return contrast = Math.max(.1f, contrast - .1f);
    }

    @Override
    public void invert() {
        invert = !invert;
    }

    @Override
    public boolean isInverted() {
        return invert;
    }

    @Override
    public ShaderProgram getShader() {
        return hueify;
    }

    public void resize(int width, int height) {
        hueify.begin();
        hueify.setUniformf("u_resolution", width, height);
        hueify.end();
    }
    
    @Override
    public Color getClear() {
        return clear;
    }
    
    public void update() {
        Palette p = getPalette();
        float contrast = getContrast();

        // Copy AMD formula on wikipedia for smooth step in GLSL so our
        // background can be the same as the shader
        // Scale, bias and saturate x to 0..1 range
        contrast = MathUtils.clamp((contrast - .5f) / (1f - .5f), 0.0f, 1.0f);
        // Evaluate polynomial
        contrast = contrast * contrast * (3 - 2 * contrast);

        clear.set(isInverted() ? currentHue.high : currentHue.low);
        clear.lerp(isInverted() ? p.low : p.high, contrast);

        // bind the attribute
        hueify.begin();
        hueify.setUniformf("contrast", getContrast());
        if (isInverted()) {
            hueify.setUniformf("low", p.high);
            hueify.setUniformf("high", p.low);
        }
        else {
            hueify.setUniformf("low", p.low);
            hueify.setUniformf("high", p.high);
        }
        hueify.setUniformi("vignette", p.vignette ? 1 : 0);
        hueify.end();
    }
}
