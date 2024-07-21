package jaxon.build;

import jaxon.Jaxon;
import jaxon.sjc.SJCUtil;

import java.io.*;
import java.nio.file.Files;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class BuildUtil
{
	public static String buildName = "build";
	
	public static void buildCLI(String[] args) throws IOException
	{
		//TODO read for -out= and consider that the buildName
		
		String buildScript = args[1];
		String[] dirs = new String[args.length - 2];
		for (int i = 2; i < args.length; i++)
			dirs[i - 2] = new File(args[i]).getAbsolutePath();
		
		if (buildScript.equalsIgnoreCase("win-exe"))
		{
			setupEnv("native");
			SJCUtil.sjcCLI(merge(new String[]{"-s", "512k", "-a", "4198912", "-l", "-o", "boot", "-O", "#exe"}, dirs));
			exportBuild("OUT_WIN.EXE", "build/windows/build.exe");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("win-app"))
		{
			setupEnv("native");
			SJCUtil.sjcCLI(merge(new String[]{"-s", "512k", "-a", "4198912", "-l", "-o", "boot", "-O", "#win"}, dirs));
			exportBuild("OUT_WIN.EXE", "build/windows/" + buildName + ".exe");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("lin"))
		{
			setupEnv("native");
			SJCUtil.sjcCLI(merge(new String[]{"-s", "512k", "-a", "1049008", "-l", "-o", "boot", "-O", "#llb"}, dirs));
			exportBuild("OUT_LIN.O", "build/linux/" + buildName);
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("atmega"))
		{
			setupEnv("atmega");
			SJCUtil.sjcCLI(merge(new String[]{"-t", "atmega", "-L", "-P", "batmel32.bin", "-y", "-e", "0x60", "-E", "-a", "0", "-o", "boot", "-B", "-C", "-k"}, dirs));
			exportBuild("BOOT_ATM.HEX", "build/atmega/" + buildName + ".hex");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("os-32"))
		{
			setupEnv("operating-system");
			SJCUtil.sjcCLI(merge(new String[]{"-t", "ia32", "-o", "boot", "-O", "#floppy32"}, dirs));
			exportBuild("BOOT_FLP.IMG", "build/operating-system/" + buildName + ".img");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("os-64"))
		{
			setupEnv("operating-system");
			SJCUtil.sjcCLI(merge(new String[]{"-t", "amd64", "-o", "boot", "-O", "#floppy64"}, dirs));
			exportBuild("BOOT_FLP.IMG", "build/operating-system/" + buildName + ".img");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else
		{
			System.out.println("Read the documentation at https://konloch.com/Jaxon");
		}
	}
	
	public static void setupEnv(String environment)
	{
		File tempDir = new File("");
		
		try
		{
			tempDir = Files.createTempDirectory("jaxon").toFile();
			System.setProperty("user.dir", tempDir.getAbsolutePath());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		if (environment.equalsIgnoreCase("native"))
		{
			setupEnvFile("/environment/native/bootconf.txt", "bootconf.txt");
			setupEnvFile("/environment/native/file_lin.bin", "file_lin.bin");
			setupEnvFile("/environment/native/file_llb.bin", "file_llb.bin");
			setupEnvFile("/environment/native/file_win.bin", "file_win.bin");
		}
		else if (environment.equalsIgnoreCase("atmega"))
		{
			setupEnvFile("/environment/atmega/bootconf.txt", "bootconf.txt");
			setupEnvFile("/environment/atmega/batmel32.bin", "batmel32.bin");
		}
		else if (environment.equalsIgnoreCase("operating-system"))
		{
			setupEnvFile("/environment/operating-system/bootconf.txt", "bootconf.txt");
			setupEnvFile("/environment/operating-system/bts_dsk.bin", "bts_dsk.bin");
			setupEnvFile("/environment/operating-system/b64_dsk.bin", "b64_dsk.bin");
		}
		
		tempDir.deleteOnExit();
	}
	
	public static void setupEnvFile(String localPath, String envPath)
	{
		File envFile = new File(envPath).getAbsoluteFile();
		
		try (InputStream in = Jaxon.class.getResourceAsStream(localPath); OutputStream out = new FileOutputStream(envFile))
		{
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1)
			{
				out.write(buffer, 0, bytesRead);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Failed to setup env file", e);
		}
		
		envFile.deleteOnExit();
	}
	
	public static void exportBuild(String currentOutputBinary, String newOutputBinary) throws IOException
	{
		File outputBinary = new File(currentOutputBinary).getAbsoluteFile();
		File newOutputBinaryFile = new File(newOutputBinary);
		File newOutputBinaryFileParent = newOutputBinaryFile.getParentFile();
		
		if (!newOutputBinaryFileParent.exists() || !newOutputBinaryFileParent.isDirectory())
			newOutputBinaryFileParent.mkdirs();
		
		while (newOutputBinaryFile.exists())
		{
			Files.delete(newOutputBinaryFile.toPath());
		}
		
		if (outputBinary.exists())
			outputBinary.renameTo(newOutputBinaryFile);
	}
	
	private static String[] merge(String[] partA, String[] partB)
	{
		String[] array = new String[partA.length + partB.length];
		
		int index = 0;
		for (String arg : partA)
			array[index++] = arg;
		
		for (String dir : partB)
			array[index++] = dir;
		
		return array;
	}
}