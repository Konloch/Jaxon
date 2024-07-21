package java.io;

/**
 * @author Konloch
 * @since 7/21/2024
 */
public class File
{
	private String path;
	
	public File(String path)
	{
		this.path = path;
	}
	
	public boolean mkdir()
	{
		return System._system.createDirectory(path);
	}
	
	public boolean delete()
	{
		return System._system.delete(path);
	}
	
	public boolean rename(String newPath)
	{
		boolean success = System._system.rename(path, newPath);
		
		if(success)
			path = newPath;
		
		return success;
	}
	
	//TODO mkdirs(), getParent(), listFiles()
}
