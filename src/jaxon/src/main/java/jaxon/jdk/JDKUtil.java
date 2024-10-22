package jaxon.jdk;

import jaxon.build.BuildUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class JDKUtil
{
	public static void jdkCLI(String[] args) throws IOException
	{
		File newJDK = new File(args[1]);
		
		if(newJDK.exists())
		{
			System.out.println(newJDK.getAbsolutePath() + " already exists");
			System.out.println("Export this JDK into a blank directory.");
			return;
		}
		
		try
		{
			File newJavacEXE = new File(newJDK, "bin/javac.exe");
			File newJavaEXE = new File(newJDK, "bin/java.exe");
			File newJavac = new File(newJDK, "bin/javac");
			File newJava = new File(newJDK, "bin/java");
			File newRTJar = new File(newJDK, "jre/lib/rt.jar");
			
			//Notes on why we use javac and don't make java:
			//intellij lets "javaC" version response format work even though it uses "java" for it, looks like it just searches for "major.minor version so "1.8"
			//when this breaks, just clone the tools/fake-javac and add java.exe with the expected format
			
			//export blank rt.jar
			BuildUtil.export("/empty_jdk/javac.exe", newJavacEXE);
			BuildUtil.export("/empty_jdk/javac.exe", newJavaEXE);
			BuildUtil.export("/empty_jdk/javac", newJavac);
			BuildUtil.export("/empty_jdk/javac", newJava);
			BuildUtil.export("/empty_jdk/rt.jar", newRTJar);
			
			System.out.println("Created Jaxon-Blank-JDK: " + newJDK);
			System.out.println("Import that SDK inside of Intellij and use that for your Jaxon projects.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			System.out.println();
			System.out.println("Solution: Try running as admin");
		}
	}
}
