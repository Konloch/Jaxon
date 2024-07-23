package jaxon;

import jaxon.build.BuildUtil;
import jaxon.jdk.JDKUtil;
import jaxon.sjc.SJCUtil;
import jaxon.systempath.SystemPathUtil;
import jaxon.templates.TemplateUtil;
import jaxon.version.VersionUtil;
import jaxon.zip.ZipUtil;

import java.io.*;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class Jaxon
{
	public static void main(String[] args) throws IOException
	{
		if (args.length <= 0)
		{
			System.out.println("Incorrect Usage: Read the documentation at https://konloch.com/Jaxon");
			return;
		}
		
		String command = args[0];
		
		if (command.equalsIgnoreCase("sjc") && args.length > 2)
		{
			int newArgumentsLength = args.length - 1;
			String[] trimmedArgs = new String[newArgumentsLength];
			int i;
			for (i = 2; i < args.length; i++)
			{
				int normalizedIndex = i - 1;
				trimmedArgs[normalizedIndex] = args[i];
			}
			
			SJCUtil.sjcCLI(trimmedArgs);
		}
		else if (command.equalsIgnoreCase("build") && args.length > 2)
			BuildUtil.buildCLI(args);
		else if (command.equalsIgnoreCase("template") && args.length > 2)
			TemplateUtil.templateCLI(args);
		else if (command.equalsIgnoreCase("zip") && args.length > 2)
			ZipUtil.zipCLI(args);
		else if (command.equalsIgnoreCase("jdk") && args.length > 2)
			JDKUtil.jdkCLI(args);
		else if (command.equalsIgnoreCase("system-path") && args.length > 2)
			SystemPathUtil.systemPathCLI(args);
		else if (command.equalsIgnoreCase("install"))
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
		else if (command.equalsIgnoreCase("uninstall"))
			SystemPathUtil.systemPathCLI(new String[]{"", "remove"});
		else if (command.equalsIgnoreCase("version"))
			VersionUtil.versionCLI(args);
		else
			System.out.println("Incorrect Usage: Read the documentation at https://konloch.com/Jaxon");
	}
}