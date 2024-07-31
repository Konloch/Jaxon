package kernel.hardware.keyboard;

import util.IDebug;
import util.StrBuilder;

public class KeyEvent implements IDebug {
    public char Key;
    public boolean IsDown;

    public KeyEvent(char key, boolean isDown) {
        Key = key;
        IsDown = isDown;
    }

    public KeyEvent() {
        Key = 0;
        IsDown = false;
    }

    @Override
    public String Debug() {
        return new StrBuilder()
                .Append("KeyEvent {")
                .Append("Key: ").Append(kernel.hardware.keyboard.Key.Name(Key))
                .Append(", IsDown: ").Append(IsDown)
                .Append("}")
                .toString();
    }

}
