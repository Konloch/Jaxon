/* Copyright (C) 2007, 2008, 2009 Stefan Frenz
 *
 * This file is part of SJC, the Small Java Compiler written by Stefan Frenz.
 *
 * SJC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SJC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SJC. If not, see <http://www.gnu.org/licenses/>.
 */

package sjc.real;

/**
 * NativeReal: native floating point operations
 *
 * @author S. Frenz
 * @version 091005 removed unneeded methods
 * version 090207 added copyright notice
 * version 070817 added getLongOf*
 * version 070812 added compareConst*
 * version 070808 initial version
 */

public class NativeReal extends Real
{
	public int buildFloat(long mant, int exp)
	{
		return Float.floatToRawIntBits((float) mant * pow10Float(exp));
	}
	
	public int negateFloat(int number)
	{
		return Float.floatToRawIntBits(-Float.intBitsToFloat(number));
	}
	
	public int binOpFloat(int op1, int op2, int op)
	{
		float o1 = Float.intBitsToFloat(op1), o2 = Float.intBitsToFloat(op2), res = 0.0f;
		switch (op)
		{
			case A_MINUS:
				res = o1 - o2;
				break;
			case A_PLUS:
				res = o1 + o2;
				break;
			case A_MUL:
				res = o1 * o2;
				break;
			case A_DIV:
				res = o1 / o2;
				break;
		}
		return Float.floatToRawIntBits(res);
	}
	
	public int compareConstFloat(int le, int ri, int op)
	{
		float l = Float.intBitsToFloat(le), r = Float.intBitsToFloat(ri);
		switch (op)
		{
			case C_LW:
				if (l < r)
					return 1;
				else
					return 0;
			case C_LE:
				if (l <= r)
					return 1;
				else
					return 0;
			case C_EQ:
				if (l == r)
					return 1;
				else
					return 0;
			case C_GE:
				if (l >= r)
					return 1;
				else
					return 0;
			case C_GT:
				if (l > r)
					return 1;
				else
					return 0;
			case C_NE:
				if (l != r)
					return 1;
				else
					return 0;
		}
		return 0;
	}
	
	public long getLongOfFloat(int number)
	{
		return (long) Float.intBitsToFloat(number);
	}
	
	public long buildDouble(long mant, int exp)
	{
		return Double.doubleToRawLongBits((double) mant * pow10Double(exp));
	}
	
	public long negateDouble(long number)
	{
		return Double.doubleToRawLongBits(-Double.longBitsToDouble(number));
	}
	
	public long binOpDouble(long op1, long op2, int op)
	{
		double o1 = Double.longBitsToDouble(op1), o2 = Double.longBitsToDouble(op2), res = 0.0;
		switch (op)
		{
			case A_MINUS:
				res = o1 - o2;
				break;
			case A_PLUS:
				res = o1 + o2;
				break;
			case A_MUL:
				res = o1 * o2;
				break;
			case A_DIV:
				res = o1 / o2;
				break;
		}
		return Double.doubleToRawLongBits(res);
	}
	
	public int compareConstDouble(long le, long ri, int op)
	{
		double l = Double.longBitsToDouble(le), r = Double.longBitsToDouble(ri);
		switch (op)
		{
			case C_LW:
				if (l < r)
					return 1;
				else
					return 0;
			case C_LE:
				if (l <= r)
					return 1;
				else
					return 0;
			case C_EQ:
				if (l == r)
					return 1;
				else
					return 0;
			case C_GE:
				if (l >= r)
					return 1;
				else
					return 0;
			case C_GT:
				if (l > r)
					return 1;
				else
					return 0;
			case C_NE:
				if (l != r)
					return 1;
				else
					return 0;
		}
		return 0;
	}
	
	public long getLongOfDouble(long number)
	{
		return (long) Double.longBitsToDouble(number);
	}
	
	public int buildFloatFromDouble(long number)
	{
		return Float.floatToRawIntBits((float) Double.longBitsToDouble(number));
	}
	
	public long buildDoubleFromFloat(int number)
	{
		return Double.doubleToRawLongBits(Float.intBitsToFloat(number));
	}
	
	public float pow10Float(int exp)
	{
		float res = 1.0f, mul = 10.0f;
		
		if (exp < 0)
		{
			exp = -exp;
			mul = 1.0f / mul;
		}
		while (exp > 0)
		{
			if ((exp & 1) != 0)
				res *= mul;
			exp = exp >>> 1;
			mul *= mul;
		}
		return res;
	}
	
	public double pow10Double(int exp)
	{
		double res = 1.0, mul = 10.0;
		
		if (exp < 0)
		{
			exp = -exp;
			mul = 1.0 / mul;
		}
		while (exp > 0)
		{
			if ((exp & 1) != 0)
				res *= mul;
			exp = exp >>> 1;
			mul *= mul;
		}
		return res;
	}
}
