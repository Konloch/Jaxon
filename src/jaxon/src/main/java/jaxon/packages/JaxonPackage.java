package jaxon.packages;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Konloch
 * @since 8/2/2024
 */
public class JaxonPackage
{
	public final String name;
	public final String version;
	public final String url;
	public final List<JaxonDependency> dependencies = new ArrayList<>();
	
	public JaxonPackage(String name, String version, String url)
	{
		this.name = name;
		this.version = version;
		this.url = url;
	}
	
	@Override
	public String toString()
	{
		return "JaxonPackage{" + "name='" + name + '\'' + ", version='" + version + '\'' + ", url='" + url + '\'' + '}';
	}
}
