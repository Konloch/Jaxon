package jaxon;

import sjc.compbase.StringList;
import sjc.osio.BinWriter;
import sjc.osio.OsIO;
import sjc.osio.TextPrinter;
import sjc.osio.sun.FileBinWriter;
import sjc.osio.sun.StreamTextPrinter;

import java.io.*;

/**
 * @author Konloch
 * @author S. Frenz
 */

public class JaxonIO extends OsIO
{
	private StringList result;
	private final OutputStream stdOut;
	
	public JaxonIO(OutputStream streamAsStdOut)
	{
		stdOut = streamAsStdOut;
	}
	
	public BinWriter getNewBinWriter()
	{
		return new FileBinWriter();
	}
	
	public TextPrinter getNewFilePrinter(String filename)
	{
		return new StreamTextPrinter(filename == null ? filename : new File(filename).getAbsolutePath(), stdOut);
	}
	
	public boolean isDir(String name)
	{
		return (new File(name).getAbsoluteFile()).isDirectory();
	}
	
	public StringList listDir(String name, boolean recurse)
	{
		result = null;
		appendDir(new File(name).getAbsoluteFile(), recurse);
		return result;
	}
	
	public byte[] readFile(String fname)
	{
		InputStream is;
		int cnt;
		byte[] data;
		
		try
		{
			is = new FileInputStream(new File(fname).getAbsolutePath());
			cnt = is.available();
			data = new byte[cnt];
			if (is.read(data, 0, cnt) != cnt)
			{
				is.close();
				return null;
			}
			
			is.close();
		}
		catch (IOException e)
		{
			return null;
		}
		
		return data;
	}
	
	public long getTimeInfo()
	{
		return System.nanoTime();
	}
	
	private void appendDir(File dir, boolean recurse)
	{
		File[] entries;
		StringList newFile;
		int i;
		
		entries = dir.listFiles();
		for (i = 0; i < entries.length; i++)
		{
			if (entries[i].getName().startsWith("."))
				continue;
			if (entries[i].isDirectory())
			{
				if (recurse)
					appendDir(entries[i], true);
			}
			else
			{
				try
				{
					newFile = new StringList(entries[i].getCanonicalPath());
					newFile.next = result;
					result = newFile;
				}
				catch (IOException e)
				{
					System.out.print("IO-error: " + e.getMessage());
				}
			}
		}
	}
}
