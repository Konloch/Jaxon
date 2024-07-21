package jaxon;

import sjc.compbase.Context;
import sjc.osio.sun.ReflectionSymbols;
import sjc.symbols.SymbolFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Konloch
 * @since 7/18/2024
 */
public class Jaxon
{
	public static String buildName = "build";
	
	public static void main(String[] args) throws IOException
	{
		if (args.length < 2)
		{
			System.out.println("Incorrect Usage: Read the documentation at https://konloch.com/Jaxon");
			return;
		}
		
		String command = args[0];
		
		if (command.equalsIgnoreCase("sjc"))
		{
			int newArgumentsLength = args.length - 1;
			String[] trimmedArgs = new String[newArgumentsLength];
			int i;
			for (i = 2; i < args.length; i++)
			{
				int normalizedIndex = i - 1;
				trimmedArgs[normalizedIndex] = args[i];
			}
			sjc(trimmedArgs);
		}
		else if (command.equalsIgnoreCase("build"))
			build(args);
		else if (command.equalsIgnoreCase("template"))
			template(args);
		else if (command.equalsIgnoreCase("zip"))
			zip(args);
		else
			System.out.println("Read the documentation at https://konloch.com/Jaxon");
	}
	
	private static void sjc(String[] args)
	{
		int res;
		Context ctx = new Context(new JaxonIO(System.out));
		SymbolFactory.preparedReflectionSymbols = new ReflectionSymbols();
		if ((res = ctx.compile(args, "vJRE")) == 0)
			ctx.writeSymInfo();
		
		if (res != 0)
			System.out.println("Results: " + res);
	}
	
	private static void template(String[] args) throws IOException
	{
		String template = args[1];
		
		if (template.equalsIgnoreCase("console"))
			createTemplate("console");
		else if (template.equalsIgnoreCase("graphical"))
			createTemplate("graphical");
		else if (template.equalsIgnoreCase("operating-system"))
			createTemplate("operating-system");
		else if (template.equalsIgnoreCase("atmega"))
		{
			createTemplate("atmega");
			String warning = "WARNING: ATmega currently fails to build - this will need to be resolved";
			System.err.println(warning);
			System.out.println(warning);
		}
		else if (template.equalsIgnoreCase("barebones"))
		{
			createTemplate("barebones");
			String warning = "WARNING: You should instead use the template 'console' unless you know what you're doing";
			System.err.println(warning);
			System.out.println(warning);
		}
	}
	
	private static void build(String[] args) throws IOException
	{
		//TODO read for -out= and consider that the buildName
		
		String buildScript = args[1];
		String[] dirs = new String[args.length - 2];
		for (int i = 2; i < args.length; i++)
			dirs[i - 2] = new File(args[i]).getAbsolutePath();
		
		if (buildScript.equalsIgnoreCase("win-exe"))
		{
			setupEnv("native");
			sjc(merge(new String[]{"-s", "512k", "-a", "4198912", "-l", "-o", "boot", "-O", "#exe"}, dirs));
			exportBuild("OUT_WIN.EXE", "build/windows/build.exe");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("win-app"))
		{
			setupEnv("native");
			sjc(merge(new String[]{"-s", "512k", "-a", "4198912", "-l", "-o", "boot", "-O", "#win"}, dirs));
			exportBuild("OUT_WIN.EXE", "build/windows/" + buildName + ".exe");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("lin"))
		{
			setupEnv("native");
			sjc(merge(new String[]{"-s", "512k", "-a", "1049008", "-l", "-o", "boot", "-O", "#llb"}, dirs));
			exportBuild("OUT_LIN.O", "build/linux/" + buildName);
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("atmega"))
		{
			setupEnv("atmega");
			sjc(merge(new String[]{"-t", "atmega", "-L", "-P", "batmel32.bin", "-y", "-e", "0x60", "-E", "-a", "0", "-o", "boot", "-B", "-C", "-k"}, dirs));
			exportBuild("BOOT_ATM.HEX", "build/atmega/" + buildName + ".hex");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("os-32"))
		{
			setupEnv("operating-system");
			sjc(merge(new String[]{"-t", "ia32", "-o", "boot", "-O", "#floppy32"}, dirs));
			exportBuild("BOOT_FLP.IMG", "build/operating-system/" + buildName + ".img");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else if (buildScript.equalsIgnoreCase("os-64"))
		{
			setupEnv("operating-system");
			sjc(merge(new String[]{"-t", "amd64", "-o", "boot", "-O", "#floppy64"}, dirs));
			exportBuild("BOOT_FLP.IMG", "build/operating-system/" + buildName + ".img");
			exportBuild("syminfo.txt", "build/build_sym_info.txt");
		}
		else
		{
			System.out.println("Read the documentation at https://konloch.com/Jaxon");
		}
	}
	
	private static void setupEnv(String environment)
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
	
	private static void setupEnvFile(String localPath, String envPath)
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
	
	private static void exportBuild(String currentOutputBinary, String newOutputBinary) throws IOException
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
	
	private static void createTemplate(String path)
	{
		if(unzipFolder("/templates/" + path + ".zip", path))
		{
			File location = new File(path);
			System.out.println("Added the " + path + " template into " + location.getAbsolutePath());
			System.out.println("Open that folder with your favorite Java IDE (Intellij is supported)");
			System.out.println("Build using the build scripts provided by the templates");
		}
	}
	
	private static boolean unzipFolder(String localPath, String outputPath)
	{
		boolean noErrors = true;
		File parentFolder = new File(outputPath);
		try (ZipInputStream zis = new ZipInputStream(Jaxon.class.getResourceAsStream(localPath)))
		{
			ZipEntry zipEntry;
			while ((zipEntry = zis.getNextEntry()) != null)
			{
				File file = new File(outputPath, zipEntry.getName());
				
				//prevent zip-slip
				if (!file.getAbsolutePath().startsWith(parentFolder.getAbsolutePath()))
					continue;
				
				file.getParentFile().mkdirs();
				
				if (zipEntry.isDirectory())
					file.mkdirs();
				
				//you've heard of else-if, now let me show you the else-try
				else try (OutputStream fos = new FileOutputStream(file))
				{
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = zis.read(buffer)) != -1)
					{
						fos.write(buffer, 0, bytesRead);
					}
				}
			}
		}
		catch (IOException e)
		{
			noErrors = false;
			throw new RuntimeException("Failed to unzip folder", e);
		}
		
		return noErrors;
	}
	
	private static void zip(String[] args) throws IOException
	{
		String[] dirs = new String[args.length - 1];
		for (int i = 1; i < args.length; i++)
			dirs[i - 1] = args[i];
		
		for (int i = 0; i < dirs.length; i++)
		{
			String path = dirs[i++];
			String name = dirs[i];
			
			File folder = new File(path);
			
			if (!folder.isDirectory())
			{
				System.out.println("Error: " + path + " is not a directory");
				continue;
			}
			
			FileOutputStream fos = new FileOutputStream(name + ".zip");
			ZipOutputStream zos = new ZipOutputStream(fos);
			
			zipFolder(folder, "", zos);
			zos.close();
			fos.close();
			System.out.println("Folder " + path + " zipped to " + name + ".zip");
		}
	}
	
	public static void zipFolder(File folder, String parentFolder, ZipOutputStream zos) throws IOException
	{
		String parent = parentFolder.isEmpty() ? "" : parentFolder + "/";
		for (File file : folder.listFiles())
		{
			if (folder.getName().equals(".idea"))
				continue;
			
			if (file.isDirectory())
				zipFolder(file, parent + file.getName(), zos);
			else
			{
				zos.putNextEntry(new ZipEntry(parent + file.getName()));
				FileInputStream fis = new FileInputStream(file);
				
				byte[] buffer = new byte[1024];
				int len;
				while ((len = fis.read(buffer)) > 0)
				{
					zos.write(buffer, 0, len);
				}
				
				fis.close();
				zos.closeEntry();
			}
		}
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
