package jaxon.installer;

import jaxon.jdk.JDKUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static jaxon.JaxonConstants.*;

/**
 * @author Konloch
 * @since 7/26/2024
 */
public class Installer
{
	public static void install() throws IOException
	{
		if(JAXON_BINARY_FILE.exists())
		{
			System.out.println("Jaxon is already installed.");
			System.out.println("To remove Jaxon use `jaxon uninstall` or manually delete `" + JAXON_ROOT_DIR.getAbsolutePath() + "`");
			return;
		}
		
		if(!JAXON_ROOT_DIR.exists())
			JAXON_ROOT_DIR.mkdirs();
		
		adjustInstallationState(true);
		
		if(!JAXON_JDK_DIR.exists())
			JDKUtil.jdkCLI(new String[]{"", JAXON_JDK_DIR.getAbsolutePath()});
		else //assume JDK is already installed
		{
			System.out.println("Re-using Jaxon-Blank-JDK: " + JAXON_JDK_DIR.getAbsolutePath());
			System.out.println("Import that SDK inside of Intellij and use that for your Jaxon projects.");
		}
	}
	
	public static void uninstall() throws IOException
	{
		adjustInstallationState(false);
	}
	
	public static void upgrade() throws IOException
	{
		while(JAXON_BINARY_FILE.exists())
			Files.delete(JAXON_BINARY_FILE.toPath());
		
		install();
	}
	
	public static void adjustInstallationState(boolean installer)
	{
		if(!IS_WINDOWS)
		{
			System.out.println("This command is currently windows only, let us know if you need this feature");
			System.out.println("By opening an issue on https://konloch.com/jaxon or by submitting a pull request!");
			System.out.println();
			
			if(IS_NIX)
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
		
		try
		{
			if (installer)
			{
				File selfBinary = new File(Installer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				
				if(selfBinary.exists() && !JAXON_BINARY_FILE.exists())
				{
					if(selfBinary.getName().toLowerCase().endsWith("jar") || selfBinary.isDirectory() || !selfBinary.isFile())
					{
						System.out.println("You cannot add a Java jar to the System-Path");
						System.out.println("Either download a compiled version of Jaxon, or compile it yourself");
						System.out.println();
						return;
					}
					
					if(!JAXON_BIN_DIR.exists())
						JAXON_BIN_DIR.mkdirs();
					
					//copy self to Jaxon installation path
					Files.copy(selfBinary.toPath(), JAXON_BINARY_FILE.toPath());
					
					//add bin folder to System-Path
					modifyPathWindows(JAXON_BIN_DIR.getAbsolutePath(), true);
					
					System.out.println();
					System.out.println("Jaxon has been successfully installed");
					System.out.println();
				}
				else
				{
					System.out.println("Unable to copy Jaxon binary to " + JAXON_BINARY_FILE.getAbsolutePath());
					System.out.println("If you already have Jaxon installed, try doing 'jaxon uninstall' first");
					System.out.println();
				}
			}
			else //uninstaller
			{
				if(!JAXON_BINARY_FILE.exists())
				{
					System.out.println("Jaxon is not currently installed");
					return;
				}
				
				modifyPathWindows(JAXON_BIN_DIR.getAbsolutePath(), false);
				
				while(JAXON_BINARY_FILE.exists())
					Files.delete(JAXON_BINARY_FILE.toPath());
				
				JAXON_BIN_DIR.delete();
				
				while(API_TOKEN_FILE.exists())
					Files.delete(API_TOKEN_FILE.toPath());
				
				System.out.println();
				System.out.println("Jaxon has been successfully uninstalled");
				System.out.println();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			System.out.println();
			System.out.println("Solution: Try running as Jaxon admin");
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
