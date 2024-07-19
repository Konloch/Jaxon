package kernel.windows;

import app.CalcTAinfo;
import app.RayTrace;
import app.Scene;

/**
 * @author S. Frenz
 */
public class Window
{
	public final static int XRES = 800, YRES = 600;
	
	/**
	 * Constants for messages
	 */
	private final static int WM_DESTROY = 2;
	private final static int WM_PAINT = 15;
	private final static int WM_CLOSE = 16;
	private final static int WM_QUIT = 18;
	
	/**
	 * Constants for getSystemMetrics
	 */
	private final static int SM_CXSIZEFRAME = 32;
	private final static int SM_CYSIZEFRAME = 33;
	private final static int SM_CYCAPTION = 4;
	
	/**
	 * Window class name
	 */
	private final static byte[] clsName = MAGIC.toByteArray("RayWND", true);
	
	/**
	 * Caption of the window
	 */
	private final static byte[] caption = MAGIC.toByteArray("WinRay", true);
	
	/**
	 * Placeholder for the WNDCLASSEX struct
	 */
	public static int[] wndCls;
	
	/**
	 * Address of the placeholder for the MSG struct
	 */
	private static int msgAddr;
	
	/**
	 * Handle to this program
	 */
	private static int moduleHandle;
	
	/**
	 * Handle to the created window
	 */
	private static int windowHandle;
	
	/**
	 * Flag for the event loop, set by procWind
	 */
	private static boolean goOn;
	
	/**
	 * Array containing the image information
	 */
	private static int[][] image;
	
	/**
	 * remember how many lines are calculated so far
	 */
	private static int linesDone;
	
	/**
	 * buffer for paintstruct
	 */
	private static PAINTSTRUCT ps;
	
	/**
	 * handle to bitmap and pointer to pixels of it
	 */
	private static int bitmapHandle;
	private static int bitmapBytes;
	
	/**
	 * Method initializing the functions needed for this program
	 */
	private static void initFunctions()
	{
		int handleDLL = Win32Lib.loadLibrary(MAGIC.toByteArray("kernel32.dll", true)); /*-4*/
		getModuleHandle = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("GetModuleHandleA", true));
		getLastError = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("GetLastError", true));
		setLastError = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("SetLastError", true));
		
		handleDLL = Win32Lib.loadLibrary(MAGIC.toByteArray("user32.dll", true));
		createWindow = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("CreateWindowExA", true));
		registerClass = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("RegisterClassExA", true));
		defWinProc = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("DefWindowProcA", true));
		loadCursor = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("LoadCursorA", true));
		loadIcon = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("LoadIconA", true));
		peekMessage = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("PeekMessageA", true));
		getMessage = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("GetMessageA", true));
		dispatchMessage = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("DispatchMessageA", true));
		translateMessage = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("TranslateMessage", true));
		beginPaint = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("BeginPaint", true));
		endPaint = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("EndPaint", true));
		getSystemMetrics = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("GetSystemMetrics", true));
		invalidateRect = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("InvalidateRect", true));
		updateWindow = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("UpdateWindow", true));
		getDC = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("GetDC", true));
		releaseDC = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("ReleaseDC", true));
		destroyWindow = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("DestroyWindow", true));
		postQuitMessage = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("PostQuitMessage", true));
		
		handleDLL = Win32Lib.loadLibrary(MAGIC.toByteArray("gdi32.dll", true));
		createCompatibleDC = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("CreateCompatibleDC", true));
		selectObject = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("SelectObject", true));
		bitBlt = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("BitBlt", true));
		deleteDC = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("DeleteDC", true));
		createDIBSection = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("CreateDIBSection", true));
		deleteObject = Win32Lib.loadFunction(handleDLL, MAGIC.toByteArray("DeleteObject", true));
		
		// set module handle
		moduleHandle = getModuleHandle(0);
	}
	
	private static void initPaint(int width, int height)
	{
		int hdc = getDC(windowHandle);
		BITMAPINFO bmp = null;
		
		bmp = (BITMAPINFO) MAGIC.cast2Struct(MAGIC.addr((new byte[40])[0])); //no space for palette required
		bmp.info.biSize = 40;
		bmp.info.biWidth = width;
		bmp.info.biHeight = height;
		bmp.info.biCompression = 0; //==BI_RGB
		bmp.info.biBitCount = (short) 32;
		bmp.info.biPlanes = (short) 1;
		bmp.info.biSizeImage = 0;
		bmp.info.biClrImportant = 0;
		bmp.info.biClrUsed = 0;
		bmp.info.biXPelsPerMeter = 0;
		bmp.info.biYPelsPerMeter = 0;
		bitmapHandle = createDIBSection(hdc, bmp, 0, MAGIC.addr(bitmapBytes), 0, 0);
		releaseDC(windowHandle, hdc);
	}
	
	/**
	 * Method dealing with events sent to the window (called by windows and therefore inverted parameter order)
	 *
	 * @param lpar
	 * @param wpar
	 * @param msgID
	 * @param hwnd
	 * @return
	 */
	private static int procWind(int lpar, int wpar, int msgID, int hwnd)
	{
		int hdc, hdcMem, xLeft, xRight, yTop, yBottom, oldBitmap;
		
		switch (msgID)
		{
			case WM_DESTROY:
			case WM_CLOSE:
				goOn = false;
				destroyWindow(windowHandle);
				postQuitMessage(0);
				break;
			case WM_PAINT:
				hdc = beginPaint(windowHandle, ps); //fills PAINTSTRUCT ps with update region
				xLeft = ps.rcPaint.left;
				if (XRES < (xRight = ps.rcPaint.right))
					xRight = XRES;
				yTop = ps.rcPaint.top;
				if (linesDone < (yBottom = ps.rcPaint.bottom))
					yBottom = linesDone;
				hdcMem = createCompatibleDC(hdc);
				oldBitmap = selectObject(hdcMem, bitmapHandle);
				bitBlt(hdc, xLeft, yTop, xRight - xLeft, yBottom - yTop, hdcMem, xLeft, yTop, 0xCC0020);
				selectObject(hdcMem, oldBitmap);
				deleteDC(hdcMem);
				endPaint(windowHandle, ps);
				break;
			default:
				return defWinProc(hwnd, msgID, wpar, lpar);
		}
		return 0;
	}
	
	/**
	 * Method initializing the WNDCLASSEX for this window
	 */
	private static void initWndCls()
	{
		// create wndCls
		wndCls = new int[12];
		// first entry=size
		wndCls[0] = wndCls.length * 4;
		// second entry=style
		wndCls[1] = 2 + 1;
		// third entry wnd proc => set to procWind
		wndCls[2] = MAGIC.rMem32(MAGIC.cast2Ref(MAGIC.clssDesc("Window")) + MAGIC.mthdOff("Window", "procWind")) + MAGIC.getCodeOff();
		// fourth entry 2x 0
		wndCls[3] = 0;
		wndCls[4] = 0;
		// sixth entry handle to module
		wndCls[5] = moduleHandle;
		// seventh entry icon
		wndCls[6] = loadIcon(0, 32516);
		// eighth entry cursor
		wndCls[7] = loadCursor(0, 32512);
		// nineth entry: background 16=COLOR_BTNFACE+1
		wndCls[8] = 16;
		// tenth entry: menu name => null
		wndCls[9] = 0;
		// eleventh entry: wndcls name
		wndCls[10] = MAGIC.addr(clsName[0]);
		// twelveth entry: hIconSm==null
		wndCls[11] = 0;
		// register it
		registerClass(MAGIC.addr(wndCls[0]));
	}
	
	/**
	 * Method to get module handle from windows
	 *
	 * @return handle to requested module
	 */
	@SJC.WinDLL
	private native static int getModuleHandle(int owner);
	
	private static int getModuleHandle;
	
	/**
	 * Method to create DIB section
	 *
	 * @return handle to created section
	 */
	@SJC.WinDLL
	private native static int createDIBSection(int hdc, BITMAPINFO info, int usage, int pBitmapBytes, int hSection, int offset);
	
	private static int createDIBSection;
	
	/**
	 * Default windows message handling method
	 */
	@SJC.WinDLL
	private native static int defWinProc(int hwnd, int msgID, int wpar, int lpar);
	
	private static int defWinProc;
	
	/**
	 * Method retrieving the last error
	 *
	 * @return the last error code
	 */
	@SJC.WinDLL
	private native static int getLastError();
	
	private static int getLastError;
	
	@SJC.WinDLL
	private native static void setLastError(int no);
	
	private static int setLastError;
	
	/**
	 * Method to obtain a handle to the default cursor
	 *
	 * @return a handle to the default cursor or null
	 */
	@SJC.WinDLL
	private native static int loadCursor(int cursorName, int stdCursor);
	
	private static int loadCursor;
	
	/**
	 * Method loading the icon for this program
	 *
	 * @return handle to icon
	 */
	@SJC.WinDLL
	private native static int loadIcon(int iconName, int stdIcon);
	
	private static int loadIcon;
	
	/**
	 * Method to register a class
	 */
	@SJC.WinDLL
	private native static void registerClass(int pWndCls);
	
	private static int registerClass;
	
	/**
	 * Method to obtain a message passed to this window
	 *
	 * @return
	 */
	@SJC.WinDLL
	private native static int getMessage(int pMsg, int hwnd, int wMsgFilterMin, int wMsgFilterMax);
	
	private static int getMessage;
	
	@SJC.WinDLL
	private native static int peekMessage(int pMsg, int hwnd, int wMsgFilterMin, int wMsgFilterMax, boolean remove);
	
	private static int peekMessage;
	
	/**
	 * Method dispatching and translating messages passed to this process
	 *
	 * @param pMsg true for dispatching false for translating
	 */
	@SJC.WinDLL
	private native static void translateMessage(int pMsg);
	
	private static int translateMessage;
	
	@SJC.WinDLL
	private native static void dispatchMessage(int pMsg);
	
	private static int dispatchMessage;
	
	/**
	 * Method to obtain system metrics
	 *
	 * @param par parameter to deliver
	 * @return requested parameter
	 */
	@SJC.WinDLL
	private native static int getSystemMetrics(int par);
	
	private static int getSystemMetrics;
	
	/**
	 * Method creating the main window
	 */
	private static int createWindow;
	
	private static int createWindow(int width, int height)
	{
		int result = 0; /*-4*/
		int addrWNDCLS = MAGIC.addr(clsName[0]); /*-8*/
		int addrCaption = MAGIC.addr(caption[0]); /*-12*/
		
		MAGIC.inline(0x6A, 0x00); //push byte 0  lparam==null => no additional data
		MAGIC.inline(0xFF, 0x35);
		MAGIC.inlineOffset(4, moduleHandle); //push dword [ebp-4] => instance of module
		MAGIC.inline(0x6A, 0x00); // push byte 0  hMenu==null
		MAGIC.inline(0x6A, 0x00); // push byte 0  hWndParent==null
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, height); //push dword [ebp+8] => height
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, width); //push dword [ebp+12] => width
		MAGIC.inline(0x6A, 0x00); // push byte 0  y
		MAGIC.inline(0x6A, 0x00); // push byte 0  x
		MAGIC.inline(0x68, 0x00, 0x00, 0x8A, 0x10); // push dword 0x108A0000
		// WS_BORDER, WS_VISIBLE,
		// WS_SYSTEM, WS_MINIMIZEBOX
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, addrCaption); // push dword [ebp-12] captionString
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, addrWNDCLS); // push dword [ebp-8] WNDCLASSEX
		MAGIC.inline(0x6A, 0x00); // push byte 0
		
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inlineOffset(4, createWindow);
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, result); // mov [ebp-4],eax
		return result;
	}
	
	/**
	 * Get drawing context
	 */
	@SJC.WinDLL
	private native static int getDC(int hWindow);
	
	private static int getDC;
	
	/**
	 * Method initializing a paint action
	 *
	 * @param handleWindow a handle to the window
	 * @param ps           a PAINTSTRUCT obtaining the information needed
	 * @return a handle to the device context
	 */
	@SJC.WinDLL
	private native static int beginPaint(int handleWindow, PAINTSTRUCT ps);
	
	private static int beginPaint;
	
	/**
	 * Method ending a paint action
	 *
	 * @param hWnd a handle to the window
	 * @param ps   a PAINTSTRUCT obtaining the information needed
	 */
	@SJC.WinDLL
	private native static void endPaint(int hWnd, PAINTSTRUCT ps);
	
	private static int endPaint;
	
	/**
	 * Method creating a memory device context compatible with the specified
	 * device context
	 *
	 * @param hdc a handle to the desired device context
	 * @return a memory device context
	 */
	@SJC.WinDLL
	private native static int createCompatibleDC(int hdc);
	
	private static int createCompatibleDC;
	
	/**
	 * Method selecting an object into the specified device context
	 *
	 * @param hdcMem  handle to the device context
	 * @param hBitmap handle to the object to be selected
	 * @return int containing the result
	 */
	@SJC.WinDLL
	private native static int selectObject(int hdcMem, int hBitmap);
	
	private static int selectObject;
	
	/**
	 * Bitblocktransfer
	 */
	@SJC.WinDLL
	private native static int bitBlt(int srcHdc, int xDest, int yDest, int width, int height, int dstHdc, int xSrc, int ySrc, int rop);
	
	private static int bitBlt;
	
	/**
	 * Release drawing context
	 */
	@SJC.WinDLL
	private native static int releaseDC(int hwnd, int hdc);
	
	private static int releaseDC;
	
	/**
	 * Delete drawing context
	 */
	@SJC.WinDLL
	private native static int deleteDC(int hdc);
	
	private static int deleteDC;
	
	/**
	 * Add rectangle to invalidated region
	 */
	@SJC.WinDLL
	private native static void invalidateRect(int hWnd, RECT rect, boolean bErase);
	
	private static int invalidateRect;
	
	/**
	 * Send WM_PAINT message to update window
	 */
	@SJC.WinDLL
	private native static void updateWindow(int hWnd);
	
	private static int updateWindow;
	
	/**
	 * Send WM_QUIT message to the application
	 */
	@SJC.WinDLL
	private native static void postQuitMessage(int result);
	
	private static int postQuitMessage;
	
	/**
	 * Destroys the window
	 */
	@SJC.WinDLL
	private native static void destroyWindow(int hWnd);
	
	private static int destroyWindow;
	
	/**
	 * Delete created object
	 */
	@SJC.WinDLL
	private native static void deleteObject(int bHnd);
	
	private static int deleteObject;
	
	/**
	 * Method starting the raytracer
	 */
	public static void run()
	{
		//-- fields for image processing
		RayTrace rt;
		CalcTAinfo cTAi;
		RECT lineRect = null;
		boolean wait;
		int[] line;
		
		msgAddr = MAGIC.addr((new byte[44])[0]);
		ps = (PAINTSTRUCT) MAGIC.cast2Struct(MAGIC.addr((new byte[64])[0]));
		lineRect = (RECT) MAGIC.cast2Struct(MAGIC.addr((new byte[16])[0]));
		//---
		// init all needed functions
		initFunctions();
		// reset errors
		getLastError();
		setLastError(0);
		// init WNDCLASSEX
		initWndCls();
		
		windowHandle = createWindow(getSystemMetrics(SM_CXSIZEFRAME) + XRES, getSystemMetrics(SM_CYSIZEFRAME) + getSystemMetrics(SM_CYCAPTION) + YRES);
		if (windowHandle == 0)
			return;
		initPaint(XRES, YRES);
		
		image = new int[YRES][XRES];
		rt = new RayTrace(new Scene());
		rt.init(XRES, YRES);
		cTAi = new CalcTAinfo();
		
		lineRect.left = 0;
		lineRect.right = XRES;
		goOn = true;
		wait = false; //do not wait for messages until calculation is done
		while (goOn)
		{ //keep running until procWind tells us to leave
			if (linesDone < YRES)
			{ //something left to calculate
				rt.renderLine(line = image[lineRect.top = cTAi.y = linesDone++], cTAi);
				memCpy32(MAGIC.addr(line[0]), XRES * YRES * 4 - (lineRect.bottom = linesDone) * (XRES * 4) + bitmapBytes, XRES);
				invalidateRect(windowHandle, lineRect, false);
				updateWindow(windowHandle);
			}
			else
				wait = true; //nothing more to calculate, wait for messages now
			
			while (goOn && (wait ? getMessage(msgAddr, windowHandle, 0, 0)
					
					: peekMessage(msgAddr, windowHandle, 0, 0, true)) != 0)
			{
				
				translateMessage(msgAddr);
				
				dispatchMessage(msgAddr);
				
			}
		}
		deleteObject(bitmapHandle);
	}
	
	private final static void memCpy32(int src, int dst, int dwordCnt)
	{
		MAGIC.inline(0x56);             //push esi
		MAGIC.inline(0x57);             //push edi
		MAGIC.inline(0xFC);             //cld
		MAGIC.inline(0x8B, 0x75);
		MAGIC.inlineOffset(1, src); //mov esi,[ebp+16]
		MAGIC.inline(0x8B, 0x7D);
		MAGIC.inlineOffset(1, dst); //mov edi,[ebp+12]
		MAGIC.inline(0x8B, 0x4D);
		MAGIC.inlineOffset(1, dwordCnt); //mov ecx,[ebp+8]
		MAGIC.inline(0xF3, 0xA5);       //rep movsd
		MAGIC.inline(0x5F);             //pop edi
		MAGIC.inline(0x5E);             //pop esi
	}
}