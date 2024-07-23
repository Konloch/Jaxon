package sysutils.exec;


//VIM - Vi IMpaired
public class TextEditor extends Executable
{
	//region constructor
	static
	{
		ExecutableStore.addExecutableFactory(new ExecutableFactory()
		{
			@Override
			public Executable createExecutable()
			{
				return new TextEditor();
			}
			
			@Override
			public String getName()
			{
				return "vim";
			}
		});
	}
	//endregion
	
	//region editor state
	private final int MAX_FILE_BUFFER = 8 * 1024 * 80;
	private final int MODE_COMMAND = 0;
	private final int MODE_INSERT = 1;
	
	private final int editorMode = 0;
	private final char[] fileBuffer = new char[MAX_FILE_BUFFER];
	private final int writePointer = 0;
	private final int consoleLinePointer = 0;
	
	//endregion
	
	@Override
	public int execute()
	{
		return 0;
	}
}
