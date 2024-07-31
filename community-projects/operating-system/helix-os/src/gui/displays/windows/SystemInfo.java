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
import java.util.StrBuilder;
import java.util.queue.QueueInt;

public class SystemInfo extends Window
{
	private TextField _textField;
	private String _text;
	private int _drawEveryNth = 1;
	private int _drawCounter = 0;
	private StrBuilder _sb;
	
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
		
		_sb = new StrBuilder(500);
	}
	
	public void drawContent()
	{
		if (_textField.needsRedraw())
			_textField.draw();
		
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
			_needsRedraw = false;
		
		return _needsRedraw;
	}
	
	@Override
	public void moveBy(int dragDiffX, int dragDiffY)
	{
		super.moveBy(dragDiffX, dragDiffY);
	}
	
	@Override
	public void update()
	{
		int gcExecTime = GarbageCollector.InfoLastRunTimeMs;
		int gcObjectsCollected = GarbageCollector.InfoLastRunCollectedObjects;
		int gcBytesCollected = GarbageCollector.InfoLastRunCollectedBytes;
		int gcEmptyObjectsCompacted = GarbageCollector.InfoLastRunCompactedEmptyObjects;
		
		_gcExecTimes.Put(gcExecTime);
		_gcObjectsCollected.Put(gcObjectsCollected);
		_gcBytesCollected.Put(gcBytesCollected);
		_gcEmptyObjectsCompacted.Put(gcEmptyObjectsCompacted);
		
		int consumedMemory = MemoryManager.GetUsedSpace();
		int freeMemory = MemoryManager.GetFreeSpace();
		int objectCount = MemoryManager.GetObjectCount();
		int emptyObjectCount = MemoryManager.GetEmptyObjectCount();
		
		int taskCount = Scheduler.getTaskCount();
		
		_sb.ClearKeepCapacity();
		
		_sb.AppendLine("Window Manger:").Append("  ").Append("Average Draw Time ").Append(WindowManager.infoAvgRenderTimeMs).Append(" ms").AppendLine();
		
		_sb.AppendLine();
		_sb.AppendLine("Memory:").Append("  ").Append("Consumed: ").Append(Memory.FormatBytesToKb(consumedMemory)).AppendLine().Append("  ").Append("Free: ").Append(Memory.FormatBytesToKb(freeMemory)).AppendLine().Append("  ").Append("Objects: ").Append(objectCount).AppendLine().Append("  ").Append("Empty Objects: ").Append(emptyObjectCount).AppendLine();
		
		_sb.AppendLine();
		_sb.AppendLine("GC:").Append("  ").Append("Last Run Time: ").Append(GarbageCollector.InfoLastRunTimeMs).Append(" ms").AppendLine().Append("  ").Append("Last Run Marked: ").Append(GarbageCollector.InfoLastRunCollectedObjects).AppendLine().Append("  ").Append("Last Run Collected: ").Append(Memory.FormatBytes(GarbageCollector.InfoLastRunCollectedBytes)).AppendLine().Append("  ").Append("Last Run Compacted: ").Append(GarbageCollector.InfoLastRunCompactedEmptyObjects).AppendLine();
		
		_sb.AppendLine();
		_sb.AppendLine("Tasks:").Append("  ").Append("Count: ").Append(taskCount).AppendLine();
		
		_text = _sb.toString();
		_textField.ClearText();
		_textField.Write(_text);
	}
}
