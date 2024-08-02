package jaxon.packages;

/**
 * @author Konloch
 * @since 8/2/2024
 */
public class JaxonDependency
{
	public final String name;
	public final String version;
	
	public JaxonDependency(String name, String version)
	{
		this.name = name;
		this.version = version;
	}
	
	@Override
	public String toString()
	{
		return "JaxonDependency{" + "name='" + name + '\'' + ", version='" + version + '\'' + '}';
	}
}
