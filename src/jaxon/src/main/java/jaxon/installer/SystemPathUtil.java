package jaxon.installer;

import jaxon.JaxonConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static jaxon.installer.Installer.*;

/**
 * @author Konloch
 * @since 7/23/2024
 */
public class SystemPathUtil
{
	public static void systemPathCLI(String[] args)
	{
		String option = args[1];
		
		if(!isWindows())
		{
			System.out.println("This command is currently windows only, let us know if you need this feature");
			System.out.println("By opening an issue on https://konloch.com/jaxon or by submitting a pull request!");
			System.out.println();
			
			if(isNix())
			{
				System.out.println("Linux OS Hint:");
				System.out.println("1) You can preform this action manually by adding jaxon to your .bashrc file");
				System.out.println("2) This file is located in: /home/your-user-name/.bashrc");
				System.out.println("3) Add export PATH=\"your-dir:$PATH\" to the last line of the file, where your-dir is the directory you want to add.");
				System.out.println("4) Restart your terminal.");
			}
			else //assume mac
			{
				System.out.println("Mac OS Hint:");
				System.out.println("1) You can preform this action manually by adding jaxon to your .bash_profile file");
				System.out.println("2) This file is located in: /Users/your-user-name/.bash_profile");
				System.out.println("3) Add export PATH=\"your-dir:$PATH\" to the last line of the file, where your-dir is the directory you want to add.");
				System.out.println("4) Restart your terminal.");
			}
			//credits to https://gist.github.com/nex3/c395b2f8fd4b02068be37c961301caa7
			return;
		}
		
		if(option.equalsIgnoreCase("add"))
			adjustSystemPathWindows(true);
		else if(option.equalsIgnoreCase("remove"))
			adjustSystemPathWindows(false);
		else
			System.out.println(JaxonConstants.INCORRECT_USAGE);
	}
	
	private static void adjustSystemPathWindows(boolean addOrRemove)
	{
		try
		{
			File jaxonRoot = resolveJaxonRoot();
			File jaxonBin = new File(jaxonRoot, "bin");
			File jaxonBinary = new File(jaxonBin, isWindows() ? "jaxon.exe" : "jaxon");
			
			if (addOrRemove)
			{
				File selfBinary = new File(SystemPathUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				
				if(selfBinary.exists() && !jaxonBinary.exists())
				{
					if(selfBinary.getName().toLowerCase().endsWith("jar") || selfBinary.isDirectory() || !selfBinary.isFile())
					{
						System.out.println("You cannot add a Java jar to the System-Path");
						System.out.println("Either download a compiled version of Jaxon, or compile it yourself");
						System.out.println();
						return;
					}
					
					if(!jaxonBin.exists())
						jaxonBin.mkdirs();
					
					//copy self to Jaxon installation path
					Files.copy(selfBinary.toPath(), jaxonBinary.toPath());
					
					//add bin folder to System-Path
					modifyPathWindows(jaxonBin.getAbsolutePath(), true);
					
					System.out.println();
					System.out.println("Jaxon has been successfully installed");
					System.out.println();
				}
				else
				{
					System.out.println("Unable to copy Jaxon binary to " + jaxonBinary.getAbsolutePath());
					System.out.println("If you already have Jaxon installed, try doing 'jaxon uninstall' first");
					System.out.println();
				}
			}
			else
			{
				if(!jaxonBinary.exists())
				{
					System.out.println("Jaxon is not currently installed");
					return;
				}
				
				modifyPathWindows(jaxonBin.getAbsolutePath(), false);
				
				while(jaxonBinary.exists())
					Files.delete(jaxonBinary.toPath());
				
				jaxonBin.delete();
				
				System.out.println();
				System.out.println("Jaxon has been successfully uninstalled from the System-Path");
				System.out.println();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			System.out.println();
			System.out.println("Solution: Try running as admin");
			System.out.println();
		}
	}
	
	private static void modifyPathWindows(String path, boolean addPath) throws IOException, InterruptedException
	{
		String currentPath = System.getenv("PATH");
		
		String updatedPath;
		if (addPath)
		{
			if (!currentPath.contains(path))
				updatedPath = currentPath + ";" + path;
			else
				updatedPath = currentPath;
		}
		else
		{
			if (currentPath.contains(path))
				updatedPath = currentPath
						//remove instance of surrounding
						.replace(";" + path + ";", ";")
						//check for all other instances of path entry
						.replace(";" + path, "")
						.replace(path + ";", "");
			else
				updatedPath = currentPath;
		}
		
		EasyProcess process = EasyProcess.from(new ProcessBuilder(
				"reg", "add", "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment",
				"/v", "Path", "/t", "REG_EXPAND_SZ", "/d", updatedPath, "/f"
		));
		
		process.waitFor();
	}
}