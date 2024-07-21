package jaxon.jdk;

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
		File inputJDK = new File(args[1]);
		File newJDK = new File(args[1] + ".no.rt");
		
		System.out.println("Attempting to clone JDK: " + inputJDK);
		
		try
		{
			File srcZip = new File(inputJDK, "src.zip");
			File rtJar = new File(inputJDK, "jre/lib/rt.jar");
			
			if (!inputJDK.isDirectory())
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
			
			copyJDK(inputJDK, newJDK);
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
		FileChannel inputChannel = new FileInputStream(source).getChannel();
		FileChannel outputChannel = new FileOutputStream(dest).getChannel();
		outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		inputChannel.close();
		outputChannel.close();
	}
}
