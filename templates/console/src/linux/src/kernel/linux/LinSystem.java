package kernel.linux;

/**
 * @author S. Frenz
 */
public class LinSystem extends System
{
	public LinSystem()
	{
		platform = "Linux";
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
		return false;
	}
	
	public boolean createDirectory(String path)
	{
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
	
	public byte[] read(String path)
	{
		return new byte[0];
	}
	
	public void write(String path, int offset, byte[] bytes, int length, boolean append)
	{
	
	}
	
	@Override
	public void print(int c)
	{
		MAGIC.inline(0xB8, 0x04, 0x00, 0x00, 0x00); //mov eax,4 (print string)
		MAGIC.inline(0xBB, 0x01, 0x00, 0x00, 0x00); //mov ebx,1 (handle for std-out)
		MAGIC.inline(0x8D, 0x4D, 0x08);             //lea ecx,[ebp+8] (address of string)
		MAGIC.inline(0x89, 0xDA);                   //mov edx,ebx (length of string: 1)
		MAGIC.inline(0xCD, 0x80);                   //call kernel
		//sync for debugging
		//MAGIC.inline(0xB8, 0x94, 0x00, 0x00, 0x00); //mov eax,148 (sync file)
		//MAGIC.inline(0xBB, 0x01, 0x00, 0x00, 0x00); //mov ebx,1 (handle for std-out)
		//MAGIC.inline(0xCD, 0x80);                   //call kernel
	}
}