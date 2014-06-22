package GenericSystems;

import GenericComponents.Position;
import GenericComponents.Renderable;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class RenderSystem extends EntitySystem {

	private SpriteBatch batch;
	
	@SuppressWarnings("unchecked")
	public RenderSystem(SpriteBatch batch)
	{
		super(Aspect.getAspectForAll(Renderable.class, Position.class));
		this.batch = batch;
	}
	
	protected void process(Entity e) {
		Position p = e.getComponent(Position.class);
		Renderable r = e.getComponent(Renderable.class);
		
		batch.draw(r.getSprite(), p.getX(), p.getY());
	}
	
	public void setCamera(Camera c)
	{
		batch.setProjectionMatrix(c.combined);
	}

	@Override
	protected void processEntities(ImmutableBag<Entity> entities) {
		batch.begin();
	
		for (int i = 0; i < entities.size(); i++)
		{
			Entity e = entities.get(i);
			process(e);
		}
		
		batch.end();
	}

	@Override
	protected boolean checkProcessing() {
		return true;
	}
}
