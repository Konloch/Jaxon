package os.commands;

import kernel.Kernel;
import kernel.Scheduler;
import os.screen.Color;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.tasks.DelayTask;
import os.tasks.Task;
import os.utils.stringTemplate.StringTemplate;

public class MemLayout extends CommandTask {

    private Scheduler scheduler;
    private DelayTask currentTask;

    private final StringTemplate rowFormat = new StringTemplate(" {18cx} | {18cx} | {20l} \n");

    // | 32     16 | 15    08 | 07    00 |
    // |     -     |    c1    |    c2    |
    private final int rowColors =
                    (Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE) << 8) |
                    Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE | Color.BRIGHT);

    private final int storeAt = 0x8000;
    private BIOS.MemGenerator generator;
    private BIOS.MemorySegment segment;
    private int row;

    public MemLayout(Scheduler scheduler, Terminal out) {
        super("mem", "Print memory layout", out);
        this.scheduler = scheduler;
        this.currentTask = null;
    }

    @Override
    public void setup(String[] args) {
        this.row = -1;
        this.generator = new BIOS.MemGenerator(storeAt);
        this.setDone(false);
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

        if(currentTask != null) {
            if(!currentTask.isDone())
                return;

            scheduler.removeTask(currentTask);
        }

        // https://wiki.osdev.org/Detecting_Memory_(x86)#Getting_an_E820_Memory_Map
        segment = generator.next();
        if(segment == null) {
            setDone(true);
        } else {
            currentTask = new DelayTask(
                    new MemPrinter(segment, "memprint_sub"),
                    20, "memprint_sub_delay");
            scheduler.addTask(currentTask);
        }
    }

    public void printTitle() {
        int currentColor = out.getColor();
        out.setColor(Color.BLACK, Color.GRAY);

        rowFormat.start(out);
        rowFormat.p("Base Address").p("Length").p("Type");

        out.setColor(currentColor);
    }

//    public void printSegment() {
//        int currentColor = out.getColor();
//
//        // odd-even alternating colors
//        out.setColor(rowColors >>> (8 * (row & 0x01)));
//        row++;
//        rowFormat.start(out);
//        rowFormat.p(segment.baseAddress).p(segment.length);
//
//        switch(segment.type) {
//            case 1:
//                rowFormat.p("(1) Free Memory");
//                break;
//
//            case 2:
//                rowFormat.p("(2) Reserved Memory");
//                break;
//
//            case 3:
//                rowFormat.p("(3) ACPI reclaimable");
//                break;
//
//            case 4:
//                rowFormat.p("(4) ACPI NVS memory");
//                break;
//
//            case 5:
//                rowFormat.p("(-) Bad memory");
//                break;
//
//            default:
//                rowFormat.p("Unknown");
//        }
//
//        out.setColor(currentColor);
//    }

    private class MemPrinter extends Task {
        private BIOS.MemorySegment segment;

        public MemPrinter(BIOS.MemorySegment segment, String name) {
            super(name);
            this.segment = segment;
        }

        @Override
        public void run() {
            int currentColor = out.getColor();

            // odd-even alternating colors
            out.setColor(rowColors >>> (8 * (row & 0x01)));
            row++;
            rowFormat.start(out);
            rowFormat.p(segment.baseAddress).p(segment.length);

            switch(segment.type) {
                case 1:
                    rowFormat.p("(1) Free Memory");
                    break;

                case 2:
                    rowFormat.p("(2) Reserved Memory");
                    break;

                case 3:
                    rowFormat.p("(3) ACPI reclaimable");
                    break;

                case 4:
                    rowFormat.p("(4) ACPI NVS memory");
                    break;

                case 5:
                    rowFormat.p("(-) Bad memory");
                    break;

                default:
                    rowFormat.p("Unknown");
            }

            out.setColor(currentColor);
        }
    }
}
