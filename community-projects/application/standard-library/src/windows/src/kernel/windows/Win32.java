package kernel.windows;

/**
 * @author Konloch
 * @author S. Frenz
 * @since 7/21/2024
 */
public class Win32
{
	/**
	 * Buffer needed to convert W to A
	 */
	private static byte[] buffer;
	
	/**
	 * Length of the buffer
	 */
	private static final int BUFFER_LENGTH = 128;
	
	public static String[] getCommandLineArgs()
	{
		int argc = MAGIC.rMem32(rte.DynamicRuntime._cntParam);
		int base = MAGIC.rMem32(rte.DynamicRuntime._ptrParam);
		
		//if arg count is only 1, don't read the params
		if (argc < 2)
			return new String[0];
		
		String[] args = new String[argc];
		for (int i = 0; i < argc; i++)
		{
			int addr = MAGIC.rMem32(base + (i << 2));
			StringBuilder sb = new StringBuilder();
			int c;
			
			while ((c = MAGIC.rMem16(addr)) != 0)
			{
				sb.append((char) c);
				addr += 2;
			}
			
			args[i] = sb.toString();
		}
		
		//shrink args so it skips the executable name
		String[] argsNew = new String[argc-1];
		Arrays.copy(args, 1, argsNew, 0, argsNew.length);
		return argsNew;
	}
	
	/**
	 * Method to load a given DLL
	 *
	 * @param dllName the name of the DLL
	 * @return a handle to the dll or 0 if no such DLL was found
	 */
	public static int loadLibrary(String dllName)
	{
		int handle /*-4*/;
		
		// check if buffer is valid
		if (buffer == null)
			buffer = new byte[BUFFER_LENGTH];
		
		// copy String and terminate with 0 byte
		for (handle = 0; handle < dllName.length(); handle++)
			buffer[handle] = (byte) dllName.value[handle];
		
		buffer[handle] = (byte) 0;
		
		// call LoadLibrary from kernel32.dll
		handle = MAGIC.addr(buffer[0]);
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_4);  //push dword [ebp-4]
		MAGIC.inline(x86.PUSH, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_LoadLibraryA);                                    //call LoadLibrary
		
		// save to handle
		MAGIC.inline(x86.MOVE_REGISTER_TO_REGISTER, x86.MODRM_RM | x86.REG_EBP, x86.EBP_MINUS_4);   //mov [ebp-4],eax
		
		return handle;
	}
	
	/**
	 * Method to load a function of a given DLL
	 *
	 * @param handle  a handle to the DLL whose function is to be loaded
	 * @param fctName the name of the function
	 * @return the address of the function or 0 if no such function was found
	 */
	public static int loadFunction(int handle, String fctName)
	{
		int procAddr/*-4*/;
		
		// check if handle is valid
		if (handle == 0)
			return 0;
		
		prepareBuffer(fctName);
		
		// call GetProcAddress
		procAddr = MAGIC.addr(buffer[0]);
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_4);  // push dword [ebp-4] (==addr)
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_PLUS_12);  // push dword [ebp+12] (==handle)
		MAGIC.inline(x86.PUSH, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_GetProcAddress);                                  // call getProcAddress
		
		// save result to procAddr
		MAGIC.inline(x86.MOVE_REGISTER_TO_REGISTER, x86.MODRM_RM | x86.REG_EBP,  x86.EBP_MINUS_4);  // mov [ebp-4],eax
		
		return procAddr;
	}
	
	/**
	 * Prepares the buffer for a string by copying it and adding a null terminator.
	 *
	 * @param src the string to prepare
	 */
	private static void prepareBuffer(String src)
	{
		prepareBuffer(src, 0);
	}
	
	/**
	 * Prepares the buffer for a string by copying it and adding a null terminator.
	 *
	 * @param src    the string to prepare
	 * @param srcPos the source position offset
	 */
	private static void prepareBuffer(String src, int srcPos)
	{
		if (buffer == null)
			buffer = new byte[BUFFER_LENGTH];
		
		for (int i = 0; i < src.length(); i++)
			buffer[srcPos + i] = (byte) src.charAt(i);
		
		//null terminator
		buffer[srcPos + src.length()] = 0;
	}
	
	/**
	 * Method to show a message box with the given caption and text
	 *
	 * @param title   the caption of the window
	 * @param message the text in the window
	 */
	public static void showMessageBox(String title, String message)
	{
		int handleDLL = loadLibrary("user32.dll"); /*-4*/
		int fctAddress = loadFunction(handleDLL, "MessageBoxA"); /*ebp-8*/
		int addrMessage; /*ebp-12*/
		int addrTitle; /*ebp-16*/
		
		if (fctAddress == 0)
			throw new RuntimeException("Failed to load MessageBoxA function.");
		
		if (title == null || message == null)
			throw new NullPointerException("Caption or text object is null");
		
		if (title.length() + message.length() + 2 > BUFFER_LENGTH)
			throw new BufferOverflowException("Title combined with message exceeds buffer maximum length");
		
		prepareBuffer(title);
		prepareBuffer(message, title.length() + 2);
		
		// set addresses of strings
		addrMessage = MAGIC.addr(buffer[title.length() + 2]);
		addrTitle = MAGIC.addr(buffer[0]);
		
		// call MessageBoxW
		MAGIC.inline(x86.PUSH_IMMEDIATE_BYTE, 0x00);                                                    // push byte 0 => uType==MB_OK
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_16);     // push dword [ebp-16] => address of caption
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12);     // push dword [ebp-12] => address of text
		MAGIC.inline(x86.PUSH_IMMEDIATE_BYTE, 0x00);                                                    // push byte 0 => parent==null
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);        // call function
	}
	
	public static void print(int c)
	{
		MAGIC.inline(x86.PUSH_IMMEDIATE_BYTE, 0x00);                                                // push byte 0 (no overlap)
		MAGIC.inline(x86.LOAD_EFFECTIVE_ADDRESS, x86.MODRM_RM | x86.REG_EBP, x86.EBP_MINUS_4);      // lea eax,[ebp-4] (address of result)
		MAGIC.inline(x86.PUSH_REGISTER_EAX);                                                        //push eax
		MAGIC.inline(x86.PUSH_IMMEDIATE_BYTE, 0x01);                                                // push byte 1 (single character)
		MAGIC.inline(x86.LOAD_EFFECTIVE_ADDRESS, x86.MODRM_RM | x86.REG_EBP, x86.EBP_PLUS_8);       // lea eax,[ebp+8] (address of string)
		MAGIC.inline(x86.PUSH_REGISTER_EAX);                                                        // push eax
		
		//Push handle for standard output
		MAGIC.inline(x86.PUSH, 0x35); MAGIC.inline32(rte.DynamicRuntime._hndStdOut);
		
		//Call WriteFile function
		MAGIC.inline(x86.PUSH, 0x15); MAGIC.inline32(rte.DynamicRuntime._Kernel_WriteFile);
	}
	
	public static boolean createDirectory(String path)
	{
		int kernel32Handle = loadLibrary("kernel32.dll"); /*-4*/
		int createDirectoryAddress = loadFunction(kernel32Handle, "CreateDirectoryA"); /*ebp-8*/
		int addrPath; /*ebp-12*/
		
		if (createDirectoryAddress == 0)
			throw new RuntimeException("Failed to load CreateDirectoryA function.");
		
		if (path == null)
			throw new NullPointerException("Path is null.");
		
		if (path.length() + 1 > BUFFER_LENGTH)
			throw new BufferOverflowException("eek");
		
		//prepare the buffer with the path
		prepareBuffer(path);
		
		addrPath = MAGIC.addr(buffer[0]);
		
		MAGIC.inline(x86.PUSH_IMMEDIATE_BYTE, 0x00);                                                //PUSH lpSecurityAttributes (NULL)
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12] => address of path
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);    // call function
		
		return true;
	}
	
	public static boolean deleteFile(String path)
	{
		int kernel32Handle = loadLibrary("kernel32.dll"); /*-4*/
		int createDirectoryAddress = loadFunction(kernel32Handle, "DeleteFileA"); /*ebp-8*/
		int addrPath; /*ebp-12*/
		
		if (createDirectoryAddress == 0)
			throw new RuntimeException("Failed to load CreateDirectoryA function.");
		
		if (path == null)
			throw new NullPointerException("Path is null.");
		
		if (path.length() + 1 > BUFFER_LENGTH)
			throw new BufferOverflowException("eek");
		
		//prepare the buffer with the path
		prepareBuffer(path);
		
		addrPath = MAGIC.addr(buffer[0]);
		
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12] => address of path
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);    // call function
		
		return true;
	}
	
	public static boolean deleteDirectory(String path)
	{
		int kernel32Handle = loadLibrary("kernel32.dll"); /*-4*/
		int createDirectoryAddress = loadFunction(kernel32Handle, "RemoveDirectoryA"); /*ebp-8*/
		int addrPath; /*ebp-12*/
		
		if (createDirectoryAddress == 0)
			throw new RuntimeException("Failed to load CreateDirectoryA function.");
		
		if (path == null)
			throw new NullPointerException("Path is null.");
		
		if (path.length() + 1 > BUFFER_LENGTH)
			throw new BufferOverflowException("eek");
		
		//prepare the buffer with the path
		prepareBuffer(path);
		
		addrPath = MAGIC.addr(buffer[0]);
		
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12] => address of path
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);    // call function
		
		return true;
	}
	
	public static boolean rename(String oldPath, String newPath)
	{
		int handleDLL = loadLibrary("kernel32.dll"); /*-4*/
		int fctAddress = loadFunction(handleDLL, "MoveFileA"); /*ebp-8*/
		int addrOldPath; /*ebp-12*/
		int addrNewPath; /*ebp-16*/
		
		if (fctAddress == 0)
			throw new RuntimeException("Failed to load MoveFileA function.");
		
		if (oldPath == null || newPath == null)
			throw new NullPointerException("Old or new path is null");
		
		if (oldPath.length() + newPath.length() + 2 > BUFFER_LENGTH)
			throw new BufferOverflowException("Old path combined with new path exceeds buffer maximum length");
		
		// load the params onto the buffer
		prepareBuffer(oldPath);
		prepareBuffer(newPath, oldPath.length() + 2);
		
		// set addresses of strings
		addrOldPath = MAGIC.addr(buffer[oldPath.length() + 2]);
		addrNewPath = MAGIC.addr(buffer[0]);
		
		// call MoveFileA
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12] => address of old path
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_16); // push dword [ebp-16] => address of new path
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);    // call function
		
		// save results to handleDLL
		MAGIC.inline(x86.MOVE_REGISTER_TO_REGISTER, x86.MODRM_RM | x86.REG_EBP, x86.EBP_MINUS_16);  // mov [ebp-16],eax
		
		// assuming that if EAX is 0, the function failed
		return addrNewPath != 0;
	}
	
	public static boolean setMemoryAddress(int address, int value)
	{
		int handleDLL = loadLibrary("kernel32.dll"); /*-4*/
		int fctAddress = loadFunction(handleDLL, "WriteProcessMemory"); /*ebp-8*/
		int addrAddress; /*ebp-12*/
		int addrValue; /*ebp-16*/
		int processHandle = -1; /*ebp-20*/
		int bytesWritten = 0; /*ebp-24*/
		
		if (fctAddress == 0)
			throw new RuntimeException("Failed to load WriteProcessMemory function.");
		
		if (address == 0)
			throw new NullPointerException("Address is null");
		
		// get the current process handle
		processHandle = loadFunction(handleDLL, "GetCurrentProcess");
		
		// set addresses of strings
		addrAddress = address;
		addrValue = value;
		
		// call WriteProcessMemory
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_28); // push dword [ebp-28] => bytes written
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_24); // push dword [ebp-24] => process handle
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12] => value
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_32); // push dword [ebp-32] => address
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);    // call function
		
		// save results to bytesWritten
		MAGIC.inline(x86.MOVE_REGISTER_TO_REGISTER, x86.MODRM_RM | x86.REG_EBP, x86.EBP_MINUS_16);  // mov [ebp-16],eax
		
		// assuming that if EAX is 0, the function failed
		return bytesWritten != 0;
	}
	
	public static void testSetMemoryAddress()
	{
		int handleDLL = loadLibrary("kernel32.dll");
		int handleMSVCRT = loadLibrary("msvcrt.dll");
		
		// allocate a block of memory
		int allocAddress = loadFunction(handleMSVCRT, "malloc");
		MAGIC.inline(x86.PUSH, 0x75, 0xF4); // push dword [ebp-4] => size
		MAGIC.inline(x86.PUSH, 0x55, 0xF8); // call function
		int allocatedAddress = MAGIC.addr(buffer[0]);
		
		// set a value at a specific address within the allocated block
		int setValue = 0x12345678;
		int setAddress = allocatedAddress + 4;
		setMemoryAddress(setAddress, setValue);
		
		// read the value at the same address
		int bytesRead = 0;
		int readValue = 0;
		int readAddress = setAddress;
		int processHandle = loadFunction(handleDLL, "GetCurrentProcess");
		int readFunction = loadFunction(handleDLL, "ReadProcessMemory");
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12] => bytes read
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_16); // push dword [ebp-16] => process handle
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_20); // push dword [ebp-20] => value
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_24); // push dword [ebp-24] => address
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);    // call function
		MAGIC.inline(x86.MOVE_REGISTER_TO_REGISTER, x86.MODRM_RM | x86.REG_EBP, x86.EBP_MINUS_16);  // mov [ebp-16],eax
		readValue = MAGIC.addr(buffer[0]);
		
		// compare the read value with the original value
		if (readValue == setValue)
			System.out.println("setMemoryAddress function worked correctly!");
		else
			System.out.println("setMemoryAddress function failed!");
		
		// free the allocated memory
		int freeAddress = loadFunction(handleMSVCRT, "free");
		MAGIC.inline(x86.PUSH, x86.MODRM_RM | x86.REG_OPCODE_PUSH | x86.REG_EBP, x86.EBP_MINUS_12); // push dword [ebp-12] => address
		MAGIC.inline(x86.CALL_NEAR, x86.MODRM_RM | x86.REG_CALL | x86.REG_EBP, x86.EBP_MINUS_8);    // call function
	}
}
