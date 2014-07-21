package scenes;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;

import core.common.Storymode;

public abstract class UI extends Stage {

	protected Skin skin;
	protected Image fader;
	protected AssetManager manager;
	protected Image pointer;
	
	public UI(AssetManager manager)
	{
		super(new ScalingViewport(Scaling.fit, Storymode.InternalRes[0], Storymode.InternalRes[1]));
		this.manager = manager;
		
		load();
	}

	/**
	 * Load any resources needed by this ui
	 */
	protected abstract void load();
	
	/**
	 * Initializes all elements of the ui after the manager has finished loading
	 */
	public abstract void init();
	
	public static Group makeWindow(Skin skin, int width, int height)
	{
		return makeWindow(skin, width, height, false);
	}
	
	/**
	 * Custom required method to create complex actors that are recognized
	 * as a single window in order to provide tiling of the ninepatch 
	 */
	public static Group makeWindow(Skin skin, int width, int height, boolean filled)
	{
		Group group = new Group();
		group.setSize(width, height);
		group.setTouchable(Touchable.childrenOnly);

		TextureRegion p = skin.getRegion("window");
		TextureRegion[] split = {
				new TextureRegion(p, 0, 0, 32, 32),   //tl
				new TextureRegion(p, 32, 0, 32, 32),  //tc
				new TextureRegion(p, 64, 0, 32, 32),  //tr
				new TextureRegion(p, 0, 32, 32, 32),  //ml
				new TextureRegion(p, 32, 32, 32, 32), //mc
				new TextureRegion(p, 64, 32, 32, 32), //mr
				new TextureRegion(p, 0, 64, 32, 32),  //bl
				new TextureRegion(p, 32, 64, 32, 32), //bc
				new TextureRegion(p, 64, 64, 32, 32)  //br
		};
		
		//setup corners
		Image tl = new Image(new TextureRegionDrawable(split[0]));
		tl.setPosition(0, height-32);
		
		Image tr = new Image(new TextureRegionDrawable(split[2]));
		tr.setPosition(width-32, height-32);
		
		Image bl = new Image(new TextureRegionDrawable(split[6]));
		bl.setPosition(0, 0);
		
		Image br = new Image(new TextureRegionDrawable(split[8]));
		br.setPosition(width-32, 0);
		
		group.addActor(tl);
		group.addActor(tr);
		group.addActor(bl);
		group.addActor(br);
		
		//setup sides
		Image t = new Image(new TiledDrawable(split[1]));
		t.setPosition(32, height-32);
		t.setWidth(width-64);
		group.addActor(t);
		
		Image l = new Image(new TiledDrawable(split[3]));
		l.setPosition(0, 32);
		l.setHeight(height-64);
		group.addActor(l);
		
		Image r = new Image(new TiledDrawable(split[5]));
		r.setPosition(width-32, 32);
		r.setHeight(height-64);
		group.addActor(r);
		
		Image b = new Image(new TiledDrawable(split[7]));
		b.setPosition(32, 0);
		b.setWidth(width-64);
		group.addActor(b);

		//setup center
		if (filled)
		{
			Image c = new Image(new TiledDrawable(split[4]));
			c.setPosition(32, 32);
			c.setSize(width-64, height-64);
			group.addActor(c);
		}

		return group;
	}
	
	public void resize(int width, int height){
		getViewport().update(width, height);
	}
	
	/**
	 * Positions the pointer next to an actor
	 * @param focus
	 * @param center 
	 */
	public void showPointer(Actor focus, int hAlignment, int vAlignment)
	{
		Vector2 v = new Vector2();
		focus.localToStageCoordinates(v);
		float xOffset = 0,
			  yOffset = 0;
		if (hAlignment == Align.left)
		{
			xOffset = -pointer.getWidth();
		}
		else if (hAlignment == Align.center)
		{
			xOffset = focus.getWidth()/2 - pointer.getWidth()/2;
		}
		else if (hAlignment == Align.right)
		{
			xOffset = focus.getWidth() + pointer.getWidth();
		}
		
		if (vAlignment == Align.top)
		{
			yOffset = focus.getHeight() - pointer.getHeight();
		}
		else if (vAlignment == Align.center)
		{
			yOffset = focus.getHeight()/2 - pointer.getHeight()/2;
		}
		else if (vAlignment == Align.bottom)
		{
			yOffset = 0;
		}
		
		
		pointer.setPosition(v.x+xOffset, v.y+yOffset);
		pointer.setScale((hAlignment == Align.right)?-1:1, 1);
		pointer.setVisible(true);
	}
	
	public void hidePointer()
	{
		pointer.setVisible(false);
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		unhook();
	}
	
	/**
	 * Unhook any injected services from the parent that you decided to attach,
	 * you freakin' lazy dingus.
	 */
	protected void unhook() {}
}
