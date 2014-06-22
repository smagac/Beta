package GenericComponents;

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
	
	public String toString()
	{
		return this.adjective + " " + this.type;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public int hashCode()
	{
		int prime = 31;
		int hash = 0;
		hash += adjective.hashCode() + prime;
		hash += type.hashCode() + prime;
		hash +=  description.hashCode() + prime;
		return hash;
	}
}
