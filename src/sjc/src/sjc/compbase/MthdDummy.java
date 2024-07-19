/* Copyright (C) 2005, 2006, 2007, 2008, 2009 Stefan Frenz
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

package sjc.compbase;

/**
 * MthdDummy: dummy-implementation of Mthd as placeholder
 *
 * @author S. Frenz
 * @version 091116 adopted simplified Mthd-signature
 * version 091111 adopted optimized resolve-signature
 * version 090207 added copyright notice
 * version 080414 adopted change in Mthd
 * version 071215 adopted change in Mthd
 * version 070913 adopted change in Mthd
 * version 070727 changed id from PureID to String
 * version 070127 adopted change in genOutput
 * version 061225 added genInlineOutput
 * version 060607 initial version
 */

public class MthdDummy extends Mthd
{
	public MthdDummy(String ii, int im, int fid, int il, int ic)
	{
		super(ii, im, fid, il, ic);
	}
	
	public Mthd copy()
	{
		return null;
	}
	
	public boolean checkNameAndType(Unit owner, Context ctx)
	{
		ctx.out.println("Invalid call to MthdDummy.checkNameAndType");
		return false;
	}
	
	public boolean resolve(Context ctx)
	{
		ctx.out.println("Invalid call to MthdDummy.resolve");
		return false;
	}
	
	public void genOutput(Context ctx)
	{
		ctx.out.println("Invalid call to MthdDummy.genOutput");
		ctx.err = true;
	}
	
	public void genInlineOutput(Context ctx)
	{
		ctx.out.println("Invalid call to MthdDummy.genInlineOutput");
		ctx.err = true;
	}
}
