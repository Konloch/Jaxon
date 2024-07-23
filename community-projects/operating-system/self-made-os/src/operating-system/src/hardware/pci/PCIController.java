package hardware.pci;

public class PCIController
{
	private static final int PCI_BUS_COUNT = 256;
	private static final int PCI_DEV_PER_BUS = 32;
	private static final int PCI_FUNC_PER_DEV = 8;
	private static final int PCI_ADDR_IO_PORT = 0x0CF8;
	private static final int PCI_DATA_IO_PORT = 0x0CFC;
	private static final byte DEV_TYPE = 0x0;
	private static PCIDeviceVector recognizedDevices;
	
	private static class PCIDeviceVector
	{
		private PCIDevice[] backStorage;
		private int capacity;
		private int size;
		
		PCIDeviceVector()
		{
			capacity = 8;
			size = 0;
			backStorage = new PCIDevice[capacity];
		}
		
		public void addDevice(PCIDevice dev)
		{
			if (size == capacity)
				expandSize();
			backStorage[size] = dev;
			size++;
		}
		
		private void expandSize()
		{
			int oldCap = capacity;
			capacity *= 2;
			PCIDevice[] newBackStorage = new PCIDevice[capacity];
			for (int i = 0; i < oldCap; i++)
			{
				newBackStorage[i] = backStorage[i];
			}
			backStorage = newBackStorage;
		}
		
		public PCIDevice[] getDevices()
		{
			PCIDevice[] tmp = new PCIDevice[size];
			for (int i = 0; i < size; i++)
			{
				tmp[i] = backStorage[i];
			}
			return tmp;
		}
	}
	
	private static int generateAddress(byte type, byte register, byte function, byte device, byte busNum)
	{
		return ((0x80 << 24) | ((busNum & 0xFF) << 16) | ((device & 0x1F) << 11) | ((function & 0x07) << 8) | ((register & 0x3F) << 2) | (type & 0x03));
	}
	
	private static PCIDevice getFunctionRegisters(byte bus, byte dev, byte func)
	{
		int reg0, reg1, reg2, reg3;
		int addr = generateAddress(DEV_TYPE, (byte) 0x0, func, dev, bus);
		writeAddrToIO(addr);
		reg0 = readDataFromIO();
		//invalid device
		if (reg0 == 0xFFFFFFFF || reg0 == 0x0)
		{
			return null;
		}
		addr = generateAddress(DEV_TYPE, (byte) 0x1, func, dev, bus);
		writeAddrToIO(addr);
		reg1 = readDataFromIO();
		addr = generateAddress(DEV_TYPE, (byte) 0x2, func, dev, bus);
		writeAddrToIO(addr);
		reg2 = readDataFromIO();
		addr = generateAddress(DEV_TYPE, (byte) 0x3, func, dev, bus);
		writeAddrToIO(addr);
		reg3 = readDataFromIO();
		
		
		//process data
		PCIDevice retval = new PCIDevice((reg0 & 0xFFFF0000) >> 16, (reg0 & 0x0000FFFF), (reg1 & 0xFFFF0000) >> 16, (reg1 & 0x0000FFFF), (reg2 & 0xFF000000) >> 24, (reg2 & 0x00FF0000) >> 16, (reg2 & 0x0000FF00) >> 8, (reg2 & 0x000000FF), (reg3 & 0xFF000000) >> 24, (reg3 & 0x00FF0000) >> 16, (reg3 & 0x0000FF00) >> 8, (reg3 & 0x000000FF), bus, dev, func);
		return retval;
	}
	
	private static void writeAddrToIO(int addr)
	{
		MAGIC.wIOs32(PCI_ADDR_IO_PORT, addr);
	}
	
	private static int readDataFromIO()
	{
		return MAGIC.rIOs32(PCI_DATA_IO_PORT);
	}
	
	private static void recognizeDevices()
	{
		recognizedDevices = new PCIDeviceVector();
		for (int i = 0; i < PCI_BUS_COUNT; i++)
		{
			for (int j = 0; j < PCI_DEV_PER_BUS; j++)
			{
				for (int k = 0; k < PCI_FUNC_PER_DEV; k++)
				{
					PCIDevice dev = getFunctionRegisters((byte) (i & 0xFF), (byte) (j & 0xFF), (byte) (k & 0xFF));
					if (dev != null)
					{
						recognizedDevices.addDevice(dev);
					}
				}
				
			}
		}
	}
	
	public static PCIDevice[] getRecognizedDevices()
	{
		if (recognizedDevices == null)
			recognizeDevices();
		return recognizedDevices.getDevices();
	}
}
