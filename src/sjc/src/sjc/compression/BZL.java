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
 * BZL: slow but strong BWT/LZW compression
 *
 * @author S. Frenz
 * @version 091112 made status int to avoid implicit conversion
 * version 090207 added copyright notice
 * version 060817 initial version
 */

public class BZL extends Compressor
{
	//global variables
	private final static int BLOCK_SIZE = 1024 * 1024;
	private final static int RUN_LENGTH_MAX = 512;
	private final static int BWTBUF_SIZE = 65536;
	private final static int FF_BUG = 0x01;
	private final static int BLOCKERROR = 0x7F;
	private final static int CRCPOLY = 0x82608EDB;
	private byte[] mem;
	private short[] membuf16;
	private int[] membuf32, membwtbuf;
	private byte[] srcFile, dstFile;
	private int srcOffset, srcSize, dstOffset;
	
	//bwt encoding
	private final static int MAX_STACK = 1024;
	private final static int MAX_INSERTSORT = 20;
	private int sp;
	private int[] s_lb;
	private int[] s_ub;
	private int[] s_d;
	
	//m1ff2 encoding and decoding
	private int[] m1ff2ord;
	
	//zip encoding and decoding
	private final static int CODE_VALUE_BITS = 16;
	private final static int TOP_VALUE = ((1 << CODE_VALUE_BITS) - 1);
	private final static int FIRST_QTR = ((TOP_VALUE >> 2) + 1);
	private final static int HALF = ((TOP_VALUE >> 1) + 1);
	private final static int THIRD_QTR = (((3 * TOP_VALUE) >> 2) + 1);
	private final static int MAX_FREQ_MODEL = 16384;
	private final static int MAX_FREQUENCY2 = 128; /* 16384 */
	private final static int MAX_FREQUENCY1 = 256;
	private final static int NO_OF_SYMBOLS_0 = 3;
	private final static int NO_OF_GROUPS_0 = 16;
	private final static int NO_LEVEL_1 = (7 + 1);
	private final static int MULT = 2;
	private int byteBuffer, low, high, bits_to_go;
	private int bits_to_follow;
	private byte[] myBuf;
	private int myBufOff;
	private int[] SM1_i;        //even: char_to_index
	private int[][] tab_SM1_i;  //odd: index_to_char
	private int[] SM1_f;        //even: freq
	private int[][] tab_SM1_f;  //odd: cum
	private int[][] freq_tab_SM0;
	private int[] threshold;
	private int[][][] groups;
	private int[] tab_cum;
	
	public int compress(byte[] src, int sourceSize, byte[] dst, int maxDestSize)
	{
		int i, out_size, bleft, thisblocksize;
		int block_size = BLOCK_SIZE;
		
		bleft = sourceSize;
		if (bleft < block_size)
			block_size = bleft; //only one block => block_size==file_size
		
		srcFile = src;
		srcOffset = 0;
		srcSize = sourceSize;
		dstFile = dst;
		dstOffset = 16;
		out_size = 0;
		
		mem = new byte[(block_size + RUN_LENGTH_MAX + 1 + 32) * 4];
		membuf16 = new short[block_size + RUN_LENGTH_MAX + 32];
		membuf32 = new int[block_size + RUN_LENGTH_MAX + 32];
		membwtbuf = new int[BWTBUF_SIZE + 32];
		s_lb = new int[MAX_STACK];
		s_ub = new int[MAX_STACK];
		s_d = new int[MAX_STACK];
		m1ff2ord = new int[256];
		SM1_i = new int[(NO_LEVEL_1 + 1) * 2]; //even: char_to_index
		tab_SM1_i = new int[NO_LEVEL_1][256 * 2];  //odd: index_to_char
		SM1_f = new int[(NO_LEVEL_1 + 1) * 2]; //even: freq
		tab_SM1_f = new int[NO_LEVEL_1][256 * 2];  //odd: cum
		freq_tab_SM0 = new int[NO_OF_GROUPS_0][NO_OF_SYMBOLS_0];
		threshold = new int[NO_OF_GROUPS_0];
		groups = new int[NO_OF_SYMBOLS_0][NO_OF_SYMBOLS_0][NO_OF_SYMBOLS_0];
		tab_cum = new int[NO_OF_SYMBOLS_0 + 1];
		
		thisblocksize = block_size;
		for (i = 0; bleft > 0; ++i)
		{
			if (bleft < block_size)
				thisblocksize = bleft;
			out_size += compressBlock(thisblocksize);
			bleft -= thisblocksize;
		}
		
		dstOffset = 0;
		writeInt(block_size);
		writeInt(out_size); //packed_size
		writeInt(sourceSize); //orig_size
		writeInt(i); //nb_of_block
		return 16 + out_size;
	}
	
	private int readArr(byte[] dst, int offset, int max)
	{
		int i;
		
		if (srcOffset + max > srcSize)
			max = srcSize - srcOffset;
		i = offset;
		max += offset;
		while (i < max)
			dst[i++] = srcFile[srcOffset++];
		return max;
	}
	
	private void writeByte(byte val)
	{
		dstFile[dstOffset++] = val;
	}
	
	private void writeInt(int val)
	{
		dstFile[dstOffset++] = (byte) val;
		dstFile[dstOffset++] = (byte) (val >> 8);
		dstFile[dstOffset++] = (byte) (val >> 16);
		dstFile[dstOffset++] = (byte) (val >> 24);
	}
	
	private void writeArr(byte[] src, int offset, int len)
	{
		int i = 0;
		while (i < len)
			dstFile[dstOffset++] = src[offset + i++];
	}
	
	private int compressBlock(int size)
	{
		boolean ff_bug = false, err = false;
		int status = 0;
		int buffer1off, buffer2off, buf_out1off, buf_out2off = 0;
		int len, len2 = 0, len_max, first = 0, tot1 = 0, tot2 = 0, block_len;
		int crc, pos = 0, deb = 0;
		
		buffer1off = 0;
		pos = srcOffset;
		deb = dstOffset;
		
		len = readArr(mem, buffer1off, size);
		
		block_len = len;
		len_max = len;
		crc = calcCRC32(mem, buffer1off, len, -1);
		
		if (len > 64)
		{
			/*- Beginning ---- BWT ----------------*/
			/* to avoid a bug if the block ends with a run of 0xFF */
			if (mem[buffer1off + len - 1] == (byte) 0xFF)
			{
				mem[buffer1off + len - 1] -= mem[buffer1off + len - 2];
				ff_bug = true;
			}
			first = codeBWT(len, mem, membuf16, membuf32, membwtbuf);
			/*- End ---------- BWT ----------------*/
			
			/*- Beginning ---- MTF Coding ---------*/
			codeM1FF2(mem, len);
			/*- End ---------- MTF Coding ---------*/
			
			/*- Beginning ---- Split --------------*/
			//use buffer1 in four regions, all at 32 byte aligned offset
			buf_out1off = (buffer1off + len + 1 + 31) & ~31;
			buffer2off = (buf_out1off + len + 1 + 31) & ~31;
			buf_out2off = (buffer2off + len + 1 + 31) & ~31;
			len2 = split(mem, buffer1off, buffer1off + len, buffer2off);
			/*- End ---------- Split --------------*/
			
			/*- Beginning ---- Arith Compression --*/
			tot1 = codeZIP1(len, mem, buffer1off, buf_out1off);
			tot2 = codeZIP2(len2, mem, buffer2off, buf_out2off);
			/*- End ---------- Arith Compression --*/
			
			/* if we can't compress the block, we get the original block */
			if (tot1 + tot2 > len_max)
			{
				err = true;
				
				buf_out1off = 0;
				
				srcOffset = pos;
				tot1 = readArr(mem, buf_out1off, block_len);
				
				len_max = tot1;
			}
		} /* (len>50) */
		else
		{
			buf_out1off = buffer1off;
			err = true;
			tot1 = len;
		}
		
		/*- Beginning ---- Writings -----------*/
		
		/*  status
		 *
		 *  8   7   6   5   4   3   2   1
		 *    |err|                   |ffb|
		 */
		if (ff_bug)
			status |= FF_BUG;
		if (err)
			status |= BLOCKERROR;
		
		/* we add a small margin, in case... */
		len_max += 32;
		
		writeInt(len_max);
		writeInt(crc);
		writeByte((byte) status);
		if ((status & BLOCKERROR) == 0)
		{
			writeInt(len);
			writeInt(len2);
			writeInt(first);
			writeInt(tot2);
			writeArr(mem, buf_out2off, tot2);
		}
		writeInt(tot1);
		writeArr(mem, buf_out1off, tot1);
		
		/*- End ---------- Writings -----------*/
		
		return dstOffset - deb;
	}
	
	private int calcCRC32(byte[] arr, int off, int off_end, int crc)
	{
		int i, j;
		
		for (i = off; i < off_end; i++)
		{
			crc ^= ((int) arr[i]) & 0xFF;
			for (j = 0; j < 8; j++)
			{
				if ((crc & 1) != 0)
					crc = (crc >>> 1) ^ CRCPOLY;
				else
					crc = crc >>> 1;
			}
		}
		return crc;
	}
	
	private int key(short[] arr, int[] off, int rel, int depth)
	{
		return ((int) arr[off[rel] + depth]) & 0xFFFF;
	}
	
	private int median(int a, int b, int c)
	{
		if (a < b)
		{
			if (b < c)
				return b;
			else if (a < c)
				return c;
			else
				return a;
		}
		else
		{
			if (a < c)
				return a;
			else if (b < c)
				return c;
			else
				return b;
		}
	}
	
	private void shellSort(short[] arr, int[] off, int lowerb, int upperb, int depth)
	{
		int i, j, baoff, bboff, t;
		
		if (upperb - lowerb > 10)
		{
			for (i = lowerb + 4; i <= upperb; ++i)
			{
				t = off[i];
				for (j = i - 4; j >= lowerb; j -= 4)
				{
					baoff = depth + off[j];
					bboff = depth + t;
					while (arr[bboff] == arr[baoff])
					{
						baoff++;
						bboff++;
					}
					if ((((int) arr[bboff]) & 0xFFFF) > (((int) arr[baoff]) & 0xFFFF))
						break;
					off[j + 4] = off[j];
				}
				off[j + 4] = t;
			}
		}
		
		for (i = lowerb + 1; i <= upperb; ++i)
		{
			t = off[i];
			for (j = i - 1; j >= lowerb; --j)
			{
				baoff = depth + off[j];
				bboff = depth + t;
				while (arr[bboff] == arr[baoff])
				{
					baoff++;
					bboff++;
				}
				if ((((int) arr[bboff]) & 0xFFFF) > (((int) arr[baoff]) & 0xFFFF))
					break;
				off[j + 1] = off[j];
			}
			off[j + 1] = t;
		}
	}
	
	private void push(int a, int b, int c)
	{
		s_lb[sp] = a;
		s_ub[sp] = b;
		s_d[sp] = c;
		sp++;
	}
	
	private void ternarySort(short[] arr, int[] off, int lowerb_t, int upperb_t, int depth_t)
	{
		int v, depth, n_v, t;
		int lb, ub, i, j, lowerb, upperb, lb_v, ub_v;
		int r, r1, r2, r3;
		int v1, v2, v3, v_1, v_2, v_3;
		
		lowerb = lowerb_t;
		upperb = upperb_t;
		depth = depth_t;
		
		sp = 1;
		while (sp > 0)
		{
			r = upperb - lowerb + 1;
			
			if (r <= MAX_INSERTSORT)
			{
				if (r >= 2)
					shellSort(arr, off, lowerb, upperb, depth);
				--sp;
				lowerb = s_lb[sp];
				upperb = s_ub[sp];
				depth = s_d[sp];
				continue;
			}
			
			if (r > 64)
			{ /* median-of-3 median-of-3 */
				r = r >> 3;
				lb = lowerb;
				
				v_1 = key(arr, off, lb, depth);
				v_2 = key(arr, off, lb += r, depth);
				v_3 = key(arr, off, lb += r, depth);
				v1 = median(v_1, v_2, v_3);
				
				v_1 = key(arr, off, lb += r, depth);
				v_2 = key(arr, off, lb += r, depth);
				v_3 = key(arr, off, lb += r, depth);
				v2 = median(v_1, v_2, v_3);
				
				v_1 = key(arr, off, lb += r, depth);
				v_2 = key(arr, off, lb + r, depth);
				v_3 = key(arr, off, upperb, depth);
				v3 = median(v_1, v_2, v_3);
				
				v = median(v1, v2, v3);
			}
			else
			{ /* median-of-3 */
				v1 = key(arr, off, lowerb, depth);
				v2 = key(arr, off, lowerb + (r >> 1), depth);
				v3 = key(arr, off, upperb, depth);
				v = median(v1, v2, v3);
			}
			
			i = lb = lowerb;
			j = ub = upperb;
			
			while (true)
			{
				while (i <= j && (r = key(arr, off, i, depth) - v) <= 0)
				{
					if (r == 0)
					{
						t = off[i];
						off[i] = off[lb];
						off[lb++] = t;
					}
					i++;
				}
				while (i <= j && (r = key(arr, off, j, depth) - v) >= 0)
				{
					if (r == 0)
					{
						t = off[j];
						off[j] = off[ub];
						off[ub--] = t;
					}
					j--;
				}
				
				if (i > j)
					break;
				
				t = off[i];
				off[i++] = off[j];
				off[j--] = t;
			}
			
			if (ub < lb)
			{
				depth += 2;
				continue;
			}
			
			lb_v = lowerb;
			ub_v = i - 1;
			if ((lb - lowerb) < (i - lb))
				n_v = lb - lowerb;
			else
				n_v = i - lb;
			for (; n_v > 0; --n_v)
			{
				t = off[lb_v];
				off[lb_v] = off[ub_v];
				off[ub_v] = t;
				lb_v++;
				ub_v--;
			}
			
			lb_v = i;
			ub_v = upperb;
			if ((ub - j) < (upperb - ub))
				n_v = ub - j;
			else
				n_v = upperb - ub;
			for (; n_v > 0; --n_v)
			{
				t = off[lb_v];
				off[lb_v] = off[ub_v];
				off[ub_v] = t;
				lb_v++;
				ub_v--;
			}
			
			/* sort the smallest partition    */
			/* to minimize stack requirements */
			r1 = (i - lb) - 1;
			r2 = (ub - i);
			r3 = (upperb - lowerb) + (lb - ub) - 1;
			
			if (r1 < r2)
			{  /* r1<r2 */
				if (r3 <= r1)
				{ /* r3<=r1<r2 */
					push(upperb - r2, upperb, depth);
					push(lowerb, lowerb + r1, depth);
					lowerb += r1 + 1;
					upperb -= r2 + 1;
					depth += 2;
				}
				else if (r3 <= r2)
				{  /* r1<r3<=r2 */
					push(upperb - r2, upperb, depth);
					push(lowerb + r1 + 1, upperb - r2 - 1, depth + 2);
					upperb = lowerb + r1;
				}
				else
				{  /* r1<r2<r3 */
					push(lowerb + r1 + 1, upperb - r2 - 1, depth + 2);
					push(upperb - r2, upperb, depth);
					upperb = lowerb + r1;
				}
			}
			else
			{   /* r1>r2 */
				if (r3 >= r1)
				{ /* r3>=r1>r2 */
					push(lowerb + r1 + 1, upperb - r2 - 1, depth + 2);
					push(lowerb, lowerb + r1, depth);
					lowerb = upperb - r2;
				}
				else if (r3 >= r2)
				{  /* r1>r3>=r2 */
					push(lowerb, lowerb + r1, depth);
					push(lowerb + r1 + 1, upperb - r2 - 1, depth + 2);
					lowerb = upperb - r2;
				}
				else
				{  /* r1>r2>r3 */
					push(lowerb, lowerb + r1, depth);
					push(upperb - r2, upperb, depth);
					lowerb += r1 + 1;
					upperb -= r2 + 1;
					depth += 2;
				}
			}
		}
	}
	
	private int codeBWT(int len, byte[] bufinout, short[] buffer16, int[] pointer, int[] bwtbuf)
	{
		int i, val, l, k, j, res, deb, buoff, bulen, pp;
		
		for (i = 0; i < BWTBUF_SIZE; i++)
			bwtbuf[i] = 0;
		val = (((int) bufinout[0]) & 0xFF);
		bufinout[len] = (byte) -1;
		
		for (i = 0; i < len; ++i)
		{
			val = ((val & 0xFF) << 8) | (((int) bufinout[i + 1]) & 0xFF); /* strip buffer over words */
			bwtbuf[val]++;                /* update bucket sizes */
			buffer16[i] = (short) val;     /* store word */
		}
		j = len + RUN_LENGTH_MAX + 32;
		for (; i < j; i++)
			buffer16[i] = (short) 0xFFFF;
		
		/* compute start offset of each bucket */
		l = bwtbuf[0];
		bwtbuf[0] = 0;
		
		for (j = 1; j < BWTBUF_SIZE; ++j)
		{
			k = bwtbuf[j];
			bwtbuf[j] = bwtbuf[j - 1] + l;
			l = k;
		}
		
		/* finalize bucket sorting */
		bulen = len;
		for (buoff = 0; buoff < bulen; ++buoff)
		{
			pointer[bwtbuf[((int) buffer16[buoff]) & 0xFFFF]++] = buoff;
		}
		
		/* finalize BWT sort by sorting each bucket */
		deb = 0;
		l = bwtbuf[0];
		for (j = 0; j < BWTBUF_SIZE; ++j)
		{
			if (l > 1)
			{
				if (l <= (MAX_INSERTSORT + 2))
					shellSort(buffer16, pointer, deb, deb + l - 1, 2);
				else
					ternarySort(buffer16, pointer, deb, deb + l - 1, 2);
			}
			deb += l;
			l = bwtbuf[j + 1] - bwtbuf[j]; /* width of next bucket */
		}
		
		i = 0;
		pp = pointer[0];
		
		while (pp != 0)
		{
			bufinout[i] = (byte) (buffer16[pp - 1] >> 8);
			pp = pointer[++i];
		}
		
		bufinout[i] = (byte) (buffer16[len - 1] >> 8);
		res = i;
		i++;
		
		for (; i < len; ++i)
			bufinout[i] = (byte) (buffer16[pointer[i] - 1] >> 8);
		
		return res;
	}
	
	private void codeM1FF2(byte[] bufinout, int len)
	{
		int i, c, l = 0, flag = 0;
		
		for (i = 0; i < 256; ++i)
			m1ff2ord[i] = i;
		
		for (i = 0; i < len; ++i)
		{
			c = (((int) bufinout[i]) & 0xFF);
			l = 0;
			while (m1ff2ord[l] != c)
				l++;
			bufinout[i] = (byte) l;
			
			if (l > 1)
			{
				for (; l > 1; --l)
					m1ff2ord[l] = m1ff2ord[l - 1];
				m1ff2ord[1] = c;
				l = 1;
			}
			else if ((l == 1) && flag == 1)
			{
				m1ff2ord[1] = m1ff2ord[0];
				m1ff2ord[0] = c;
			}
			flag = l;
		}
	}
	
	private int split(byte[] bufinout, int off, int end, int dest)
	{
		int od = dest;
		
		for (; off < end; ++off)
			if ((((int) bufinout[off]) & 0xFF) >= 2)
			{
				bufinout[dest++] = bufinout[off];
				bufinout[off] = (byte) 2;
			}
		
		return dest - od;
	}
	
	private void bitPlusFollow1()
	{
		byteBuffer = byteBuffer >> 1;
		byteBuffer |= 0x80;
		if (--bits_to_go == 0)
		{
			myBuf[myBufOff++] = (byte) byteBuffer;
			bits_to_go = 8;
		}
		for (; bits_to_follow > 0; --bits_to_follow)
		{
			byteBuffer = byteBuffer >> 1;
			if (--bits_to_go == 0)
			{
				myBuf[myBufOff++] = (byte) byteBuffer;
				bits_to_go = 8;
			}
		}
	}
	
	private void bitPlusFollow0()
	{
		byteBuffer = byteBuffer >> 1;
		if (--bits_to_go == 0)
		{
			myBuf[myBufOff++] = (byte) byteBuffer;
			bits_to_go = 8;
		}
		for (; bits_to_follow > 0; --bits_to_follow)
		{
			byteBuffer = byteBuffer >> 1;
			byteBuffer |= 0x80;
			if (--bits_to_go == 0)
			{
				myBuf[myBufOff++] = (byte) byteBuffer;
				bits_to_go = 8;
			}
		}
	}
	
	private void encodeSymbolSM0(int symbol, int[] cumfreq, int cumfreq0)
	{
		int range = high - low + 1;
		
		high = low + ((cumfreq[symbol - 1] * range) / cumfreq0) - 1;
		low += (cumfreq[symbol] * range) / cumfreq0;
		for (; ; )
		{
			if (high < HALF)
				bitPlusFollow0();
			else if (low >= HALF)
			{
				bitPlusFollow1();
				low -= HALF;
				high -= HALF;
			}
			else if ((low >= FIRST_QTR) && (high < THIRD_QTR))
			{
				bits_to_follow++;
				low -= FIRST_QTR;
				high -= FIRST_QTR;
			}
			else
				break;
			low = low << 1;
			high = high << 1;
			high++;
		}
	}
	
	private void encodeSymbolSM1(int symbol, int[] t, int cumfreq0)
	{
		int range = high - low + 1;
		
		high = low + ((t[(symbol - 1) * 2 + 1] * range) / cumfreq0) - 1;
		low += (t[(symbol) * 2 + 1] * range) / cumfreq0;
		for (; ; )
		{
			if (high < HALF)
				bitPlusFollow0();
			else if (low >= HALF)
			{
				bitPlusFollow1();
				low -= HALF;
				high -= HALF;
			}
			else if ((low >= FIRST_QTR) && (high < THIRD_QTR))
			{
				bits_to_follow++;
				low -= FIRST_QTR;
				high -= FIRST_QTR;
			}
			else
				break;
			low = low << 1;
			high = high << 1;
			high++;
		}
	}
	
	private void startOutputingBits()
	{
		byteBuffer = 0;
		bits_to_go = 8;
	}
	
	private void doneOutputingBits()
	{
		myBuf[myBufOff++] = (byte) (byteBuffer >>> bits_to_go);
	}
	
	private void startEncoding()
	{
		low = 0;
		high = TOP_VALUE;
		bits_to_follow = 0;
	}
	
	private void doneEncoding()
	{
		bits_to_follow++;
		if (low < FIRST_QTR)
			bitPlusFollow0();
		else
			bitPlusFollow1();
	}
	
	private void startModel1()
	{
		int i, j;
		
		groups[0][0][0] = 0;
		threshold[0] = 128 * MULT;
		
		groups[0][0][1] = 1;
		threshold[1] = 48 * MULT;
		
		groups[0][1][0] = 2;
		groups[1][1][0] = 2;
		groups[1][0][0] = 2;
		threshold[2] = 64 * MULT;
		
		groups[0][1][1] = 3;
		groups[1][1][1] = 3;
		threshold[3] = 64 * MULT;
		
		groups[0][2][0] = 4;
		groups[2][0][0] = 4;
		groups[2][1][0] = 4;
		threshold[4] = 72 * MULT;
		
		groups[1][2][1] = 5;
		groups[1][2][0] = 5;
		threshold[5] = 72 * MULT;
		
		groups[2][2][1] = 6;
		groups[2][2][0] = 6;
		threshold[6] = 72 * MULT;
		
		groups[2][0][1] = 7;
		groups[0][0][2] = 7;
		threshold[7] = 72 * MULT;
		
		groups[0][1][2] = 8;
		groups[1][1][2] = 8;
		groups[2][0][2] = 8;
		threshold[8] = 72 * MULT;
		
		groups[2][1][2] = 9;
		groups[0][2][2] = 9;
		groups[1][2][2] = 9;
		threshold[9] = 72 * MULT;
		
		groups[0][2][1] = 10;
		groups[2][1][1] = 10;
		threshold[10] = 72 * MULT;
		
		groups[1][0][2] = 11;
		threshold[11] = 64 * MULT;
		
		groups[1][0][1] = 12;
		threshold[12] = 64 * MULT;
		
		groups[2][2][2] = 13;
		threshold[13] = 128 * MULT;
		
		for (i = 0; i < NO_OF_GROUPS_0; i++)
			for (j = 0; j < NO_OF_SYMBOLS_0; j++)
				freq_tab_SM0[i][j] = 0;
	}
	
	private int getCumEncode(int[] cum_freq, int freq1, int freq2, int freq3)
	{
		freq1++;
		freq2++;
		freq3++;
		cum_freq[0] = freq1 + freq2 + freq3;
		cum_freq[3] = 0;
		
		if (freq1 >= freq2)
		{ /* freq1>=freq2 */
			if (freq2 >= freq3)
			{ /* freq1>=freq2>=freq3 */
				cum_freq[1] = freq2 + freq3;
				cum_freq[2] = freq3;
				return (3 << 4) | (2 << 2) | 1;
			}
			else
			{ /* freq3>freq2 */
				cum_freq[2] = freq2;
				if (freq3 >= freq1)
				{ /* freq3>=freq1>=freq2 */
					cum_freq[1] = freq1 + freq2;
					return (1 << 4) | (3 << 2) | 2;
				}
				else
				{ /* freq1>freq3>=freq2 */
					cum_freq[1] = freq3 + freq2;
					return (2 << 4) | (3 << 2) | 1;
				}
			}
		}
		else
		{ /* freq2>freq1 */
			if (freq1 >= freq3)
			{ /* freq2>freq1>freq3 */
				cum_freq[1] = freq1 + freq3;
				cum_freq[2] = freq3;
				return (3 << 4) | (1 << 2) | 2;
			}
			else
			{ /* freq3>=freq1 */
				cum_freq[2] = freq1;
				if (freq2 >= freq3)
				{ /* freq2>freq3>freq1 */
					cum_freq[1] = freq3 + freq1;
					return (2 << 4) | (1 << 2) | 3;
				}
				else
				{ /* freq3>=freq2>freq1 */
					cum_freq[1] = freq2 + freq1;
					return (1 << 4) | (2 << 2) | 3;
				}
			}
		}
	}
	
	private int codeZIP1(int len, byte[] buf, int bufinOff, int scratchBufferOff)
	{
		boolean in_run = true;
		int ch1 = 0, ch2 = 0, ch3 = 0, i = 0, group, symbol, ch, max;
		int[] freq_tab;
		
		myBuf = buf;
		myBufOff = scratchBufferOff;
		
		startModel1();
		startOutputingBits();
		startEncoding();
		
		while (i < len)
		{
			ch = (int) buf[bufinOff + i++] & 0xFF;
			
			group = groups[ch3][ch2][ch1];
			freq_tab = freq_tab_SM0[group];
			
			symbol = getCumEncode(tab_cum, freq_tab[0], freq_tab[1], freq_tab[2]);
			symbol = (symbol >> (ch * 2)) & 3;
			encodeSymbolSM0(symbol, tab_cum, tab_cum[0]);
			
			ch3 = ch2;
			ch2 = ch1;
			ch1 = ch;
			
			if (!in_run && (ch | group) == 0)
			{
				in_run = true;
				freq_tab[0] = freq_tab[0] >> 1;
				freq_tab[0] += MULT + 1;
				freq_tab[1] = freq_tab[1] >> 1;
				freq_tab[2] = freq_tab[2] >> 1;
			}
			else
			{
				if (ch != 0)
					in_run = false;
				
				if (in_run)
					max = 8 * 1024;
				else
					max = threshold[group];
				if (tab_cum[0] > max)
				{
					freq_tab[0] = freq_tab[0] >> 1;
					freq_tab[1] = freq_tab[1] >> 1;
					freq_tab[2] = freq_tab[2] >> 1;
				}
				
				freq_tab[ch] += MULT;
			}
		}
		
		doneEncoding();
		doneOutputingBits();
		
		return myBufOff - scratchBufferOff;
	}
	
	private void startModel2deep(int[] t_i, int[] t_f)
	{
		int i;
		
		for (i = 0; i < t_f[0]; ++i)
		{
			t_i[i * 2] = i + 1;
			t_i[(i + 1) * 2 + 1] = i;
		}
		
		t_f[0 + 1] = t_f[0];
		for (i = 1; i <= t_f[0]; ++i)
		{
			t_f[i * 2] = 1;
			t_f[i * 2 + 1] = t_f[0] - i;
		}
	}
	
	private void startModel2()
	{
		int i, cum = 0, max, l;
		
		for (i = 0; i < NO_LEVEL_1; ++i)
		{
			SM1_i[i * 2] = i + 1;
			SM1_i[(i + 1) * 2 + 1] = i;
		}
		
		SM1_f[(0 + 1) * 2] = 1;
		i = 1;
		
		max = 254;
		while (max > 0)
		{
			l = 1 << i;
			if (max < l)
			{
				i--;
				tab_SM1_f[i][0] += max;
				tab_SM1_f[i + 1][0] = 0;
				startModel2deep(tab_SM1_i[i], tab_SM1_f[i]);
				max = 0;
			}
			else
			{
				max -= l;
				SM1_f[(i + 1) * 2] = 1;
				tab_SM1_f[i][0] = l;
				startModel2deep(tab_SM1_i[i], tab_SM1_f[i]);
			}
			i++;
		}
		
		SM1_f[0] = i;
		for (; i > 0; --i)
		{
			SM1_f[i * 2 + 1] = cum;
			cum += SM1_f[i * 2];
		}
		SM1_f[0 + 1] = cum;
	}
	
	private void updateModel2(int symbol, int[] t_i, int[] t_f)
	{
		int k, fs, cum, max = t_f[0] * MAX_FREQUENCY2, ch_k, ch_symbol;
		
		if (max > MAX_FREQ_MODEL)
			max = MAX_FREQ_MODEL;
		
		if (t_f[0 + 1] >= max)
		{
			cum = 0;
			for (k = t_f[0]; k > 0; --k)
			{
				t_f[k * 2 + 1] = cum;
				cum += (t_f[k * 2] = (t_f[k * 2] + 1) >> 1);
			}
			t_f[0 + 1] = cum;
		}
		
		k = symbol - 1;
		fs = t_f[symbol * 2];
		while ((k > 0) && (fs == t_f[k * 2]))
			k--;
		
		if (++k < symbol)
		{
			ch_k = t_i[k * 2 + 1];
			ch_symbol = t_i[symbol * 2 + 1];
			t_i[k * 2 + 1] = ch_symbol;
			t_i[symbol * 2 + 1] = ch_k;
			t_i[ch_k * 2] = symbol;
			t_i[ch_symbol * 2] = k;
		}
		
		t_f[k * 2]++;
		while (k > 0)
			t_f[(--k) * 2 + 1]++;
	}
	
	private void updateModel1(int smbl)
	{
		int k, fs, cum, symbol = smbl;
		int ch_k, ch_symbol;
		
		if (SM1_f[0 + 1] >= MAX_FREQUENCY1)
		{
			cum = 0;
			for (k = SM1_f[0]; k > 0; --k)
			{
				SM1_f[k * 2 + 1] = cum;
				cum += (SM1_f[k * 2] = (SM1_f[k * 2] + 1) >> 1);
			}
			SM1_f[0 + 1] = cum;
		}
		
		k = symbol - 1;
		fs = SM1_f[symbol * 2];
		while ((k > 0) && (fs == SM1_f[k * 2]))
			k--;
		
		if (++k < symbol)
		{
			ch_k = SM1_i[k * 2 + 1];
			ch_symbol = SM1_i[symbol * 2 + 1];
			SM1_i[k * 2 + 1] = ch_symbol;
			SM1_i[symbol * 2 + 1] = ch_k;
			SM1_i[ch_k * 2] = symbol;
			SM1_i[ch_symbol * 2] = k;
		}
		
		symbol = k;
		k--;
		fs++;
		while ((k > 0) && (fs == SM1_f[k * 2]))
			k--;
		
		if (++k < symbol)
		{
			ch_k = SM1_i[k * 2 + 1];
			ch_symbol = SM1_i[symbol * 2 + 1];
			SM1_i[k * 2 + 1] = ch_symbol;
			SM1_i[symbol * 2 + 1] = ch_k;
			SM1_i[ch_k * 2] = symbol;
			SM1_i[ch_symbol * 2] = k;
		}
		
		SM1_f[k * 2]++;
		SM1_f[symbol * 2]++;
		while (symbol > k)
			SM1_f[(--symbol) * 2 + 1]++;
		while (symbol > 0)
			SM1_f[(--symbol) * 2 + 1] += 2;
	}
	
	private int codeZIP2(int len, byte[] buf, int bufinOff, int scratchBufferOff)
	{
		int i = 0, j, ch, symbol;
		
		myBuf = buf;
		myBufOff = scratchBufferOff;
		
		startModel2();
		startOutputingBits();
		startEncoding();
		
		while (i < len)
		{
			ch = (((int) buf[bufinOff + i++]) & 0xFF) - 2;
			
			j = 0;
			if (ch != 0)
			{
				ch++;
				while ((ch >> ++j) != 1)
					;
				if (tab_SM1_f[j][0] == 0)
					j--;
				ch -= 1 << j;
			}
			
			symbol = SM1_i[j * 2];
			encodeSymbolSM1(symbol, SM1_f, SM1_f[0 + 1]);
			updateModel1(symbol);
			
			if (j != 0)
			{
				symbol = tab_SM1_i[j][ch * 2];
				encodeSymbolSM1(symbol, tab_SM1_f[j], tab_SM1_f[j][0 + 1]);
				updateModel2(symbol, tab_SM1_i[j], tab_SM1_f[j]);
			}
		}
		
		doneEncoding();
		doneOutputingBits();
		
		return myBufOff - scratchBufferOff;
	}
}
