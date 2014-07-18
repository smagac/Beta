package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

public class LabeledTicker<T> extends Table {

	private TextButton left;
	private TextButton right;
	private Label label;
	private Label valueLabel;
	
	private T[] values;
	private int index;
	
	private Runnable leftClick;
	private Runnable rightClick;
	
	public final Runnable defaultLeftClick = new Runnable(){
		@Override
		public void run() {
			setValue(index-1);
		}
	};
	
	public final Runnable defaultRightClick = new Runnable(){
		@Override
		public void run() {
			setValue(index+1);
		}
	};
	
	public LabeledTicker(String name, T[] values, Skin skin)
	{
		super();
		this.values = values;
		index = 0;
		label = new Label(name, skin, "prompt");
		label.setAlignment(Align.left);
		left = new TextButton("<", skin, "big");
		left.pad(10);
		right = new TextButton(">", skin, "big");
		right.pad(10);
		valueLabel = new Label(values[0].toString(), skin, "prompt");
		valueLabel.setAlignment(Align.center);
		
		
		Table buttons = new Table();
		buttons.right();
		buttons.add(left).colspan(1).width(48f);
		buttons.add(valueLabel).colspan(1).width(74f);
		buttons.add(right).colspan(1).width(48f);
		
		add(label).colspan(1).expandX().fillX();
		add(buttons).colspan(2).expandX().fillX();
		
		left.addListener(new InputListener(){
			@Override
			public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				left.setChecked(true);
			}
			
			@Override
			public void exit(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				left.setChecked(false);
			}
			
			@Override
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				if (leftClick != null)
				{
					leftClick.run();	
					return true;
				}
				return false;
			}
		});
		
		right.addListener(new InputListener(){
			@Override
			public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				right.setChecked(true);
			}
			
			@Override
			public void exit(InputEvent evt, float x, float y, int pointer, Actor fromActor)
			{
				right.setChecked(false);
			}
			
			@Override
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				if (rightClick != null)
				{
					rightClick.run();
					return true;
				}
				return false;
			}
		});
		
		setLeftAction(defaultLeftClick);
		setRightAction(defaultRightClick);	
		
		pack();
	}
	
	public void setLeftAction(Runnable r)
	{
		leftClick = r;
	}
	
	public void setRightAction(Runnable r)
	{
		rightClick = r;
	}
	
	public void setValue(int index)
	{
		this.index = Math.max(0, Math.min(values.length-1, index));
		valueLabel.setText(values[index].toString());
	}
	
	public void changeValues(T[] values)
	{
		this.values = values;
		setValue(0);
	}
	
	public T getValue()
	{
		return values[index];
	}
	
	public int getValueIndex()
	{
		return index;
	}
	
	public int length()
	{
		return values.length;
	}
}
