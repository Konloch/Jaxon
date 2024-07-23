package os.tasks.specificTasks;

import kernel.Kernel;
import os.screen.Cursor;
import os.screen.Terminal;
import os.tasks.Task;

public class SchedulerInfoPrinter extends Task {
    private final Cursor c;
    private final int posX = 0;
    private final int posY = Terminal.ROWS - 1;

    private int count = 0;

    public SchedulerInfoPrinter(String name) {
        super(name);
        this.c = new Cursor();
    }

    @Override
    public void run() {
        c.setCursor(posX, posY);
        c.print(Kernel.globalScheduler.getAmountOfTasks());
        c.print(" Tasks running (");
        c.print(count++);
        c.print(')');
    }
}
