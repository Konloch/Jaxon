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
 * EmulReal: handle floating point numbers with integer operations
 *
 * @author S. Frenz
 * @version 091005 removed unneeded methods
 * version 090207 added copyright notice
 * version 080712 removed unneccessary type conversion
 * version 080206 fixed bug in buildFloat and buildDouble with mant==0
 * version 070824 fixed bug in buildFloatFromDouble
 * version 070817 added getLongOf*
 * version 070812 added compareConst*
 * version 070808 initial version
 */

public class EmulReal extends Real
{
	public static final int FLT_POS_ZERO = 0x00000000;
	public static final int FLT_NEG_ZERO = 0x80000000;
	public static final int FLT_POS_INF = 0x7F800000;
	public static final int FLT_NEG_INF = 0xFF800000;
	public static final int FLT_NAN = 0x7FC00000;
	
	private static final int FLT_SIGN_BIT = 0x80000000;
	private static final int FLT_EXP_MASK = 0x7F800000;
	private static final int FLT_MANT_MASK = 0x007FFFFF;
	private static final int FLT_IMPL_ONE = 0x00800000;
	private static final int FLT_EXP_SHIFT = 23;
	private static final int FLT_EXP_BIAS = 150;
	
	public static final long DBL_POS_ZERO = 0x0000000000000000L;
	public static final long DBL_NEG_ZERO = 0x8000000000000000L;
	public static final long DBL_POS_INF = 0x7FF0000000000000L;
	public static final long DBL_NEG_INF = 0xFFF0000000000000L;
	public static final long DBL_NAN = 0x7FF8000000000000L;
	
	private static final long DBL_SIGN_BIT = 0x8000000000000000L;
	private static final long DBL_EXP_MASK = 0x7FF0000000000000L;
	private static final long DBL_MANT_MASK = 0x000FFFFFFFFFFFFFL;
	private static final long DBL_IMPL_ONE = 0x0010000000000000L;
	private static final int DBL_EXP_SHIFT = 52;
	private static final int DBL_EXP_BIAS = 1075;
	
	// ### --- ### float library ### --- ###
	
	public int buildFloat(long mant, int base10exp)
	{
		boolean neg = false;
		int base2exp = 0, tmp;
		
		if (mant == 0l)
			return FLT_POS_ZERO;
		//get sign bit
		if (mant < 0l)
		{
			neg = true;
			mant = -mant;
		}
		//respect base10exp
		if (base10exp != 0)
		{
			//check for overflow
			if (base10exp < -38)
				return neg ? FLT_NEG_ZERO : FLT_POS_ZERO;
			if (base10exp > 39)
				return neg ? FLT_NEG_INF : FLT_POS_INF;
			//left align for better precision
			if ((tmp = 59 - bitsUsed(mant)) > 0)
			{
				mant = mant << tmp;
				base2exp -= tmp;
			}
			//divide/multiply as required
			while (base10exp > 0)
			{
				if ((mant & 0xF800000000000000l) != 0l)
				{
					mant = mant >>> 5;
					base2exp += 5;
				}
				mant *= 10l;
				base10exp--;
			}
			while (base10exp < 0)
			{
				if ((mant & 0xF800000000000000l) == 0l)
				{
					mant = mant << 4;
					base2exp -= 4;
				}
				mant /= 10l;
				base10exp++;
			}
		}
		//reduce used bits in mant to a maximum of 31
		if ((tmp = bitsUsed(mant) - 31) > 0)
			return buildFloat(neg, (int) (mant >>> tmp), base2exp + tmp);
		return buildFloat(neg, (int) mant, base2exp);
	}
	
	public int buildFloat(boolean negative, int mant, int base2exp)
	{
		int res, tmp;
		
		if (mant == 0)
			return negative ? FLT_POS_ZERO : FLT_NEG_ZERO;
		//set res as mant using bit 24
		if ((tmp = bitsUsed(mant) - (FLT_EXP_SHIFT + 1)) == 0)
			res = mant; //nothing to adopt
		else
		{ //adjust base2exp and shift
			base2exp += tmp;
			if (tmp > 0)
			{ //too many bits used, reduce precision with rounding
				res = shiftRounded32(mant, tmp);
				if (bitsUsed(res) > FLT_EXP_SHIFT + 1)
				{ //overflow after rounding
					res = res >>> 1;
					base2exp++;
				}
			}
			else
				res = mant << -tmp; //too few bits used, fill up with zeros
		}
		//check exponent and build value
		if (base2exp > 127)
			res = FLT_POS_INF; //infinity
		else if (base2exp < -173)
			res = FLT_POS_ZERO; //result is 0
		else if (base2exp < -FLT_EXP_BIAS + 1)
			res = shiftRounded32(res, -FLT_EXP_BIAS + 1 - base2exp); //subnormal value
		else
			res = (res & FLT_MANT_MASK) | ((base2exp + FLT_EXP_BIAS) << FLT_EXP_SHIFT); //normal value
		//return value
		return negative ? res | FLT_SIGN_BIT : res;
	}
	
	public int binOpFloat(int op1, int op2, int op)
	{
		switch (op)
		{
			case A_MINUS:
				return addFloat(op1, negateFloat(op2));
			case A_PLUS:
				return addFloat(op1, op2);
			case A_MUL:
				return mulFloat(op1, op2);
			case A_DIV:
				return divFloat(op1, op2);
		}
		return 0;
	}
	
	public int negateFloat(int op1)
	{
		return op1 ^ FLT_SIGN_BIT;
	}
	
	public boolean isNegativeFloat(int op)
	{
		return op < 0; //sign bits are in same position
	}
	
	public int getExponentFloat(int op)
	{
		return ((op & FLT_EXP_MASK) >> FLT_EXP_SHIFT) - FLT_EXP_BIAS;
	}
	
	public int getMantissaFloat(int op)
	{
		return (op & FLT_EXP_MASK) == 0 ? (op & FLT_MANT_MASK) << 1 : (op & FLT_MANT_MASK) | FLT_IMPL_ONE;
	}
	
	public boolean isNaNFloat(int op)
	{
		return (op & ~FLT_SIGN_BIT) > FLT_POS_INF;
	}
	
	public boolean isInfiniteFloat(int op)
	{
		return (op & ~FLT_SIGN_BIT) == FLT_POS_INF;
	}
	
	public boolean isZeroFloat(int op)
	{
		return (op & ~FLT_SIGN_BIT) == 0;
	}
	
	public int addFloat(int op1, int op2)
	{
		boolean inf1, inf2, neg1, neg2, zero1, zero2;
		int exp1, exp2, expDiff, mant1, mant2, tmp;
		
		//check special cases NaN and infinite and zero, extract components of operands
		if (isNaNFloat(op1) || isNaNFloat(op2))
			return FLT_NAN;
		neg1 = isNegativeFloat(op1);
		neg2 = isNegativeFloat(op2);
		if ((inf1 = isInfiniteFloat(op1)) | (inf2 = isInfiniteFloat(op2)))
		{ //at least one op is infinite
			if (inf1 && inf2)
			{ //both infinite
				if (neg1 == neg2)
					return op1; //both have same sign
				return FLT_NAN; //different signs -> result is NaN
			}
			if (inf1)
				return op1; //infinite+finite
			return op2; //finite+infinite
		}
		if ((zero1 = isZeroFloat(op1)) | (zero2 = isZeroFloat(op2)))
		{ //at least one op is zero
			if (zero1 && zero2)
			{ //both zero
				if (neg1 == neg2)
					return op1; //keep sign if same zero-values
				return FLT_POS_ZERO; //positive zero as result of addition of different zero-values
			}
			if (zero1)
				return op2; //zero+nonzero
			return op1; //nonzero+zero
		}
		mant1 = getMantissaFloat(op1) << 5;
		mant2 = getMantissaFloat(op2) << 5;
		exp1 = getExponentFloat(op1) - 5;
		exp2 = getExponentFloat(op2) - 5;
		
		//equalize exponents, result in exp1
		if ((expDiff = exp1 - exp2) < 0)
		{ //exp1<exp2
			mant1 = shiftRounded32(mant1, -expDiff);
			exp1 = exp2;
		}
		else if (expDiff > 0)
			mant2 = shiftRounded32(mant2, expDiff); //exp1>exp2
		
		//get resulting sign in neg1, if signs different negate smaller mantissa
		if (neg1 ^ neg2)
		{ //signs different
			if (mant1 > mant2)
				mant2 = -mant2; //negate mant2, keep neg1
			else
			{ //negate mant1, invert neg1
				mant1 = -mant1;
				neg1 = !neg1;
			}
		}
		
		//do the real add operation and set up the new value
		mant1 += mant2;
		if ((tmp = buildFloat(neg1, mant1, exp1)) == FLT_NEG_ZERO)
			return FLT_POS_ZERO; //make zero positive
		return tmp;
	}
	
	public int mulFloat(int op1, int op2)
	{
		boolean neg;
		int exp, bits;
		long mant;
		
		//check special cases NaN and infinite
		if (isNaNFloat(op1) || isNaNFloat(op2))
			return FLT_NAN;
		neg = isNegativeFloat(op1) ^ isNegativeFloat(op2);
		if (isInfiniteFloat(op1) || isInfiniteFloat(op2))
		{
			if (isZeroFloat(op1) || isZeroFloat(op2))
				return FLT_NAN;
			return neg ? FLT_NEG_INF : FLT_POS_INF;
		}
		
		//do the real mul operation and set up the new value by multiplying to 24*24=48<64 bit
		exp = getExponentFloat(op1) + getExponentFloat(op2);
		mant = (long) getMantissaFloat(op1) * (long) getMantissaFloat(op2);
		if ((bits = bitsUsed(mant) - 31) > 0)
			return buildFloat(neg, (int) (mant >>> bits), exp + bits);
		return buildFloat(neg, (int) mant, exp);
	}
	
	public int divFloat(int op1, int op2)
	{
		boolean neg, inf1, inf2, zero1, zero2;
		int exp, bits;
		long mant;
		
		//check special cases NaN and infinite and zero
		if (isNaNFloat(op1) || isNaNFloat(op2))
			return FLT_NAN;
		neg = isNegativeFloat(op1) ^ isNegativeFloat(op2);
		if ((inf1 = isInfiniteFloat(op1)) | (inf2 = isInfiniteFloat(op2)))
		{ //at least one op is infinite
			if (inf1 && inf2)
				return FLT_NAN; //both infinite
			if (inf1)
				return neg ? FLT_NEG_INF : FLT_POS_INF; //first operand infinite
			return neg ? FLT_NEG_ZERO : FLT_POS_ZERO; //second operand infinite
		}
		if ((zero1 = isZeroFloat(op1)) | (zero2 = isZeroFloat(op2)))
		{ //at least one op is zero
			if (zero1 && zero2)
				return FLT_NAN; //both zero
			if (zero1)
				return neg ? FLT_NEG_ZERO : FLT_POS_ZERO; //first operand zero
			return neg ? FLT_NEG_INF : FLT_POS_INF; //second operand zero
		}
		
		//do the real div operation and set up the new value
		//shift dividend to the left to increase precision
		mant = getMantissaFloat(op1);
		bits = 63 - bitsUsed(mant); //bits to shift op1 left
		mant = (mant << bits) / ((long) getMantissaFloat(op2));
		exp = getExponentFloat(op1) - bits - getExponentFloat(op2);
		if ((bits = bitsUsed(mant) - 31) > 0)
			return buildFloat(neg, (int) (mant >>> bits), exp + bits);
		return buildFloat(neg, (int) mant, exp);
	}
	
	public int compareConstFloat(int le, int ri, int op)
	{
		switch (op)
		{
			case C_LW:
				if (isNaNFloat(le) || isNaNFloat(ri))
					return 0;
				if (le == FLT_NEG_ZERO)
					le = FLT_POS_ZERO;
				if (ri == FLT_NEG_ZERO)
					ri = FLT_POS_ZERO;
				return (compareHelperFloat(le, ri) < 0) ? 1 : 0;
			case C_LE:
				if (isNaNFloat(le) || isNaNFloat(ri))
					return 0;
				if (le == FLT_NEG_ZERO)
					le = FLT_POS_ZERO;
				if (ri == FLT_NEG_ZERO)
					ri = FLT_POS_ZERO;
				return (compareHelperFloat(le, ri) <= 0) ? 1 : 0;
			case C_EQ:
				return ((le == ri && !isNaNFloat(le)) || (isZeroFloat(le) && isZeroFloat(ri))) ? 1 : 0;
			case C_GE:
				if (isNaNFloat(le) || isNaNFloat(ri))
					return 0;
				if (le == FLT_NEG_ZERO)
					le = FLT_POS_ZERO;
				if (ri == FLT_NEG_ZERO)
					ri = FLT_POS_ZERO;
				return (compareHelperFloat(le, ri) >= 0) ? 1 : 0;
			case C_GT:
				if (isNaNFloat(le) || isNaNFloat(ri))
					return 0;
				if (le == FLT_NEG_ZERO)
					le = FLT_POS_ZERO;
				if (ri == FLT_NEG_ZERO)
					ri = FLT_POS_ZERO;
				return (compareHelperFloat(le, ri) > 0) ? 1 : 0;
			case C_NE:
				return ((le == ri && !isNaNFloat(le)) || (isZeroFloat(le) && isZeroFloat(ri))) ? 0 : 1;
		}
		return 0;
	}
	
	public long getLongOfFloat(int number)
	{
		boolean negative;
		int exp;
		long mant;
		
		if (isNaNFloat(number))
			return 0l;
		negative = isNegativeFloat(number);
		exp = getExponentFloat(number);
		mant = getMantissaFloat(number);
		if (exp > 0)
		{
			if (exp >= 63 || (mant >> (63 - exp) != 0l))
				return negative ? 0x8000000000000000l : 0x7FFFFFFFFFFFFFFFl;
			mant = mant << exp;
		}
		else if (exp < 0)
		{
			if (exp < -24)
				return 0l;
			mant = mant >> -exp;
		}
		return negative ? -mant : mant;
	}
	
	// ### --- ### double library ### --- ###
	
	public long buildDouble(long mant, int base10exp)
	{
		boolean neg = false;
		int base2exp = 0, tmp;
		
		if (mant == 0l)
			return DBL_POS_ZERO;
		//get sign bit
		if (mant < 0l)
		{
			neg = true;
			mant = -mant;
		}
		//respect base10exp
		if (base10exp != 0)
		{
			//check for overflow
			if (base10exp < -345)
				return neg ? DBL_NEG_ZERO : DBL_POS_ZERO;
			if (base10exp > 345)
				return neg ? DBL_NEG_INF : DBL_POS_INF;
			//left align for better precision
			if ((tmp = 59 - bitsUsed(mant)) > 0)
			{
				mant = mant << tmp;
				base2exp -= tmp;
			}
			//divide/multiply as required
			while (base10exp > 0)
			{
				if ((mant & 0xF800000000000000l) != 0l)
				{
					mant = mant >>> 5;
					base2exp += 5;
				}
				mant *= 10l;
				base10exp--;
			}
			while (base10exp < 0)
			{
				if ((mant & 0xF800000000000000l) == 0l)
				{
					mant = mant << 4;
					base2exp -= 4;
				}
				mant /= 10l;
				base10exp++;
			}
		}
		return buildDouble(neg, mant, base2exp);
	}
	
	public long buildDouble(boolean negative, long mant, int base2exp)
	{
		int tmp;
		long res;
		
		if (mant == 0l)
			return negative ? DBL_NEG_ZERO : DBL_POS_ZERO;
		//set res as mant using bit 52
		if ((tmp = bitsUsed(mant) - (DBL_EXP_SHIFT + 1)) == 0)
			res = mant; //nothing to adopt
		else
		{ //adjust base2exp and shift
			base2exp += tmp;
			if (tmp > 0)
			{ //too many bits used, reduce precision with rounding
				res = shiftRounded64(mant, tmp);
				if (bitsUsed(res) > DBL_EXP_SHIFT + 1)
				{ //overflow after rounding
					res = res >>> 1;
					base2exp++;
				}
			}
			else
				res = mant << -tmp; //too few bits used, fill up with zeros
		}
		//check exponent and build value
		if (base2exp > 960)
			res = DBL_POS_INF; //infinity
		else if (base2exp < -1085)
			res = DBL_POS_ZERO; //result is 0
		else if (base2exp < -DBL_EXP_BIAS + 1)
			res = shiftRounded64(res, -DBL_EXP_BIAS + 1 - base2exp); //subnormal value
		else
			res = (res & DBL_MANT_MASK) | (((long) (base2exp + DBL_EXP_BIAS)) << DBL_EXP_SHIFT); //normal value
		//return value
		return negative ? res | DBL_SIGN_BIT : res;
	}
	
	public long binOpDouble(long op1, long op2, int op)
	{
		switch (op)
		{
			case A_MINUS:
				return addDouble(op1, negateDouble(op2));
			case A_PLUS:
				return addDouble(op1, op2);
			case A_MUL:
				return mulDouble(op1, op2);
			case A_DIV:
				return divDouble(op1, op2);
		}
		return 0l;
	}
	
	public long negateDouble(long op)
	{
		return op ^ DBL_SIGN_BIT;
	}
	
	public boolean isNegativeDouble(long op)
	{
		return op < 0l; //sign bits are in same position
	}
	
	public int getExponentDouble(long op)
	{
		return ((int) ((op & DBL_EXP_MASK) >> DBL_EXP_SHIFT)) - DBL_EXP_BIAS;
	}
	
	public long getMantissaDouble(long op)
	{
		return (op & DBL_EXP_MASK) == 0l ? (op & DBL_MANT_MASK) << 1 : (op & DBL_MANT_MASK) | DBL_IMPL_ONE;
	}
	
	public boolean isNaNDouble(long op)
	{
		return (op & ~DBL_SIGN_BIT) > DBL_POS_INF;
	}
	
	public boolean isInfiniteDouble(long op)
	{
		return (op & ~DBL_SIGN_BIT) == DBL_POS_INF;
	}
	
	public boolean isZeroDouble(long op)
	{
		return (op & ~DBL_SIGN_BIT) == 0l;
	}
	
	public long addDouble(long op1, long op2)
	{
		boolean inf1, inf2, neg1, neg2, zero1, zero2;
		int exp1, exp2, expDiff;
		long mant1, mant2, tmp;
		
		//check special cases NaN and infinite and zero, extract components of operands
		if (isNaNDouble(op1) || isNaNDouble(op2))
			return DBL_NAN;
		neg1 = isNegativeDouble(op1);
		neg2 = isNegativeDouble(op2);
		if ((inf1 = isInfiniteDouble(op1)) | (inf2 = isInfiniteDouble(op2)))
		{ //at least one op is infinite
			if (inf1 && inf2)
			{ //both infinite
				if (neg1 == neg2)
					return op1; //both have same sign
				return DBL_NAN; //different signs -> result is NaN
			}
			if (inf1)
				return op1; //infinite+finite
			return op2; //finite+infinite
		}
		if ((zero1 = isZeroDouble(op1)) | (zero2 = isZeroDouble(op2)))
		{ //at least one op is zero
			if (zero1 && zero2)
			{ //both zero
				if (neg1 == neg2)
					return op1; //keep sign if same zero-values
				return DBL_POS_ZERO; //positive zero as result of addition of different zero-values
			}
			if (zero1)
				return op2; //zero+nonzero
			return op1; //nonzero+zero
		}
		mant1 = getMantissaDouble(op1) << 5;
		mant2 = getMantissaDouble(op2) << 5;
		exp1 = getExponentDouble(op1) - 5;
		exp2 = getExponentDouble(op2) - 5;
		
		//equalize exponents, result in exp1
		if ((expDiff = exp1 - exp2) < 0)
		{ //exp1<exp2
			mant1 = shiftRounded64(mant1, -expDiff);
			exp1 = exp2;
		}
		else if (expDiff > 0)
			mant2 = shiftRounded64(mant2, expDiff); //exp1>exp2
		
		//get resulting sign in neg1, if signs different negate smaller mantissa
		if (neg1 ^ neg2)
		{ //signs different
			if (mant1 > mant2)
				mant2 = -mant2; //negate mant2, keep neg1
			else
			{ //negate mant1, invert neg1
				mant1 = -mant1;
				neg1 = !neg1;
			}
		}
		
		//do the real add operation and set up the new value
		mant1 += mant2;
		
		if ((tmp = buildDouble(neg1, mant1, exp1)) == DBL_NEG_ZERO)
			return DBL_POS_ZERO; //make zero positive
		return tmp;
	}
	
	public long mulDouble(long op1, long op2)
	{
		boolean neg;
		int exp, bits;
		long mant1, mant2, m1lo, m1hi, m2lo, m2hi, tmp1, tmp2, tmp3;
		
		//check special cases NaN and infinite
		if (isNaNDouble(op1) || isNaNDouble(op2))
			return DBL_NAN;
		neg = isNegativeDouble(op1) ^ isNegativeDouble(op2);
		if (isInfiniteDouble(op1) || isInfiniteDouble(op2))
		{
			if (isZeroDouble(op1) || isZeroDouble(op2))
				return DBL_NAN;
			return neg ? DBL_NEG_INF : DBL_POS_INF;
		}
		
		//do the real mul operation and set up the new value by multiplying to 53*53=106<128 bit
		exp = getExponentDouble(op1) + getExponentDouble(op2);
		mant1 = getMantissaDouble(op1);
		mant2 = getMantissaDouble(op2);
		m1lo = mant1 & 0x1FFFFFFFl; //29 of 64 bits used
		m1hi = mant1 >>> 29;       //24 of 64 bits used
		m2lo = mant2 & 0x1FFFFFFFl; //29 of 64 bits used
		m2hi = mant2 >>> 29;       //24 of 64 bits used
		tmp1 = m1lo * m2lo;           //maximum of 58 bits used
		tmp2 = m1lo * m2hi + m1hi * m2lo; //maximum of 54 bits used
		tmp3 = m1hi * m2hi;           //maximum of 48 bits used
		tmp1 += (tmp2 & 0x1FFFFFFFl) << 29; //add upper part of tmp2 to tmp1
		tmp3 += (tmp2 >>> 29) + (tmp1 >>> 58); //add lower part of tmp2 to tmp3, respect carry of tmp1/2-add
		if (tmp3 == 0l)
			return buildDouble(neg, tmp1, exp); //upper bits stored in tmp3 all zero
		return buildDouble(neg, ((tmp1 & 0x03FFFFFFFFFFFFFFl) >>> (bits = bitsUsed(tmp3))) | (tmp3 << (58 - bits)), exp + bits); //shift tmp1 after masking carry to the right in a way that tmp3 has enough space
	}
	
	public long divDouble(long op1, long op2)
	{
		boolean neg, inf1, inf2, zero1, zero2;
		int exp, bits1, bits2;
		long mant1, mant2, mant = 0l;
		
		//check special cases NaN and infinite and zero
		if (isNaNDouble(op1) || isNaNDouble(op2))
			return DBL_NAN;
		neg = isNegativeDouble(op1) ^ isNegativeDouble(op2);
		if ((inf1 = isInfiniteDouble(op1)) | (inf2 = isInfiniteDouble(op2)))
		{ //at least one op is infinite
			if (inf1 && inf2)
				return DBL_NAN; //both infinite
			if (inf1)
				return neg ? DBL_NEG_INF : DBL_POS_INF; //first operand infinite
			return neg ? DBL_NEG_ZERO : DBL_POS_ZERO; //second operand infinite
		}
		if ((zero1 = isZeroDouble(op1)) | (zero2 = isZeroDouble(op2)))
		{ //at least one op is zero
			if (zero1 && zero2)
				return DBL_NAN; //both zero
			if (zero1)
				return neg ? DBL_NEG_ZERO : DBL_POS_ZERO; //first operand zero
			return neg ? DBL_NEG_INF : DBL_POS_INF; //second operand zero
		}
		
		//do the real div operation and set up the new value, unfortunately long precision is not enough
		//therefore do slow shift/div/mod/sub-divide
		mant1 = getMantissaDouble(op1);
		mant2 = getMantissaDouble(op2);
		exp = getExponentDouble(op1) - getExponentDouble(op2);
		while (true)
		{
			bits1 = 63 - bitsUsed(mant1);
			bits2 = 64 - bitsUsed(mant);
			if (bits2 < bits1)
				bits1 = bits2;
			if (bits1 <= 8)
				break;
			mant1 = mant1 << bits1;
			mant = mant << bits1;
			exp -= bits1;
			mant |= mant1 / mant2;
			mant1 %= mant2;
		}
		return buildDouble(neg, mant, exp);
	}
	
	public int compareConstDouble(long le, long ri, int op)
	{
		switch (op)
		{
			case C_LW:
				if (isNaNDouble(le) || isNaNDouble(ri))
					return 0;
				if (le == DBL_NEG_ZERO)
					le = DBL_POS_ZERO;
				if (ri == DBL_NEG_ZERO)
					ri = DBL_POS_ZERO;
				return (compareHelperDouble(le, ri) < 0) ? 1 : 0;
			case C_LE:
				if (isNaNDouble(le) || isNaNDouble(ri))
					return 0;
				if (le == DBL_NEG_ZERO)
					le = DBL_POS_ZERO;
				if (ri == DBL_NEG_ZERO)
					ri = DBL_POS_ZERO;
				return (compareHelperDouble(le, ri) <= 0) ? 1 : 0;
			case C_EQ:
				return ((le == ri && !isNaNDouble(le)) || (isZeroDouble(le) && isZeroDouble(ri))) ? 1 : 0;
			case C_GE:
				if (isNaNDouble(le) || isNaNDouble(ri))
					return 0;
				if (le == DBL_NEG_ZERO)
					le = DBL_POS_ZERO;
				if (ri == DBL_NEG_ZERO)
					ri = DBL_POS_ZERO;
				return (compareHelperDouble(le, ri) >= 0) ? 1 : 0;
			case C_GT:
				if (isNaNDouble(le) || isNaNDouble(ri))
					return 0;
				if (le == DBL_NEG_ZERO)
					le = DBL_POS_ZERO;
				if (ri == DBL_NEG_ZERO)
					ri = DBL_POS_ZERO;
				return (compareHelperDouble(le, ri) > 0) ? 1 : 0;
			case C_NE:
				return ((le == ri && !isNaNDouble(le)) || (isZeroDouble(le) && isZeroDouble(ri))) ? 0 : 1;
		}
		return 0;
	}
	
	public long getLongOfDouble(long number)
	{
		boolean negative;
		int exp;
		long mant;
		
		if (isNaNDouble(number))
			return 0l;
		negative = isNegativeDouble(number);
		exp = getExponentDouble(number);
		mant = getMantissaDouble(number);
		if (exp > 0)
		{
			if (exp >= 63 || (mant >> (63 - exp) != 0l))
				return negative ? 0x8000000000000000l : 0x7FFFFFFFFFFFFFFFl;
			mant = mant << exp;
		}
		else if (exp < 0)
		{
			if (exp < -53)
				return 0l;
			mant = mant >> -exp;
		}
		return negative ? -mant : mant;
	}
	
	// ### --- ### conversion between float and double ### --- ###
	
	public int buildFloatFromDouble(long number)
	{
		int exp, shift;
		long mant;
		if (isNaNDouble(number))
			return FLT_NAN;
		exp = getExponentDouble(number);
		mant = getMantissaDouble(number);
		if ((shift = bitsUsed(mant) - 30) > 0)
		{
			mant = mant >>> shift;
			exp += shift;
		}
		return buildFloat(isNegativeDouble(number), (int) mant, exp);
	}
	
	public long buildDoubleFromFloat(int number)
	{
		if (isNaNFloat(number))
			return DBL_NAN;
		return buildDouble(isNegativeFloat(number), getMantissaFloat(number), getExponentFloat(number));
	}
	
	// ### --- ### little helper ### --- ###
	
	private int compareHelperFloat(int le, int ri)
	{
		if (le < 0)
			return (ri < 0) ? ri - le : -1;
		return (ri < 0) ? 1 : le - ri;
	}
	
	private int compareHelperDouble(long le, long ri)
	{
		//conversion to int possible because only sign and ==0 matters
		if (le < 0l)
			return (ri < 0l) ? (int) ((ri - le) >>> 32) : -1;
		return (ri < 0l) ? 1 : (int) ((le - ri) >>> 32);
	}
	
	private static int bitsUsed(long number)
	{
		if (number == 0l)
			return 0;
		//check upper int, afterwards work on 32 bit
		if ((int) (number >>> 32) == 0)
			return bitsUsed((int) number);
		return bitsUsed((int) (number >>> 32)) + 32;
	}
	
	private static int bitsUsed(int number)
	{
		int used = 32, mask = 0xFFFF0000, bits = 16;
		
		if (number == 0)
			return 0;
		//search highest bit set
		while ((number & 0x80000000) == 0 && bits != 0)
		{
			if ((number & mask) == 0)
			{
				number = number << bits;
				used -= bits;
			}
			bits = bits >>> 1;
			mask = mask << bits;
		}
		//return value
		return used;
	}
	
	private static long shiftRounded64(long number, int shift)
	{
		return (number >>> shift) + ((number >>> (shift - 1)) & 1l);
	}
	
	private static int shiftRounded32(int number, int shift)
	{
		return (shift >= 32) ? 0 : (number >>> shift) + ((number >>> (shift - 1)) & 1);
	}
}
