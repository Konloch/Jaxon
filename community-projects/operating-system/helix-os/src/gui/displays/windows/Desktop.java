package gui.displays.windows;

import formats.fonts.Font7x8;
import formats.fonts.Font9x16;
import gui.Window;
import gui.components.button.BitmapButton;
import gui.components.button.ButtonClickedEventArgs;
import gui.images.BackgroundBarn;
import gui.images.EditorIcon;
import gui.images.FlowFieldIcon;
import gui.images.InfoIcon;
import kernel.Kernel;
import kernel.display.Bitmap;
import kernel.trace.logging.Logger;

public class Desktop extends Window
{
	private Bitmap _background;
	
	private static String _btnLaunchEditor = "Btn_Launch_Editor";
	private static String _btnLaunchSystemInfo = "Btn_Launch_SystemInfo";
	private static String _btnLaunchFlowField = "Btn_Launch_FlowField";
	
	public Desktop(String title)
	{
		super(title, 0, 0, Kernel.Display.width(), Kernel.Display.height(), false);
		_isDraggable = false;
		_background = BackgroundBarn.load().scale(Kernel.Display.width(), Kernel.Display.height());
		
		addWidget(new BitmapButton(_btnLaunchEditor, 10, 10, 60, EditorIcon.load(), this));
		
		addWidget(new BitmapButton(_btnLaunchSystemInfo, 10, 100, 60, InfoIcon.load(), this));
		
		addWidget(new BitmapButton(_btnLaunchFlowField, 10, 190, 60, FlowFieldIcon.load(), this));
		
	}
	
	public void drawContent()
	{
		renderTarget.blit(0, 0, _background, false);
		drawWidgets();
	}
	
	@Override
	public void update()
	{
	
	}
	
	@Override
	public boolean onButtonClicked(ButtonClickedEventArgs event)
	{
		if (super.onButtonClicked(event))
			return true;
		
		if (event.buttonName == _btnLaunchSystemInfo)
		{
			Logger.trace("Desktop", "Launch System Info");
			SystemInfo sysinfo = new SystemInfo("System Info", 40, 40, 400, 400, 8, 0, 2, Font7x8.INSTANCE);
			Kernel.WindowManager.addWindow(sysinfo);
			return true;
		}
		else if (event.buttonName == _btnLaunchEditor)
		{
			Editor editor = new Editor("Editor", 40, 40, 600, 400, 8, 0, 2, Font9x16.INSTANCE);
			Kernel.WindowManager.addWindow(editor);
			return true;
		}
		else if (event.buttonName == _btnLaunchFlowField)
		{
			FlowField flowField = new FlowField("Flow Field", 40, 40, 800, 600);
			Kernel.WindowManager.addWindow(flowField);
			return true;
		}
		return false;
	}
}
