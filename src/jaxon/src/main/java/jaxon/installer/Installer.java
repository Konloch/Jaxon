package jaxon.installer;

import jaxon.jdk.JDKUtil;
import jaxon.systempath.SystemPathUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Konloch
 * @since 7/26/2024
 */
public class Installer
{
	public static void install() throws IOException
	{
		File jaxonRoot = SystemPathUtil.resolveJaxonRoot();
		File jaxonBin = new File(jaxonRoot, "bin");
		File jaxonJDK = new File(jaxonRoot, "JDK");
		File jaxonBinary = new File(jaxonBin, SystemPathUtil.isWindows() ? "jaxon.exe" : "jaxon");
		
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
}
