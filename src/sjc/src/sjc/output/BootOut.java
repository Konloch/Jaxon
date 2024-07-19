/* Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012 Stefan Frenz
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

import sjc.memory.ImageContainer;
import sjc.osio.BinWriter;
import sjc.osio.OsIO;
import sjc.osio.TextPrinter;
import sjc.osio.TextReader;

/**
 * BootOut: bootable output-file, configurable with a textfile
 *
 * @author S. Frenz
 * @version 120227 cleaned up "package sjc." typo
 * version 101027 fixed ignoring additional section names
 * version 101021 fixed block padding and keyword detection
 * version 101018 fixed section search if searched name contains non-alpha-characters
 * version 100513 adopted changed TextReader
 * version 100504 adopted changed BinWriter
 * version 090303 adopted changed osio package structure
 * version 090207 added copyright notice
 * version 080202 adopted removal of TextReader.eof()
 * version 070725 added support for quotes in strings
 * version 070723 fixed calculation
 * version 070721 removed isOpSeperator
 * version 070105 removed prefixing
 * version 061221 integrated hexout
 * version 060818 added support for compression-keywords
 * version 060707 integrated confscan, values with simple expressions
 * version 060628 adapted new BinWriter
 * version 060607 initial version
 */

public class BootOut extends OutputFormat
{
	private final static int K_INVALID = -1;
	private final static int K_NONE = 0;
	
	//keyword to start and end a section
	private final static int K_SECTION = 1;
	private final static int K_ENDSECT = 2;
	
	//keywords to specify the image
	private final static int K_DESTFILE = 3;
	private final static int K_HEXDFILE = 4;
	private final static int K_SETADDR = 5;
	private final static int K_BLOCKSIZE = 6;
	private final static int K_MAXIMGSIZE = 7;
	private final static int K_BLOWIMAGE = 8;
	private final static int K_FILLIMAGE = 9;
	private final static int K_APPENDIMG = 10;
	private final static int K_READBUF = 11;
	private final static int K_WRITEBUF = 12;
	
	//keywords to place numbers
	private final static int K_OFFSET = 13;
	private final static int K_VALUE = 14;
	private final static int K_IMAGEADDR = 15;
	private final static int K_IMAGESIZE = 16;
	private final static int K_BLOCKCNT = 17;
	private final static int K_CRC = 18;
	private final static int K_UNITADDR = 19;
	private final static int K_CODEADDR = 20;
	private final static int K_COMPRADDR = 21;
	private final static int K_COMPRSIZE = 22;
	
	//constants
	private final static int LINELENGTH = 255;
	private final static String ERR_IMAGEDONE = "image already appended, give blow/fill after maximagesize";
	
	private final static String DEFFILE = "bootconf.txt";
	private final static String DEFSECT = "default";
	private String file, sect;
	private boolean gotParm;
	
	private int curLine;
	private boolean error;
	private int imageSize, blockCnt, compressedImageOrigLen;
	
	private OsIO osio;
	private TextPrinter out;
	private ImageContainer img, compressedImg;
	private TextReader r;
	private char[] buffer;
	private int bufLen, bufPos;
	
	public static void printValidParameters(TextPrinter v)
	{
		v.println("   FILE#SECT - use file FILE and therein section SECT");
		v.print("               default is ");
		v.print(DEFFILE);
		v.print("#");
		v.println(DEFSECT);
	}
	
	public boolean setParameter(String parm, TextPrinter v)
	{
		int splitter;
		
		if (gotParm)
		{
			v.println("BootOut expects only one parameter containing file and section");
			return false;
		}
		gotParm = true;
		splitter = parm.indexOf('#');
		if (splitter == -1)
			file = parm;
		else
		{
			if (splitter > 0)
				file = parm.substring(0, splitter);
			sect = parm.substring(splitter + 1);
			if (sect.indexOf('#') != -1)
			{
				v.println("BootOut with invalid parameter (multiple \"#\")");
				return false;
			}
		}
		return true;
	}
	
	public boolean checkParameter(OsIO io, TextPrinter v)
	{
		TextReader configFile;
		
		osio = io;
		out = v;
		configFile = new TextReader();
		if (file == null)
			file = DEFFILE;
		if (sect == null)
			sect = DEFSECT;
		if (!configFile.initData(osio.readFile(file)))
		{ //file not found
			out.print("BootOut could not open configuration file ");
			out.println(file);
			return false;
		}
		buffer = new char[LINELENGTH];
		if (!init(configFile, sect))
		{ //section not found
			out.print(" for BootOut in file ");
			out.println(file);
			return false;
		}
		//file and section found, everything else will be checked and done later
		return true;
	}
	
	public boolean writeOutput(ImageContainer iic, ImageContainer iciic, int icil)
	{
		String strDummy;
		BinWriter w = null;
		int i, scratchSize, blockSize = 1, byteCnt, maxSize = -1;
		int pos, len, crcPoly, filler = 0;
		char lb;
		boolean blowImage = false, fillImage = false, imageDone = false;
		byte[] scratch = null;
		
		img = iic;
		imageSize = img.memBlockLen;
		compressedImg = iciic;
		compressedImageOrigLen = icil;
		do
		{
			switch (getKey())
			{
				case K_INVALID:
					out.print("Unknown keyword");
					errCloseLine();
					return false;
				case K_NONE: //nothing here -> ignore
				case K_SECTION: //section-alias -> ignore
					bufPos = bufLen; //skip rest of line
					break;
				case K_ENDSECT:
					if (w != null)
						w.close();
					return true;
				case K_DESTFILE:
					if ((w = openDestFile(w, K_DESTFILE)) == null)
						return false; //output already done
					break;
				case K_HEXDFILE:
					if ((w = openDestFile(w, K_HEXDFILE)) == null)
						return false; //output already done
					break;
				case K_SETADDR:
					removeBlanks();
					if ((i = getValue()) < 0)
					{
						out.print("Invalid address");
						errCloseLine();
						return false;
					}
					if (!w.setAddress(i))
					{
						out.print("Keyword \"setaddr\" invalid for current writer, try hex output");
						errCloseLine();
						return false;
					}
					break;
				case K_BLOCKSIZE:
					removeBlanks();
					if ((blockSize = getNum()) < 1)
					{
						out.print("Invalid blocksize");
						errCloseLine();
						return false;
					}
					blockCnt = (imageSize + blockSize - 1) / blockSize;
					break;
				case K_MAXIMGSIZE:
					removeBlanks();
					maxSize = getNum();
					if (maxSize < 1)
					{
						out.print("Invalid maximum image size");
						errCloseLine();
						return false;
					}
					if (imageSize > maxSize)
					{
						out.print("Image to big for destination, reduce below ");
						out.print(maxSize >>> 10);
						out.println(" KB");
						return false;
					}
					break;
				case K_BLOWIMAGE:
					if (fillImage)
					{
						out.print("Can not blow, fill is already given");
						errCloseLine();
						return false;
					}
					if (imageDone)
					{
						out.print(ERR_IMAGEDONE);
						errCloseLine();
						return false;
					}
					blowImage = true;
					break;
				case K_FILLIMAGE:
					if (blowImage)
					{
						out.print("Can not fill, blow is already given");
						errCloseLine();
						return false;
					}
					if (imageDone)
					{
						out.print(ERR_IMAGEDONE);
						errCloseLine();
						return false;
					}
					fillImage = true;
					removeBlanks();
					filler = getNum();
					if (filler < 0 || filler > 255)
					{
						out.print("Invalid or no filler given, specify 0-0xFF as filling byte");
						errCloseLine();
						return false;
					}
					break;
				case K_APPENDIMG:
					if (w == null)
					{
						out.print("No file given before write");
						errCloseLine();
						return false;
					}
					if (!img.appendImage(w))
					{
						out.println("Error writing to destination");
						return false;
					}
					byteCnt = imageSize;
					if (blockSize > 1)
					{
						if (scratch == null || scratch.length < blockSize)
							scratch = new byte[blockSize];
						for (i = 0; i < blockSize; i++)
							scratch[i] = (byte) 0;
						i = blockSize - (imageSize % blockSize);
						if (i == blockSize)
							i = 0;
						else if (!w.write(scratch, 0, i))
						{
							out.println("Error writing to destination for block-fill");
							return false;
						}
						byteCnt += i;
					}
					if (blowImage)
					{
						if (maxSize == -1)
						{
							out.print("Give image size before appending blown image");
							errCloseLine();
							return false;
						}
						if (!w.setSize(maxSize - byteCnt))
						{
							out.print("Error blowing destination file");
							return false;
						}
					}
					if (fillImage)
					{
						if (maxSize == -1)
						{
							out.print("Give image size before appending filled image");
							errCloseLine();
							return false;
						}
						if (blockSize < 512)
							scratchSize = 512;
						else
							scratchSize = blockSize;
						if (scratch == null || scratch.length < scratchSize)
							scratch = new byte[scratchSize];
						for (i = 0; i < scratchSize; i++)
							scratch[i] = (byte) filler;
						byteCnt = maxSize - byteCnt;
						while (byteCnt > 0)
						{
							if (byteCnt >= scratchSize)
							{
								w.write(scratch, 0, scratchSize);
								byteCnt -= scratchSize;
							}
							else
							{
								w.write(scratch, 0, byteCnt);
								break;
							}
						}
					}
					imageDone = true;
					break;
				case K_READBUF:
					removeBlanks();
					if ((strDummy = getString()) == null)
					{
						out.print("No filename given");
						errCloseLine();
						return false;
					}
					if ((scratch = osio.readFile(strDummy)) == null)
					{
						out.print("Error reading file ");
						out.println(strDummy);
						return false;
					}
					break;
				case K_WRITEBUF:
					if (w == null)
					{
						out.print("No file given before write");
						errCloseLine();
						return false;
					}
					w.write(scratch, 0, scratch.length);
					break;
				case K_OFFSET:
					removeBlanks();
					if ((pos = getNum()) < 0 || getChar() != '.' || ((lb = getChar()) != 'b' && lb != 'l') || ((len = getNum()) != 1 && len != 2 && len != 4))
					{
						out.print("Expected position, '.', l/b and size for offset");
						errCloseLine();
						return false;
					}
					removeBlanks();
					switch (getKey())
					{
						case K_CRC:
							removeBlanks();
							crcPoly = getNum();
							if (crcPoly == -1)
							{
								out.print("Invalid or no crc");
								errCloseLine();
								return false;
							}
							if (!placeValue(scratch, pos, lb, len, img.getCRC(crcPoly, blockSize), out))
							{
								errCloseLine();
								return false;
							}
							break;
						case K_VALUE:
							removeBlanks();
							i = getValue();
							if (error)
							{
								out.print("Invalid value to write");
								errCloseLine();
								return false;
							}
							if (!placeValue(scratch, pos, lb, len, i, out))
							{
								errCloseLine();
								return false;
							}
							break;
						default:
							out.print("Wrong keyword for offset");
							errCloseLine();
							return false;
					}
					break;
				default:
					out.print("Unexpected statement");
					errCloseLine();
					return false;
			}
			removeBlanks();
			if (bufPos != bufLen)
			{
				out.print("Extra characters");
				errCloseLine();
				return false;
			}
		} while (nextLine());
		out.print("Unexpected end of section");
		errCloseLine();
		return false;
	}
	
	private BinWriter openDestFile(BinWriter w, int type)
	{
		String strDummy;
		
		if (w != null)
		{
			out.print("Destination already given");
			errCloseLine();
			return null;
		}
		removeBlanks();
		if ((strDummy = getString()) == null)
		{
			out.print("No filename given");
			errCloseLine();
			return null;
		}
		w = osio.getNewBinWriter();
		if (!w.open(strDummy))
		{
			out.print("Could not open destination file ");
			out.print(strDummy);
			errCloseLine();
			return null;
		}
		switch (type)
		{
			case K_DESTFILE:
				break;
			case K_HEXDFILE:
				w = new HexOut(out, w);
				break;
			default:
				out.println("### internal error in BootOut");
				return null;
		}
		return w;
	}
	
	private boolean placeValue(byte[] buf, int pos, char lb, int len, int val, TextPrinter v)
	{
		int i, addr;
		
		if (buf.length < pos + len)
		{
			v.print("Offset to high for buffer");
			return false;
		}
		for (i = 0; i < len; i++)
		{
			if (lb == 'l')
				addr = i; //little endian
			else
				addr = len - 1 - i; //big endian
			buf[pos + addr] = (byte) val;
			val = val >> 8;
		}
		if (val != 0 && val != -1)
		{
			v.print("Value exceeds variable size");
			return false;
		}
		return true;
	}
	
	private void errCloseLine()
	{
		out.print(" in line ");
		out.print(curLine);
		out.print(" in file ");
		out.println(file);
	}
	
	private boolean init(TextReader ir, String section)
	{
		int type;
		r = ir; //exptect r to be at the correct position, do not seek
		curLine = 0;
		error = false;
		while (nextLine())
		{
			type = getKey();
			if (type == K_SECTION)
			{
				removeBlanks();
				if (checkName(section))
					return true; //section found
			}
		}
		//end of file, section not found
		out.print("BootOut: could not find section ");
		out.print(section);
		return false;
	}
	
	private boolean nextLine()
	{
		boolean noComment = true;
		
		bufLen = 0;
		if (r.nextChar == '\0')
			return false;
		while (r.nextChar == '\n')
			r.readChar(); //skip line feeds
		curLine = r.line;
		bufPos = 0;
		while (r.nextChar != '\n')
		{ //search next line feed
			if (r.nextChar != '\r')
			{ //ignore carriage return
				if (noComment)
				{
					if (r.nextChar == '#')
						noComment = false;
					else
						buffer[bufLen++] = r.nextChar;
				}
				if (bufLen >= LINELENGTH)
				{
					out.print("BootOut: line too long, reduce below ");
					out.print(LINELENGTH);
					errCloseLine();
					return false;
				}
			}
			if (r.nextChar == '\0')
				return true;
			r.readChar();
		}
		return true;
	}
	
	private int getKey()
	{
		removeBlanks();
		if (bufPos >= bufLen)
			return K_NONE;
		if (checkInternalIdent("section"))
			return K_SECTION;
		if (checkInternalIdent("endsection"))
			return K_ENDSECT;
		if (checkInternalIdent("destfile"))
			return K_DESTFILE;
		if (checkInternalIdent("hexdestfile"))
			return K_HEXDFILE;
		if (checkInternalIdent("setaddr"))
			return K_SETADDR;
		if (checkInternalIdent("blocksize"))
			return K_BLOCKSIZE;
		if (checkInternalIdent("maximagesize"))
			return K_MAXIMGSIZE;
		if (checkInternalIdent("blowimage"))
			return K_BLOWIMAGE;
		if (checkInternalIdent("fillimage"))
			return K_FILLIMAGE;
		if (checkInternalIdent("appendimage"))
			return K_APPENDIMG;
		if (checkInternalIdent("readbuf"))
			return K_READBUF;
		if (checkInternalIdent("writebuf"))
			return K_WRITEBUF;
		if (checkInternalIdent("offset"))
			return K_OFFSET;
		if (checkInternalIdent("value"))
			return K_VALUE;
		if (checkInternalIdent("imageaddr"))
			return K_IMAGEADDR;
		if (checkInternalIdent("imagesize"))
			return K_IMAGESIZE;
		if (checkInternalIdent("blockcnt"))
			return K_BLOCKCNT;
		if (checkInternalIdent("crc"))
			return K_CRC;
		if (checkInternalIdent("unitaddr"))
			return K_UNITADDR;
		if (checkInternalIdent("codeaddr"))
			return K_CODEADDR;
		if (checkInternalIdent("compraddr"))
			return K_COMPRADDR;
		if (checkInternalIdent("comprsize"))
			return K_COMPRSIZE;
		error = true;
		return K_INVALID;
	}
	
	private int getNum()
	{
		int base = 10, num = 0;
		char c;
		boolean hasValue = false;
		
		if (bufPos + 2 < bufLen && buffer[bufPos] == '0' && (buffer[bufPos + 1] == 'x' || buffer[bufPos + 1] == 'X'))
		{
			base = 16;
			bufPos += 2;
		}
		else if (bufPos >= bufLen)
		{
			error = true;
			return -1;
		}
		while (bufPos < bufLen)
		{
			c = buffer[bufPos];
			if (c >= '0' && c <= '9')
				num = num * base + (c - '0');
			else
			{
				if (base == 10)
				{
					if (hasValue)
						return num;
					error = true;
					return -1;
				}
				if (c >= 'A' && c <= 'F')
					num = num * base + (c - 'A') + 10;
				else if (c >= 'a' && c <= 'f')
					num = num * base + (c - 'a') + 10;
				else
				{
					if (hasValue)
						return num;
					error = true;
					return -1;
				}
			}
			hasValue = true;
			bufPos++;
		}
		return num;
	}
	
	private int getValue()
	{
		int val;
		char op;
		
		val = getFak();
		removeBlanks();
		while (!error && bufPos < bufLen && ((op = buffer[bufPos]) == '+' || op == '-'))
		{
			bufPos++;
			removeBlanks();
			if (op == '+')
				val += getFak();
			else
				val -= getFak();
			removeBlanks();
		}
		return val;
	}
	
	private int getFak()
	{
		int val1, val2;
		char op;
		
		val1 = getOperand();
		removeBlanks();
		while (!error && bufPos < bufLen && ((op = buffer[bufPos]) == '*' || op == '/' || op == '%' || op == '&' || op == '|'))
		{
			bufPos++;
			removeBlanks();
			val2 = getOperand();
			if (op == '*')
				val1 *= val2;
			else if (op == '/')
			{
				if (val2 == 0)
				{
					error = true;
					return 0;
				}
				else
					val1 /= val2;
			}
			else if (op == '%')
			{
				if (val2 == 0)
				{
					error = true;
					return 0;
				}
				else
					val1 %= val2;
			}
			else if (op == '&')
				val1 &= val2;
			else
				val1 |= val2;
			removeBlanks();
		}
		return val1;
	}
	
	private int getOperand()
	{
		char c;
		int val;
		
		if (error || bufPos >= bufLen)
		{
			error = true;
			return 0;
		}
		c = buffer[bufPos]; //do not consume the character, needed elsewhere
		if (c >= '0' && c <= '9')
			return getNum();
		if (c == '(')
		{
			bufPos++;
			removeBlanks();
			val = getValue();
			if (error || bufPos >= bufLen || buffer[bufPos++] != ')')
			{
				error = true;
				return -1;
			}
			return val;
		}
		switch (getKey())
		{
			case K_IMAGEADDR:
				return img.baseAddress;
			case K_IMAGESIZE:
				return imageSize;
			case K_BLOCKCNT:
				return blockCnt;
			case K_UNITADDR:
				return img.startUnit;
			case K_CODEADDR:
				return img.startCode;
			case K_COMPRADDR:
				if (compressedImg != null)
					return compressedImg.baseAddress;
				out.print("no compressed image available for COMPRADDR");
				errCloseLine();
				break;
			case K_COMPRSIZE:
				if (compressedImg != null)
					return compressedImageOrigLen;
				out.print("no compressed image available for COMPRSIZE");
				errCloseLine();
				break;
		}
		//invalid keyword
		error = true;
		return -1;
	}
	
	private char getChar()
	{
		if (bufPos >= bufLen)
			return '\0';
		return buffer[bufPos++];
	}
	
	private String getString()
	{
		int start;
		boolean quote = false;
		
		if (bufPos >= bufLen)
			return null;
		removeBlanks();
		if (buffer[start = bufPos] == '"')
		{
			quote = true;
			start = ++bufPos;
		}
		while (bufPos < bufLen && (quote || (buffer[bufPos] != ' ' && buffer[bufPos] != '\t')))
		{
			if (buffer[bufPos] == '"')
			{
				bufPos++;
				if (quote && (bufPos >= bufLen || buffer[bufPos] == ' ' || buffer[bufPos] == '\t'))
					return new String(buffer, start, bufPos - 1 - start);
				out.println("BootOut: quotes not supported between chars of string");
				return null;
			}
			bufPos++;
		}
		if (quote)
		{
			out.println("BootOut: string not closed by quote");
			return null;
		}
		return new String(buffer, start, bufPos - start);
	}
	
	private void removeBlanks()
	{
		if (bufPos >= bufLen)
			return;
		while (buffer[bufPos] == ' ' || buffer[bufPos] == '\t')
		{
			if (bufPos >= bufLen)
				return;
			bufPos++;
		}
	}
	
	//only checks for alpha-chars ignoring case, given key has to be lower case
	private boolean checkInternalIdent(String key)
	{
		int i;
		char c;
		for (i = 0; i < key.length(); i++)
		{
			if (bufPos + i >= bufLen)
				return false; //input exceeded
			c = buffer[bufPos + i];
			if (c >= 'A' && c <= 'Z')
				c = (char) (c - ('A' + 'a')); //lower case check
			if (c != key.charAt(i))
				return false; //wrong char
		}
		//string found
		bufPos += i; //move current position
		//check if alpha-string in file also ends
		return (bufPos == bufLen || !(buffer[bufPos] >= 'a' && buffer[bufPos] <= 'z') || !(buffer[bufPos] >= 'A' && buffer[bufPos] <= 'Z'));
	}
	
	//checks if name equals remaining string in line discarding blanks
	private boolean checkName(String name)
	{
		int i;
		for (i = 0; i < name.length(); i++)
		{
			if (bufPos + i >= bufLen)
				return false; //input exceeded
			if (buffer[bufPos + i] != name.charAt(i))
				return false; //wrong char
		}
		//string found
		bufPos += i; //move current position
		removeBlanks(); //remove remaining spaces
		return bufPos == bufLen; //hit only if string in file also ends
	}
}
