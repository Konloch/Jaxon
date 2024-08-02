package jaxon.packages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Konloch
 * @since 8/2/2024
 */
public class PackageManagerUtils
{
	public static List<JaxonPackage> recommendClosestPackageByName(String inputName)
	{
		List<JaxonPackage> closestPackages = new ArrayList<>();
		
		for (List<JaxonPackage> packages : PackageManager.packages.values())
		{
			JaxonPackage jaxonPackage = packages.get(0);
			
			int distance = levenshteinDistance(inputName, jaxonPackage.name);
			
			if (distance <= 2)
				closestPackages.add(jaxonPackage);
		}
		
		//sort the list by closest towards the input name
		closestPackages.sort(Comparator.comparingInt(pkg -> levenshteinDistance(inputName, pkg.name)));
		
		//only return the top 3
		return closestPackages.subList(0, Math.min(3, closestPackages.size()));
	}
	
	public static List<JaxonPackage> recommendClosestPackageByVersion(String inputVersion, List<JaxonPackage> jaxonPackages)
	{
		List<JaxonPackage> closestPackages = new ArrayList<>();
		int[] inputVer = parseVersion(inputVersion);
		
		for (JaxonPackage jaxonPackage : jaxonPackages)
		{
			int[] packageVer = parseVersion(jaxonPackage.version);
			int distance = versionDistance(inputVer, packageVer);
			
			//add to closest if the distance is within an acceptable range, or decide on a cut-off
			//e.g., accept within 1 major, 1 minor, and 0 patches
			if (distance <= 110)
				closestPackages.add(jaxonPackage);
		}
		
		//sort the list by closest versions
		closestPackages.sort(Comparator.comparingInt(pkg -> versionDistance(inputVer, parseVersion(pkg.version))));
		
		//only return the top 3
		return closestPackages.subList(0, Math.min(3, closestPackages.size()));
	}
	
	public static int levenshteinDistance(String s1, String s2)
	{
		int[] cost = new int[s2.length() + 1];
		
		for (int i = 0; i <= s2.length(); i++)
			cost[i] = i;
		
		for (int j = 1; j <= s1.length(); j++)
		{
			int lastValue = cost[0];
			cost[0] = j;
			for (int i = 1; i <= s2.length(); i++)
			{
				int newValue = cost[i];
				int match = lastValue + (s1.charAt(j - 1) == s2.charAt(i - 1) ? 0 : 1);
				int insert = cost[i - 1] + 1;
				int delete = cost[i] + 1;
				cost[i] = Math.min(Math.min(insert, delete), match);
				lastValue = newValue;
			}
		}
		
		return cost[s2.length()];
	}
	
	public static boolean compare(String versionA, String versionB)
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
	
	private static int[] parseVersion(String version)
	{
		String[] parts = version.split("\\.");
		return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
	}
	
	private static int versionDistance(int[] version1, int[] version2)
	{
		int majorDiff = Math.abs(version1[0] - version2[0]);
		int minorDiff = Math.abs(version1[1] - version2[1]);
		int patchDiff = Math.abs(version1[2] - version2[2]);
		
		//major, minor and patch all have different weights
		return majorDiff * 100 + minorDiff * 10 + patchDiff;
	}
}
