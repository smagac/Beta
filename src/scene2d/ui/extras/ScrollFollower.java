package scene2d.ui.extras;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class ScrollFollower extends ChangeListener {

	List<?> list;
	ScrollPane pane;
	
	public ScrollFollower(ScrollPane pane, List<?> list)
	{
		this.pane = pane;
		this.list = list;
	}
	
	@Override
	public void changed(ChangeEvent event, Actor actor) {
		float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + pane.getHeight()/2);
		pane.scrollTo(0, list.getHeight()-y, pane.getWidth(), pane.getHeight());
	}

}
