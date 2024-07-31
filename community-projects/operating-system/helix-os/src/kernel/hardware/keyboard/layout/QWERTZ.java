package kernel.hardware.keyboard.layout;

import kernel.hardware.keyboard.Key;

public class QWERTZ extends ALayout
{
	public static final QWERTZ INSTANCE = new QWERTZ();
	
	@Override
	public char logicalKey(int physicalKey, boolean shift, boolean alt)
	{
		// @formatter:off
		switch (physicalKey)
		{
			//   CODE                  SHIFT ALT   LOWER            UPPER                   ALT
			case 0x01:
				return key(shift, alt, Key.ESCAPE);
			case 0x02:
				return key(shift, alt, Key.ONE, Key.EXCLAMATION_MARK);
			case 0x03:
				return key(shift, alt, Key.TWO, Key.QUOTATION_MARK, Key.TWO);
			case 0x04:
				return key(shift, alt, Key.THREE, Key.SECTION, Key.THREE);
			case 0x05:
				return key(shift, alt, Key.FOUR, Key.DOLLAR_KEY);
			case 0x06:
				return key(shift, alt, Key.FIVE, Key.PERCENT_KEY);
			case 0x07:
				return key(shift, alt, Key.SIX, Key.AMPERSAND);
			case 0x08:
				return key(shift, alt, Key.SEVEN, Key.SLASH, Key.LCURLY);
			case 0x09:
				return key(shift, alt, Key.EIGHT, Key.LPAREN, Key.LSQUARE);
			case 0x0A:
				return key(shift, alt, Key.NINE, Key.RPAREN, Key.RSQUARE);
			case 0x0B:
				return key(shift, alt, Key.ZERO, Key.EQUALS, Key.RCURLY);
			case 0x0C:
				return key(shift, alt, Key.SHARP_S, Key.QUESTION_MARK, Key.BACKSLASH);
			case 0x0D:
				return key(shift, alt, Key.AGUE_ACCENT, Key.GRAVE_ACCENT);
			case 0x0E:
				return key(shift, alt, Key.BACKSPACE);
			case 0x0F:
				return key(shift, alt, Key.TAB);
			case 0x10:
				return key(shift, alt, Key.q, Key.Q, Key.AT_SIGN);
			case 0x11:
				return key(shift, alt, Key.w, Key.W);
			case 0x12:
				return key(shift, alt, Key.e, Key.E, Key.EURO_SIGN);
			case 0x13:
				return key(shift, alt, Key.r, Key.R);
			case 0x14:
				return key(shift, alt, Key.t, Key.T);
			case 0x15:
				return key(shift, alt, Key.y, Key.Y);
			case 0x16:
				return key(shift, alt, Key.u, Key.U);
			case 0x17:
				return key(shift, alt, Key.i, Key.I);
			case 0x18:
				return key(shift, alt, Key.o, Key.O);
			case 0x19:
				return key(shift, alt, Key.p, Key.P);
			case 0x1A:
				return key(shift, alt, Key.u, Key.U);
			case 0x1B:
				return key(shift, alt, Key.PLUS, Key.ASTERISK, Key.TILDE);
			case 0x1C:
				return key(shift, alt, Key.ENTER);
			case 0xE01C:
				return key(shift, alt, Key.ENTER);
			case 0x1D:
				return key(shift, alt, Key.LCTRL);
			case 0xE01D:
				return key(shift, alt, Key.RCTRL);
			case 0x1E:
				return key(shift, alt, Key.a, Key.A);
			case 0x1F:
				return key(shift, alt, Key.s, Key.S);
			case 0x20:
				return key(shift, alt, Key.d, Key.D);
			case 0x21:
				return key(shift, alt, Key.f, Key.F);
			case 0x22:
				return key(shift, alt, Key.g, Key.G);
			case 0x23:
				return key(shift, alt, Key.h, Key.H);
			case 0x24:
				return key(shift, alt, Key.j, Key.J);
			case 0x25:
				return key(shift, alt, Key.k, Key.K);
			case 0x26:
				return key(shift, alt, Key.l, Key.L);
			case 0x27:
				return key(shift, alt, Key.o, Key.o);
			case 0x28:
				return key(shift, alt, Key.a, Key.A);
			case 0x29:
				return key(shift, alt, Key.CARET, Key.NONE);
			case 0x2A:
				return key(shift, alt, Key.LSHIFT);
			case 0x2B:
				return key(shift, alt, Key.POUND_KEY, Key.SINGLE_QUOTE);
			case 0x2C:
				return key(shift, alt, Key.z, Key.Z);
			case 0x2D:
				return key(shift, alt, Key.x, Key.X);
			case 0x2E:
				return key(shift, alt, Key.c, Key.C);
			case 0x2F:
				return key(shift, alt, Key.v, Key.V);
			case 0x30:
				return key(shift, alt, Key.b, Key.B);
			case 0x31:
				return key(shift, alt, Key.n, Key.N);
			case 0x32:
				return key(shift, alt, Key.m, Key.M);
			case 0x33:
				return key(shift, alt, Key.COMMA, Key.SEMICOLON);
			case 0x34:
				return key(shift, alt, Key.DOT, Key.COLON);
			case 0x35:
				return key(shift, alt, Key.MINUS, Key.UNDERSCORE);
			case 0xE035:
				return key(shift, alt, Key.SLASH);
			case 0x36:
				return key(shift, alt, Key.RSHIFT);
			case 0x37:
				return key(shift, alt, Key.ASTERISK);
			case 0x38:
				return key(shift, alt, Key.LALT);
			case 0xE038:
				return key(shift, alt, Key.RALT);
			case 0x39:
				return key(shift, alt, Key.SPACE);
			case 0x3A:
				return key(shift, alt, Key.CAPSLOCK);
			case 0x3B:
				return key(shift, alt, Key.F1);
			case 0x3C:
				return key(shift, alt, Key.F2);
			case 0x3D:
				return key(shift, alt, Key.F3);
			case 0x3E:
				return key(shift, alt, Key.F4);
			case 0x3F:
				return key(shift, alt, Key.F5);
			case 0x40:
				return key(shift, alt, Key.F6);
			case 0x41:
				return key(shift, alt, Key.F7);
			case 0x42:
				return key(shift, alt, Key.F8);
			case 0x43:
				return key(shift, alt, Key.F9);
			case 0x44:
				return key(shift, alt, Key.F10);
			case 0x45:
				return key(shift, alt, Key.NUMLOCK);
			case 0x46:
				return key(shift, alt, Key.SCROLLLOCK);
			case 0x47:
				return key(shift, alt, Key.HOME);
			case 0xE047:
				return key(shift, alt, Key.HOME);
			case 0x48:
				return key(shift, alt, Key.ARROW_UP);
			case 0xE048:
				return key(shift, alt, Key.ARROW_UP);
			case 0x49:
				return key(shift, alt, Key.PAGE_UP);
			case 0xE049:
				return key(shift, alt, Key.PAGE_UP);
			case 0x4A:
				return key(shift, alt, Key.MINUS);
			case 0x4B:
				return key(shift, alt, Key.ARROW_LEFT);
			case 0xE04B:
				return key(shift, alt, Key.ARROW_LEFT);
			case 0x4C:
				return key(shift, alt, Key.NONE);
			case 0x4D:
				return key(shift, alt, Key.ARROW_RIGHT);
			case 0xE04D:
				return key(shift, alt, Key.ARROW_RIGHT);
			case 0x4E:
				return key(shift, alt, Key.PLUS);
			case 0x4F:
				return key(shift, alt, Key.END);
			case 0xE04F:
				return key(shift, alt, Key.END);
			case 0x50:
				return key(shift, alt, Key.ARROW_DOWN);
			case 0xE050:
				return key(shift, alt, Key.ARROW_DOWN);
			case 0x51:
				return key(shift, alt, Key.PAGE_DOWN);
			case 0xE051:
				return key(shift, alt, Key.PAGE_DOWN);
			case 0x52:
				return key(shift, alt, Key.INSERT);
			case 0xE052:
				return key(shift, alt, Key.INSERT);
			case 0x53:
				return key(shift, alt, Key.DELETE);
			case 0xE053:
				return key(shift, alt, Key.DELETE);
			case 0x56:
				return key(shift, alt, Key.LESS_THAN, Key.GREATER_THAN, Key.PIPE);
			case 0x57:
				return key(shift, alt, Key.F11);
			case 0x58:
				return key(shift, alt, Key.F12);
			case 0xE05B:
				return key(shift, alt, Key.SUPER);
			case 0xE05C:
				return key(shift, alt, Key.SUPER);
			case 0xE05D:
				return key(shift, alt, Key.MENU);
			case 0xE11D45:
				return key(shift, alt, Key.PAUSE);
			default:
				return Key.NONE;
		}
		// @formatter:on
	}
	
	@SJC.Inline
	private static char key(boolean shift, boolean alt, char keyLower, char keyUpper, char keyAlt)
	{
		if (alt)
			return keyAlt;
		
		if (shift)
			return keyUpper;
		
		return keyLower;
	}
	
	@SJC.Inline
	private static char key(boolean shift, boolean alt, char keyLower, char keyUpper)
	{
		if (shift)
			return keyUpper;
		
		return keyLower;
	}
	
	@SJC.Inline
	private static char key(boolean shift, boolean alt, char key)
	{
		return key;
	}
}
