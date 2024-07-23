package os.commands;

import os.keyboard.ASCII;
import os.keyboard.KeyEvent;
import os.keyboard.KeyboardController;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.stringTemplate.StringTemplate;
import rte.ImageHelper;

public class ObjectViewer extends CommandTask {
    private int count;
    private Object current;
    private int lastEnd;
    private int y, x;

    private StringTemplate tmpl = new StringTemplate("#{79}from {75x}to   {75x}is same: {5}");

    public ObjectViewer(Terminal out) {
        super("objview", "View objects", out);
    }

    @Override
    public void setup(String[] args) {
        this.current = ImageHelper.getFirstObject();
        while(MAGIC.cast2Ref(current) < MAGIC.imageBase + MAGIC.rMem32(MAGIC.imageBase + 4))
            current = current._r_next;

        this.y = out.getY();
        this.x = out.getX();
        this.count = this.lastEnd = 0;

        this.setDone(false);
    }

    @Override
    public void run() {
        if(isDone() || current == null)
            return;

        if(KeyboardController.getKeyBuffer().isEmpty())
            return;

        KeyEvent key = KeyboardController.getKeyBuffer().pop();
        if(key.code == ASCII.q || key.code == ASCII.Q)
            setDone(true);
        else {
            out.setPos(x, y);
            tmpl.start(out)
                    .p(count++)
                    .p(ImageHelper.startAddress(current))
                    .p(ImageHelper.endAddress(current))
                    .p(lastEnd == ImageHelper.startAddress(current));
            lastEnd = ImageHelper.endAddress(current);
            current = current._r_next;
        }

    }
}
