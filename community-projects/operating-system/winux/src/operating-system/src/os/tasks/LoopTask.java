package os.tasks;

import os.interrupt.Handler;

public class LoopTask extends Task{

    private final Task t;
    private int delay;
    private int fireNext;

    public LoopTask(Task t, int delay, String name) {
        super(name);

        this.t = t;
        this.delay = delay;
        this.fireNext = Handler.time + delay;
    }

    @Override
    public void run() {
        if(isDone())
            return;

        if(t.isDone()) {
            setDone(true);
            return;
        }

        if(Handler.time < fireNext)
            return;

        t.run();
        fireNext = Handler.time + delay;
    }

    public void setDelay(int delay) {
        this.fireNext = this.fireNext - this.delay + delay;
        this.delay = delay;
    }

    public Task getTask() {
        return t;
    }
}
