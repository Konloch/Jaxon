package kernel.windows;

public class Win32Lib
{
	
	/**
	 * Buffer needed to convert W to A
	 */
	private static byte[] buffer;
	
	/**
	 * Length of the buffer
	 */
	private static final int BUFFER_LENGTH = 128;
	
	/**
	 * Method to load a given DLL
	 *
	 * @param dllName the name of the DLL
	 * @return a handle to the dll or 0 if no such DLL was found
	 */
	public static int loadLibrary(byte[] dllName)
	{
		int handle = MAGIC.addr(dllName[0]);
		
		// call LoadLibrary from kernel32.dll
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, handle); //push dword [ebp-4]
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_LoadLibraryA); //call LoadLibrary
		
		// save to handle
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, handle); //mov [ebp-4],eax
		
		return handle;
	}
	
	/**
	 * Method to load a function of a given DLL
	 *
	 * @param handle  a handle to the DLL whose function is to be loaded
	 * @param fctName the name of the function
	 * @return the address of the function or 0 if no such function was found
	 */
	public static int loadFunction(int handle, byte[] fctName)
	{
		int procAddr;
		
		// check if handle is valid
		if (handle == 0)
			return 0;
		
		// call GetProcAddress
		procAddr = MAGIC.addr(fctName[0]);
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, procAddr);  // push dword [ebp-4] (==addr)
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, handle);  // push dword [ebp+12] (==handle)
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_GetProcAddress); // call getProcAddress
		
		// save result to procAddr
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, procAddr);  // mov [ebp-4],eax
		
		return procAddr;
	}
}