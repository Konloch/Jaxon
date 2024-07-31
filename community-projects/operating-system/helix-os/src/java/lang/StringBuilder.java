package java.lang;

import java.util.IDebug;
import java.util.vector.VecByte;

public class StringBuilder
{
	private VecByte strBuffer;
	private static final int DEFAULT_CAPACITY = 16;
	
	public StringBuilder()
	{
		this.strBuffer = new VecByte(DEFAULT_CAPACITY);
	}
	
	public StringBuilder(int initialCapacity)
	{
		this.strBuffer = new VecByte(initialCapacity);
	}
	
	public void clearKeepCapacity()
	{
		strBuffer.clearKeepCapacity();
	}
	
	@SJC.Inline
	public int length()
	{
		return strBuffer.size();
	}
	
	public String toString()
	{
		byte[] buffer = strBuffer.toArray();
		return new String(buffer);
	}
	
	@SJC.Inline
	public StringBuilder appendLine()
	{
		return append('\n');
	}
	
	@SJC.Inline
	public StringBuilder append(byte c)
	{
		strBuffer.add(c);
		return this;
	}
	
	@SJC.Inline
	public StringBuilder appendLine(byte c)
	{
		return append(c).appendLine();
	}
	
	@SJC.Inline
	public StringBuilder append(IDebug dbg)
	{
		return append(dbg.debug());
	}
	
	@SJC.Inline
	public StringBuilder appendLine(IDebug dbg)
	{
		return append(dbg.debug()).appendLine();
	}
	
	public StringBuilder append(String str)
	{
		if (str == null)
			str = "null";
		
		byte[] bytes = str.getBytes();
		strBuffer.addAll(bytes);
		return this;
	}
	
	@SJC.Inline
	public StringBuilder appendLine(String str)
	{
		return append(str).appendLine();
	}
	
	@SJC.Inline
	public StringBuilder append(char c)
	{
		return append((byte) c);
	}
	
	@SJC.Inline
	public StringBuilder appendLine(char c)
	{
		return append(c).appendLine();
	}
	
	@SJC.Inline
	public StringBuilder append(int i, int base)
	{
		return append(Integer.toString(i, base));
	}
	
	@SJC.Inline
	public StringBuilder appendLine(int i, int base)
	{
		return append(i, base).appendLine();
	}
	
	@SJC.Inline
	public StringBuilder append(int i)
	{
		return append(Integer.toString(i, 10));
	}
	
	@SJC.Inline
	public StringBuilder appendLine(int i)
	{
		return append(i, 10).appendLine();
	}
	
	@SJC.Inline
	public StringBuilder append(long i, int base)
	{
		return append(Long.toString(i, base));
	}
	
	@SJC.Inline
	public StringBuilder appendLine(long i, int base)
	{
		return append(i, base).appendLine();
	}
	
	@SJC.Inline
	public StringBuilder append(boolean b)
	{
		return append(b ? "true" : "false");
	}
	
	@SJC.Inline
	public StringBuilder appendLine(boolean b)
	{
		return append(b).appendLine();
	}
}
