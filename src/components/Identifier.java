package components;

import com.artemis.Component;

public class Identifier extends Component {

	String adjective;
	String type;
	String description;
	boolean hidden;
	
	public Identifier(String base, String adjective, boolean hidden)
	{
		this.type = base;
		this.adjective = adjective;
	}
	
	@Override
	public String toString()
	{
		return this.adjective + " " + this.type;
	}
	
	public boolean hidden()
	{
		return hidden;
	}
}
