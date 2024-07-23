package os.keyboard;

public class KeyEvent {

    public final int code;
    public final boolean alt;
    public final boolean ctrl;
    public final boolean superKey;

    public KeyEvent(int code, boolean alt, boolean ctrl, boolean superKey) {
        this.code = code;
        this.alt = alt;
        this.ctrl = ctrl;
        this.superKey = superKey;
    }
}
