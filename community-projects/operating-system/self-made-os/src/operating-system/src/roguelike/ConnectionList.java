package roguelike;

public class ConnectionList
{
	public static class Connection
	{
		private Room r1, r2;
	}
	
	private static class ConnectionWrapper
	{
		private final Connection c;
		private ConnectionWrapper next;
		
		ConnectionWrapper(Connection c)
		{
			this.c = c;
		}
	}
	
	private ConnectionWrapper head = null;
	private int size = 0;
	
	private void addConnection(Connection c)
	{
		ConnectionWrapper cw = new ConnectionWrapper(c);
		if (head != null)
		{
			ConnectionWrapper p = head;
			while (p.next != null)
				p = p.next;
			p.next = cw;
		}
		else
		{
			head = cw;
		}
		size++;
	}
	
	private Connection[] getConnections()
	{
		Connection[] retval = new Connection[size];
		int i = 0;
		ConnectionWrapper cw = head;
		while (cw != null)
		{
			retval[i] = cw.c;
			cw = cw.next;
			i++;
		}
		return retval;
	}
}