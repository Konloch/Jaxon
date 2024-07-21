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
	
	
	// Constants for file access, creation disposition, and attributes
	private static final int GENERIC_WRITE = 0x40000000;
	private static final int OPEN_EXISTING = 3;
	private static final int CREATE_ALWAYS = 2;
	private static final int FILE_ATTRIBUTE_NORMAL = 0x80;
	private static final int INVALID_HANDLE_VALUE = -1;
	
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
		MAGIC.inline(0xFF, 0x75, 0xFC);                        //push dword [ebp-4]
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_LoadLibraryA); //call LoadLibrary
		
		// save to handle
		MAGIC.inline(0x89, 0x45, 0xFC); //mov [ebp-4],eax
		
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
		MAGIC.inline(0xFF, 0x75, 0xFC);  // push dword [ebp-4] (==addr)
		MAGIC.inline(0xFF, 0x75, 0x0C);  // push dword [ebp+12] (==handle)
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(rte.DynamicRuntime._Kernel_GetProcAddress); // call getProcAddress
		
		// save result to procAddr
		MAGIC.inline(0x89, 0x45, 0xFC);  // mov [ebp-4],eax
		
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
	 * @param src the string to prepare
	 * @param srcPos the source position offset
	 */
	private static void prepareBuffer(String src, int srcPos)
	{
		if (buffer == null)
			buffer = new byte[BUFFER_LENGTH];
		
		for (int i = 0; i < src.length(); i++)
			buffer[srcPos + i] = (byte) src.charAt(i);
		
		//null terminator
		buffer[src.length()] = 0;
	}
	
	/**
	 * Method to show a message box with the given caption and text
	 *
	 * @param title the caption of the window
	 * @param message the text in the window
	 */
	public static void showMessageBox(String title, String message)
	{
		int handleDLL = loadLibrary("user32.dll"); /*-4*/
		int fctAddress = loadFunction(handleDLL, "MessageBoxA"); /*ebp-8*/
		int addrMessage; /*ebp-12*/
		int addrTitle; /*ebp-16*/
		
		if(fctAddress == 0)
			return;
		
		if (title == null || message == null)
			throw new NullPointerException("Caption or text object is null");
			
		if (title.length() + message.length() + 2 > BUFFER_LENGTH)
			throw new BufferOverflowException("Title combined with message is greater than buffer maximum length");
		
		prepareBuffer(title);
		prepareBuffer(message, title.length() + 2);
		
		// set addresses of strings
		addrMessage = MAGIC.addr(buffer[title.length() + 2]);
		addrTitle = MAGIC.addr(buffer[0]);
		
		// call MessageBoxW
		MAGIC.inline(0x6A, 0x00);  // push byte 0 => uType==MB_OK
		MAGIC.inline(0xFF, 0x75, 0xF0); // push dword [ebp-16 => address of caption
		MAGIC.inline(0xFF, 0x75, 0xF4); // push dword [ebp-12] => address of text
		MAGIC.inline(0x6A, 0x00);  // push byte 0 => parent==null
		MAGIC.inline(0xFF, 0x55, 0xF8); // call function
	}
}
