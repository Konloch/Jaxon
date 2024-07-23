package os.tasks.specificTasks;

import kernel.Kernel;
import kernel.Scheduler;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.tasks.Task;

public class TerminalTask extends Task {
    private Terminal t;
    private TerminalPrompter p;

    private Scheduler scheduler;

    private CommandTask commandRunning;

    public TerminalTask(Scheduler prompterScheduler, String name) {
        this(new Terminal(), prompterScheduler, name);
    }

    public TerminalTask(Terminal t, Scheduler prompterScheduler, String name) {
        super(name);
        int i;
        char[] prompterName = new char[name.length() + 7];

        this.t = t;
        this.scheduler = prompterScheduler;
        this.commandRunning = null;

        for(i = 0; i < name.length(); i++) {
            prompterName[i] = name.charAt(i);
        }

        prompterName[i++] = '_';
        prompterName[i++] = 'p';
        prompterName[i++] = 'r';
        prompterName[i++] = 'o';
        prompterName[i++] = 'm';
        prompterName[i++] = 'p';
        prompterName[i] = 't';

        p = new TerminalPrompter(new String(prompterName), t);
        t.focus();
        p.startPrompt();
        prompterScheduler.addTask(p);
    }

    @Override
    public void run() {
        if(commandRunning != null) {
            if(commandRunning.isDone()) {
                scheduler.removeTask(commandRunning);
                commandRunning = null;
                t.focus();
                p.startPrompt();
            }
            return;
        }

        if(p.isCommandReady()) {

            for (CommandTask cmd : Kernel.commands) {
                if (p.getCommandInputted().startsWith(cmd.getName())) {
                    cmd.setup(p.getArgsInputted());
                    this.commandRunning = cmd;
                    this.scheduler.addTask(cmd);
                    return;
                }
            }
            if(p.getCommandInputted().startsWith("clear")) {
                t.clear();
            }
            p.startPrompt();
        }
    }
}
