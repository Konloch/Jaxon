package video;

/**
 * Represents a character on the screen.
 *
 * @see video.VideoMemory
 */
public class VideoChar extends STRUCT
{
	
	/**
	 * The ASCII code of the character.
	 */
	public byte ascii;
	
	/**
	 * The color code of the character.
	 *
	 * <p>
	 * <table cellpadding=3 border=1 width=“100%”>
	 * <tr align=“center”>
	 * <th colspan=8>Byte</th>
	 * </tr>
	 * <tr align=“center”>
	 * <td>Bit</td>
	 * <td>7</td>
	 * <td>6</td>
	 * <td>5</td>
	 * <td>4</td>
	 * <td>3</td>
	 * <td>2</td>
	 * <td>1</td>
	 * <td>0</td>
	 * </tr>
	 * <tr align=“center”>
	 * <td>Function</td>
	 * <td>Blink</td>
	 * <td colspan=“3”>Background</td>
	 * <td>Bright</td>
	 * <td colspan=“3”>Foreground</td>
	 * </tr>
	 * </table>
	 * </p>
	 * <p>
	 * <table cellpadding=3 border=1 width=“100%”>
	 * <tr align=“center”>
	 * <th colspan=8>color</th>
	 * </tr>
	 * <tr align=“center”>
	 * <td>No.</td>
	 * <td>0</td>
	 * <td>1</td>
	 * <td>2</td>
	 * <td>3</td>
	 * <td>4</td>
	 * <td>5</td>
	 * <td>6</td>
	 * <td>7</td>
	 * </tr>
	 * <tr align=“center”>
	 * <td>Color</td>
	 * <td>Black</td>
	 * <td>Blue</td>
	 * <td>Green</td>
	 * <td>Turquoise</td>
	 * <td>Red</td>
	 * <td>Violet</td>
	 * <td>Brown</td>
	 * <td>Grey</td>
	 * </tr>
	 * </table>
	 * </p>
	 */
	public byte color;
}
