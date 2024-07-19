/* Copyright (C) 2006, 2007, 2008, 2009 Stefan Frenz
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

package sjc.compression;

/**
 * LZW: simple and fast LZW-compression
 *
 * @author S. Frenz
 * @version 090207 added copyright notice
 * version 060817 initial version
 */

public class LZW extends Compressor
{
	private boolean getMatch(byte[] source, int offset, int sourceSize, int[] hashBuffer, int[] paramBuf)
	{
		int hashValue, oldOffset, size, key, b0, b1, b2;
		boolean res = false;
		
		if (offset + 2 >= sourceSize)
			return false; //not enough data for hash-calculation
		b0 = (int) source[offset] & 0xFF;
		b1 = (int) source[offset + 1] & 0xFF;
		b2 = (int) source[offset + 2] & 0xFF;
		hashValue = ((40543 * ((((b0 << 4) ^ b1) << 4) ^ b2)) >>> 4) & 0xFFF;
		oldOffset = hashBuffer[hashValue];
		key = offset - oldOffset;
		if ((oldOffset != -1) && (key < 2048))
		{
			size = 0;
			while ((size < 18) && (offset + size < sourceSize))
			{
				if (source[offset + size] != source[oldOffset + size])
					break;
				size++;
			}
			if (size >= 3)
			{
				paramBuf[0] = key;
				paramBuf[1] = size;
				res = true;
			}
		}
		hashBuffer[hashValue] = offset;
		return res;
	}
	
	public int compress(byte[] src, int sourceSize, byte[] dst, int maxDestSize)
	{
		int bit = 0, command = 0, srcBuf, size, key;
		int sourceOffset = 0, destOffset = 2, destCommand = 0, i;
		int[] paramBuf, hashBuffer;
		
		maxDestSize -= 4;
		paramBuf = new int[2];
		hashBuffer = new int[4096];
		for (i = 0; i < 4096; i++)
			hashBuffer[i] = -1;
		
		while ((sourceOffset < sourceSize) && (destOffset <= maxDestSize))
		{
			if (bit > 15)
			{
				dst[destCommand] = (byte) command;
				dst[destCommand + 1] = (byte) (command >>> 8);
				destCommand = destOffset;
				destOffset += 2;
				bit = 0;
			}
			srcBuf = (int) src[sourceOffset] & 0xFF;
			size = 1;
			while ((sourceOffset + size < sourceSize) && (size < 135))
			{
				if (srcBuf != ((int) src[sourceOffset + size] & 0xFF))
					break;
				size++;
			}
			if (size >= 8)
			{
				dst[destOffset] = (byte) (size + 120); //size in 8..135 -> byte in 0x80..0xFF
				destOffset++;
				dst[destOffset] = (byte) srcBuf;
				destOffset++;
				sourceOffset += size;
				command = (command << 1) + 1;
			}
			else if (getMatch(src, sourceOffset, sourceSize, hashBuffer, paramBuf))
			{
				key = paramBuf[0];
				size = paramBuf[1];
				dst[destOffset] = (byte) (key >>> 4); //key in 0..2047 -> byte in 0x00..0x7F
				destOffset++;
				dst[destOffset] = (byte) (((key << 4) & 0xF0) | (size - 3)); //size in 3..18 -> lo-nibble in 0x0..0xF
				destOffset++;
				sourceOffset += size;
				command = (command << 1) + 1;
			}
			else
			{
				dst[destOffset] = (byte) srcBuf;
				sourceOffset++;
				destOffset++;
				command = command << 1;
			}
			bit++;
		}
		command = command << (16 - bit);
		dst[destCommand] = (byte) command;
		dst[destCommand + 1] = (byte) (command >>> 8);
		return destOffset;
	}
}
