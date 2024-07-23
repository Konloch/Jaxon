package os.commands;

import os.screen.Color;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.stringTemplate.StringTemplate;
import rte.DynamicRuntime;
import rte.EmptyObject;

public class PrintEmptyObject extends CommandTask {

    private StringTemplate table = new StringTemplate("{10cx} \u00b3 {10cx}\n");
    private int row;
    private EmptyObject obj;

    // | 32     16 | 15    08 | 07    00 |
    // |     -     |    c1    |    c2    |
    private final int rowColors =
                    (Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE) << 8) |
                    Color.mix(Color.GRAY | Color.BRIGHT, Color.BLUE | Color.BRIGHT);

    public PrintEmptyObject(Terminal out) {
        super("empties", "Print empty objects", out);
    }

    @Override
    public void setup(String[] args) {
        table.start(out);
        obj = DynamicRuntime.firstEmptyObject;
        row = -1;
        setDone(obj == null);
    }

    @Override
    public void run() {
        int oldColor;

        if(isDone())
            return;

        if(row == -1) {
            table.start(out);
            table.p("Start").p("Size");
            row = 0;
            return;
        }

        oldColor = out.getColor();
        out.setColor(rowColors >>> (8 * (row & 0x01)));
        table.start(out);
        table.p(MAGIC.cast2Ref(obj)).p(obj._r_scalarSize);
        obj = obj.next;
        out.setColor(oldColor);
        row++;

        setDone(obj == null);
    }
}
