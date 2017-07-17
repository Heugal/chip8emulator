public class Chip8
{
	public static void main(String[] args)
	{
		setupGraphics();
		setupInput();

		Cpu chip8 = new Cpu();
		chip8.loadGame(args[0]);

		while(true)
		{
			chip8.emulateCycle();

			if (chip8.drawFlag)
			{
				drawGraphics();
			}

			chip8.setKeys();
		}
	}

	protected static void setupGraphics()
	{
		// do nothing for right now
	}

	protected static void setupInput()
	{
		// do nothing for right now
	}

	protected static void drawGraphics()
	{
		// do nothing for right now
	}
}
