package os.commands;

import os.tasks.CommandTask;

public class CC extends CommandTask {

    public CC() {
        super("cc", "Cause breakpoint exception", null);
    }

    private void causeCC() {
        sthElse(-1, 'F', (byte) 0x0F);
    }

    private void sthElse(int arg1, char arg2, byte arg3) {
        more();
    }

    private void more() {
        MAGIC.inline(0xCC);
    }

    @Override
    public void run() {
        causeCC();
        setDone(true);
    }

    @Override
    public void setup(String[] args) {
        /* do nothing */
    }
}
