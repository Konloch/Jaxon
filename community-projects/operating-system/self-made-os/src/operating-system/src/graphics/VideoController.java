package graphics;

import utils.ASCIIControlSequences;

//Handles low level access to Video components
public class VideoController
{
	
	//define constants for ASCII control sequences
	private static final int TAB_SIZE = 4;
	private static int videoMemoryPosition = 0;
	private static final VideoMemory vidMem = (VideoMemory) MAGIC.cast2Struct(VideoMemory.VIDEO_MEMORY_STARTPOS);
	
	//Writes a VideoChar to the graphics output
	protected static void handleChar(int ascii, int color)
	{
		if (videoMemoryPosition < 0 || videoMemoryPosition >= 2000)
			videoMemoryPosition = 0;
		
		//TODO: support ascii_bell? that should be handled on the Console level
		switch ((char) ascii)
		{
			case ASCIIControlSequences.NULL: //nothing to do
			case ASCIIControlSequences.VERTICAL_TAB: //not supported
			case ASCIIControlSequences.BELL: //not supported
			case ASCIIControlSequences.EOF: //not supported
			case ASCIIControlSequences.ESCAPE: //no escape sequences on console
			{
				return;
			}
			
			case ASCIIControlSequences.BACKSPACE:
			{
				videoMemoryPosition--;
				setCharacterAtPos(videoMemoryPosition, ASCIIControlSequences.SPACE, ConsoleColors.DEFAULT_CONSOLE_COLOR);
				return;
			}
			
			case ASCIIControlSequences.HORIZONTAL_TAB:
			{
				//go to the next tab position, every TAB_SIZE positions
				videoMemoryPosition += TAB_SIZE - (videoMemoryPosition % TAB_SIZE);
				return;
			}
			
			case ASCIIControlSequences.LINE_FEED:
			{
				//newline + carriage return
				carriageReturn();
				newLine();
				return;
			}//not supported
			
			case ASCIIControlSequences.FORM_FEED:
			{
				clearVideoMemory();
				return;
			}
			
			case ASCIIControlSequences.CARRIAGE_RETURN:
			{
				carriageReturn();
				return;
			}
			
			default:
			{
				//printable ascii characters
				if ((ascii & 0xFF) >= 0x20 && (ascii & 0xFF) < 0xFE)
				{
					writeCharToMemory(ascii, color);
					return;
				}
			}
		}
	}
	
	//inline for performance?
	@SJC.Inline
	private static void writeCharToMemory(int as, int cl)
	{
		setCharacterAtPos(videoMemoryPosition, as, cl);
		videoMemoryPosition++;
	}
	
	protected static void writeCharDirectly(int as, int x, int y, int cl)
	{
		setCharacterAtPos(x + y * VideoMemory.VIDEO_MEMORY_COLUMNS, as, cl);
	}
	
	protected static void writeCharDebug(int as, int cl)
	{
		writeCharToMemory(as, cl);
	}
	
	protected static void clearVideoMemory()
	{
		videoMemoryPosition = 0;
		assert vidMem != null;
		while (videoMemoryPosition < VideoMemory.VIDEO_MEMORY_LENGTH)
		{
			setCharacterAtPos(videoMemoryPosition, ASCIIControlSequences.SPACE, ConsoleColors.DEFAULT_CONSOLE_COLOR);
			videoMemoryPosition++;
		}
		videoMemoryPosition = 0;
	}
	
	static VideoCharCopy[] getCurrentVideoMem()
	{
		VideoCharCopy[] temp = new VideoCharCopy[VideoMemory.VIDEO_MEMORY_LENGTH];
		for (int i = 0; i < VideoMemory.VIDEO_MEMORY_LENGTH; i++)
		{
			temp[i] = new VideoCharCopy(vidMem.pos[i].ascii, vidMem.pos[i].color);
		}
		return temp;
	}
	
	private static void carriageReturn()
	{
		videoMemoryPosition -= videoMemoryPosition % VideoMemory.VIDEO_MEMORY_COLUMNS;
	}
	
	private static void newLine()
	{
		videoMemoryPosition += VideoMemory.VIDEO_MEMORY_COLUMNS;
	}
	
	protected static void setPos(int x, int y)
	{
		videoMemoryPosition = y * VideoMemory.VIDEO_MEMORY_COLUMNS + x;
	}
	
	protected static void updateCursor()
	{
		//current pos is videoMemoryPosition+1
		MAGIC.wIOs8(0x3D4, (byte) 0x0F);
		MAGIC.wIOs8(0x3D5, (byte) (videoMemoryPosition & 0xFF));
		MAGIC.wIOs8(0x3D4, (byte) 0x0E);
		MAGIC.wIOs8(0x3D5, (byte) ((videoMemoryPosition >> 8) & 0xFF));
	}
	
	protected static void disableCursor()
	{
		MAGIC.wIOs8(0x3D4, (byte) 0x0A);
		MAGIC.wIOs8(0x3D5, (byte) 0x20);
	}
	
	public static void enableCursor()
	{
		MAGIC.wIOs8(0x3D4, (byte) 0x0A);
		MAGIC.wIOs8(0x3D5, (byte) ((MAGIC.rIOs8(0x3D5) & 0xC0) | 14));
		
		MAGIC.wIOs8(0x3D4, (byte) 0x0B);
		MAGIC.wIOs8(0x3D5, (byte) ((MAGIC.rIOs8(0x3D5) & 0xE0) | 15));
	}
	
	@SJC.Inline
	private static void setCharacterAtPos(int pos, int as, int cl)
	{
		vidMem.pos[pos].ascii = (byte) as;
		vidMem.pos[pos].color = (byte) cl;
	}
	
	
	public static int getXPos()
	{
		return videoMemoryPosition % VideoMemory.VIDEO_MEMORY_COLUMNS;
	}
	
	public static int getYPos()
	{
		return videoMemoryPosition / VideoMemory.VIDEO_MEMORY_COLUMNS;
	}
	
	
}
