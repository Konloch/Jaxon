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

package sjc.emulation;


/**
 * Device driver for text VGA screen
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 060608 initial version
 */
public class BasicVGA extends AddressRange
{
	private final static int BASE = 0xB8000;
	private final static int width = 80, height = 25;
	
	private final TextScreen outDrv;
	private final byte[] buffer;
	
	public BasicVGA(TextScreen out)
	{
		super(AddressRange.RAM, BASE, width * height * 2);
		outDrv = out;
		buffer = new byte[width * height * 2];
	}
	
	public byte read8(int address)
	{
		return buffer[address - BASE];
	}
	
	public short read16(int address)
	{
		return (short) (((int) read8(address) & 0xFF) | (((int) read8(address + 1) & 0xFF) << 8));
	}
	
	public int read32(int address)
	{
		return ((int) read16(address) & 0xFFFF) | (((int) read16(address + 2) & 0xFFFF) << 16);
	}
	
	public long read64(int address)
	{
		return ((long) read32(address) & 0xFFFFFFFFl) | (((long) read32(address + 4) & 0xFFFFFFFFl) << 32);
	}
	
	public void write8(int address, byte v)
	{
		int off = address - BASE, x, y;
		
		buffer[off] = v;
		off /= 2;
		x = off % width;
		y = off / width;
		if ((address & 1) == 0)
			outDrv.setChar(x, y, (char) ((int) v & 0xFF));
		else
			outDrv.setColor(x, y, v);
		outDrv.redraw(x, y);
	}
	
	public void write16(int address, short v)
	{
		write8(address, (byte) v);
		write8(address + 1, (byte) (v >>> 8));
	}
	
	public void write32(int address, int v)
	{
		write16(address, (short) v);
		write16(address + 2, (short) (v >>> 16));
	}
	
	public void write64(int address, long v)
	{
		write32(address, (int) v);
		write32(address + 4, (int) (v >>> 32));
	}
}
