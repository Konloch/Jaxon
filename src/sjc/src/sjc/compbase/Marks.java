/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Stefan Frenz
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
 * Marks: definition of marks that may be set as compiler-hint
 *
 * @author S. Frenz
 * @version 101222 added K_FOCD
 * version 101125 added K_NTCB
 * version 101021 added K_ASRT
 * version 100512 got K_EXPC, K_NWIN from Modifier, added K_FCDG
 * version 100402 added K_IGNU mark
 * version 091112 added K_EXPC mark, immediatly moved to Modifier as MC_EXPCONV
 * version 091104 added K_SLHI mark
 * version 090626 added K_SEPC and K_NSPC marks
 * version 090207 added copyright notice
 * version 081015 added K_PRCD and K_NOPT marks
 * version 080616 added K_THRW mark
 * version 080104 added K_PROF and K_NPRF mark
 * version 061231 added K_NINL mark
 * verison 061225 added K_FINL mark
 * version 061109 cleaned up optdebug-mark
 * version 060607 initial version
 */

public class Marks
{
	public final static int K_INTR = 0x00001; //method is used to handle interrupts
	public final static int K_DEBG = 0x00002; //write debug output for method
	public final static int K_FINL = 0x00004; //force method inlining
	public final static int K_NINL = 0x00008; //no automatic method inlining
	public final static int K_PROF = 0x00010; //generate profiling calls
	public final static int K_NPRF = 0x00020; //avoid profiling calls
	public final static int K_THRW = 0x00040; //this method is used to throw an exception
	public final static int K_PRCD = 0x00080; //print method code if possible
	public final static int K_NOPT = 0x00100; //avoid optimization
	public final static int K_SEPC = 0x00200; //generate stack extreme pointer check
	public final static int K_NSPC = 0x00400; //avoid stack extreme pointer check
	public final static int K_SLHI = 0x00800; //enable source line hint integration
	public final static int K_IGNU = 0x01000; //ignore unit (do not compile)
	public final static int K_FOCD = 0x02000; //force own class descriptor
	public final static int K_EXPC = 0x04000; //force explicit type conversion
	public final static int K_NWIN = 0x08000; //windows dll-call
	public final static int K_FCDG = 0x10000; //force code generation
	public final static int K_ASRT = 0x20000; //enable assert checks
	public final static int K_NTCB = 0x40000; //native callback, to not clean up in return statement
}
