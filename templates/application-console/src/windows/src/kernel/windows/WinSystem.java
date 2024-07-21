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
	
	public boolean isDirectory(String path)
	{
		return false;
	}
	
	public boolean doesExist(String path)
	{
		return false;
	}
	
	public boolean delete(String path)
	{
		if(isDirectory(path))
			return Win32.deleteDirectory(path);
		else
			return Win32.deleteFile(path);
	}
	
	public boolean createDirectory(String path)
	{
		Win32.createDirectory(path);
		return false;
	}
	
	public boolean rename(String oldPath, String newPath)
	{
		return false;
	}
	
	public long getSize(String path)
	{
		return 0;
	}
	
	public String[] listDirectory(String path)
	{
		return null;
	}
	
	public OutputStream read(String path)
	{
		//TODO temporarily just read into a ByteArrayInputStream
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(128);
		
		return outputStream;
	}
	
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
