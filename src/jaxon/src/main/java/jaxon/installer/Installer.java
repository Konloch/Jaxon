package jaxon.installer;

import jaxon.jdk.JDKUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Konloch
 * @since 7/26/2024
 */
public class Installer
{
	public static void install() throws IOException
	{
		File jaxonRoot = resolveJaxonRoot();
		File jaxonBin = new File(jaxonRoot, "bin");
		File jaxonJDK = new File(jaxonRoot, "JDK");
		File jaxonBinary = new File(jaxonBin, isWindows() ? "jaxon.exe" : "jaxon");
		
		if(jaxonBinary.exists())
		{
			System.out.println("Jaxon is already installed.");
			System.out.println("To remove Jaxon use `jaxon uninstall` or manually delete `" + jaxonRoot.getAbsolutePath() + "`");
			return;
		}
		
		if(!jaxonRoot.exists())
			jaxonRoot.mkdirs();
		
		SystemPathUtil.systemPathCLI(new String[]{"", "add"});
		
		if(!jaxonJDK.exists())
			JDKUtil.jdkCLI(new String[]{"", jaxonJDK.getAbsolutePath()});
		else //assume JDK is already installed
		{
			System.out.println("Re-using Jaxon-Blank-JDK: " + jaxonJDK.getAbsolutePath());
			System.out.println("Import that SDK inside of Intellij and use that for your Jaxon projects.");
		}
	}
	
	public static void uninstall()
	{
		SystemPathUtil.systemPathCLI(new String[]{"", "remove"});
	}
	
	public static void upgrade() throws IOException
	{
		File jaxonRoot = resolveJaxonRoot();
		File jaxonBin = new File(jaxonRoot, "bin");
		File jaxonBinary = new File(jaxonBin, isWindows() ? "jaxon.exe" : "jaxon");
		
		while(jaxonBinary.exists())
			Files.delete(jaxonBinary.toPath());
		
		install();
	}
	
	public static boolean isWindows()
	{
		return System.getProperty("os.name").toLowerCase().contains("win");
	}
	
	public static boolean isNix()
	{
		String os = System.getProperty("os.name").toLowerCase();
		return os.contains("nix") || os.contains("nux") || os.contains("bsd");
	}
	
	public static File resolveJaxonRoot()
	{
		File defaultLocation =  new File(System.getProperty("user.home"), ".jaxon");
		
		//if jaxon was previously installed using the default directory, continue to use that
		if(defaultLocation.exists())
			return defaultLocation;
		
		//handle XDG Base Directory - https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html
		if(isNix())
		{
			File homeLocal = new File(System.getProperty("user.home"), ".local");
			
			if(homeLocal.exists())
				return new File(new File(homeLocal, "share"), ".jaxon");
			
			File homeConfig = new File(System.getProperty("user.home"), ".config");
			if(homeConfig.exists())
				return new File(homeConfig, ".jaxon");
		}
		
		//return jaxon default location
		return defaultLocation;
	}
}
