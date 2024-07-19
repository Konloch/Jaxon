/* Copyright (C) 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

package sjc.output;

import sjc.osio.BinWriter;
import sjc.osio.TextPrinter;

/**
 * HexOut: write the created image as intel or atmel hex file
 * see http://www.scienceprog.com/shelling-the-intel-8-bit-hex-file-format/
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 100504 adopted changed BinWriter
 * version 091005 removed unneeded methods
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 070114 reduced access level where possible
 * version 061221 changed from autonomous OutputFormat to supporter of BootOut
 * version 061001 initial version
 */

public class HexOut extends BinWriter
{
	private final byte[] dataBuffer;
	private final byte[] textBuffer;
	private int checkSum, addr;
	private final TextPrinter out;
	private final BinWriter destFile;
	
	protected HexOut(TextPrinter errOut, BinWriter iDF)
	{
		out = errOut;
		destFile = iDF;
		dataBuffer = new byte[16];
		textBuffer = new byte[46];
		textBuffer[0] = (byte) ':'; //starter is always the same
		addr = -1;
	}
	
	public void close()
	{
		dataBuffer[0] = (byte) 0xFF;
		writeBuffer(0, 1, 0, 1);
		destFile.close();
	}
	
	public boolean open(String fname)
	{
		//not supported for HexOut (BinWriter destFile has to be opened already)
		return false;
	}
	
	public boolean setSize(int addBytes)
	{
		return write(null, 0, addBytes);
	}
	
	public boolean setAddress(int newAddress)
	{
		addr = newAddress;
		return true;
	}
	
	public boolean write(byte[] what, int offset, int len)
	{
		int i, off = 0, now = 16;
		
		if ((addr & 0xFFFF0000) != 0 || ((addr + len) & 0xFFFF0000) != 0)
		{
			out.println("HexOut error: address or length exceeds 16 bit range (or not yet initialized)");
			return false;
		}
		if (what == null)
			for (i = 0; i < 16; i++)
				dataBuffer[i] = (byte) 0;
		while (len > 0)
		{
			if (len < 16)
				now = len;
			if (what != null)
				for (i = 0; i < now; i++)
					dataBuffer[i] = what[offset + off + i];
			if (!writeBuffer(now, now, addr, 0))
				return false;
			addr += now;
			off += now;
			len -= now;
		}
		return true;
	}
	
	private boolean writeBuffer(int tellLen, int realLen, int addr, int type)
	{
		int i;
		
		checkSum = 0; //reset counter
		insertByte(tellLen, 0); //byte count
		insertByte(addr >>> 8, 1); //higher byte of address
		insertByte(addr & 0xFF, 2); //lower byte of address
		insertByte(type, 3); //record type
		for (i = 0; i < realLen; i++)
			insertByte(dataBuffer[i], 4 + i); //data bytes
		if (realLen == tellLen)
		{ //place checksum only if tellLen==realLen
			insertByte(-(checkSum & 0xFF), 4 + realLen); //checksum
			i = 11 + (realLen << 1);
		}
		else
			i = 9 + (realLen << 1); //no checksum
		textBuffer[i] = (byte) 10; //new line
		return destFile.write(textBuffer, 0, i + 1); //write buffer
	}
	
	private void insertByte(int value, int bPos)
	{
		int d;
		
		checkSum += (value &= 0xFF);
		bPos = (bPos << 1) + 1;
		if ((d = value >>> 4) < 10)
			textBuffer[bPos++] = (byte) (d + 48);
		else
			textBuffer[bPos++] = (byte) (d + 55);
		if ((d = value & 0xF) < 10)
			textBuffer[bPos] = (byte) (d + 48);
		else
			textBuffer[bPos] = (byte) (d + 55);
	}
}
