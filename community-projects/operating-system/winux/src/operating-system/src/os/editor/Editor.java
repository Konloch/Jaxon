package os.editor;

import os.filesystem.File;
import os.filesystem.FileStream;
import os.filesystem.FileSystem;
import os.keyboard.ASCII;
import os.keyboard.KeyEvent;
import os.keyboard.KeyboardController;
import os.screen.Color;
import os.screen.Cursor;
import os.screen.Terminal;
import os.tasks.CommandTask;
import os.utils.Math;
import os.utils.StringBuffer;
import os.utils.stringTemplate.StringTemplate;

public class Editor extends CommandTask {

    private String filename;
    private StringBuffer[] content;
    private int cY, cX;
    private int scrollY;

    private int templIndex;
    private final StringTemplate[] rowTemplates;

    public Editor() {
        super("nano", "Create new file or edit existing file", new Terminal(' '));
        rowTemplates = new StringTemplate[4];
        rowTemplates[0] = new StringTemplate("{1r} {78<79}");
        rowTemplates[1] = new StringTemplate("{2r} {77<78}");
        rowTemplates[2] = new StringTemplate("{3r} {76<77}");
        rowTemplates[3] = new StringTemplate("{4r} {75<76}");
    }

    private void readFile(String name) {
        int i;
        File f;
        FileStream stream;

        templIndex = 0;
        f = FileSystem.get(filename);
        if(f != null) {
            content = new StringBuffer[f.countLines()];
            stream = new FileStream(f);

            i = 0;
            while(stream.hasNext()) {
                content[i++] = new StringBuffer(stream.getLine());
                if(i == 10 || i == 100 || i == 1000)
                    templIndex++;
            }
        }
    }

    private void writeScreen() {
        int y;
        int row;
        //StringTemplate template = new StringTemplate("{2r} {77<78}");

        int curColor = out.getColor();
        int lineNumColor = Color.mix(Color.CYAN, Color.BLACK);

        StringTemplate t = rowTemplates[templIndex];

        out.disableCursor();
        out.setPos(0,0);
        for(y = 0, row = scrollY; y < Terminal.ROWS - 1; y++, row++) {
            t.start(out);
            if(row >= content.length || content[row] == null) {
                t.p("").p("");
            } else {
                out.setColor(lineNumColor);
                t.p(row);
                out.setColor(curColor);
                t.p(content[row].toString());
            }
        }
        out.enableCursor();

        updateCursorPos();
    }

    @Override
    public void setup(String[] args) {
        templIndex = 0;

        if(args.length > 1) {
            this.filename = args[1];
            readFile(args[1]);
        }

        if(content == null) {
            content = new StringBuffer[16];
            content[0] = new StringBuffer();
        }

        cY = cX = scrollY = 0;
        out.focus();
        out.enableCursor();
        writeScreen();

        Cursor.directPrintString("Esc: Write and Quit", 0, Terminal.ROWS - 2, Color.mix(Color.GRAY | Color.BRIGHT, Color.GRAY));
        setDone(false);
    }

    private void updateCursorPos() {
        if(cY - scrollY >= Terminal.ROWS - 1) {
            scrollY = cY - (Terminal.ROWS - 2);
            writeScreen();
        } else if(cY < scrollY) {
            scrollY = cY;
            writeScreen();
        }

        out.moveCursor(cX + templIndex + 2, cY - scrollY);
    }

    private void deleteCharAtCurrentPosition() {
        int i;
        if(cX > 0) {
            cX--;
            content[cY].delete(cX);
            out.moveCursorHorizontally(-1);
            for(i = cX; i < content[cY].size(); i++)
                out.print(content[cY].charAt(i));
            out.print(' ');
            out.moveCursorHorizontally(-(content[cY].size()-cX));

        }
    }

    private void addNewLine() {
        int y;

        // increase buffer?
        if(content[content.length-1] != null) {
            StringBuffer[] tmp = new StringBuffer[content.length * 2];
            for (int i = 0; i < content.length; i++) {
                tmp[i] = content[i];
            }

            content = tmp;
        }

        // move everything below cY down
        y = cY;
        while(content[++y] != null);
        while(y > cY+1) {
            content[y] = content[y-1];
            y--;
        }

        // cut current line (if newline happens in the middle of a string)
        content[cY+1] = new StringBuffer(content[cY].toString().substring(cX));
        content[cY].deleteFrom(cX);

        // move cursor
        cY++;
        cX = 0;

        // does line numbering introduce more digits?
        if(cY == 10 || cY == 100 || cY == 1000) {
            templIndex++;
        }

        writeScreen();
    }

    private void writeFile() {
        int size, y;
        StringBuffer result;

        size = 0;
        for(y = 0; content[y] != null; y++) {
            size += content[y].size() + 1;
        }

        result = new StringBuffer(size-1);
        result.append(content[0]);
        for(y = 1; content[y] != null; y++) {
            result.append('\n').append(content[y]);
        }

        // we assume filename != null
        FileSystem.overwrite(new File(filename, result.getCharArr()));
    }


    @Override
    public void run() {
        if(KeyboardController.getKeyBuffer().isEmpty())
            return;

        KeyEvent k = KeyboardController.getKeyBuffer().pop();

        if (ASCII.isPrintable(k.code)) {

            if (k.code == ASCII.NEW_LINE) {
                addNewLine();

            } else {
                content[cY].append((char) k.code);
                out.print((char) k.code);
                cX++;
                if(cX == 75)
                    addNewLine();
            }

        } else if (k.code == ASCII.BACKSPACE) {
            if(cX > 0) {
                deleteCharAtCurrentPosition();
                if(k.superKey) {
                    while(cX > 0)
                        deleteCharAtCurrentPosition();
                } else if(k.alt) {
                    while(cX > 0 && content[cY].charAt(cX) != ' ')
                        deleteCharAtCurrentPosition();
                }

            } else if(cY > 0) {
                int y = cY;
                cX = content[cY-1].size();
                content[cY-1].append(content[cY]);

                while(y < (content.length - 1) &&  content[y] != null) {
                    content[y] = content[y+1];
                    y++;
                }
                content[y] = null;

                cY--;
                if(scrollY > 0 && Terminal.ROWS - 1 > content.length - scrollY)
                    scrollY--;

                writeScreen();
            }
        } else if(k.code == ASCII.ARROW_UP && cY > 0) {
            cY--;
            cX = Math.min(content[cY].size(), cX);
        } else if(k.code == ASCII.ARROW_DOWN && cY < content.length-1 && content[cY+1] != null) {
            cY++;
            cX = Math.min(content[cY].size(), cX);
        } else if(k.code == ASCII.ARROW_LEFT && cX > 0) {
            if(k.superKey)
                cX = 0;
            else if(k.alt) {
                cX--;
                // skip whitespace, then word
                while(cX > 0 && content[cY].charAt(cX) == ' ') { cX--; }
                while(cX > 0 && content[cY].charAt(cX-1) != ' ') { cX--; }

            } else
                cX--;
        } else if(k.code == ASCII.ARROW_RIGHT && cX < content[cY].size()) {
            if(k.superKey)
                cX = content[cY].size();
            else if(k.alt) {
                cX++;
                // skip word, then whitespace
                while(cX < content[cY].size() && content[cY].charAt(cX) != ' ') { cX++; }
                while(cX < content[cY].size() && content[cY].charAt(cX) == ' ') { cX++; }
            } else
                cX++;

        } else if(k.code == ASCII.ESC) {
            writeFile();
            content = null;
            setDone(true);
        }

        updateCursorPos();
    }


}
