package kernel.windows;

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
		MAGIC.inline(x86.PUSH_BYTE, 0x00);                      //push byte 0 (no overlap)
		MAGIC.inline(x86.LEA_EAX, x86.MODRM_EAX, 0xFC);         //lea eax,[ebp-4] (address of result)
		MAGIC.inline(x86.PUSH_EAX);                             //push eax
		MAGIC.inline(x86.PUSH_BYTE, 0x01);                      //push byte 1 (single character)
		MAGIC.inline(x86.LEA_EAX, x86.MODRM_EAX, 0x08);         //lea eax,[ebp+8] (address of string)
		MAGIC.inline(x86.PUSH_EAX);                             //push eax
		MAGIC.inline(x86.CALL_NEAR, 0x35);
		MAGIC.inline32(rte.DynamicRuntime._hndStdOut);          //push handle
		MAGIC.inline(x86.CALL_NEAR, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_WriteFile);   //call
	}
}
