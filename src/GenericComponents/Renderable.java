package GenericComponents;

import com.artemis.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Renderable extends Component {

	private final TextureRegion sprite;
	
	public Renderable(TextureRegion region)
	{
		sprite = region;
	}

	public TextureRegion getSprite() {
		return sprite;
	}
}
