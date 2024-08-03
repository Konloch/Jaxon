package jaxon;

import jaxon.build.BuildUtil;
import jaxon.installer.Installer;
import jaxon.jdk.JDKUtil;
import jaxon.sjc.SJCUtil;
import jaxon.packages.PackageManager;
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
			System.out.println(JaxonConstants.INCORRECT_USAGE);
			return;
		}
		
		String command = args[0];
		
		try
		{
			if (command.equalsIgnoreCase("sjc") && args.length >= 2)
				SJCUtil.sjcJaxon(args);
			else if (command.equalsIgnoreCase("sjc-env") && args.length >= 4)
				SJCUtil.sjcJaxonEnvironment(args);
			else if (command.equalsIgnoreCase("build") && args.length >= 2)
				BuildUtil.buildCLI(args);
			else if (command.equalsIgnoreCase("package") && args.length >= 2)
				PackageManager.preformCLI(args);
			else if (command.equalsIgnoreCase("zip") && args.length >= 2)
				ZipUtil.zipCLI(args);
			else if (command.equalsIgnoreCase("jdk") && args.length >= 2)
				JDKUtil.jdkCLI(args);
			else if (command.equalsIgnoreCase("api") && args.length >= 2)
				PackageManager.createAPIFile(args);
			else if (command.equalsIgnoreCase("init"))
				PackageManager.init(args);
			else if (command.equalsIgnoreCase("install"))
				Installer.install();
			else if (command.equalsIgnoreCase("uninstall"))
				Installer.uninstall();
			else if (command.equalsIgnoreCase("upgrade"))
				Installer.upgrade();
			else if (command.equalsIgnoreCase("version"))
				VersionUtil.versionCLI(args);
			else
				System.out.println(JaxonConstants.INCORRECT_USAGE);
		}
		catch (RuntimeException e)
		{
			System.err.println(e.getMessage());
			//e.printStackTrace(); //enable as neededyy
		}
	}
}