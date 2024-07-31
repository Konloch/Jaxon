package gui.displays.windows;

import formats.fonts.Font9x16;
import gui.Window;
import gui.components.FlowFieldControls;
import gui.components.FlowFieldView;
import gui.components.TextField;
import gui.components.button.BitmapButton;
import gui.components.button.ButtonClickedEventArgs;
import gui.images.ArrowLeftIcon;
import gui.images.ArrowRightIcon;
import kernel.Kernel;

public class FlowField extends Window {

    FlowFieldView _flowFieldView;
    FlowFieldControls _controls;

    TextField _txtNoiseScale;
    TextField _txtParticlesCount;

    private static final String BTN_INC_SCALE = "Btn_Inc_Scale";
    private static final String BTN_DEC_SCALE = "Btn_Dec_Scale";
    private static final String BTN_INC_PARTICLES = "Btn_Inc_Count";
    private static final String BTN_DEC_PARTICLES = "Btn_Dec_Count";

    public FlowField(
            String title,
            int x,
            int y,
            int width,
            int height) {
        super(title, x, y, width, height, true);
        _controls = new FlowFieldControls();
        float spaceForControls = 0.4f;
        int flowFieldWidth = (int) (ContentWidth * (1 - spaceForControls));
        int controlsWidth = (int) (ContentWidth * spaceForControls);
        _flowFieldView = new FlowFieldView(0, 0, flowFieldWidth, ContentHeight, _controls);

        int controlsStartX = flowFieldWidth;
        int fg = Kernel.Display.Rgb(255, 255, 255);
        int bg = Kernel.Display.Rgb(0, 0, 0);
        Font9x16 font = Font9x16.Instance;
        int fontHeight = font.Height();

        int buttonLeftX = controlsStartX + 10;
        int buttonRightX = controlsStartX + controlsWidth - 20;
        int buttonsSize = 20;

        int textFieldStartX = buttonLeftX + buttonsSize + 10;

        _txtNoiseScale = new TextField(
                textFieldStartX, 30,
                200, fontHeight,
                0, 1, 0,
                fg, bg, false, font);

        AddWidget(new BitmapButton(BTN_DEC_SCALE,
                buttonLeftX, _txtNoiseScale.Y - 3,
                buttonsSize, ArrowLeftIcon.Load(), this));

        AddWidget(new BitmapButton(BTN_INC_SCALE,
                buttonRightX, _txtNoiseScale.Y - 3,
                buttonsSize, ArrowRightIcon.Load(), this));

        _txtParticlesCount = new TextField(
                textFieldStartX, 30 * 2,
                200, fontHeight,
                0, 1, 0,
                fg, bg, false, font);

        AddWidget(new BitmapButton(BTN_DEC_PARTICLES,
                buttonLeftX, _txtNoiseScale.Y - 3 + 30,
                buttonsSize, ArrowLeftIcon.Load(), this));

        AddWidget(new BitmapButton(BTN_INC_PARTICLES,
                buttonRightX, _txtNoiseScale.Y - 3 + 30,
                buttonsSize, ArrowRightIcon.Load(), this));
    }

    @Override
    public void DrawContent() {
        RenderTarget.Rectangle(ContentRelativeX, ContentRelativeY, ContentWidth, ContentHeight, 0);

        _flowFieldView.Draw();
        RenderTarget.Blit(ContentRelativeX, ContentRelativeY, _flowFieldView.RenderTarget, false);

        _txtNoiseScale.ClearText();
        _txtNoiseScale.Write("Noise Scale: ".append((int) _controls.NoiseScale));
        _txtNoiseScale.Draw();
        RenderTarget.Blit(_txtNoiseScale.X, _txtNoiseScale.Y, _txtNoiseScale.RenderTarget, false);

        _txtParticlesCount.ClearText();
        _txtParticlesCount.Write("Particles: ".append(_controls.ParticleAmount));
        _txtParticlesCount.Draw();
        RenderTarget.Blit(_txtParticlesCount.X, _txtParticlesCount.Y, _txtParticlesCount.RenderTarget, false);
    }

    @Override
    public void Update() {
        _flowFieldView.Update();
        _needsRedraw = true;
    }

    @Override
    public boolean OnButtonClicked(ButtonClickedEventArgs button) {
        if (super.OnButtonClicked(button)) {
            return true;
        }

        if (button.ButtonName == BTN_INC_SCALE) {
            _controls.NoiseScale *= 1.1f;
            return true;
        } else if (button.ButtonName == BTN_DEC_SCALE) {
            _controls.NoiseScale /= 1.1f;
            return true;
        } else if (button.ButtonName == BTN_INC_PARTICLES) {
            _controls.ParticleAmount = (int) (_controls.ParticleAmount * 1.3f);
            return true;
        } else if (button.ButtonName == BTN_DEC_PARTICLES) {
            _controls.ParticleAmount = (int) (_controls.ParticleAmount / 1.3f);
            return true;
        }

        return false;
    }
}
