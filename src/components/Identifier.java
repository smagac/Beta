package components;

import com.artemis.Component;

public class Identifier extends Component {

	String adjective;
	String type;
	String description;
	
	public Identifier(String base, String adjective)
	{
		this.type = base;
		this.adjective = adjective;
	}
	
	public Identifier(String base, String adjective, String description)
	{
		this.type = base;
		this.adjective = adjective;
		this.description = description;
	}
	
	@Override
	public String toString()
	{
		return this.adjective + " " + this.type;
	}
	
	public String getDescription()
	{
		return description;
	}
}
