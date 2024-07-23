package apps.tetris;

public class JTetromino extends Tetromino
{
	public JTetromino(byte color)
	{
		super(color);
		this.squares[0] = new Square(-1, 0);
		this.squares[1] = new Square(1, -1);
		this.squares[2] = new Square(0, -1);
		this.squares[3] = new Square(-1, -1);
	}
}