package gui.displays.windows;

import formats.fonts.Font7x8;
import formats.fonts.Font9x16;
import gui.Window;
import gui.components.button.ButtonClickedEventArgs;
import gui.components.button.BitmapButton;
import gui.images.BackgroundBarn;
import gui.images.EditorIcon;
import gui.images.FlowFieldIcon;
import gui.images.InfoIcon;
import kernel.Kernel;
import kernel.display.Bitmap;
import kernel.trace.logging.Logger;

public class Desktop extends Window {
    private Bitmap _background;

    private static String _btnLaunchEditor = "Btn_Launch_Editor";
    private static String _btnLaunchSystemInfo = "Btn_Launch_SystemInfo";
    private static String _btnLaunchFlowField = "Btn_Launch_FlowField";

    public Desktop(
            String title) {
        super(title, 0, 0, Kernel.Display.Width(), Kernel.Display.Height(), false);
        _isDraggable = false;
        _background = BackgroundBarn.Load().Scale(Kernel.Display.Width(), Kernel.Display.Height());

        AddWidget(new BitmapButton(
                _btnLaunchEditor,
                10,
                10,
                60,
                EditorIcon.Load(),
                this));

        AddWidget(new BitmapButton(
                _btnLaunchSystemInfo,
                10,
                100,
                60,
                InfoIcon.Load(),
                this));

        AddWidget(new BitmapButton(
                _btnLaunchFlowField,
                10,
                190,
                60,
                FlowFieldIcon.Load(),
                this));

    }

    public void DrawContent() {
        RenderTarget.Blit(0, 0, _background, false);
        DrawWidgets();
    }

    @Override
    public void Update() {

    }

    @Override
    public boolean OnButtonClicked(ButtonClickedEventArgs event) {
        if (super.OnButtonClicked(event))
            return true;

        if (event.ButtonName == _btnLaunchSystemInfo) {
            Logger.Trace("Desktop", "Launch System Info");
            SystemInfo sysinfo = new SystemInfo(
                    "System Info",
                    40,
                    40,
                    400,
                    400,
                    8,
                    0,
                    2,
                    Font7x8.Instance);
            Kernel.WindowManager.AddWindow(sysinfo);
            return true;
        } else if (event.ButtonName == _btnLaunchEditor) {
            Editor editor = new Editor(
                    "Editor",
                    40,
                    40,
                    600,
                    400,
                    8,
                    0,
                    2,
                    Font9x16.Instance);
            Kernel.WindowManager.AddWindow(editor);
            return true;
        } else if (event.ButtonName == _btnLaunchFlowField) {
            FlowField flowField = new FlowField(
                    "Flow Field",
                    40,
                    40,
                    800,
                    600);
            Kernel.WindowManager.AddWindow(flowField);
            return true;
        }
        return false;
    }
}
