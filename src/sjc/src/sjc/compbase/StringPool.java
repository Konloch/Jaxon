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
 * StringPool: pool for strings re-usage, may reduce memory requirements
 *
 * @author S. Frenz
 * @version 100505 added binary tree mode with length criterium, streamlined getString signature, added hashmap pool
 * version 090207 added copyright notice
 * version 060608 inserted different pooling strategies
 * version 060607 initial version
 */

public class StringPool
{
	//private StringList s, last; //for pools based on StringList
	//private String str; //for pools with tree
	//private StringPool left, right; //for pools with binary tree
	
	private BinaryStringPool[] hashmap; //for hashmap pools
	
	private static class BinaryStringPool
	{
		private String str; //for pools with tree
		private BinaryStringPool left, right; //for pools with binary tree
		
		public String getString(char[] buf, int len)
		{
			int i, diff = 0, max, strLen;
			
			if (str == null)
			{
				str = new String(buf, 0, len);
				return str;
			}
			strLen = str.length();
			if (len <= strLen)
				max = len;
			else
				max = strLen;
			for (i = 0; i < max; i++)
			{
				if ((diff = buf[i] - str.charAt(i)) != 0)
					break; //different character
			}
			if (i == len && i == strLen)
				return str;
			if (diff < 0)
			{
				if (right == null)
					right = new BinaryStringPool();
				return right.getString(buf, len);
			}
			if (left == null)
				left = new BinaryStringPool();
			return left.getString(buf, len);
		}
	}
	
	//pool with hash map as entry and then a binary tree
	public StringPool()
	{
		if (hashmap == null)
		{
			hashmap = new BinaryStringPool[1024];
			for (int i = 0; i < 1024; i++)
				hashmap[i] = new BinaryStringPool();
		}
	}
	
	public String getString(char[] buf, int len)
	{
		if (len == 0)
			return "";
		return hashmap[((int) buf[0] & 0x3F) + ((len & 0xF) << 6)].getString(buf, len);
	}
	
	//pool as binary tree with length as first criterium
  /*public String getString(char buf[], int len) {
    int i,  diff, strLen;
    
    if (str==null) {
      str=new String(buf, 0, len);
      return str;
    }
    strLen=str.length();
    if ((diff=(len-strLen))==0) {
      for (i=0; i<len; i++) {
        if ((diff=(int)(buf[i]-str.charAt(i)))!=0) break; //different character
      }
      if (i==len) return str;
    }
    if (diff<0) {
      if (right==null) right=new StringPool();
      return right.getString(buf, len);
    }
    if (left==null) left=new StringPool();
    return left.getString(buf, len);
  }*/
	
	//pool as binary tree (17687839 ticks with 9333 kb)
  /*public String getString(char buf[], int len) {
    int i,  diff=0, max, strLen;
    
    if (str==null) {
      str=new String(buf, 0, len);
      return str;
    }
    strLen=str.length();
    if (len<=strLen) max=len;
    else max=strLen;
    for (i=0; i<max; i++) {
      if ((diff=(int)(buf[i]-str.charAt(i)))!=0) break; //different character
    }
    if (i==len && i==strLen) return str;
    if (diff<0) {
      if (right==null) right=new StringPool();
      return right.getString(buf, len);
    }
    if (left==null) left=new StringPool();
    return left.getString(buf, len);
  }*/
	
	//no pooling (15877454 ticks with 11602 kb)
  /*public String getString(char buf[], int len) {
    return new String(buf, 0, len);
  }*/
	
	//pool with linear StringPool and re-insertion (21294959 ticks with 9333 kb)
  /*public String getS2tring(char buf[], int len) {
    int i;
    String to;
    StringList t=s, told=null;
    
    while (t!=null) {
      to=t.str;
      if (to.length()==len) {
        for (i=0; i<len; i++) if (buf[i]!=to.charAt(i)) break; //different character
        if (i==len) {
          if (told!=null) {
            if (t==last) last=told;
            told.next=t.next;
            t.next=s;
            s=t;
          }
          return t.str;
        }
      }
      told=t;
      t=t.next;
    }
    to=new String(buf, 0, len);
    last=new StringList(last, to);
    if (s==null) s=last;
    return to;
  }*/
	
	//pool with linear StringList (36414160 ticks with 9333 kb)
	/*public String getString(char buf[], int len) {
		int i;
		String to;
		StringList t=s;
		
		while (t!=null) {
			to=t.str;
			if (to.length()==len) {
				for (i=0; i<len; i++) if (buf[i]!=to.charAt(i)) break; //different character
				if (i==len) return t.str;
			}
			t=t.next;
		}
		to=new String(buf, 0, len);
		last=new StringList(last, to);
		if (s==null) s=last;
		return to;
	}*/
}
