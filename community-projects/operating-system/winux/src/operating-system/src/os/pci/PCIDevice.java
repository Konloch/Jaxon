package os.pci;
/*
CC = ClassCode
+-------------+-------------+-------------+-------------+
| 31       24 | 23       16 | 15       08 | 07       00 |
+-------------+-------------+-------------+-------------+
|         Device ID         |         Vendor ID         |
|           Status          |          Command          |
|    BaseCC   |    SubCC    |  Interface |   Revision   |
|     BIST    |    Header   |   Latency  |     CLG      |
+-------------+-------------+-------------+-------------+
*/
public class PCIDevice {
    public int deviceID, vendorID;
    public int status, command;
    public int baseClassCode, subClassCode, interf, revision;
    public int bist, header, latency, clg;

    public int bus, device, function;
}
