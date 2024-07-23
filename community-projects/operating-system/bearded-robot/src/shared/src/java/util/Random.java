/**
 * Algorithmen und Signaturen wurden aus
 * http://grepcode.com/file_/repository.grepcode.com/java/root/jdk/openjdk/6-b14/java/util/Random.java/?v=source
 * übernommen. Für diese Teile gilt folgender Copyright-Hinweis:
 * <p>
 * <p>
 * <p>
 * Copyright 1995-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * <p>
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 * <p>
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * <p>
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * <p>
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package java.util;

public class Random
{
	// TODO: Java compliant implementation.
	
	private static final long multiplier = 0x5DEECE66DL;
	
	private static final long mask = (1L << 48) - 1;
	
	private static final long addend = 0xBL;
	
	private static long seedUniquifier = 8682522807148012L;
	
	private long seed;
	
	public Random()
	{
		this(++seedUniquifier + System.rdtsc());
	}
	
	public Random(long seed)
	{
		setSeed(seed);
	}
	
	public void setSeed(long seed)
	{
		this.seed = (seed ^ multiplier) & mask;
	}
	
	protected int next(int bits)
	{
		long oldseed, nextseed;
		do
		{
			oldseed = this.seed;
			nextseed = (oldseed * multiplier + addend) & mask;
		} while (!(nextseed == (this.seed = (this.seed == oldseed) ? nextseed : this.seed)));
		return (int) (nextseed >>> (48 - bits));
	}
	
	public int nextInt()
	{
		return next(32);
	}
	
	public int nextInt(int n)
	{
		if (n <= 0)
			return 0;
		
		if ((n & -n) == n)
			return (int) ((n * (long) next(31)) >> 31);
		
		int bits, val;
		do
		{
			bits = next(31);
			val = bits % n;
		} while (bits - val + (n - 1) < 0);
		return val;
	}
}
