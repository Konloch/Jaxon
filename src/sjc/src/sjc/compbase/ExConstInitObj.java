/* Copyright (C) 2007, 2008, 2009, 2010 Stefan Frenz
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
 * ExConstInitObj: parent of constant objects created at compile time
 *
 * @author S. Frenz
 * @version 100312 added support for flash objects
 * version 100114 reorganized constant object handling
 * version 091209 added generateObject
 * version 091021 adopted changed modifier declarations
 * version 091005 moved to compbase-package
 * version 091001 adopted changed memory interface
 * version 090718 adopted move of modifier flags from Vrbl to AccVar
 * version 090207 added copyright notice
 * version 080616 added dependsOn, got getCompInitConstObjectVrbl
 * version 070918 initial version
 */

public abstract class ExConstInitObj extends ExAccVrbl
{
	public Object outputLocation;
	public Mthd dependsOn; //if set, check if method code is generated
	public ExConstInitObj nextConstInit;
	public boolean inFlash;
	
	public abstract boolean generateObject(Context ctx, boolean doFlash);
	
	public abstract String getDebugValue();
	
	public ExConstInitObj(int fid, int il, int ic)
	{
		super(fid, il, ic);
		Vrbl tmp;
		dest = tmp = new Vrbl(null, Modifier.M_PUB | Modifier.M_FIN | Modifier.M_STAT | Modifier.MF_ISWRITTEN, fid, il, ic);
		tmp.init = this;
	}
	
	public boolean isCompInitConstObject(Context ctx)
	{
		return true;
	}
	
	public ExConstInitObj getConstInitObj(Context ctx)
	{
		return this;
	}
	
	public int getOutputLocationOffset(Context ctx)
	{
		return ctx.embConstRAM && !inFlash ? ctx.ramOffset : 0;
	}
}
