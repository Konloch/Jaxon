package os.commands;

import kernel.Kernel;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.stringTemplate.StringTemplate;

public class Help extends CommandTask {

    public Help(Terminal out) {
        super("help", "View this help", out);
    }

    @Override
    public void setup(String[] args) {
        setDone(false);
    }

    @Override
    public void run() {
        StringTemplate t = new StringTemplate("{10} - {}\n");
        for(CommandTask cmd : Kernel.commands) {
            t.start(out);
            t.p(cmd.getName()).p(cmd.getDescription());
        }

        setDone(true);
    }
}
