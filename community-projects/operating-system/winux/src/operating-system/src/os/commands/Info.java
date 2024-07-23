package os.commands;

import kernel.Kernel;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.ObjectEntrySizes;
import rte.DynamicRuntime;
import rte.ImageHelper;

public class Info extends CommandTask {

    private static final int IMAGE = 0;
    private static final int OBJECTS = 1;
    private static final int TASKS = 2;
    private static final int RELOC = 3;

    private int infoReading;
    private String more = null;

    public Info(Terminal out) {
        super("info", "Get info of [[imageBase|objects|tasks]]", out);
    }

    @Override
    public void setup(String[] args) {
        infoReading = -1;
        if(args.length > 1) {
            if(args[1].equals("imageBase"))
                infoReading = IMAGE;
            else if(args[1].equals("objects"))
                infoReading = OBJECTS;
            else if(args[1].equals("tasks"))
                infoReading = TASKS;
            else if(args[1].equals("entry")) {
                infoReading = RELOC;
                more = (args.length < 3) ? null : args[2];
            }
        }

        if(infoReading == -1) {
            out.println("Add one of the following arguments");
            out.println("  imageBase    - Information about the image base");
            out.println("  objects      - How many objects have been created");
            out.println("  tasks        - List active tasks");
            out.println("  entry [[obj]]- reloc and scalar entries of [[obj]]");
            setDone(true);
        } else {
            setDone(false);
        }
    }

    @Override
    public void run() {

        switch (infoReading) {
            case IMAGE:
                printImageInfo();
                break;

            case OBJECTS:
                printObjectsInfo();
                break;

            case TASKS:
                Kernel.globalScheduler.printInfo(out);
                break;

            case RELOC:
                if(more == null) {
                    out.println("Try one of the following");
                    ObjectEntrySizes.printPossibilities(out);
                    break;
                }

                int relocs = ObjectEntrySizes.getInstRelocEntries(more);
                int scalars = ObjectEntrySizes.getInstScalarEntries(more);
                if(relocs < 0) {
                    out.print(more);
                    out.println(" not found, try one of those:");
                    ObjectEntrySizes.printPossibilities(out);
                } else {
                    out.print(relocs);
                    out.println(" reloc entries");
                    out.print(scalars);
                    out.println(" scalars");
                }
                break;
        }



        setDone(true);
    }


    private void printImageInfo() {
        out.print("Image start     : "); out.printHex(MAGIC.rMem32(MAGIC.imageBase)); out.println();
        out.print("Image size      : "); out.printHex(MAGIC.rMem32(MAGIC.imageBase+4)); out.println();
        out.print("Class deskr     : "); out.printHex(MAGIC.rMem32(MAGIC.imageBase+8)); out.println();
        out.print("First code byte : "); out.printHex(MAGIC.rMem32(MAGIC.imageBase+12)); out.println();
        out.print("First object    : "); out.printHex(MAGIC.rMem32(MAGIC.imageBase+16)); out.println();
        out.print("RAM-init address: "); out.printHex(MAGIC.rMem32(MAGIC.imageBase+20)); out.println();
        out.print("Relative code st: "); out.printHex(MAGIC.rMem32(MAGIC.imageBase+24)); out.println();
        out.print("lo hi relocBytes: "); out.printHex(MAGIC.rMem32(MAGIC.imageBase+28)); out.println();
    }

    private void printObjectsInfo() {
        out.print("Static objects : ");
        out.println(ImageHelper.countStaticObjects());
        out.print("Dynamic objects: ");
        out.println(ImageHelper.countRuntimeObjects());
        out.print("All objects    : ");
        out.println(ImageHelper.countAllObjects());
        out.print("Empty objects  : ");
        out.println(DynamicRuntime.countEmptyObjects());
    }
}
