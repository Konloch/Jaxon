package graphics;

public class VideoMemory extends STRUCT
{
	public static final int VIDEO_MEMORY_ROWS = 25;
	public static final int VIDEO_MEMORY_COLUMNS = 80;
	static final int VIDEO_MEMORY_LENGTH = VIDEO_MEMORY_COLUMNS * VIDEO_MEMORY_ROWS;
	static final int VIDEO_MEMORY_STARTPOS = 0xB8000;
	static final int VIDEO_MEMORY_ENDPOS = 0xB8000 + VIDEO_MEMORY_LENGTH - 1;
	@SJC(count = 2000)
	VideoChar[] pos;
}


