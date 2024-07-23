package apps.tetris;

import bios.BIOS;
import keyboard.Keyboard;
import keyboard.KeyboardListener;
import scheduling.Task;
import timer.Timer;

public class Tetris extends Task
{
	private static final int ScreenWidth = 320;
	private static final int ScreenHeight = 200;
	private static final int XSquareNumber = 10;
	private static final int YSquareNumber = 20;
	private static final int Delay = 1000;
	
	private final byte[][] pixelCache = new byte[ScreenWidth][ScreenHeight];
	private final Listener listener = new Listener(this);
	
	private long lastChangedTime = 0;
	
	private Field field;
	
	private Tetromino currentTetromino = null;
	
	private int tetrominoX = 0;
	private int tetrominoY = 0;
	
	private int level = 1;
	private int score = 0;
	private int rows = 0;
	private boolean isGameOver = false;
	
	@Override
	protected void onStart()
	{
		// Switch to graphics mode.
		BIOS.regs.EAX = 0x0013;
		BIOS.rint(0x10);
		
		Keyboard.initstance().addListener(this.listener);
		
		this.field = new Field(XSquareNumber, YSquareNumber);
		this.currentTetromino = null;
		this.tetrominoX = 0;
		this.tetrominoY = 0;
		this.lastChangedTime = 0;
		this.level = 1;
		this.score = 0;
		this.rows = 0;
		this.isGameOver = false;
	}
	
	@Override
	protected void onStop()
	{
		Keyboard.initstance().removeListener(this.listener);
		
		// Switch back to text mode
		BIOS.regs.EAX = 0x0003;
		BIOS.rint(0x10);
	}
	
	@Override
	protected void onSchedule()
	{
		drawGui();
		drawScore();
		drawLevel();
		
		// Draw playing field
		for (int i = 0; i < XSquareNumber; i++)
		{
			for (int j = 0; j < YSquareNumber; j++)
			{
				byte color;
				if (this.field.isSet(i, j))
					color = this.field.colorAt(i, j);
				else
				{
					color = 0;
					if (currentTetromino != null)
					{
						for (int k = 0; k < currentTetromino.squares.length; k++)
						{
							Square square = currentTetromino.squares[k];
							if (tetrominoX + square.x == i && tetrominoY + square.y == j)
								color = currentTetromino.color();
						}
					}
				}
				
				drawSquare(i, j, color);
			}
		}
		
		// Automatic fall of the Tetromino
		if ((Timer.getUpTime() - this.lastChangedTime) > (Delay - (this.level - 1) * 5))
		{
			this.lastChangedTime = Timer.getUpTime();
			stepDown();
		}
		
		// Calculation of the level and points for completed rows
		int r = this.field.cleanUp();
		int f;
		switch (r)
		{
			case 1:
				f = 40;
				break;
			case 2:
				f = 100;
				break;
			case 3:
				f = 300;
				break;
			case 4:
				f = 1200;
				break;
			default:
				f = 0;
				break;
		}
		this.score += f * this.level;
		this.rows += r;
		this.level = (this.rows / 10) + 1;
		
		if (isGameOver)
			drawGameOver();
		
		// Transfer the pixels to the video memory
		flushPixelCache();
	}
	
	private void flushPixelCache()
	{
		for (int i = 0; i < ScreenWidth; i++)
		{
			for (int j = 0; j < ScreenHeight; j++)
				MAGIC.wMem8(0xA0000 + i + j * ScreenWidth, pixelCache[i][j]);
		}
	}
	
	private boolean stepDown()
	{
		boolean result = false;
		
		if (this.currentTetromino == null)
		{
			this.tetrominoY = YSquareNumber;
			this.tetrominoX = XSquareNumber / 2;
			this.currentTetromino = Tetromino.create();
		}
		
		Tetromino tetromino = this.currentTetromino;
		Square[] squares = tetromino.squares;
		
		for (int i = 0; i < squares.length; i++)
		{
			int x = this.tetrominoX + squares[i].x;
			int y = this.tetrominoY + squares[i].y;
			if (y == 0 || this.field.isSet(x, y - 1))
			{
				finishCurrentTetromino();
				break;
			}
		}
		
		if (this.currentTetromino != null)
		{
			this.tetrominoY--;
			result = true;
		}
		
		return result;
	}
	
	private void stepLeft()
	{
		Tetromino tetromino = this.currentTetromino;
		if (tetromino == null)
			return;
		
		Square[] squares = tetromino.squares;
		boolean isInBound = true;
		
		for (int i = 0; i < squares.length; i++)
		{
			int x = this.tetrominoX + squares[i].x;
			int y = this.tetrominoY + squares[i].y;
			if (x == 0 || this.field.isSet(x - 1, y))
			{
				isInBound = false;
				break;
			}
		}
		
		if (isInBound)
		{
			this.tetrominoX--;
		}
	}
	
	private void stepRight()
	{
		Tetromino tetromino = this.currentTetromino;
		if (tetromino == null)
			return;
		
		Square[] squares = tetromino.squares;
		boolean isInBound = true;
		
		for (int i = 0; i < squares.length; i++)
		{
			int x = this.tetrominoX + squares[i].x;
			int y = this.tetrominoY + squares[i].y;
			if (x == XSquareNumber - 1 || this.field.isSet(x + 1, y))
			{
				isInBound = false;
				break;
			}
		}
		
		if (isInBound)
		{
			this.tetrominoX++;
		}
	}
	
	private void rotate()
	{
		Tetromino tetromino = this.currentTetromino;
		if (tetromino == null)
			return;
		
		Square[] squares = tetromino.squares;
		boolean isInBound = true;
		
		int x0 = this.tetrominoX + squares[0].y;
		int x1 = this.tetrominoX + squares[1].y;
		int x2 = this.tetrominoX + squares[2].y;
		int x3 = this.tetrominoX + squares[3].y;
		int y0 = this.tetrominoY + squares[0].x * -1;
		int y1 = this.tetrominoY + squares[1].x * -1;
		int y2 = this.tetrominoY + squares[2].x * -1;
		int y3 = this.tetrominoY + squares[3].x * -1;
		
		if (x0 < 0 || x0 >= XSquareNumber || y0 < 0 || y0 >= YSquareNumber || this.field.isSet(x0, y0))
			isInBound = false;
		else if (x1 < 0 || x1 >= XSquareNumber || y1 < 0 || y1 >= YSquareNumber || this.field.isSet(x1, y1))
			isInBound = false;
		else if (x2 < 0 || x2 >= XSquareNumber || y2 < 0 || y2 >= YSquareNumber || this.field.isSet(x2, y2))
			isInBound = false;
		else if (x3 < 0 || x3 >= XSquareNumber || y3 < 0 || y3 >= YSquareNumber || this.field.isSet(x3, y3))
			isInBound = false;
		
		if (isInBound)
			tetromino.rotate();
	}
	
	private void finishCurrentTetromino()
	{
		if (this.currentTetromino != null)
		{
			Tetromino tetromino = this.currentTetromino;
			Square[] squares = tetromino.squares;
			for (int i = 0; i < squares.length; i++)
			{
				int x = this.tetrominoX + squares[i].x;
				int y = this.tetrominoY + squares[i].y;
				// Prüfe, ob im Feld
				if (y < YSquareNumber)
					this.field.set(x, y, tetromino.color());
				else
					this.isGameOver = true;
			}
			
			this.currentTetromino = null;
		}
	}
	
	private static class Listener extends KeyboardListener
	{
		
		private final Tetris tetris;
		
		public Listener(Tetris tetris)
		{
			this.tetris = tetris;
		}
		
		@Override
		public void onKeyDown(int value, int keyCode, boolean isChar, int flags)
		{
			if (!isChar)
			{
				switch (value)
				{
					case Keyboard.DOWN:
						if (this.tetris.stepDown())
							this.tetris.score++;
						
						break;
					case Keyboard.LEFT:
						this.tetris.stepLeft();
						break;
					case Keyboard.RIGHT:
						this.tetris.stepRight();
						break;
					case Keyboard.UP:
						this.tetris.rotate();
						break;
					default:
						break;
				}
			}
		}
		
		@Override
		public void onKeyUp(int value, int keyCode, boolean isChar, int flags)
		{
		}
	}
	
	// ASSISTANCE METHODS *******************************************************************************
	
	private int pixelOffset(int x, int y)
	{
		return x + y * ScreenWidth;
	}
	
	private void draw0(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || j >= 20 || i < 5 || i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw1(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 && i < 5)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (i >= 5 && i < 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 20)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw2(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || j >= 20)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 10 && j < 15)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j < 10 && i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 15 && i < 5)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw3(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || j >= 20)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 10 && j < 15)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw4(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 10 && i < 5)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 10 && j < 15)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw5(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || j >= 20)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 10 && j < 15)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 15 && i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j < 10 && i < 5)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw6(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || j >= 20)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 10 && j < 15)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 15 && i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (i < 5)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw7(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw8(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || j >= 20 || i < 5 || i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 10 && j < 15)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void draw9(int x, int y)
	{
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 25; j++)
			{
				if (j < 5 || j >= 20)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j >= 10 && j < 15)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (i >= 10)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
				if (j < 10 && i < 5)
					this.pixelCache[x + i][y + j] = (byte) 0x28;
			}
		}
	}
	
	private void drawDigit(int x, int y, int digit)
	{
		switch (digit)
		{
			case 0:
				draw0(x, y);
				break;
			case 1:
				draw1(x, y);
				break;
			case 2:
				draw2(x, y);
				break;
			case 3:
				draw3(x, y);
				break;
			case 4:
				draw4(x, y);
				break;
			case 5:
				draw5(x, y);
				break;
			case 6:
				draw6(x, y);
				break;
			case 7:
				draw7(x, y);
				break;
			case 8:
				draw8(x, y);
				break;
			case 9:
				draw9(x, y);
				break;
		}
	}
	
	private void drawScore()
	{
		int scr = this.score;
		for (int i = 8; i > 0; i--)
		{
			int digit = scr % 10;
			scr /= 10;
			drawDigit(130 + 20 * i, 50, digit);
		}
	}
	
	private void drawLevel()
	{
		int lv = this.level;
		for (int i = 8; i > 0; i--)
		{
			int digit = lv % 10;
			lv /= 10;
			drawDigit(130 + 20 * i, 130, digit);
		}
	}
	
	private void drawGui()
	{
		// Hintergrund
		for (int i = 0; i < 30; i++)
		{
			for (int j = 0; j < 200; j++)
			{
				pixelCache[i][j] = (byte) 0x68;
			}
		}
		for (int i = 130; i < 320; i++)
		{
			for (int j = 0; j < 200; j++)
			{
				pixelCache[i][j] = (byte) 0x68;
			}
		}
		
		// Beschriftung
		for (int i = 150; i < 320; i++)
		{
			for (int j = 0; j < 200; j++)
			{
				// SCORE
				if (j >= 20 && j < 25)
				{
					if ((i >= 150 && i < 165) || (i >= 170 && i < 185) || (i >= 190 && i < 205) || (i >= 210 && i < 225) || (i >= 230 && i < 245))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 25 && j < 30)
				{
					if ((i >= 150 && i < 155) || (i >= 170 && i < 175) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 215) || (i >= 220 && i < 225) || (i >= 230 && i < 235))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 30 && j < 35)
				{
					if ((i >= 150 && i < 165) || (i >= 170 && i < 175) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 225) || (i >= 230 && i < 245))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 35 && j < 40)
				{
					if ((i >= 160 && i < 165) || (i >= 170 && i < 175) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 215) || (i >= 215 && i < 220) || (i >= 230 && i < 235))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 40 && j < 45)
				{
					if ((i >= 150 && i < 165) || (i >= 170 && i < 185) || (i >= 190 && i < 205) || (i >= 210 && i < 215) || (i >= 220 && i < 225) || (i >= 230 && i < 245))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				
				// LEVEL
				if (j >= 100 && j < 105)
				{
					if ((i >= 150 && i < 155) || (i >= 170 && i < 185) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 225) || (i >= 230 && i < 235))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 105 && j < 110)
				{
					if ((i >= 150 && i < 155) || (i >= 170 && i < 175) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 215) || (i >= 230 && i < 235))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 110 && j < 115)
				{
					if ((i >= 150 && i < 155) || (i >= 170 && i < 185) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 225) || (i >= 230 && i < 235))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 115 && j < 120)
				{
					if ((i >= 150 && i < 155) || (i >= 170 && i < 175) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 215) || (i >= 230 && i < 235))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
				if (j >= 120 && j < 125)
				{
					if ((i >= 150 && i < 165) || (i >= 170 && i < 185) || (i >= 195 && i < 200) || (i >= 210 && i < 225) || (i >= 230 && i < 245))
					{
						pixelCache[i][j] = (byte) 0x28;
					}
				}
			}
		}
	}
	
	private void drawSquare(int x, int y, byte color)
	{
		for (int i = 0; i < 10; i++)
		{
			for (int j = 0; j < 10; j++)
			{
				pixelCache[30 + 10 * x + i][10 * YSquareNumber - 10 - 10 * y + j] = color;
			}
		}
	}
	
	private void drawGameOver()
	{
		for (int i = 0; i < 320; i++)
			for (int j = 0; j < 200; j++)
				pixelCache[i][j] = (byte) 0x00;
		
		for (int i = 70; i < 320; i++)
		{
			for (int j = 0; j < 200; j++)
			{
				if (j >= 75 && j < 80)
				{
					if ((i >= 70 && i < 85) || (i >= 95 && i < 100) || (i >= 110 && i < 115) || (i >= 120 && i < 125) || (i >= 130 && i < 145) || (i >= 170 && i < 185) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 225) || (i >= 230 && i < 245))
						pixelCache[i][j] = (byte) 0x28;
				}
				if (j >= 80 && j < 85)
				{
					if ((i >= 70 && i < 75) || (i >= 90 && i < 95) || (i >= 100 && i < 105) || (i >= 110 && i < 125) || (i >= 130 && i < 135) || (i >= 170 && i < 175) || (i >= 180 && i < 185) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 215) || (i >= 230 && i < 235) || (i >= 240 && i < 245))
						pixelCache[i][j] = (byte) 0x28;
				}
				if (j >= 85 && j < 90)
				{
					if ((i >= 70 && i < 85) || (i >= 90 && i < 105) || (i >= 110 && i < 115) || (i >= 120 && i < 125) || (i >= 130 && i < 145) || (i >= 170 && i < 175) || (i >= 180 && i < 185) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 225) || (i >= 230 && i < 245))
						pixelCache[i][j] = (byte) 0x28;
				}
				if (j >= 90 && j < 95)
				{
					if ((i >= 70 && i < 75) || (i >= 80 && i < 85) || (i >= 90 && i < 95) || (i >= 100 && i < 105) || (i >= 110 && i < 115) || (i >= 120 && i < 125) || (i >= 130 && i < 135) || (i >= 170 && i < 175) || (i >= 180 && i < 185) || (i >= 190 && i < 195) || (i >= 200 && i < 205) || (i >= 210 && i < 215) || (i >= 230 && i < 240))
						pixelCache[i][j] = (byte) 0x28;
				}
				if (j >= 95 && j < 100)
				{
					if ((i >= 70 && i < 85) || (i >= 90 && i < 95) || (i >= 100 && i < 105) || (i >= 110 && i < 115) || (i >= 120 && i < 125) || (i >= 130 && i < 145) || (i >= 170 && i < 185) || (i >= 195 && i < 200) || (i >= 210 && i < 225) || (i >= 230 && i < 235) || (i >= 240 && i < 245))
						pixelCache[i][j] = (byte) 0x28;
				}
			}
		}
	}
}
