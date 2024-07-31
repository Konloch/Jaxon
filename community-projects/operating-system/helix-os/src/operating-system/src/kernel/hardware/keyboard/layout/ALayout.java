package kernel.hardware.keyboard.layout;

public abstract class ALayout {
    public abstract char LogicalKey(int physicalKey, boolean shift, boolean alt);
}
