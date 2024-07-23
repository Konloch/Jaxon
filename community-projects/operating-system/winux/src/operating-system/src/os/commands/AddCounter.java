package os.commands;

import kernel.Scheduler;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.tasks.LoopTask;
import os.tasks.specificTasks.Counter;
import os.utils.StringBuffer;

public class AddCounter extends CommandTask {
    private static int y = Terminal.ROWS-1;
    private static int counterInc = 0;

    private Scheduler scheduler;
    private int delay;

    public AddCounter(Scheduler s, Terminal out) {
        super("addcounter", "Add counter task with [[delay]]", out);
        this.scheduler = s;
    }

    @Override
    public void setup(String[] args) {
        int delay, i;
        char c;

        setDone(true);

        if(args.length < 2) {
            out.println("Specify [[delay]] for counter");
            return;
        }

        delay = 0;
        for(i = 0; i < args[1].length(); i++) {
            c = args[1].charAt(i);
            if('0' <= c && c <= '9')
                delay = (delay * 10) + (c - '0');
        }

        if(delay == 0) {
            out.println("Delay has to be greater than 0");
            return;
        }

        this.delay = delay;
        setDone(false);
    }

    @Override
    public void run() {
        if(isDone())
            return;

        StringBuffer sb;
        String innerName, outerName;

        sb = new StringBuffer(22);
        sb.append("counterLoop_").append(counterInc);
        outerName = sb.toString();

        sb.append("_inner");
        innerName = sb.toString();
        scheduler.addTask(new LoopTask(new Counter(y, innerName), delay, outerName));

        if(--y < 0)
            y = Terminal.ROWS-1;

        out.print("Added counter #");
        out.println(counterInc++);

        setDone(true);
    }
}
