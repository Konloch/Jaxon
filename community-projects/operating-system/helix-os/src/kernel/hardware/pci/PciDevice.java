package kernel.hardware.pci;

import java.util.IDebug;
import java.lang.StringBuilder;

public class PciDevice implements IDebug
{
	public final int vendorId;
	public final int deviceId;
	public final int command;
	public final int status;
	public final int revision;
	public final int itf;
	public final int subClassCode;
	public final int baseClassCode;
	public final int cls;
	public final int latency;
	public final int header;
	public final int bist;
	
	public final int bus;
	public final int device;
	public final int function;
	
	public PciDevice(int bus, int device, int function, int vendorId, int deviceId, int command, int status, int revision, int itf, int subClassCode, int baseClassCode, int cls, int latency, int header, int bist)
	{
		this.vendorId = vendorId;
		this.deviceId = deviceId;
		this.command = command;
		this.status = status;
		this.revision = revision;
		this.itf = itf;
		this.subClassCode = subClassCode;
		this.baseClassCode = baseClassCode;
		this.cls = cls;
		this.latency = latency;
		this.header = header;
		this.bist = bist;
		this.bus = bus;
		this.device = device;
		this.function = function;
	}
	
	public String BaseClassName()
	{
		switch (baseClassCode)
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
	public String debug()
	{
		StringBuilder sb = new StringBuilder(512);
		sb.append("PCI(").append(bus).append(':').append(device).append(':').append(function).append("){").append("Vendor=").append(vendorId).append(", ").append("Device=").append(deviceId).append(", ").append("Command=").append(command).append(", ").append("Status=").append(status).append(", ").append("Revision=").append(revision).append(", ").append("Itf=").append(itf).append(", ").append("SubClass=").append(subClassCode).append(", ").append("BaseClass=").append(baseClassCode).append(" (").append(BaseClassName()).append("), ").append("Cls=").append(cls).append(", ").append("Latency=").append(latency).append(", ").append("Header=").append(header).append(", ").append("Bist=").append(bist).append("}");
		return sb.toString();
	}
}
