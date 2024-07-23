package kernel;

import os.screen.Cursor;
import os.screen.Terminal;
import os.tasks.DelayTask;
import os.tasks.LoopTask;
import os.tasks.Task;

public class Scheduler extends Task {
    private Task[] tasks;
    private int ptr;
    private int current;

    public Scheduler(String name) {
        this(name, 16);
    }

    public Scheduler(String name, int taskBuffer) {
        super(name);
        this.tasks = new Task[taskBuffer];
        this.ptr = 0;
        this.current = 0;
    }

    public void addTask(Task t) {
        Task[] tmp;
        int i;

        if(ptr == tasks.length) {
            tmp = new Task[tasks.length * 2];
            for (i = 0; i < tasks.length; i++) {
                tmp[i] = tasks[i];
            }

            tasks = tmp;
        }

        tasks[ptr++] = t;
    }

    public void removeTask(int id) {
        int i;
        if(id < 0 || id >= ptr)
            return;

        for (i = id + 1; i < ptr; i++) {
            tasks[i-1] = tasks[i];
        }
        ptr--;

        if(id < current)
            current--;
    }

    public void removeTask(Task task) {
        int i;
        for(i = 0; i < ptr; i++) {

            if(tasks[i] == task) {
                removeTask(i);
                break;
            }

        }
    }

    @Override
    public void run() {
        if(ptr == 0)
            return;

        if(tasks[current].isDone()) {
            removeTask(current);
        } else {
            tasks[current].run();
            if (++current >= tasks.length)
                current = 0;
        }
    }

    public int getAmountOfTasks() {
        return ptr+1;
    }

    public void printInfo(Terminal t) {
        for (int i = 0; i < ptr; i++) {
            t.print(i);
            t.print('=');
            t.print(tasks[i].getName());

            if(tasks[i] instanceof DelayTask) {
                t.print(" -> ");
                t.print(((DelayTask) tasks[i]).getTaskToCall().getName());
            } else if(tasks[i] instanceof LoopTask) {
                t.print(" -> ");
                t.print(((LoopTask) tasks[i]).getTask().getName());
            }

            t.println();
        }
    }

    public void runIndefinitely() {
        while(true) {
            if(tasks.length == 0)
                continue;

            if(current >= ptr)
                current = 0;

            tasks[current].run();
            current++;
        }
    }
}
