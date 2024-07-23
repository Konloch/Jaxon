package kernel;

import graphics.Console;
import graphics.ConsoleColors;
import graphics.VideoMemory;
import hardware.Serial;
import rte.SMthdBlock;

public class ErrorScreen
{
	//9 registers with 4 bytes
	private static final int EIP_OFFSET = 9 * 4;
	private static final int STACK_BEGINNING = 0x9BFFC;
	
	public static void showErrorScreenWithStackframe(int ebp, String reason)
	{
		fillScreen();
		Console.print("Error: ".concat(reason));
		Console.print(". Stackframe:\n");
		printStackframe(ebp);
	}
	
	public static void BreakpointScreen(int ebp)
	{
		showErrorScreenWithStackframe(ebp, "Breakpoint");
	}
	
	@SJC.Inline
	public static void printStackframe(int ebp)
	{
		//load previous ebp and eip
		int eip = MAGIC.rMem32(ebp + EIP_OFFSET);
		//ebp = MAGIC.rMem32(ebp);
		do
		{
			//print stackframe
			Console.print("ebp:");
			Console.printHex(ebp);
			Console.print(", eip:");
			Console.printHex(eip);
			SMthdBlock m = SymbolResolution.findMethodBlock(eip);
			if (m != null)
			{
				Console.print(", method:");
				Console.print(m.owner.name.concat(".".concat(m.namePar)));
			}
			else
			{
				Console.print(", method cannot be resolved");
			}
			Console.print('\n');
			//check that we don't land in the first page
			ebp = MAGIC.rMem32(ebp);
			Serial.print("EBP: ");
			Serial.printHex(ebp, 8);
			Serial.print('\n');
			eip = MAGIC.rMem32(ebp + 4);
			Serial.print("EIP: ");
			Serial.printHex(eip, 8);
			Serial.print('\n');
		} while (ebp <= STACK_BEGINNING && ebp > 0);
	}
	
	//fills the entire screen with a red color
	private static void fillScreen()
	{
		Console.setColor(ConsoleColors.FG_BLACK, ConsoleColors.BG_RED, false);
		for (int i = 0; i < VideoMemory.VIDEO_MEMORY_COLUMNS * VideoMemory.VIDEO_MEMORY_ROWS; i++)
		{
			Console.print(' ');
		}
		Console.setCursor(0, 0);
	}
	
	public static void showPageFaultError(int errorCode, int CR2)
	{
		fillScreen();
		Console.print("Error: Page fault\nError code (hex): ");
		Console.printHex(errorCode);
		Console.println();
		Console.print("Details:\n");
		//present bit
		if ((errorCode & 1) > 0)
		{
			Console.print("Page-protection violation. ");
		}
		else
		{
			Console.print("non-present page. ");
		}
		//write bit
		if ((errorCode & 2) > 0)
		{
			Console.print("Write-access violation. ");
		}
		else
		{
			Console.print("Read-access violation. ");
		}
		Console.println();
		Console.print("Address which resulted in violation: ");
		Console.printHex(CR2);
		Console.println();
	}
}