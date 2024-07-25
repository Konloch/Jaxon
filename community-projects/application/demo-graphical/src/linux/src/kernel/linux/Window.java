package kernel.linux;

import app.WindowDraw;
import app.DrawBitmap;

/**
 * @author S. Frenz
 */
public class Window
{
	public final static int XRES = 800, YRES = 600;
	
	public final static int RTLD_NOW = 0x00002;
	public final static int RTLD_GLOBAL = 0x00100;
	
	public final static int ZPixmap = 2;
	
	public final static int ButtonPressMask = 0x00000004;
	public final static int ButtonPress = 4;
	public final static int ExposureMask = 0x00008000;
	public final static int Expose = 12;
	
	public final static int XA_STRING = 31;
	
	public static int XOpenDisplay;
	public static int XDisplayWidth, XDisplayHeight, XDisplayDepth;
	public static int XDefaultScreen, XDefaultRootWindow;
	public static int XCreateSimpleWindow, XMapWindow, XSetWMName, XCloseDisplay;
	public static int XDefaultGC;
	public static int XFlush;
	public static int XSelectInput, XNextEvent, XCheckWindowEvent;
	public static int XSetForeground, XDrawLine;
	public static int XDefaultVisual, XCreateImage, XPutImage;
	
	/**
	 * Method initializing the functions needed for this program
	 */
	private static void initFunctions()
	{
		if (MAGIC.rMem32(MAGIC.imageBase - 12) == 0 || MAGIC.rMem32(MAGIC.imageBase - 8) == 0)
		{
			System.println("no library function loaded");
			rte.DynamicRuntime.exit(-1);
		}
		int lib = dlopen(MAGIC.toByteArray("libX11.so.6", true), RTLD_NOW | RTLD_GLOBAL);
		if ((XOpenDisplay = dlsym(lib, MAGIC.toByteArray("XOpenDisplay", true))) == 0 || (XDisplayWidth = dlsym(lib, MAGIC.toByteArray("XDisplayWidth", true))) == 0 || (XDisplayHeight = dlsym(lib, MAGIC.toByteArray("XDisplayHeight", true))) == 0 || (XDisplayDepth = dlsym(lib, MAGIC.toByteArray("XDefaultDepth", true))) == 0 || (XDefaultScreen = dlsym(lib, MAGIC.toByteArray("XDefaultScreen", true))) == 0 || (XDefaultRootWindow = dlsym(lib, MAGIC.toByteArray("XDefaultRootWindow", true))) == 0 || (XCreateSimpleWindow = dlsym(lib, MAGIC.toByteArray("XCreateSimpleWindow", true))) == 0 || (XMapWindow = dlsym(lib, MAGIC.toByteArray("XMapWindow", true))) == 0 || (XSetWMName = dlsym(lib, MAGIC.toByteArray("XSetWMName", true))) == 0 || (XCloseDisplay = dlsym(lib, MAGIC.toByteArray("XCloseDisplay", true))) == 0 || (XDefaultGC = dlsym(lib, MAGIC.toByteArray("XDefaultGC", true))) == 0 || (XFlush = dlsym(lib, MAGIC.toByteArray("XFlush", true))) == 0 || (XSelectInput = dlsym(lib, MAGIC.toByteArray("XSelectInput", true))) == 0 || (XNextEvent = dlsym(lib, MAGIC.toByteArray("XNextEvent", true))) == 0 || (XCheckWindowEvent = dlsym(lib, MAGIC.toByteArray("XCheckWindowEvent", true))) == 0 || (XSetForeground = dlsym(lib, MAGIC.toByteArray("XSetForeground", true))) == 0 || (XDrawLine = dlsym(lib, MAGIC.toByteArray("XDrawLine", true))) == 0 || (XDefaultVisual = dlsym(lib, MAGIC.toByteArray("XDefaultVisual", true))) == 0 || (XCreateImage = dlsym(lib, MAGIC.toByteArray("XCreateImage", true))) == 0 || (XPutImage = dlsym(lib, MAGIC.toByteArray("XPutImage", true))) == 0)
		{
			System.println("function resolving failed");
			rte.DynamicRuntime.exit(-1);
		}
	}
	
	public static int dlopen(byte[] libNameZ, int flags)
	{
		int libAddr = MAGIC.addr(libNameZ[0]);
		
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, flags);
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, libAddr);
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(MAGIC.imageBase - 12);
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, libAddr);
		MAGIC.inline(0x5B, 0x5B);
		return libAddr;
	}
	
	public static int dlsym(int libHandle, byte[] funcNameZ)
	{
		int res = dlsymReal(libHandle, funcNameZ);
		if (res != 0)
			return res;
		System.printBZ(funcNameZ);
		System.println(" missing");
		return 0;
	}
	
	public static int dlsymReal(int libHandle, byte[] funcNameZ)
	{
		int funcAddr = MAGIC.addr(funcNameZ[0]);
		
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, funcAddr);
		MAGIC.inline(0xFF, 0x75);
		MAGIC.inlineOffset(1, libHandle);
		MAGIC.inline(0xFF, 0x15);
		MAGIC.inline32(MAGIC.imageBase - 8);
		MAGIC.inline(0x89, 0x45);
		MAGIC.inlineOffset(1, funcAddr);
		MAGIC.inline(0x5B, 0x5B);
		return funcAddr;
	}
	
	public native static int XOpenDisplay(int no);
	
	public native static int XDisplayWidth(int display, int screen);
	
	public native static int XDisplayHeight(int display, int screen);
	
	public native static int XDisplayDepth(int display, int screen);
	
	public native static int XDefaultScreen(int display);
	
	public native static int XDefaultRootWindow(int display);
	
	public native static int XCreateSimpleWindow(int display, int root, int x, int y, int width, int height, int unknown, int color1, int color2);
	
	public native static void XMapWindow(int display, int window);
	
	public native static void XSetWMName(int display, int window, XTextProperty text_prop);
	
	public native static void XCloseDisplay(int display);
	
	public native static int XDefaultGC(int display, int unknown);
	
	public native static void XFlush(int display);
	
	public native static void XSelectInput(int display, int window, int mask);
	
	public native static void XNextEvent(int display, XEvent event);
	
	public native static int XCheckWindowEvent(int display, int window, int mask, XEvent event);
	
	public native static void XSetForeground(int display, int gc, int color);
	
	public native static void XDrawLine(int display, int window, int gc, int x1, int y1, int x2, int y2);
	
	public native static int XDefaultVisual(int display, int screen);
	
	public native static int XCreateImage(int display, int visual, int depth, int format, int offset, int data, int width, int height, int pad, int bytes_per_line);
	
	public native static void XPutImage(int display, int window, int gc, int image, int srcX, int srcY, int dstX, int dstY, int width, int height);
	
	/**
	 * Method starting the window
	 */
	public static void run()
	{
		//-- fields for image processing
		DrawBitmap rt;
		WindowDraw cTAi;
		
		//---
		// init all needed functions
		initFunctions();
		
		int display = XOpenDisplay(0);
		if (display == 0)
		{
			System.println("could not open default display");
			return;
		}
		int colorDepth = XDisplayDepth(display, XDefaultScreen(display));
		if (colorDepth != 24 && colorDepth != 32)
			System.println("current demo requires 24 or 32 bit color depth on screen");
		
		int mainWindow = XCreateSimpleWindow(display, XDefaultRootWindow(display), 0, 0, XRES, YRES, 0, 0, 0);
		if (mainWindow == 0)
		{
			System.println("could not create main window");
			return;
		}
		int mainGC = XDefaultGC(display, 0);
		XMapWindow(display, mainWindow);
		XTextProperty windowName;
		windowName = (XTextProperty) MAGIC.cast2Struct(MAGIC.addr((new int[4])[0]));
		windowName.stringAddr = MAGIC.addr(MAGIC.toByteArray("Graphical Demo - Linux", true)[0]);
		windowName.encoding = XA_STRING;
		windowName.format = 8;
		windowName.nitems = 6;
		XSetWMName(display, mainWindow, windowName);
		XFlush(display);
		
		int[][] image = new int[YRES][XRES];
		rt = new DrawBitmap(XRES, YRES);
		cTAi = new WindowDraw();
		
		XSelectInput(display, mainWindow, ButtonPressMask | ExposureMask);
		
		int ximage = 0;
		int[] ximageData = new int[YRES * XRES];
		int bitmapBytes = MAGIC.addr(ximageData[0]);
		XEvent event;
		XButtonEvent buttonEvent;
		event = (XEvent) MAGIC.cast2Struct(MAGIC.addr((new byte[96])[0]));
		buttonEvent = (XButtonEvent) MAGIC.cast2Struct(MAGIC.cast2Ref(event));
		if ((ximage = XCreateImage(display, XDefaultVisual(display, XDefaultScreen(display)), colorDepth, ZPixmap, 0, bitmapBytes, XRES, YRES, 8, XRES * 4)) == 0)
		{
			System.println("could not create image");
			return;
		}
		
		int line[];
		int linesDone = 0;
		boolean goOn = true;
		boolean wait = false; //do not wait for messages until calculation is done
		while (goOn)
		{ //keep running until procWind tells us to leave
			if (linesDone < YRES)
			{ //something left to calculate
				rt.renderLine(line = image[cTAi.y = linesDone], cTAi);
				memCpy32(MAGIC.addr(line[0]), linesDone * (XRES * 4) + bitmapBytes, XRES);
				XPutImage(display, mainWindow, mainGC, ximage, 0, linesDone, 0, linesDone++, XRES, 1);
				XFlush(display);
			}
			else
				wait = true; //nothing more to calculate, wait for messages now
			if (wait || XCheckWindowEvent(display, mainWindow, -1, event) != 0)
			{ //no wait, check if event existing
				if (wait)
					XNextEvent(display, event); //get event if we have to wait for it
				switch (event.type)
				{
					case Expose:
						XPutImage(display, mainWindow, mainGC, ximage, 0, 0, 0, 0, XRES, linesDone);
						XFlush(display);
						break;
					case ButtonPress:
						if (buttonEvent.window == mainWindow)
						{
							XCloseDisplay(display);
							goOn = false;
						}
						break;
				}
			}
		}
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