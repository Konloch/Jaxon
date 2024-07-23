package os.tasks;

import os.interrupt.Handler;

public class DelayTask extends Task {
    private final Task taskToCall;
    private final int until;

    public DelayTask(Task callback, int sleepLength, String name) {
        super(name);
        this.until = Handler.time + sleepLength;
        this.taskToCall = callback;
    }

    @Override
    public void run() {
        if(isDone())
            return;

        if(Handler.time < until)
            return;

        this.taskToCall.run();
        setDone(true);
    }

    public Task getTaskToCall() {
        return taskToCall;
    }
}
