package os.tasks.specificTasks;

import os.screen.Color;
import os.screen.Cursor;
import os.screen.Terminal;
import os.tasks.Task;
import os.utils.NumberHelper;


public class Counter extends Task {
    Cursor c = new Cursor();
    private int count = 0;

    private int yPos;

    public Counter(String name) {
        this(Terminal.ROWS - 1, name);
    }

    public Counter(int yPos, String name) {
        super(name);
        this.c.setColor(Color.GRAY, Color.BLACK);
        this.count = 0;
        this.yPos = yPos;
    }

    @Override
    public void run() {
        int width = NumberHelper.getIntWidth(count);

        c.setCursor(Terminal.COLS - width - 1, yPos);
        c.print(count);
        count++;
    }
}
