package os.commands;

import devices.StaticV24;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.Math;
import os.virtualMemory.VirtualMemory;

public class Page extends CommandTask {
    private int page;

    public Page(Terminal out) {
        super("page", "Load [[page]]", out);
    }

    @Override
    public void setup(String[] args) {
        if(args.length < 2)
            page = -1;
        else
            page = Math.parseInt(args[1], -1);
        setDone(false);
    }

    @Override
    public void run() {
        int pageResult = 0;

        if(isDone())
            return;

        if(page < 0) {
            out.println("page [[positive number]]     - load page number");
            setDone(true);
            return;
        }

        StaticV24.print("loading ");
        StaticV24.println(page);



        VirtualMemory.setCR3(page);
        VirtualMemory.enableVirtualMemory();
        pageResult = VirtualMemory.getCR2();



        out.print("Loading page ");
        out.print(page);
        out.print(" gave us the following: 0x");
        out.printHex(pageResult);

        setDone(true);
    }
}
