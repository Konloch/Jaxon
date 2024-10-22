package jaxon.packages;

import jaxon.installer.Installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static jaxon.JaxonConstants.API_TOKEN_FILE;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class PackageManager
{
	protected static final Map<String, List<JaxonPackage>> packages = new HashMap<>();
	
	public static void init(String[] args) throws IOException
	{
		String name = (args.length >= 2) ? args[1] : null;
		
		//download the console template
		preformCLI(new String[]{"", "console"});
		
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
	
	public static void preformCLI(String[] args) throws IOException
	{
		String inputName = args[1];
		String inputVersion = args.length >= 3 ? args[2] : null;
		String name = args.length >= 4 ? args[3] : null;
		
		//latest version is an optional parameter
		if(inputVersion != null && inputVersion.equalsIgnoreCase("latest"))
			inputVersion = null;
		
		boolean downloadLatest = inputVersion == null;
		String couldNotFind = "Could not find the package '" + inputName + "'" + (((!downloadLatest) ? " with version '" + inputVersion + "'" : ""));
		
		//announce that we're attempting to fetch the package
		System.out.println("Fetching package '" + inputName + "'" + (((!downloadLatest) ? " version '" + inputVersion + "'" : "")) + "...");
		System.out.println();
		
		//clear any previous actions
		packages.clear();
		
		//read the package list from GitHub
		String[] packageList = readPackageList();
		
		//process the package list
		int index;
		for(index = 0; index < packageList.length; index++)
		{
			String packageData = packageList[index];
			String packageDataTrimmed = packageData.trim();
			
			if(packageDataTrimmed.startsWith("#") || !packageDataTrimmed.contains("="))
				continue;
			
			String[] packageInfo = packageData.split("=", 3);
			
			if(packageInfo.length != 3)
				continue;
			
			//extract package info
			String packageName = packageInfo[0];
			String packageVersion = packageInfo[1];
			String packageURL = packageInfo[2];
			JaxonPackage jaxonPackage = new JaxonPackage(packageName, packageVersion, packageURL);
			
			//peek forward and read dependencies
			int tempIndex = index + 1;
			while(tempIndex + 1 < packageList.length)
			{
				String nextLine = packageList[tempIndex++];
				
				if(nextLine.startsWith("#") || !nextLine.contains("="))
					continue;
				
				String[] dependencyInfo = nextLine.split("=");
				
				//exit after we've reached the end of dependencies
				if(dependencyInfo.length != 2)
					break;
				
				//extract dependency info
				String dependencyName = dependencyInfo[0];
				String dependencyVersion = dependencyInfo[1];
				JaxonDependency dependency = new JaxonDependency(dependencyName, dependencyVersion);
				
				//add the dependency to the jaxon dependencies
				jaxonPackage.dependencies.add(dependency);
			}
			
			//insert package into known packages
			List<JaxonPackage> jaxonPackages = packages.getOrDefault(packageName, new ArrayList<>());
			jaxonPackages.add(jaxonPackage);
			packages.putIfAbsent(packageName, jaxonPackages);
		}
		
		//search for matching package name based on input name
		List<JaxonPackage> jaxonPackages = packages.get(inputName);
		
		if(jaxonPackages == null || jaxonPackages.isEmpty())
		{
			System.out.println(couldNotFind);
			System.out.println("Reason: Package '" + inputName + "' not found");
			System.out.println();
			
			//recommend package names that are close to the input name
			for(JaxonPackage recommendedPackage : PackageManagerUtils.recommendClosestPackageByName(inputName))
				System.out.println(" + Did you mean '" + recommendedPackage.name + "'?");
			return;
		}
		
		//search for matching package version based on input version
		JaxonPackage latestVersion = null;
		for(JaxonPackage jaxonPackage : jaxonPackages)
		{
			if(downloadLatest)
			{
				//compare versions
				if(latestVersion == null || PackageManagerUtils.compare(latestVersion.version, jaxonPackage.version))
					latestVersion = jaxonPackage;
			}
			else if(jaxonPackage.version.equalsIgnoreCase(inputVersion))
			{
				latestVersion = jaxonPackage;
				break;
			}
		}
		
		if(latestVersion == null)
		{
			System.out.println(couldNotFind);
			System.out.println("Reason: Version '" + inputVersion + "' not found");
			System.out.println();
			
			//recommend versions that are close to the input version
			for(JaxonPackage recommendedPackage : PackageManagerUtils.recommendClosestPackageByVersion(inputVersion, jaxonPackages))
				System.out.println(" + Did you mean '" + recommendedPackage.version + "'?");
		}
		else
		{
			File packageDirectory = new File(inputName);
			
			cloneWithDependencies(packageDirectory, latestVersion);
			
			System.out.println();
			System.out.println("Finished downloading all resources.");
			System.out.println();
			
			//rename to the new package
			if(name != null)
			{
				File newLocation = new File(name);
				if(!packageDirectory.renameTo(newLocation))
					System.out.println("Unable to rename directory, open your project using Intellij: " + packageDirectory.getAbsolutePath());
				else
					System.out.println("Open your project using Intellij: " + newLocation.getAbsolutePath());
			}
			else
				System.out.println("Open your project using Intellij: " + packageDirectory.getAbsolutePath());
		}
	}
	
	private static void cloneWithDependencies(File packageDirectory, JaxonPackage jaxonPackage) throws IOException
	{
		//clone dependencies in order
		for(JaxonDependency dependency : jaxonPackage.dependencies)
		{
			JaxonPackage jaxonDependency = packageFromDependency(dependency);
			cloneWithDependencies(packageDirectory, jaxonDependency);
		}
		
		//clone the package
		boolean success = GitHubAPICloneRepo.cloneRepo(packageDirectory, jaxonPackage.url);
		
		if(!success)
		{
			if(GitHubAPICloneRepo.RATE_LIMITED)
			{
				System.out.println("You are being rate-limited by the GitHub API");
				System.out.println();
				System.out.println("To get around this:");
				System.out.println(" A) Either wait the rate-limit out (1 hour)");
				System.out.println(" B) Provide Jaxon your GitHub API Token to continue accessing the GitHub API");
				System.out.println();
				System.out.println("To give Jaxon your GitHub API Token:");
				System.out.println(" 1) Visit this URL in a web-browser: https://github.com/settings/personal-access-tokens/new");
				System.out.println("    + This should open 'Fine-Grained Tokens' which is the newer and safer version");
				System.out.println(" 2) Click 'Generate Token' and copy it");
				System.out.println(" 3) Paste it into the command 'jaxon api [token]");
				System.out.println("    + Command Example: 'jaxon api github_pat_rlgygRBVVL_93TmkDOkWy_Az972SIU4L'");
				System.out.println();
			}
			
			throw new RuntimeException(GitHubAPICloneRepo.FAIL_REASON);
		}
	}
	
	private static JaxonPackage packageFromDependency(JaxonDependency dependency)
	{
		List<JaxonPackage> jaxonPackages = packages.get(dependency.name);
		
		//this means package.list was misconfigured, out of developer scope
		if(jaxonPackages == null || jaxonPackages.isEmpty())
			throw new RuntimeException("Dependency Not Found (Package Name Not Matching): " + dependency);
		
		for(JaxonPackage jaxonPackage : jaxonPackages)
		{
			if(jaxonPackage.version.equalsIgnoreCase(dependency.version))
				return jaxonPackage;
		}
		
		//this means package.list was misconfigured, out of developer scope
		throw new RuntimeException("Dependency Not Found (Package Version Not Matching): " + dependency);
	}
	
	private static String[] readPackageList() throws IOException
	{
		URL url = new URL("https://raw.githubusercontent.com/Konloch/Jaxon/master/community-projects/package.list");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		return new BufferedReader(new InputStreamReader(connection.getInputStream()))
				.lines()
				.toArray(String[]::new);
	}
	
	public static void createAPIFile(String[] args) throws IOException
	{
		String token = args[1];
		
		//write API key to disk
		Files.write(API_TOKEN_FILE.toPath(), token.getBytes(StandardCharsets.UTF_8));
		
		System.out.println("GitHub API Token Saved");
	}
}
