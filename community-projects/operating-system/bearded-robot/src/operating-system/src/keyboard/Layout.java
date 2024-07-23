package keyboard;

/**
 * Provides information on the layout of the keyboard.
 */
class Layout
{
	/**
	 * Determines whether a key code (taking into account the modifier keys)
	 * can be assigned to a char value.
	 *
	 * @param keyCode The key code to be examined.
	 * @return true if the key code can be assigned to a char value, otherwise false.
	 */
	public boolean isCharacter(int keyCode)
	{
		switch (keyCode)
		{
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 15:
			case 16:
			case 17:
			case 18:
			case 19:
			case 20:
			case 21:
			case 22:
			case 23:
			case 24:
			case 25:
			case 27:
			case 30:
			case 31:
			case 32:
			case 33:
			case 34:
			case 35:
			case 36:
			case 37:
			case 38:
			case 43:
			case 44:
			case 45:
			case 46:
			case 47:
			case 48:
			case 49:
			case 50:
			case 51:
			case 52:
			case 53:
			case 57:
				return true;
			case 71:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 72:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 73:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 75:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 76:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 77:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 79:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 80:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 81:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
			case 82:
				if (!Keyboard.initstance().isNumLk())
					break;
				return true;
		}
		return false;
	}
	
	/**
	 * Determines the value of a button. The value corresponds either to an
	 * ASCII value or a constant value.
	 *
	 * @param keyCode The code whose value is determined.
	 * @return The value of the key.
	 */
	public int value(int keyCode)
	{
		switch (keyCode)
		{
			case 2: // 1
				return 49;
			case 3: // 2
				return 50;
			case 4: // 3
				return 51;
			case 5: // 4
				return 52;
			case 6: // 5
				return 53;
			case 7: // 6
				return 54;
			case 8: // 7
				return 55;
			case 9: // 8
				return 56;
			case 10: // 9
				return 57;
			case 11: // 0
				return 48;
			case 14:
				return Keyboard.BACKSPACE;
			case 15:
				return 9;
			case 16: // q
				if (Keyboard.initstance().isMod1())
					return 81;
				
				return 113;
			case 17: // w
				if (Keyboard.initstance().isMod1())
					return 87;
				
				return 119;
			case 18: // e
				if (Keyboard.initstance().isMod1())
					return 69;
				
				return 101;
			case 19: // r
				if (Keyboard.initstance().isMod1())
					return 82;
				
				return 114;
			case 20: // t
				if (Keyboard.initstance().isMod1())
					return 84;
				
				return 116;
			case 21: // z
				if (Keyboard.initstance().isMod1())
					return 90;
				
				return 122;
			case 22: // u
				if (Keyboard.initstance().isMod1())
					return 85;
				
				return 117;
			case 23: // i
				if (Keyboard.initstance().isMod1())
					return 73;
				
				return 105;
			case 24: // o
				if (Keyboard.initstance().isMod1())
					return 79;
				
				return 111;
			case 25: // p
				if (Keyboard.initstance().isMod1())
					return 80;
				
				return 112;
			case 27:
				if (Keyboard.initstance().isMod1())
					return 42;
				
				return 43;
			case 28:
				return Keyboard.RETURN;
			case 29:
				return Keyboard.CTRL;
			case 30: // a
				if (Keyboard.initstance().isMod1())
					return 65;
				
				return 97;
			case 31: // s
				if (Keyboard.initstance().isMod1())
					return 83;
				
				return 115;
			case 32: // d
				if (Keyboard.initstance().isMod1())
					return 68;
				
				return 100;
			case 33: // f
				if (Keyboard.initstance().isMod1())
					return 70;
				
				return 102;
			case 34: // g
				if (Keyboard.initstance().isMod1())
					return 71;
				
				return 103;
			case 35: // h
				if (Keyboard.initstance().isMod1())
					return 72;
				
				return 104;
			case 36: // j
				if (Keyboard.initstance().isMod1())
					return 74;
				
				return 106;
			case 37: // k
				if (Keyboard.initstance().isMod1())
					return 75;
				
				return 107;
			case 38: // l
				if (Keyboard.initstance().isMod1())
					return 76;
				
				return 108;
			case 42:
				return Keyboard.SHIFT_LEFT;
			case 43: // #
				if (Keyboard.initstance().isMod1())
					return 35;
				
				return 39;
			case 44: // y
				if (Keyboard.initstance().isMod1())
					return 89;
				return 121;
			case 45: // x
				if (Keyboard.initstance().isMod1())
					return 88;
				
				return 120;
			case 46: // c
				if (Keyboard.initstance().isMod1())
					return 67;
				
				return 99;
			case 47: // v
				if (Keyboard.initstance().isMod1())
					return 86;
				
				return 118;
			case 48: // b
				if (Keyboard.initstance().isMod1())
					return 66;
				
				return 98;
			case 49: // n
				if (Keyboard.initstance().isMod1())
					return 78;
				
				return 110;
			case 50: // m
				if (Keyboard.initstance().isMod1())
					return 77;
				
				return 109;
			case 51: // ,
				if (Keyboard.initstance().isMod1())
					return 59;
				
				return 44;
			case 52: // .
				if (Keyboard.initstance().isMod1())
					return 58;
				
				return 46;
			case 53: // -
				if (Keyboard.initstance().isMod1())
					return 95;
				
				return 45;
			case 54:
				return Keyboard.SHIFT_RIGHT;
			case 56:
				return Keyboard.ALT;
			case 57:
				return 32;
			case 58:
				return Keyboard.CAPS_LOCK;
			case 59:
				return Keyboard.F1;
			case 60:
				return Keyboard.F2;
			case 61:
				return Keyboard.F3;
			case 62:
				return Keyboard.F4;
			case 63:
				return Keyboard.F5;
			case 64:
				return Keyboard.F6;
			case 65:
				return Keyboard.F7;
			case 66:
				return Keyboard.F8;
			case 67:
				return Keyboard.F9;
			case 68:
				return Keyboard.F10;
			case 69:
				return Keyboard.NUM_LOCK;
			case 71:
				if (Keyboard.initstance().isNumLk())
					return 55;
				
				break;
			case 72:
				if (Keyboard.initstance().isNumLk())
					return 56;
				
				break;
			case 73:
				if (Keyboard.initstance().isNumLk())
					return 57;
				
				break;
			case 75:
				if (Keyboard.initstance().isNumLk())
					return 52;
				
				break;
			case 76:
				if (Keyboard.initstance().isNumLk())
					return 53;
				
				break;
			case 77:
				if (Keyboard.initstance().isNumLk())
					return 54;
				
				break;
			case 79:
				if (Keyboard.initstance().isNumLk())
					return 49;
				
				break;
			case 80:
				if (Keyboard.initstance().isNumLk())
					return 50;
				
				break;
			case 81:
				if (Keyboard.initstance().isNumLk())
					return 51;
				
				break;
			case 82:
				if (Keyboard.initstance().isNumLk())
					return 48;
				
				break;
			case 87:
				return Keyboard.F11;
			case 88:
				return Keyboard.F12;
			case 57416:
				return Keyboard.UP;
			case 57424:
				return Keyboard.DOWN;
			case 57419:
				return Keyboard.LEFT;
			case 57421:
				return Keyboard.RIGHT;
		}
		
		return Keyboard.UNSPECIFIED;
	}
}
