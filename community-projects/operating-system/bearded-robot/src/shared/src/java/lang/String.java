package java.lang;

@SuppressWarnings("all")
public class String
{
	private char[] value;
	private int count;
	
	public String()
	{
	}
	
	public String(String original)
	{
		if (original.count > 0)
		{
			this.value = new char[original.count];
			this.count = original.count;
			for (int i = 0; i < original.count; i++)
				this.value[i] = original.value[i];
		}
	}
	
	@SJC.Inline
	public int length()
	{
		return count;
	}
	
	@SJC.Inline
	public char charAt(int i)
	{
		return value[i];
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof String)
		{
			String other = (String) obj;
			if (this.count == other.count)
			{
				for (int i = 0; i < this.count; i++)
				{
					if (this.value[i] != other.value[i])
						return false;
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public String concat(String str)
	{
		if (str.count > 0)
		{
			String result = new String();
			result.value = new char[this.count + str.count];
			result.count = this.count + str.count;
			for (int i = 0; i < this.count; i++)
				result.value[i] = this.value[i];
			
			for (int i = 0; i < str.count; i++)
				result.value[this.count + i] = str.value[i];
			
			return result;
		}
		else
		{
			return this;
		}
	}
}
