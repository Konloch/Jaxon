package jaxon.version;

/**
 * @author Konloch
 * @since 7/23/2024
 */
public class VersionUtil
{
	public static boolean DEV_MODE = false;
	public static String version = getVersion(VersionUtil.class.getPackage().getImplementationVersion());
	
	public static void versionCLI(String[] args)
	{
		System.out.println("Jaxon Version: " + version);
	}
	
	public static String getVersion(String mavenVersion)
	{
		if(mavenVersion == null)
		{
			DEV_MODE = true;
			return "Developer Mode";
		}
		
		return mavenVersion;
	}
}
