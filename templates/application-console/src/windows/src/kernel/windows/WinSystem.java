package kernel.windows;

import java.io.*;

/**
 * @author Konloch
 * @since 7/17/2024
 */
public class WinSystem extends System
{
	public WinSystem()
	{
		platform = "Windows";
	}
	
	@Override
	public String[] getCommandLineArgs()
	{
		return Win32.getCommandLineArgs();
	}
	
	@Override
	public boolean isDirectory(String path)
	{
		return false;
	}
	
	@Override
	public boolean doesExist(String path)
	{
		return false;
	}
	
	@Override
	public boolean delete(String path)
	{
		/*if(isDirectory(path))
			return Win32.deleteDirectory(path);
		else
			return Win32.deleteFile(path);*/
		
		//TODO remove when isDirectory is working
		boolean deleteDirectory = Win32.deleteDirectory(path);
		boolean deleteFile = Win32.deleteFile(path);
		return deleteDirectory || deleteFile;
	}
	
	@Override
	public boolean createDirectory(String path)
	{
		Win32.createDirectory(path);
		return false;
	}
	
	@Override
	public boolean rename(String oldPath, String newPath)
	{
		return Win32.rename(oldPath, newPath);
	}
	
	@Override
	public long getSize(String path)
	{
		return 0;
	}
	
	@Override
	public String[] listDirectory(String path)
	{
		return null;
	}
	
	@Override
	public OutputStream read(String path)
	{
		//TODO temporarily just read into a ByteArrayInputStream
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(128);
		
		return outputStream;
	}
	
	@Override
	public void write(String path, int offset, InputStream stream, boolean append) throws IOException
	{
		//Win32.write(path, offset, stream, append);
	}
	
	@Override
	public void print(int c)
	{
		Win32.print(c);
	}
}
