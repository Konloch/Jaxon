package jaxon.installer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Konloch
 * @since 7/8/2024
 */
public class EasyProcess
{
	public final Process process;
	public final ArrayList<String> out = new ArrayList<>();
	public final ArrayList<String> err = new ArrayList<>();
	public AtomicInteger threadsRunning = new AtomicInteger(2);
	
	private EasyProcess(Process process)
	{
		this.process = process;
	}
	
	public static EasyProcess from(ProcessBuilder processBuilder) throws IOException
	{
		//create a new easy process instance
		EasyProcess process = new EasyProcess(processBuilder.start());
		
		//wait to start
		while(!process.process.isAlive())
		{
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		//read out
		process.read(process.process.getInputStream(), process.out);
		
		//read err
		process.read(process.process.getErrorStream(), process.err);
		
		//return the easy process instance
		return process;
	}
	
	private void read(InputStream stream, ArrayList<String> list)
	{
		Thread readThread = new Thread(()->
		{
			BufferedReader outputReader = new BufferedReader(new InputStreamReader(stream));
			String line;
			
			while(true)
			{
				try
				{
					if ((line = outputReader.readLine()) != null)
						list.add(line);
					else
						break;
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			threadsRunning.decrementAndGet();
		});
		
		readThread.start();
	}
	
	public int waitFor() throws InterruptedException
	{
		int returnCode = process.waitFor();
		
		while(threadsRunning.get() > 0)
		{
			Thread.sleep(100);
		}
		
		return returnCode;
	}
}
