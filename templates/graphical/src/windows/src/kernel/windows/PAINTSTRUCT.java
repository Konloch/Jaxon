package kernel.windows;

import java.lang.STRUCT;

public class PAINTSTRUCT extends STRUCT
{
	int hdc;
	int fErase;
	RECT rcPaint;
	int fRestore;
	int fIncUpdate;
	//following: 32 bytes rgbReserved;
}