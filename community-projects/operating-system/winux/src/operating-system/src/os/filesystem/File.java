package os.filesystem;

public class File {

    public final String filename;
    public char[] content;

    public File(String filename, char[] content) {
        this.filename = filename;
        this.content = content;
    }

    public int size() {
        return content.length;
    }

    public int countLines() {
        int result = 1;
        for(char c : content) {
            if(c == '\n')
                result++;
        }
        return result;
    }
}
