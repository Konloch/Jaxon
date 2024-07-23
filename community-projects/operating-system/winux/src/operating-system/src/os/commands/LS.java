package os.commands;

import os.filesystem.File;
import os.filesystem.FileSystem;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.stringTemplate.StringTemplate;

public class LS extends CommandTask {

    private FileSystem.FileTraverser traverser;
    private final StringTemplate template = new StringTemplate("{4r} byte \u00b3 {}\n");


    public LS(Terminal out) {
        super("ls", "List files", out);
    }

    @Override
    public void run() {
        File f;
        if(isDone())
            return;

        f = traverser.next();
        if(f == null)
            setDone(true);
        else
            template.start(out).p(f.size()).p(f.filename);
    }

    @Override
    public void setup(String[] args) {
        this.traverser = FileSystem.traverse();
        setDone(false);
    }
}
