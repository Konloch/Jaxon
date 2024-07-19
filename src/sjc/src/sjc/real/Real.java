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

import sjc.compbase.Ops;

/**
 * Real: declaration of floating point operations
 *
 * @author S. Frenz
 * @version 091005 removed unneeded methods
 * version 090207 added copyright notice
 * version 070817 added getLongOf*
 * version 070812 added compareConst*
 * version 070808 initial version
 */

public abstract class Real
{
	public final static int A_PLUS = Ops.A_PLUS;
	public final static int A_MINUS = Ops.A_MINUS;
	public final static int A_MUL = Ops.A_MUL;
	public final static int A_DIV = Ops.A_DIV;
	public final static int C_LW = Ops.C_LW;
	public final static int C_LE = Ops.C_LE;
	public final static int C_EQ = Ops.C_EQ;
	public final static int C_GE = Ops.C_GE;
	public final static int C_GT = Ops.C_GT;
	public final static int C_NE = Ops.C_NE;
	
	//float
	public abstract int buildFloat(long mant, int exp);
	
	public abstract int negateFloat(int number);
	
	public abstract int binOpFloat(int op1, int op2, int op);
	
	public abstract int compareConstFloat(int le, int ri, int op);
	
	public abstract long getLongOfFloat(int number);
	
	//double
	public abstract long buildDouble(long mant, int exp);
	
	public abstract long negateDouble(long number);
	
	public abstract long binOpDouble(long op1, long op2, int op);
	
	public abstract int compareConstDouble(long le, long ri, int op);
	
	public abstract long getLongOfDouble(long number);
	
	//conversion between float and double
	public abstract int buildFloatFromDouble(long number);
	
	public abstract long buildDoubleFromFloat(int number);
}
