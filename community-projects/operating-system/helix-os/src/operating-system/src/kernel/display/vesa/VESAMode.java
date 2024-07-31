package kernel.display.vesa;

import kernel.Kernel;
import util.IDebug;
import util.StrBuilder;

public class VESAMode implements IDebug {
    public final boolean Graphical;
    public final int ModeNr;
    public final int LfbAddress;
    public final int XRes;
    public final int YRes;
    public final int ColorDepth;

    public VESAMode(int modeNr, int xRes, int yRes, int colDepth, int lfbAddress, boolean graphical) {
        this.ModeNr = modeNr;
        this.XRes = xRes;
        this.YRes = yRes;
        this.ColorDepth = colDepth;
        this.LfbAddress = lfbAddress;
        this.Graphical = graphical;
    }

    @Override
    public String Debug() {
        StrBuilder sb = new StrBuilder(30);
        sb.Append("VESA(").Append(ModeNr).Append("){")
                .Append(Graphical ? "Graphic" : "Text")
                .Append(", bbp=").Append(ColorDepth)
                .Append(", x=").Append(XRes)
                .Append(", y=").Append(YRes)
                .Append("}");
        return sb.toString();
    }

    public int BytesPerColor() {
        switch (ColorDepth) {
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
