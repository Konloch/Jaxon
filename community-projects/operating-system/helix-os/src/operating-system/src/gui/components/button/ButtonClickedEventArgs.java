package gui.components.button;

import util.IDebug;

public class ButtonClickedEventArgs implements IDebug {
    public String ButtonName;

    public ButtonClickedEventArgs(String buttonName) {
        ButtonName = buttonName;
    }

    @Override
    public String Debug() {
        return "ButtonClickedEventArgs: ".append(ButtonName);
    }
}
