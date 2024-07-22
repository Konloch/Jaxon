package jaxon.jdk;

import jaxon.build.BuildUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

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
			System.out.println("Export the JDK into a blank directory");
			return;
		}
		
		try
		{
			File newJavacEXE = new File(newJDK, "bin/javac.exe");
			File newJavaEXE = new File(newJDK, "bin/java.exe");
			File newJavac = new File(newJDK, "bin/javac");
			File newJava = new File(newJDK, "bin/java");
			File newRTJar = new File(newJDK, "jre/lib/rt.jar");
			
			//export blank rt.jar
			BuildUtil.export("/empty_jdk/java.exe", newJavacEXE);
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
