package gui.displays.windows;

import formats.fonts.AFont;
import gui.Window;
import gui.WindowManager;
import gui.components.TextField;
import kernel.Kernel;
import kernel.memory.GarbageCollector;
import kernel.memory.Memory;
import kernel.memory.MemoryManager;
import kernel.schedule.Scheduler;
import java.lang.StringBuilder;
import java.util.queue.QueueInt;

public class SystemInfo extends Window
{
	private TextField _textField;
	private String _text;
	private int _drawEveryNth = 1;
	private int _drawCounter = 0;
	private StringBuilder _sb;
	
	private QueueInt _gcExecTimes;
	private QueueInt _gcObjectsCollected;
	private QueueInt _gcBytesCollected;
	private QueueInt _gcEmptyObjectsCompacted;
	private int _averageOver;
	
	public SystemInfo(String title, int x, int y, int width, int height, int border, int charSpacing, int lineSpacing, AFont font)
	{
		super(title, x, y, width, height, true);
		int bg = Kernel.Display.Rgb(100, 100, 100);
		int fg = Kernel.Display.Rgb(255, 255, 255);
		_textField = new TextField(0, 0, contentWidth, contentHeight, border, charSpacing, lineSpacing, fg, bg, false, font);
		
		_averageOver = 10;
		_gcExecTimes = new QueueInt(_averageOver);
		_gcObjectsCollected = new QueueInt(_averageOver);
		_gcBytesCollected = new QueueInt(_averageOver);
		_gcEmptyObjectsCompacted = new QueueInt(_averageOver);
		
		_sb = new StringBuilder(500);
	}
	
	public void DrawContent()
	{
		if (_textField.needsRedraw())
		{
			_textField.draw();
		}
		renderTarget.Blit(contentRelativeX, contentRelativeY, _textField.renderTarget, false);
	}
	
	@Override
	public boolean needsRedraw()
	{
		_drawCounter++;
		if (_drawCounter >= _drawEveryNth)
		{
			_drawCounter = 0;
			_needsRedraw = true;
		}
		else
		{
			_needsRedraw = false;
		}
		return _needsRedraw;
	}
	
	@Override
	public void MoveBy(int dragDiffX, int dragDiffY)
	{
		super.MoveBy(dragDiffX, dragDiffY);
	}
	
	@Override
	public void update()
	{
		int gcExecTime = GarbageCollector.InfoLastRunTimeMs;
		int gcObjectsCollected = GarbageCollector.InfoLastRunCollectedObjects;
		int gcBytesCollected = GarbageCollector.InfoLastRunCollectedBytes;
		int gcEmptyObjectsCompacted = GarbageCollector.InfoLastRunCompactedEmptyObjects;
		
		_gcExecTimes.put(gcExecTime);
		_gcObjectsCollected.put(gcObjectsCollected);
		_gcBytesCollected.put(gcBytesCollected);
		_gcEmptyObjectsCompacted.put(gcEmptyObjectsCompacted);
		
		int consumedMemory = MemoryManager.GetUsedSpace();
		int freeMemory = MemoryManager.GetFreeSpace();
		int objectCount = MemoryManager.GetObjectCount();
		int emptyObjectCount = MemoryManager.GetEmptyObjectCount();
		
		int taskCount = Scheduler.GetTaskCount();
		
		_sb.clearKeepCapacity();
		
		_sb.appendLine("Window Manger:").append("  ").append("Average Draw Time ").append(WindowManager.InfoAvgRenderTimeMs).append(" ms").appendLine();
		
		_sb.appendLine();
		_sb.appendLine("Memory:").append("  ").append("Consumed: ").append(Memory.FormatBytesToKb(consumedMemory)).appendLine().append("  ").append("Free: ").append(Memory.FormatBytesToKb(freeMemory)).appendLine().append("  ").append("Objects: ").append(objectCount).appendLine().append("  ").append("Empty Objects: ").append(emptyObjectCount).appendLine();
		
		_sb.appendLine();
		_sb.appendLine("GC:").append("  ").append("Last Run Time: ").append(GarbageCollector.InfoLastRunTimeMs).append(" ms").appendLine().append("  ").append("Last Run Marked: ").append(GarbageCollector.InfoLastRunCollectedObjects).appendLine().append("  ").append("Last Run Collected: ").append(Memory.FormatBytes(GarbageCollector.InfoLastRunCollectedBytes)).appendLine().append("  ").append("Last Run Compacted: ").append(GarbageCollector.InfoLastRunCompactedEmptyObjects).appendLine();
		
		_sb.appendLine();
		_sb.appendLine("Tasks:").append("  ").append("Count: ").append(taskCount).appendLine();
		
		_text = _sb.toString();
		_textField.clearText();
		_textField.write(_text);
	}
}
