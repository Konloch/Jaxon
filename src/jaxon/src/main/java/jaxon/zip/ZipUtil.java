package jaxon.zip;

import jaxon.Jaxon;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class ZipUtil
{
	public static void zipCLI(String[] args) throws IOException
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
	
	public static boolean unzipFolder(String localPath, String outputPath)
	{
		boolean noErrors = true;
		File parentFolder = new File(outputPath);
		try (ZipInputStream zis = new ZipInputStream(ZipUtil.class.getResourceAsStream(localPath)))
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
				else
					try (OutputStream fos = new FileOutputStream(file))
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
}
