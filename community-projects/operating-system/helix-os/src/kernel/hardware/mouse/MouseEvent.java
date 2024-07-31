package kernel.hardware.mouse;

import java.util.IDebug;
import java.lang.StringBuilder;

public class MouseEvent implements IDebug
{
	public static final int LEFT_BUTTON = 1;
	public static final int RIGHT_BUTTON = 2;
	public static final int MIDDLE_BUTTON = 4;
	
	private static int _idCounter = 0;
	
	public int id = _idCounter++;
	public int xDelta;
	public int yDelta;
	public int buttonState;
	
	public MouseEvent(int xDelta, int yDelta, int buttonState)
	{
		this.xDelta = xDelta;
		this.yDelta = yDelta;
		this.buttonState = buttonState;
	}
	
	public MouseEvent()
	{
		this(0, 0, 0);
	}
	
	public boolean leftButtonPressed()
	{
		return (buttonState & LEFT_BUTTON) != 0;
	}
	
	public boolean rightButtonPressed()
	{
		return (buttonState & RIGHT_BUTTON) != 0;
	}
	
	public boolean middleButtonPressed()
	{
		return (buttonState & MIDDLE_BUTTON) != 0;
	}
	
	@Override
	public String debug()
	{
		return new StringBuilder(64).append("MouseEvent(")
				.append("Id=").append(id).append(", ").append("X_Delta=").append(xDelta).append(", ")
				.append("Y_Delta=").append(yDelta).append(", ")
				.append("ButtonState=").append(buttonState).append(")").toString();
	}
}
