package kernel.linux;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Konloch
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
	
	public InputStream read(String path)
	{
		return null;
	}
	
	public void write(String path, int offset, OutputStream stream, boolean append)
	{
	
	}
	
	@Override
	public void print(int c)
	{
		MAGIC.inline(x86.IMMEDIATE_DWORD, 0x04, 0x00, 0x00, 0x00);      //mov eax,4 (print string)
		MAGIC.inline(x86.MOVE_IMMEDIATE_TO_REGISTER, 0x01, 0x00, 0x00, 0x00);                     //mov ebx,1 (handle for std-out)
		MAGIC.inline(x86.LOAD_EFFECTIVE_ADDRESS, 0x4D, 0x08);                          //lea ecx,[ebp+8] (address of string)
		MAGIC.inline(x86.MOVE_REGISTER_TO_REGISTER, 0xDA);                            //mov edx,ebx (length of string: 1)
		MAGIC.inline(x86.SYSCALL, 0x80);                                //call kernel
		//sync for debugging
		//MAGIC.inline(x86.IMMEDIATE_DWORD, 0x94, 0x00, 0x00, 0x00);                   //mov eax,148 (sync file)
		//MAGIC.inline(x86.MOV_EBX_IMMEDIATE, 0x01, 0x00, 0x00, 0x00);                   //mov ebx,1 (handle for std-out)
		//MAGIC.inline(x86.SYSCALL, 0x80);                                     //call kernel
	}
}