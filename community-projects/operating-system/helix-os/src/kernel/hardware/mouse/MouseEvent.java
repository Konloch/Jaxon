package kernel.hardware.mouse;

import java.util.IDebug;
import java.lang.StringBuilder;

public class MouseEvent implements IDebug
{
	public static final int LEFT_BUTTON = 1;
	public static final int RIGHT_BUTTON = 2;
	public static final int MIDDLE_BUTTON = 4;
	
	private static int _idCounter = 0;
	
	public int Id = _idCounter++;
	public int X_Delta;
	public int Y_Delta;
	public int ButtonState;
	
	public MouseEvent(int xDelta, int yDelta, int buttonState)
	{
		X_Delta = xDelta;
		Y_Delta = yDelta;
		ButtonState = buttonState;
	}
	
	public MouseEvent()
	{
		this(0, 0, 0);
	}
	
	public boolean LeftButtonPressed()
	{
		return (ButtonState & LEFT_BUTTON) != 0;
	}
	
	public boolean RightButtonPressed()
	{
		return (ButtonState & RIGHT_BUTTON) != 0;
	}
	
	public boolean MiddleButtonPressed()
	{
		return (ButtonState & MIDDLE_BUTTON) != 0;
	}
	
	@Override
	public String debug()
	{
		return new StringBuilder(64).append("MouseEvent(").append("Id=").append(Id).append(", ").append("X_Delta=").append(X_Delta).append(", ").append("Y_Delta=").append(Y_Delta).append(", ").append("ButtonState=").append(ButtonState).append(")").toString();
		
	}
}
