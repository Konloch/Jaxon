package java.util;

import java.util.vector.VecByte;

public class StrBuilder
{
	private VecByte strBuffer;
	private static final int DEFAULT_CAPACITY = 16;
	
	public StrBuilder()
	{
		this.strBuffer = new VecByte(DEFAULT_CAPACITY);
	}
	
	public StrBuilder(int initialCapacity)
	{
		this.strBuffer = new VecByte(initialCapacity);
	}
	
	public void ClearKeepCapacity()
	{
		strBuffer.ClearKeepCapacity();
	}
	
	@SJC.Inline
	public int length()
	{
		return strBuffer.Size();
	}
	
	public String toString()
	{
		byte[] buffer = strBuffer.toArray();
		return new String(buffer);
	}
	
	@SJC.Inline
	public StrBuilder AppendLine()
	{
		return Append('\n');
	}
	
	@SJC.Inline
	public StrBuilder Append(byte c)
	{
		strBuffer.add(c);
		return this;
	}
	
	@SJC.Inline
	public StrBuilder AppendLine(byte c)
	{
		return Append(c).AppendLine();
	}
	
	@SJC.Inline
	public StrBuilder Append(IDebug dbg)
	{
		return Append(dbg.Debug());
	}
	
	@SJC.Inline
	public StrBuilder AppendLine(IDebug dbg)
	{
		return Append(dbg.Debug()).AppendLine();
	}
	
	public StrBuilder Append(String str)
	{
		if (str == null)
			str = "null";
		
		byte[] bytes = str.getBytes();
		strBuffer.AddAll(bytes);
		return this;
	}
	
	@SJC.Inline
	public StrBuilder AppendLine(String str)
	{
		return Append(str).AppendLine();
	}
	
	@SJC.Inline
	public StrBuilder Append(char c)
	{
		return Append((byte) c);
	}
	
	@SJC.Inline
	public StrBuilder AppendLine(char c)
	{
		return Append(c).AppendLine();
	}
	
	@SJC.Inline
	public StrBuilder Append(int i, int base)
	{
		return Append(Integer.toString(i, base));
	}
	
	@SJC.Inline
	public StrBuilder appendLine(int i, int base)
	{
		return Append(i, base).AppendLine();
	}
	
	@SJC.Inline
	public StrBuilder Append(int i)
	{
		return Append(Integer.toString(i, 10));
	}
	
	@SJC.Inline
	public StrBuilder AppendLine(int i)
	{
		return Append(i, 10).AppendLine();
	}
	
	@SJC.Inline
	public StrBuilder Append(long i, int base)
	{
		return Append(Long.toString(i, base));
	}
	
	@SJC.Inline
	public StrBuilder AppendLine(long i, int base)
	{
		return Append(i, base).AppendLine();
	}
	
	@SJC.Inline
	public StrBuilder Append(boolean b)
	{
		return Append(b ? "true" : "false");
	}
	
	@SJC.Inline
	public StrBuilder AppendLine(boolean b)
	{
		return Append(b).AppendLine();
	}
}
