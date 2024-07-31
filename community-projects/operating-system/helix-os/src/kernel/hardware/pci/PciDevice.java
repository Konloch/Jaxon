package kernel.hardware.pci;

import java.util.IDebug;
import java.util.StrBuilder;

public class PciDevice implements IDebug
{
	public final int VendorId;
	public final int DeviceId;
	public final int Command;
	public final int Status;
	public final int Revision;
	public final int Itf;
	public final int SubClassCode;
	public final int BaseClassCode;
	public final int Cls;
	public final int Latency;
	public final int Header;
	public final int Bist;
	
	public final int Bus;
	public final int Device;
	public final int Function;
	
	public PciDevice(int bus, int device, int function, int vendorId, int deviceId, int command, int status, int revision, int itf, int subclasscode, int baseclasscode, int cls, int latency, int header, int bist)
	{
		VendorId = vendorId;
		DeviceId = deviceId;
		Command = command;
		Status = status;
		Revision = revision;
		Itf = itf;
		SubClassCode = subclasscode;
		BaseClassCode = baseclasscode;
		Cls = cls;
		Latency = latency;
		Header = header;
		Bist = bist;
		Bus = bus;
		Device = device;
		Function = function;
	}
	
	public String BaseClassName()
	{
		switch (BaseClassCode)
		{
			case 0x00:
				return "Old Device";
			case 0x01:
				return "Mass Storage Controller";
			case 0x02:
				return "Network Controller";
			case 0x03:
				return "Display Controller";
			case 0x04:
				return "Multimedia Controller";
			case 0x05:
				return "Memory Controller";
			case 0x06:
				return "Bridge Device";
			case 0x07:
				return "Communication Controller";
			case 0x08:
				return "System Peripheral";
			case 0x09:
				return "Input Device";
			case 0x0A:
				return "Docking Station";
			case 0x0B:
				return "Processor";
			case 0x0C:
				return "Serial Bus Controller";
			case 0x0D:
				return "Wireless Controller";
			case 0x0E:
				return "Intelligent Controller";
			case 0x0F:
				return "Satellite Communication Controller";
		}
		return "Unknown";
	}
	
	@Override
	public String Debug()
	{
		StrBuilder sb = new StrBuilder(512);
		sb.Append("PCI(").Append(Bus).Append(':').Append(Device).Append(':').Append(Function).Append("){").Append("Vendor=").Append(VendorId).Append(", ").Append("Device=").Append(DeviceId).Append(", ").Append("Command=").Append(Command).Append(", ").Append("Status=").Append(Status).Append(", ").Append("Revision=").Append(Revision).Append(", ").Append("Itf=").Append(Itf).Append(", ").Append("SubClass=").Append(SubClassCode).Append(", ").Append("BaseClass=").Append(BaseClassCode).Append(" (").Append(BaseClassName()).Append("), ").Append("Cls=").Append(Cls).Append(", ").Append("Latency=").Append(Latency).Append(", ").Append("Header=").Append(Header).Append(", ").Append("Bist=").Append(Bist).Append("}");
		return sb.toString();
	}
}
