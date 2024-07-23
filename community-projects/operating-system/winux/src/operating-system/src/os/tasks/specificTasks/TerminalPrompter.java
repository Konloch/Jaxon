package os.tasks.specificTasks;

import os.keyboard.ASCII;
import os.keyboard.KeyEvent;
import os.keyboard.KeyboardController;
import os.screen.Terminal;
import os.tasks.Task;

public class TerminalPrompter extends Task {
    public static final int DEFAULT_BUFFER_SIZE = 128;
    public static final String PREFIX = "> ";

    private char[] input;
    private int ptr;
    private boolean prompting;

    private String commandInputted;
    private String[] argsInputted;

    private Terminal out;

    public TerminalPrompter(String name, Terminal out) {
        this(name, out, DEFAULT_BUFFER_SIZE);
    }

    public TerminalPrompter(String name, Terminal out, int buffer) {
        super(name);
        this.input = new char[buffer];
        this.ptr = 0;
        this.prompting = false;
        this.out = out;

        this.commandInputted = "";
    }

    @Override
    public void run() {
        if (!prompting)
            return;

        if(KeyboardController.getKeyBuffer().isEmpty())
            return;

        KeyEvent k = KeyboardController.getKeyBuffer().pop();

        if (ASCII.isPrintable(k.code)) {

            if (k.code == ASCII.NEW_LINE) {
                // done, convert to string
                out.print((char) k.code);
                stopPrompt();
                return;

            } else if (ptr < input.length) {
                out.print((char) k.code);
                input[ptr++] = (char) k.code;
            }

        } else if (k.code == ASCII.BACKSPACE && ptr > 0) {
            ptr--;
            if (input[ptr] == ASCII.TAB) {
                out.moveCursorHorizontally(-4);
            } else {
                out.moveCursorHorizontally(-1);
                out.print(' ');
                out.moveCursorHorizontally(-1);
            }
        }

        out.updateCursorPos();
    }

    public void startPrompt() {
        this.out.print(PREFIX);
        this.out.enableCursor();
        this.out.updateCursorPos();
        this.ptr = 0;
        this.prompting = true;
        this.commandInputted = "";

    }

    public void stopPrompt() {
        int j;
        char[] copied = new char[ptr];

        out.disableCursor();

        for (j = 0; j < ptr; j++) {
            copied[j] = input[j];
        }

        this.commandInputted = new String(copied);
        this.argsInputted = this.commandInputted.split(' ');
        this.prompting = false;
        this.ptr = 0;
    }

    @SJC.Inline
    public String getCommandInputted() {
        return commandInputted;
    }

    @SJC.Inline
    public String[] getArgsInputted() {
        return argsInputted;
    }

    @SJC.Inline
    public boolean isPrompting() {
        return prompting;
    }

    @SJC.Inline
    public boolean isCommandReady() {
        return !prompting;
    }
}
