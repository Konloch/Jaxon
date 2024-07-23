package kernel;

import devices.StaticV24;
import os.commands.*;
import os.editor.Editor;
import os.filesystem.File;
import os.filesystem.FileSystem;
import os.interrupt.Handler;
import os.interrupt.Interrupt;
import os.screen.Terminal;
import os.tasks.*;
import os.tasks.specificTasks.MarkAndSweep;
import os.tasks.specificTasks.SchedulerInfoPrinter;
import os.tasks.specificTasks.TerminalTask;
import os.utils.MethodFinder;

public class Kernel {

    public static Scheduler globalScheduler;
    public static CommandTask[] commands;

    public static void main() {
        MAGIC.doStaticInit();
        Interrupt.initPic();

//        VirtualMemory.initPageDirectory();

//        VirtualMemory.setCR3(((MAGIC.cast2Ref(VirtualMemory.pageDirectory) / (1024)) << 12) | 3);
//        VirtualMemory.enableVirtualMemory();


        BIOS.switchMode(BIOS.GRAPHICS_MODE);

//        GraphicLogo.doIntro(1);
//        sleep(30);

        BIOS.switchMode(BIOS.TEXT_MODE);

        Terminal mainTerminal = new Terminal();
        mainTerminal.enableCursor();
        mainTerminal.clear();
        mainTerminal.focus();

        globalScheduler = new Scheduler("root");
        FileSystem.add(new File("test", "lorem ipsum\nthe child who survived\nmr anderson anderson anderson".toCharArray()));

        globalScheduler.addTask(new TerminalTask(mainTerminal, globalScheduler, "mainTerminal"));
        globalScheduler.addTask(new SchedulerInfoPrinter("sPrinter"));
        //globalScheduler.addTask(new LoopTask(new MarkAndSweep("mas"), 46, "mas_loop"));

        commands = new CommandTask[13];
        commands[0] = new AddCounter(globalScheduler, mainTerminal);
        commands[1] = new CC();
        commands[2] = new Echo(mainTerminal);
        commands[3] = new Editor();
        commands[4] = new Info(mainTerminal);
        commands[5] = new LS(mainTerminal);
        commands[6] = new MarkAndSweep();
        commands[7] = new MemLayout(globalScheduler, mainTerminal);
        commands[8] = new ObjectViewer(mainTerminal);
        commands[9] = new Page(mainTerminal);
        commands[10] = new PCI(mainTerminal);
        commands[11] = new PrintEmptyObject(mainTerminal);
        commands[12] = new Help(mainTerminal);

        globalScheduler.runIndefinitely();
    }


    public static void sleep(int seconds) {
        // https://wiki.osdev.org/PIT
        // https://www.visualmicro.com/page/Timer-Interrupts-Explained.aspx

        int current = Handler.time;
        while (Handler.time < current + seconds);

//    MAGIC.wIOs8(0x70, (byte) 0);
//    byte seconds = (byte) (MAGIC.rIOs8(0x71) & 0xFF);
//    byte ts = seconds;
//    while(seconds + 20 > ts){
//      MAGIC.wIOs8(0x70, (byte) 0);
//      ts = (byte) (MAGIC.rIOs8(0x71) & 0xFF);
//    }
    }
}
