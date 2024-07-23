package graphics;

@SJC.IgnoreUnit
//TODO: finish implementation of history
public class VideoMemoryHistory
{
	public static final int HISTORY_LINE_CAPACITY = 125;
	private static int currentLine = 0;
	private static final int linePos = 0;
	private static final VideoChar[] history = new VideoChar[VideoMemory.VIDEO_MEMORY_COLUMNS * HISTORY_LINE_CAPACITY];
	
	//makes space for new lines by pushing history back by n lines
	static void advanceHistory(int lineOffset)
	{
		//out of bounds
		if (lineOffset <= 0 || lineOffset >= HISTORY_LINE_CAPACITY || lineOffset > currentLine)
			return;
		for (int line = 0; line < HISTORY_LINE_CAPACITY - lineOffset; line++)
		{
			//break when we hit the last line of the buffer that is currently used
			if (line == currentLine)
				break;
			for (int column = 0; column < VideoMemory.VIDEO_MEMORY_COLUMNS; column++)
			{
				history[(line * VideoMemory.VIDEO_MEMORY_COLUMNS) + column] = history[((line + lineOffset) * VideoMemory.VIDEO_MEMORY_COLUMNS) + column];
			}
		}
		//update currentLine
		currentLine -= lineOffset;
	}
	
	static String scrollUpLines(int lines)
	{
		return null;
	}
	
	static void writeToHistory(byte as, byte cl)
	{
	
	}
}
