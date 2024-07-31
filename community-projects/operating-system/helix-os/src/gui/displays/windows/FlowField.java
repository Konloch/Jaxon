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

public class FlowField extends Window
{
	
	FlowFieldView _flowFieldView;
	FlowFieldControls _controls;
	
	TextField _txtNoiseScale;
	TextField _txtParticlesCount;
	
	private static final String BTN_INC_SCALE = "Btn_Inc_Scale";
	private static final String BTN_DEC_SCALE = "Btn_Dec_Scale";
	private static final String BTN_INC_PARTICLES = "Btn_Inc_Count";
	private static final String BTN_DEC_PARTICLES = "Btn_Dec_Count";
	
	public FlowField(String title, int x, int y, int width, int height)
	{
		super(title, x, y, width, height, true);
		_controls = new FlowFieldControls();
		float spaceForControls = 0.4f;
		int flowFieldWidth = (int) (contentWidth * (1 - spaceForControls));
		int controlsWidth = (int) (contentWidth * spaceForControls);
		_flowFieldView = new FlowFieldView(0, 0, flowFieldWidth, contentHeight, _controls);
		
		int controlsStartX = flowFieldWidth;
		int fg = Kernel.Display.rgb(255, 255, 255);
		int bg = Kernel.Display.rgb(0, 0, 0);
		Font9x16 font = Font9x16.INSTANCE;
		int fontHeight = font.height();
		
		int buttonLeftX = controlsStartX + 10;
		int buttonRightX = controlsStartX + controlsWidth - 20;
		int buttonsSize = 20;
		
		int textFieldStartX = buttonLeftX + buttonsSize + 10;
		
		_txtNoiseScale = new TextField(textFieldStartX, 30, 200, fontHeight, 0, 1, 0, fg, bg, false, font);
		
		addWidget(new BitmapButton(BTN_DEC_SCALE, buttonLeftX, _txtNoiseScale.y - 3, buttonsSize, ArrowLeftIcon.load(), this));
		
		addWidget(new BitmapButton(BTN_INC_SCALE, buttonRightX, _txtNoiseScale.y - 3, buttonsSize, ArrowRightIcon.load(), this));
		
		_txtParticlesCount = new TextField(textFieldStartX, 30 * 2, 200, fontHeight, 0, 1, 0, fg, bg, false, font);
		
		addWidget(new BitmapButton(BTN_DEC_PARTICLES, buttonLeftX, _txtNoiseScale.y - 3 + 30, buttonsSize, ArrowLeftIcon.load(), this));
		
		addWidget(new BitmapButton(BTN_INC_PARTICLES, buttonRightX, _txtNoiseScale.y - 3 + 30, buttonsSize, ArrowRightIcon.load(), this));
	}
	
	@Override
	public void drawContent()
	{
		renderTarget.rectangle(contentRelativeX, contentRelativeY, contentWidth, contentHeight, 0);
		
		_flowFieldView.draw();
		renderTarget.blit(contentRelativeX, contentRelativeY, _flowFieldView.renderTarget, false);
		
		_txtNoiseScale.ClearText();
		_txtNoiseScale.Write("Noise Scale: ".append((int) _controls.noiseScale));
		_txtNoiseScale.draw();
		renderTarget.blit(_txtNoiseScale.x, _txtNoiseScale.y, _txtNoiseScale.renderTarget, false);
		
		_txtParticlesCount.ClearText();
		_txtParticlesCount.Write("Particles: ".append(_controls.particleAmount));
		_txtParticlesCount.draw();
		renderTarget.blit(_txtParticlesCount.x, _txtParticlesCount.y, _txtParticlesCount.renderTarget, false);
	}
	
	@Override
	public void update()
	{
		_flowFieldView.Update();
		_needsRedraw = true;
	}
	
	@Override
	public boolean onButtonClicked(ButtonClickedEventArgs button)
	{
		if (super.onButtonClicked(button))
			return true;
		
		if (button.buttonName == BTN_INC_SCALE)
		{
			_controls.noiseScale *= 1.1f;
			return true;
		}
		else if (button.buttonName == BTN_DEC_SCALE)
		{
			_controls.noiseScale /= 1.1f;
			return true;
		}
		else if (button.buttonName == BTN_INC_PARTICLES)
		{
			_controls.particleAmount = (int) (_controls.particleAmount * 1.3f);
			return true;
		}
		else if (button.buttonName == BTN_DEC_PARTICLES)
		{
			_controls.particleAmount = (int) (_controls.particleAmount / 1.3f);
			return true;
		}
		
		return false;
	}
}
