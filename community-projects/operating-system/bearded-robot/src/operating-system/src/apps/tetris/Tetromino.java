package apps.tetris;

import java.util.Random;

public abstract class Tetromino
{
	private static Random random = null;
	protected final Square[] squares = new Square[4];
	private final byte color;
	
	public static Tetromino create()
	{
		if (random == null)
			random = new Random();
		
		Tetromino tetromino = null;
		switch (random.nextInt(7))
		{
			case 0:
				tetromino = new ITetromino((byte) (0x20 + random.nextInt(0x48)));
				break;
			case 1:
				tetromino = new JTetromino((byte) (0x20 + random.nextInt(0x48)));
				break;
			case 2:
				tetromino = new LTetromino((byte) (0x20 + random.nextInt(0x48)));
				break;
			case 3:
				tetromino = new OTetromino((byte) (0x20 + random.nextInt(0x48)));
				break;
			case 4:
				tetromino = new STetromino((byte) (0x20 + random.nextInt(0x48)));
				break;
			case 5:
				tetromino = new TTetromino((byte) (0x20 + random.nextInt(0x48)));
				break;
			case 6:
				tetromino = new ZTetromino((byte) (0x20 + random.nextInt(0x48)));
				break;
		}
		return tetromino;
	}
	
	protected Tetromino(byte color)
	{
		this.color = color;
	}
	
	public void rotate()
	{
		for (int i = 0; i < squares.length; i++)
		{
			int tmp = this.squares[i].x;
			MAGIC.assign(this.squares[i].x, this.squares[i].y);
			MAGIC.assign(this.squares[i].y, tmp * -1);
		}
	}
	
	public Square square(int index)
	{
		return this.squares[index % this.squares.length];
	}
	
	public byte color()
	{
		return this.color;
	}
}
