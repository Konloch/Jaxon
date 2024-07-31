package kernel.display.vesa;

import kernel.Kernel;
import java.util.IDebug;
import java.lang.StringBuilder;

public class VESAMode implements IDebug
{
	public final boolean graphical;
	public final int modeNr;
	public final int lfbAddress;
	public final int xRes;
	public final int yRes;
	public final int colorDepth;
	
	public VESAMode(int modeNr, int xRes, int yRes, int colDepth, int lfbAddress, boolean graphical)
	{
		this.modeNr = modeNr;
		this.xRes = xRes;
		this.yRes = yRes;
		this.colorDepth = colDepth;
		this.lfbAddress = lfbAddress;
		this.graphical = graphical;
	}
	
	@Override
	public String debug()
	{
		StringBuilder sb = new StringBuilder(30);
		sb.append("VESA(").append(modeNr).append("){").append(graphical ? "Graphic" : "Text").append(", bbp=").append(colorDepth).append(", x=").append(xRes).append(", y=").append(yRes).append("}");
		return sb.toString();
	}
	
	public int BytesPerColor()
	{
		switch (colorDepth)
		{
			case 8:
				return 1;
			case 15:
			case 16:
				return 2;
			case 24:
				return 3;
			case 32:
				return 4;
			default:
				Kernel.panic("VESAMode.bytesPerColor: unsupported color depth");
				return 0;
		}
	}
}
