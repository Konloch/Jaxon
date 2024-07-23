package apps;

import apps.tests.NonCooperativeTask;
import apps.tests.VirtualMemoryTest;
import apps.tetris.Tetris;
import kernel.Kernel;
import keyboard.Keyboard;
import keyboard.KeyboardListener;
import scheduling.Task;
import video.Printer;

public class Menu extends Task
{
	private final Task f1Task = new Editor();
	
	private final Task f2Task = new VirtualMemoryTest();
	
	private final Task f3Task = new NonCooperativeTask();
	
	private final Task f4Task = new Tetris();
	
	private Task f5Task;
	
	private Task f6Task;
	
	private Task f7Task;
	
	private Task f8Task;
	
	private Task f9Task;
	
	private Task f10Task;
	
	private Task f11Task;
	
	private Task f12Task;
	
	private final Listener listener = new Listener(this);
	
	private final Printer printer = new Printer();
	
	@Override
	protected void onSchedule()
	{
		printer.setCursor(0, 0);
		
		printKeyLabel("F1", f1Task);
		printKeyLabel("F2", f2Task);
		printKeyLabel("F3", f3Task);
		printKeyLabel("F4", f4Task);
		printKeyLabel("F5", f5Task);
		printKeyLabel("F6", f6Task);
		printKeyLabel("F7", f7Task);
		printKeyLabel("F8", f8Task);
		printKeyLabel("F9", f9Task);
		printKeyLabel("F10", f10Task);
		printKeyLabel("F11", f11Task);
		printKeyLabel("F12", f12Task);
	}
	
	@Override
	protected void onStart()
	{
		Keyboard.initstance().addListener(this.listener);
	}
	
	@Override
	protected void onStop()
	{
		Keyboard.initstance().removeListener(this.listener);
	}
	
	private void printKeyLabel(String keyName, Task task)
	{
		if (task != null)
		{
			printer.setColor(Printer.WHITE, task.isStopped() ? Printer.BLACK : Printer.GREEN);
			printer.print(keyName);
			printer.print(':');
			printer.print(task.toString());
			printer.setColor(Printer.WHITE, Printer.BLACK);
			printer.print(' ');
		}
	}
	
	private static class Listener extends KeyboardListener
	{
		
		private final Menu menu;
		
		public Listener(Menu menu)
		{
			this.menu = menu;
		}
		
		@Override
		public void onKeyDown(int value, int keyCode, boolean isChar, int flags)
		{
			Task task = null;
			
			if (!isChar)
			{
				switch (value)
				{
					case Keyboard.F1:
						task = menu.f1Task;
						break;
					case Keyboard.F2:
						task = menu.f2Task;
						break;
					case Keyboard.F3:
						task = menu.f3Task;
						break;
					case Keyboard.F4:
						task = menu.f4Task;
						break;
					case Keyboard.F5:
						task = menu.f5Task;
						break;
					case Keyboard.F6:
						task = menu.f6Task;
						break;
					case Keyboard.F7:
						task = menu.f7Task;
						break;
					case Keyboard.F8:
						task = menu.f8Task;
						break;
					case Keyboard.F9:
						task = menu.f9Task;
						break;
					case Keyboard.F10:
						task = menu.f10Task;
						break;
					case Keyboard.F11:
						task = menu.f11Task;
						break;
					case Keyboard.F12:
						task = menu.f12Task;
						break;
					default:
						break;
				}
			}
			
			if (task != null)
			{
				if (task.isStopped())
				{
					Kernel.scheduler.addTask(task);
				}
				else
				{
					task.stop();
				}
			}
		}
		
		@Override
		public void onKeyUp(int value, int keyCode, boolean isChar, int flags)
		{
		}
	}
}
