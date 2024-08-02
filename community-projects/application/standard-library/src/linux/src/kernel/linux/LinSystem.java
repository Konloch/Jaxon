package kernel.linux;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Konloch
 * @since 7/17/2024
 */
public class LinSystem extends System
{
	public LinSystem()
	{
		super("Linux");
	}
	
	@Override
	public String[] getCommandLineArgs()
	{
		int argc = 0;
		int argv = 0;
		
		// Get the address of the argv array
		MAGIC.inline(x86.MOVE_MEMORY_TO_REGISTER, x86.MODRM_RM | x86.REG_EBP, x86.EBP_PLUS_8);      // mov eax, [ebp+8]
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12 => address of path
		
		// Get the value of argc
		MAGIC.inline(x86.MOVE_MEMORY_TO_REGISTER, x86.MODRM_RM | x86.REG_EBP, x86.EBP_PLUS_12);     // mov eax, [ebp+12]
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12 => address of path
		
		String[] args = new String[argc];
		
		for (int i = 0; i < argc; i++)
		{
			int addr = MAGIC.rMem32(argv + i * 4);
			StringBuilder sb = new StringBuilder();
			int c;
			
			while ((c = MAGIC.rMem8(addr)) != 0)
			{
				sb.append((char) c);
				addr++;
			}
			
			args[i] = sb.toString();
		}
		
		return args;
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
		return false;
	}
	
	@Override
	public boolean createDirectory(String path)
	{
		return false;
	}
	
	@Override
	public boolean rename(String oldPath, String newPath)
	{
		return false;
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
		return null;
	}
	
	@Override
	public void write(String path, int offset, InputStream stream, boolean append)
	{
	
	}
	
	@Override
	public void print(int c)
	{
		MAGIC.inline(x86.IMMEDIATE_DWORD, 0x04, 0x00, 0x00, 0x00);                  //mov eax,4 (print string)
		MAGIC.inline(x86.MOVE_IMMEDIATE_TO_REGISTER, 0x01, 0x00, 0x00, 0x00);       //mov ebx,1 (handle for std-out)
		MAGIC.inline(x86.LOAD_EFFECTIVE_ADDRESS, 0x4D, 0x08);                       //lea ecx,[ebp+8] (address of string)
		MAGIC.inline(x86.MOVE_REGISTER_TO_REGISTER, 0xDA);                          //mov edx,ebx (length of string: 1)
		MAGIC.inline(x86.SYSCALL, 0x80);                                            //call kernel
		//sync for debugging
		//MAGIC.inline(x86.IMMEDIATE_DWORD, 0x94, 0x00, 0x00, 0x00);                //mov eax,148 (sync file)
		//MAGIC.inline(x86.MOV_EBX_IMMEDIATE, 0x01, 0x00, 0x00, 0x00);              //mov ebx,1 (handle for std-out)
		//MAGIC.inline(x86.SYSCALL, 0x80);                                          //call kernel
	}
}