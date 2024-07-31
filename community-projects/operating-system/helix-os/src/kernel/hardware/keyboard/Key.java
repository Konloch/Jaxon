package kernel.hardware.keyboard;

public class Key
{
	public static final char NONE = 0;
	public static final char SPACE = ' ';
	public static final char EXCLAMATION_MARK = '!';
	public static final char QUOTATION_MARK = '"';
	public static final char POUND_KEY = '#';
	public static final char DOLLAR_KEY = '$';
	public static final char PERCENT_KEY = '%';
	public static final char AMPERSAND = '&';
	public static final char SINGLE_QUOTE = '\'';
	public static final char LPAREN = '(';
	public static final char RPAREN = ')';
	public static final char ASTERISK = '*';
	public static final char PLUS = '+';
	public static final char COMMA = ',';
	public static final char MINUS = '-';
	public static final char DOT = '.';
	public static final char SLASH = '/';
	public static final char ZERO = '0';
	public static final char ONE = '1';
	public static final char TWO = '2';
	public static final char THREE = '3';
	public static final char FOUR = '4';
	public static final char FIVE = '5';
	public static final char SIX = '6';
	public static final char SEVEN = '7';
	public static final char EIGHT = '8';
	public static final char NINE = '9';
	public static final char COLON = ':';
	public static final char SEMICOLON = ';';
	public static final char LESS_THAN = '<';
	public static final char EQUALS = '=';
	public static final char GREATER_THAN = '>';
	public static final char QUESTION_MARK = '?';
	public static final char AT_SIGN = '@';
	public static final char A = 'A';
	public static final char B = 'B';
	public static final char C = 'C';
	public static final char D = 'D';
	public static final char E = 'E';
	public static final char F = 'F';
	public static final char G = 'G';
	public static final char H = 'H';
	public static final char I = 'I';
	public static final char J = 'J';
	public static final char K = 'K';
	public static final char L = 'L';
	public static final char M = 'M';
	public static final char N = 'N';
	public static final char O = 'O';
	public static final char P = 'P';
	public static final char Q = 'Q';
	public static final char R = 'R';
	public static final char S = 'S';
	public static final char T = 'T';
	public static final char U = 'U';
	public static final char V = 'V';
	public static final char W = 'W';
	public static final char X = 'X';
	public static final char Y = 'Y';
	public static final char Z = 'Z';
	public static final char LSQUARE = '[';
	public static final char BACKSLASH = '\\';
	public static final char RSQUARE = ']';
	public static final char CARET = '^';
	public static final char UNDERSCORE = '_';
	public static final char GRAVE_ACCENT = '`';
	public static final char a = 'a';
	public static final char b = 'b';
	public static final char c = 'c';
	public static final char d = 'd';
	public static final char e = 'e';
	public static final char f = 'f';
	public static final char g = 'g';
	public static final char h = 'h';
	public static final char i = 'i';
	public static final char j = 'j';
	public static final char k = 'k';
	public static final char l = 'l';
	public static final char m = 'm';
	public static final char n = 'n';
	public static final char o = 'o';
	public static final char p = 'p';
	public static final char q = 'q';
	public static final char r = 'r';
	public static final char s = 's';
	public static final char t = 't';
	public static final char u = 'u';
	public static final char v = 'v';
	public static final char w = 'w';
	public static final char x = 'x';
	public static final char y = 'y';
	public static final char z = 'z';
	public static final char LCURLY = '{';
	public static final char PIPE = '|';
	public static final char RCURLY = '}';
	public static final char TILDE = '~';
	public static final char DELETE = 0x7F;
	public static final char LSHIFT = 0x100;
	public static final char RSHIFT = 0x101;
	public static final char LCTRL = 0x102;
	public static final char RCTRL = 0x103;
	public static final char LALT = 0x104;
	public static final char RALT = 0x105;
	public static final char ENTER = '\n';
	public static final char BACKSPACE = '\b';
	public static final char TAB = '\t';
	public static final char CAPSLOCK = 0x109;
	public static final char ESCAPE = 0x10A;
	public static final char SUPER = 0x10B;
	public static final char WINDOWS = SUPER;
	public static final char ARROW_UP = 0x10D;
	public static final char ARROW_DOWN = 0x10E;
	public static final char ARROW_LEFT = 0x10F;
	public static final char ARROW_RIGHT = 0x110;
	public static final char PAGE_UP = 0x111;
	public static final char PAGE_DOWN = 0x112;
	public static final char INSERT = 0x113;
	public static final char HOME = 0x114;
	public static final char END = 0x115;
	public static final char PRINT_SCREEN = 0x116;
	public static final char SCROLLLOCK = 0x117;
	public static final char PAUSE = 0x118;
	public static final char NUMLOCK = 0x119;
	// public static final int ALT_GR = 0x11A;
	public static final char MENU = 0x11B;
	public static final char F1 = 0x140;
	public static final char F2 = 0x141;
	public static final char F3 = 0x142;
	public static final char F4 = 0x143;
	public static final char F5 = 0x144;
	public static final char F6 = 0x145;
	public static final char F7 = 0x146;
	public static final char F8 = 0x147;
	public static final char F9 = 0x148;
	public static final char F10 = 0x149;
	public static final char F11 = 0x14A;
	public static final char F12 = 0x14B;
	
	public static final char SECTION = Key.NONE;
	public static final char SHARP_S = Key.S;
	public static final char EURO_SIGN = Key.DOLLAR_KEY;
	public static final char AGUE_ACCENT = Key.GRAVE_ACCENT;
	
	public static char ascii(char key)
	{
		if (key >= 0x20 && key <= 0x7E)
			return key;
		
		return 0;
	}
	
	public static String name(char key)
	{
		if (key >= 0x20 && key <= 0x7E)
		{
			char c = (char) key;
			byte[] chars = new byte[1];
			chars[0] = (byte) c;
			return new String(chars);
		}
		
		switch (key)
		{
			case Key.LSHIFT:
				return "LSHIFT";
			case Key.RSHIFT:
				return "RSHIFT";
			case Key.LCTRL:
				return "LCTRL";
			case Key.RCTRL:
				return "RCTRL";
			case Key.LALT:
				return "LALT";
			case Key.RALT:
				return "RALT";
			case Key.ENTER:
				return "ENTER";
			case Key.BACKSPACE:
				return "BACKSPACE";
			case Key.TAB:
				return "TAB";
			case Key.CAPSLOCK:
				return "CAPSLOCK";
			case Key.ESCAPE:
				return "ESCAPE";
			case Key.SUPER:
				return "SUPER";
			case Key.ARROW_UP:
				return "ARROW_UP";
			case Key.ARROW_DOWN:
				return "ARROW_DOWN";
			case Key.ARROW_LEFT:
				return "ARROW_LEFT";
			case Key.ARROW_RIGHT:
				return "ARROW_RIGHT";
			case Key.PAGE_UP:
				return "PAGE_UP";
			case Key.PAGE_DOWN:
				return "PAGE_DOWN";
			case Key.INSERT:
				return "INSERT";
			case Key.HOME:
				return "HOME";
			case Key.END:
				return "END";
			case Key.PRINT_SCREEN:
				return "PRINT_SCREEN";
			case Key.SCROLLLOCK:
				return "SCROLLLOCK";
			case Key.PAUSE:
				return "PAUSE";
			case Key.NUMLOCK:
				return "NUMLOCK";
			// case Key.ALT_GR: return "ALT_GR";
			case Key.MENU:
				return "MENU";
			case Key.DELETE:
				return "DELETE";
			case Key.F1:
				return "F1";
			case Key.F2:
				return "F2";
			case Key.F3:
				return "F3";
			case Key.F4:
				return "F4";
			case Key.F5:
				return "F5";
			case Key.F6:
				return "F6";
			case Key.F7:
				return "F7";
			case Key.F8:
				return "F8";
			case Key.F9:
				return "F9";
			case Key.F10:
				return "F10";
			case Key.F11:
				return "F11";
			case Key.F12:
				return "F12";
			default:
				return "UNKNOWN (".append(Integer.toString(key, 10)).append(")");
		}
	}
}
