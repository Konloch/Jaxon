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
		File sourceJDK = new File(args[1]);
		File newJDK = new File(args[1] + ".no.rt");
		
		if(sourceJDK.exists())
		{
			System.out.println("Source JDK: " + sourceJDK.getAbsolutePath() + " does not exists.");
			return;
		}
		
		if(newJDK.exists())
		{
			System.out.println("New JDK: " + newJDK.getAbsolutePath() + " already exists, no need to create another.");
			System.out.println("Import that SDK inside of Intellij and use that for your Jaxon projects.");
			return;
		}
		
		System.out.println("Attempting to clone JDK: " + sourceJDK);
		
		try
		{
			File srcZip = new File(sourceJDK, "src.zip");
			File rtJar = new File(sourceJDK, "jre/lib/rt.jar");
			File newRTJar = new File(newJDK, "jre/lib/rt.jar");
			
			if (!sourceJDK.isDirectory())
			{
				System.out.println("Warning: This JDK should be JDK-8 / Java 1.8");
				System.err.println("InputJDK path is not a directory. Cannot proceed.");
				return;
			}
			
			if (!srcZip.exists())
			{
				System.out.println("Warning: This JDK should be JDK-8 / Java 1.8");
				System.err.println("JDK src.zip does not exist. Cannot proceed.");
				return;
			}
			
			if (!rtJar.exists())
			{
				System.out.println("Warning: This JDK should be JDK-8 / Java 1.8");
				System.err.println("JDK src.zip does not exist. Cannot proceed.");
				return;
			}
			
			//TODO verify source JDK is actually JDK-8
			
			copyJDK(sourceJDK, newJDK);
			
			//export blank rt.jar
			BuildUtil.export("/empty_jdk/rt.jar", newRTJar);
			
			System.out.println("Fully cloned into " + newJDK);
			System.out.println("Import that SDK inside of Intellij and use that for your Jaxon projects.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			
			System.out.println();
			System.out.println("Solution: Try running as admin");
		}
	}
	
	public static void copyJDK(File inputJDK, File newJDK) throws IOException
	{
		if (!inputJDK.exists() || !inputJDK.isDirectory())
			throw new IOException("Input JDK directory does not exist: " + inputJDK);
		
		if (!newJDK.exists())
			newJDK.mkdirs();
		
		for (File file : inputJDK.listFiles())
		{
			if (file.isFile())
				copyFile(file, new File(newJDK, file.getName()));
			else if (file.isDirectory())
				copyJDK(file, new File(newJDK, file.getName()));
		}
	}
	
	private static void copyFile(File source, File dest) throws IOException
	{
		//skip src.zip and rt.jar
		if(source.getName().equals("src.zip") || source.getName().equals("rt.jar"))
			return;
		
		File parent = dest.getParentFile();
		if(!parent.exists())
			parent.mkdirs();
		
		FileChannel inputChannel = new FileInputStream(source).getChannel();
		FileChannel outputChannel = new FileOutputStream(dest).getChannel();
		outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		inputChannel.close();
		outputChannel.close();
	}
}
