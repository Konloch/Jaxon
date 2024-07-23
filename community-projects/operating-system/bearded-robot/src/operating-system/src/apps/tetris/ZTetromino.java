package apps.tetris;

public class ZTetromino extends Tetromino
{
	public ZTetromino(byte color)
	{
		super(color);
		this.squares[0] = new Square(0, 0);
		this.squares[1] = new Square(-1, 0);
		this.squares[2] = new Square(0, -1);
		this.squares[3] = new Square(1, -1);
	}
}
