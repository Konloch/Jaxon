package os.filesystem;

import devices.StaticV24;

public class FileSystem {

    protected static File[] files;
    protected static int filePtr;

    static {
        files = new File[16];
    }

    @SJC.Inline
    public static boolean exists(String filename) {
        return get(filename) != null;
    }

    @SJC.Inline
    private static int indexOf(String filename) {
        int i;
        for(i = 0; i < filePtr; i++) {
            if(filename.equals(files[i].filename))
                return i;
        }
        return -1;
    }

    public static boolean add(File f) {
        int i;
        File[] tmp;


        if(exists(f.filename))
            return false;

        if(filePtr >= files.length) {
            tmp = new File[files.length * 2];
            for (i = 0; i < filePtr; i++) {
                tmp[i] = files[i];
            }

            files = tmp;
        }

        files[filePtr++] = f;
        return true;
    }

    public static File get(String filename) {
        int i = indexOf(filename);
        if(i >= 0)
            return files[i];
        return null;
    }

    public static boolean delete(String filename) {
        int i, j;
        i = indexOf(filename);
        if(i < 0)
            return false;

        for(j = i+1; j < filePtr; j++) {
            files[j-1] = files[j];
        }
        files[--filePtr] = null;
        return true;
    }

    public static boolean rename(String from, String to) {
        int i;
        if(exists(to))
            return false;

        i = indexOf(from);
        if(i < 0)
            return false;

        MAGIC.assign(files[i].filename, to);
        return true;
    }

    public static void overwrite(File file) {
        delete(file.filename);
        add(file);
    }

    @SJC.Inline
    public static FileTraverser traverse() {
        return new FileTraverser();
    }

    public static class FileTraverser {
        private static int index;

        public FileTraverser() {
            index = 0;
        }

        public File next() {
            if(index >= filePtr)
                return null;
            return files[index++];
        }
    }
}
