package jaxon.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class TemplateUtil
{
	public static void init(String[] args) throws IOException
	{
		String name = (args.length >= 2) ? args[1] : null;
		
		//download the console template
		templateCLI(new String[]{"", "console"});
		
		//process the folder
		File console = new File("console");
		if(name != null)
		{
			File newLocation = new File(name);
			if(!console.renameTo(newLocation))
				System.out.println("Unable to rename directory, open your project using Intellij: " + console.getAbsolutePath());
			else
				System.out.println("Open your project using Intellij: " + newLocation.getAbsolutePath());
		}
		else
			System.out.println("Open your project using Intellij: " + console.getAbsolutePath());
	}
	
	public static void templateCLI(String[] args) throws IOException
	{
		String template = args[1];
		String version = args.length >= 3 ? args[2] : null;
		boolean downloadLatest = version == null;
		String[] packageList = readPackageList();
		
		String highestVersion = null;
		String downloadURL = null;
		for(String packageData : packageList)
		{
			String packageDataTrimmed = packageData.trim();
			if(packageDataTrimmed.startsWith("#") || !packageDataTrimmed.contains("="))
				continue;
			
			String[] packageInfo = packageData.split("=", 3);
			if(packageInfo.length < 3)
				continue;
			
			String packageName = packageInfo[0];
			String packageVersion = packageInfo[1];
			String packageURL = packageInfo[2];
			
			if(!packageName.equalsIgnoreCase(template))
				continue;
				
			if(downloadLatest)
			{
				//compare versions
				if(highestVersion == null || compare(highestVersion, packageVersion))
				{
					highestVersion = packageVersion;
					downloadURL = packageURL;
				}
			}
			else if(version.equalsIgnoreCase(packageVersion))
				downloadURL = packageURL;
		}
		
		if(downloadURL == null)
		{
			if(!downloadLatest)
				System.out.println("Could not find the template '" + template + "' with version `" + version + "`");
			else
				System.out.println("Could not find the template '" + template + "'");
		}
		else
		{
			File templateFile = new File(template);
			GitHubAPICloneRepo.cloneRepo(templateFile,
					downloadURL);
			System.out.println();
			System.out.println("Finished downloading all resources.");
			System.out.println("Open your new template using Intellij: " + templateFile.getAbsolutePath());
		}
	}
	
	private static String[] readPackageList() throws IOException
	{
		URL url = new URL("https://raw.githubusercontent.com/Konloch/Jaxon/master/community-projects/package.list");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		return new BufferedReader(new InputStreamReader(connection.getInputStream()))
				.lines()
				.toArray(String[]::new);
	}
	
	private static boolean compare(String versionA, String versionB)
	{
		String[] aVersionParts = versionA.split("\\.");
		String[] bVersionParts = versionB.split("\\.");
		
		for (int i = 0; i < 3; i++)
		{
			int aPart = Integer.parseInt(aVersionParts[i]);
			int bPart = Integer.parseInt(bVersionParts[i]);
			
			if (aPart > bPart)
				return true;
			else if (aPart < bPart)
				return false;
		}
		
		return false;
	}
}
