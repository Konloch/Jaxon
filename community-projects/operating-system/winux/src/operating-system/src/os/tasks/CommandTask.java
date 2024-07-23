package os.tasks;

import os.screen.Terminal;

public abstract class CommandTask extends Task {
    public final String description;
    public final Terminal out;

    private boolean done = false;

    public CommandTask(String name, String description, Terminal out) {
        super(name);
        this.description = description;
        this.out = out;
    }

    public abstract void setup(String[] args);

    public String getDescription() {
        return description;
    }
}
