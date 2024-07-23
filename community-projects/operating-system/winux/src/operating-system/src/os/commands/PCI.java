package os.commands;

import os.pci.PCIController;
import os.pci.PCIDevice;
import os.screen.Color;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.stringTemplate.StringTemplate;

public class PCI extends CommandTask {

    // write into address register...
    public static final int PCI_ADDRESS = 0x0CF8;

    // ...immediately receive data
    public static final int PCI_DATA = 0x0CFC;

    /**
     * Write to PCI address
     *
     *    31          | 30        24 | 23     16 | 15      11 | 10    8 | 7             2 | 1                     0
     * Enable bit (1) | Reserved (0) |   Bus #   |  Device #  |  Fct #  | Register Offset | Offset cont. (always 0)
     *
     * @param busAddress 0-255: bus address
     * @param devNumber 0-31: device address on bus
     * @param funcAddress 0-7: function address of a device
     * @param register 0-63: register address of function
     */
    @SJC.Inline
    public static void writeAddress(int busAddress, int devNumber, int funcAddress, int register) {
        MAGIC.wIOs32(
                PCI_ADDRESS,
                0x80000000 | (busAddress << 16) | (devNumber << 11) | (funcAddress << 8) | (register << 2)
        );
    }


    private StringTemplate templ = new StringTemplate(" {3r} \u00b3 {3r} \u00b3 {3r} \u00b3 {1} {28c} \u00b3 {5r} \u00b3 {8r} \u00b3 {8r} ");
    private PCIController.DevGenerator generator;
    private PCIDevice d;
    private int row;

    private final int rowColors =
                    (Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE) << 8) |
                    Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE | Color.BRIGHT);


    public PCI(Terminal out) {
        super("pci", "View connected PCI devices", out);
    }



    /**
     * Prints the following information for each PCI-Device
     *
     * Bus number
     * Device number
     * Function number
     * Basis class code (device category)
     * Sub class code
     * vendor id and device id
     *
     */
    public static void printPCI(Terminal out) {
        int c1, c2;
        boolean even;
        int currentColor = out.getColor();
        PCIController.DevGenerator generator;
        StringTemplate templ = new StringTemplate(" {3r} \u00b3 {3r} \u00b3 {3r} \u00b3 {1} {28c} \u00b3 {5r} \u00b3 {8r} \u00b3 {8r} ");

        out.setColor(Color.BLACK, Color.GRAY);

        templ.start(out);
        templ.p("Bus").p("Dev").p("Fct").p("").p("BaseCC").p("SubCC").p("vendorID").p("deviceID");

        generator = new PCIController.DevGenerator();
        PCIDevice d = generator.next();

        c1 = Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE);
        c2 = Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE | Color.BRIGHT);
        even = true;

        while(d != null) {
            if(even) out.setColor(c1);
            else out.setColor(c2);
            even = !even;

            templ.start(out);
            templ
                    .p(d.bus)
                    .p(d.device)
                    .p(d.function)
                    .p(d.baseClassCode)
                    .p(PCIController.getBaseClassCodeDescription(d.baseClassCode))
                    .p(d.subClassCode)
                    .p(d.vendorID)
                    .p(d.deviceID);
            d = generator.next();
        }

        out.setColor(currentColor);
    }

    @Override
    public void setup(String[] args) {
        generator = new PCIController.DevGenerator();
        row = -1;
        setDone(false);
    }

    @Override
    public void run() {
        if(generator == null || isDone())
            return;

        if(row == -1) {
            printTitle();
            row = 0;
            return;
        }

        d = generator.next();
        if(d == null) {
            setDone(true);
        } else {
            printCurrentDevice();
        }
    }

    public void printTitle() {
        int currentColor = out.getColor();

        out.setColor(Color.BLACK, Color.GRAY);
        templ.start(out);
        templ.p("Bus").p("Dev").p("Fct").p("").p("BaseCC").p("SubCC").p("vendorID").p("deviceID");

        out.setColor(currentColor);
    }

    public void printCurrentDevice() {
        int currentColor = out.getColor();

        out.setColor(rowColors >>> (8 * (row & 0x01)));
        row++;

        templ.start(out);
        templ
                .p(d.bus)
                .p(d.device)
                .p(d.function)
                .p(d.baseClassCode)
                .p(PCIController.getBaseClassCodeDescription(d.baseClassCode))
                .p(d.subClassCode)
                .p(d.vendorID)
                .p(d.deviceID);
        d = generator.next();
        out.setColor(currentColor);
    }
}
