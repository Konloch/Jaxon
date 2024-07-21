package kernel.windows;

import java.io.*;

/**
 * @author Konloch
 * @author S. Frenz
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
		return false;
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
		MAGIC.inline(x86.PUSH_IMMEDIATE_BYTE, 0x00);                            //push byte 0 (no overlap)
		MAGIC.inline(x86.LOAD_EFFECTIVE_ADDRESS, x86.MODRM_REGISTER, 0xFC);     //lea eax,[ebp-4] (address of result)
		MAGIC.inline(x86.PUSH_REGISTER_EAX);                                    //push eax
		MAGIC.inline(x86.PUSH_IMMEDIATE_BYTE, 0x01);                            //push byte 1 (single character)
		MAGIC.inline(x86.LOAD_EFFECTIVE_ADDRESS, x86.MODRM_REGISTER, 0x08);     //lea eax,[ebp+8] (address of string)
		MAGIC.inline(x86.PUSH_REGISTER_EAX);                                    //push eax
		MAGIC.inline(x86.CALL_NEAR, 0x35);
		MAGIC.inline32(rte.DynamicRuntime._hndStdOut);                          //push handle
		MAGIC.inline(x86.CALL_NEAR, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_WriteFile);                   //call
	}
}
