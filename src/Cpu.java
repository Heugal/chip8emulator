import java.util.Random;

public class Cpu
{
	// screenHeight and width of the screen
	public final int screenHeight = 32;
	public final int screenWidth = 64;

	// current opcode
	public char opcode;
	// memory for the emulator (4K)
	public byte[] memory;
	// 15 8-bit general purpose registers
	public byte[] V;
	// index register
	public char I;
	// program counter
	public char pc;
	// graphic display array
	public byte[] gfx;
	// timer registers
	public byte delay_timer;
	public byte sound_timer;
	public char[] stack;
	public char sp;
	public byte[] key;
	public boolean drawFlag = false;

	public char[] chip8_fontset =
	{
			0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
			0x20, 0x60, 0x20, 0x20, 0x70, // 1
			0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
			0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
			0x90, 0x90, 0xF0, 0x10, 0x10, // 4
			0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
			0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
			0xF0, 0x10, 0x20, 0x40, 0x40, // 7
			0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
			0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
			0xF0, 0x90, 0xF0, 0x90, 0x90, // A
			0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
			0xF0, 0x80, 0x80, 0x80, 0xF0, // C
			0xE0, 0x90, 0x90, 0x90, 0xE0, // D
			0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
			0xF0, 0x80, 0xF0, 0x80, 0x80  // F
	};

	public Cpu()
	{
		pc = 0x200;
		opcode = 0;
		I = 0;
		sp = 0;

		memory = new byte[4096];
		V = new byte[16];
		gfx = new byte[screenHeight * screenWidth];
		stack = new char[16];
		key = new byte[16];

		for (int i = 0; i < 80; ++i)
		{
			memory[i+80] = (byte) chip8_fontset[i];
		}

		delay_timer = 0;
		sound_timer = 0;
	}

	public void loadGame(String name)
	{
		// do nothing for right now
	}

	public void emulateCycle()
	{
		// fetch opcode
		opcode = (char) ((memory[pc]&0xFF) << 8 | (memory[pc+1]&0xFF));
		// decode opcode
		switch (opcode & 0xF000)
		{
			case (0x0000):
				switch (opcode & 0x00FF)
				{
					case 0x00E0:
						clearScreen();
						break;
					case 0x00EE:
						returnFromSubroutine();
						break;

					default:
						// technically its 0NNN - Calls RCA 1802 program at address NNN. Not necessary for most ROMs.
						jumpToAddress();
				}
			break;

			case (0x1000):
				jumpToAddress();
				break;

			case (0x2000):
				callSubroutine();
				break;

			case (0x3000):
				skipNextInstIfEqual();
				break;

			case (0x4000):
				skipNextInstIfNotEqual();
				break;

			case (0x5000):
				skipNextInstIfVxEqualsVy();
				break;

			case (0x6000):
				setVx();
				break;

			case (0x7000):
				addToVx();
				break;

			case (0x8000):
				switch (opcode & 0x000F)
				{
					case (0x0000):
						// 8XY0 - Vx=Vy - Sets VX to the value of VY.
						V[((opcode & 0x0F00) >> 8)] = V[((opcode & 0x00F0) >> 8)];
						pc += 2;
						break;

					case (0x0001):
						// 8XY1 - Vx=Vx|Vy - Sets VX to VX or VY. (Bitwise OR operation)
						V[((opcode & 0x0F00) >> 8)] |= V[((opcode & 0x00F0) >> 8)];
						pc += 2;
						break;

					case (0x0002):
						// 8XY2 - Vx=Vx&Vy - Sets VX to VX and VY. (Bitwise AND operation)
						V[((opcode & 0x0F00) >> 8)] &= V[((opcode & 0x00F0) >> 8)];
						pc += 2;
						break;

					case (0x0003):
						// 8XY3 - Vx=Vx^Vy - Sets VX to VX xor VY.
						V[((opcode & 0x0F00) >> 8)] ^= V[((opcode & 0x00F0) >> 8)];
						pc += 2;
						break;

					case (0x0004):
						// 8XY4 - Vx += Vy - Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there isn't.
						byte vx = V[((opcode & 0x0F00) >> 8)], vy = V[((opcode & 0x00F0) >> 4)];
						V[((opcode & 0x0F00) >> 8)] = (byte) ((vx + (vx + vy)) % 16);
						break;

					case (0x0005):
						// 8XY5 - Vx -= Vy - VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
						break;

					case (0x0006):
						// 8XY6 - Vx >> 1 - Shifts VX right by one. VF is set to the value of the least significant bit of VX before the shift.
						V[V.length-1] = (byte) (V[((opcode & 0x0F00) >> 8)] & 0x0001);
						V[((opcode & 0x0F00) >> 8)] = (byte) (V[((opcode & 0x0F00) >> 8)] >> 1);
						pc += 2;
						break;

					case (0x0007):
						// 8XY7 - Vx=Vy-Vx - Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there isn't.
						break;

					case (0x000E):
						// 8XYE - Vx << 1 - Shifts VX left by one. VF is set to the value of the most significant bit of VX before the shift.
						V[V.length-1] = (byte) ((V[((opcode & 0x0F00) >> 8)] & 0x8000) >> 12);
						V[((opcode & 0x0F00) >> 8)] = (byte) (V[((opcode & 0x0F00) >> 8)] << 1);
						pc += 2;
						break;

					default:
						System.out.println("Unknown opcode [0x8000]: 0x" + opcode);
				}
			break;

			case (0x9000):
				skipNextInstIfVxNotEqualVy();
				break;

			case (0xA000):
				setI();
				break;

			case (0xB000):
				jumpToAddressPlusV0();
				break;

			case (0xC000):
				// CXNN - Vx=rand()&NN - Sets VX to the result of a bitwise and operation on a random number (Typically: 0 to 255) and NN.
				Random rn = new Random();
				V[((opcode & 0x0F00) >> 8)] = (byte) ((byte)(rn.nextInt(Byte.MAX_VALUE - Byte.MIN_VALUE + 1) & 0x00FF) & (opcode & 0x00FF));
				pc += 2;
				break;

			case (0xD000):
				draw();
				break;

			case (0xE000):
				switch(opcode & 0x00FF)
				{
					case (0x009E):
						// EX9E - if(key()==Vx) - Skips the next instruction if the key stored in VX is pressed.
						// (Usually the next instruction is a jump to skip a code block)
						if (key[V[(opcode & 0x0F00) >> 8]] != 0)
							pc += 4;
						else
							pc += 2;
						break;

					case (0x00A1):
						// EXA1 - if(key()!=Vx) - Skips the next instruction if the key stored in VX isn't pressed.
						// (Usually the next instruction is a jump to skip a code block)
						if (key[V[(opcode & 0x0F00) >> 8]] == 0)
							pc += 4;
						else
							pc += 2;
						break;

					default:
						System.out.println("Unknown opcode [0xE000]: 0x" + opcode);
				}
				break;

			case (0xF000):
				switch(opcode & 0x00FF)
				{
					case (0x0007):
						// FX07 - Vx = get_delay() - Sets VX to the value of the delay timer.
						V[((opcode & 0x0F00) >> 8)] = delay_timer;
						break;

					case (0x000A):
						// FX0A - Vx = get_key() - A key press is awaited, and then stored in VX. (Blocking Operation.
						// All instruction halted until next key event)
						break;

					case (0x0015):
						// FX15 - delay_timer(Vx) - Sets the delay timer to VX.
						delay_timer = V[((opcode & 0x0F00) >> 8)];
						pc += 2;
						break;

					case (0x0018):
						// FX18 - sound_timer(Vx) - Sets the sound timer to VX.
						sound_timer = V[((opcode & 0x0F00) >> 8)];
						pc += 2;
						break;

					case (0x001E):
						// FX1E - I +=Vx - Adds VX to I.
						I = bitwiseAddition(I, (char) V[((opcode & 0x0F00) >> 8)]);
						pc += 2;
						break;

					case (0x0029):
						// FX29 - I=sprite_addr[Vx] - Sets I to the location of the sprite for the character in VX.
						// Characters 0-F (in hexadecimal) are represented by a 4x5 font.
						break;

					case (0x0033):
						setBCD();
						break;

					case (0x0055):
						reg_dump();
						break;

					case (0x0065):
						reg_load();
						break;
				}
			break;

			default:
				System.out.println("Unknown opcode: 0x" + opcode);
		}

		// update timers
		if (delay_timer > 0)
			--delay_timer;
		if (sound_timer > 0)
		{
			if (sound_timer == 1)
			{
				System.out.println("BEEP!\n");
				--sound_timer;
			}
		}
	}

	public void setKeys()
	{
		// do nothing for right now
	}

	////////////////////////////////////////////////////////////////////////
	// CPU instructions
	////////////////////////////////////////////////////////////////////////

	/**
	 * 0x00E0 - clear the screen
	 */
	protected void clearScreen()
	{
		for(int i = 0; i < gfx.length; i++)
		{
			gfx[i] = 0;
		}
		drawFlag = true;
		pc += 2;
	}

	/**
	 * 0x00EE - return from subroutine
	 */
	protected void returnFromSubroutine()
	{
		pc = stack[sp-1];
		stack[sp] = 0;
		--sp;
		pc += 2;
	}

	/**
	 * 0x1NNN - jump to address NNN
	 */
	protected void jumpToAddress()
	{
		pc = (char) (opcode & 0x0FFF);
	}

	/**
	 * 0x2NNN - calls subroutine at NNN
	 */
	protected void callSubroutine()
	{
		stack[sp] = pc;
		++sp;
		pc = (char) (opcode & 0x0FFF);
	}

	/**
	 * 3XNN - if(Vx==NN)
	 * Skips the next instruction if VX equals NN. (Usually the next instruction is a jump to skip a code block)
	 */
	protected void skipNextInstIfEqual()
	{
		if (V[((opcode & 0x0F00) >> 8)] == (opcode & 0x00FF))
			pc += 4;
		else
			pc += 2;
	}

	/**
	 * 4XNN - if(Vx!=NN)
	 * Skips the next instruction if VX doesn't equal NN. (Usually the next instruction is a jump to skip a code block)
	 */
	protected void skipNextInstIfNotEqual()
	{
		if (V[((opcode & 0x0F00) >> 8)] != (opcode & 0x00FF))
			pc += 4;
		else
			pc += 2;
	}

	/**
	 * 5XY0 - if(Vx==Vy)
	 * Skips the next instruction if VX equals VY. (Usually the next instruction is a jump to skip a code block)
	 */
	protected void skipNextInstIfVxEqualsVy()
	{
		if (V[((opcode & 0x0F00) >> 8)] == V[((opcode & 0x00F0) >> 4)])
			pc += 4;
		else
			pc += 2;
	}

	/**
	 * 6XNN - Vx = NN - Sets VX to NN
	 */
	protected void setVx()
	{
		V[((opcode & 0x0F00) >> 8)] = (byte) (opcode & 0x00FF);
		pc += 2;
	}

	/**
	 * 7XNN - Vx += NN - Adds NN to VX
	 */
	protected void addToVx()
	{
		V[((opcode & 0x0F00) >> 8)] = (byte) bitwiseAddition((char) V[((opcode & 0x0F00) >> 8)], (char) (opcode & 0x00FF));
		pc += 2;
	}

	protected char bitwiseAddition(char a, char b)
	{
		char result = (char)(a^b);
		char carry = (char) (a&b << 1);
		if (carry != 0)
			return bitwiseAddition(result, carry);
		return result;
	}

	/**
	 * 9XY0 - if(Vx!=Vy) - Skips the next instruction if VX doesn't equal VY. (Usually the next instruction is a jump to skip a code block)
	 */
	protected void skipNextInstIfVxNotEqualVy()
	{
		if (V[((opcode & 0x0F00) >> 8)] != V[((opcode & 0x00F0) >> 4)])
			pc += 4;
		else
			pc += 2;
	}

	/**
	 * ANNN - I = NNN - Sets I to the address NNN.
	 */
	protected void setI()
	{
		I = (char) (opcode & 0x0FFF);
		pc += 2;
	}

	/**
	 * BNNN - PC=V0+NNN - Jumps to the address NNN plus V0.
	 */
	protected void jumpToAddressPlusV0()
	{
		pc = bitwiseAddition((char)V[0], (char)(opcode & 0x0FFF));
	}

	/**
	 * DXYN - draw(Vx,Vy,N) - Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a screenHeight of N pixels.
	 * Each row of 8 pixels is read as bit-coded starting from memory location I; I value doesn’t change after the
	 * execution of this instruction. As described above, VF is set to 1 if any screen pixels are flipped from set
	 * to unset when the sprite is drawn, and to 0 if that doesn’t happen
	 */
	protected void draw()
	{
		byte x = V[(opcode & 0x0F00) >> 8];
		byte y = V[(opcode & 0x00F0) >> 4];
		char height = (char) (opcode & 0x000F);
		byte pixel;
		V[0xF] = 0;

		for (int yline = 0; yline < height; yline++)
		{
			pixel = memory[I + yline];
			for (int xline = 0; xline < 8; xline++)
			{
				if ((pixel & (0x80 >> xline)) != 0)
				{
					if (gfx[(x + xline + ((y + yline) * 64))] == 1)
						V[0xF] = 1;
					gfx[x + xline + ((y + yline) * 64)] ^= 1;
				}
			}
		}
		drawFlag = true;
		pc += 2;
	}

	/** FX33
	 * set_BCD(Vx);
	 *(I+0)=BCD(3);
	 *(I+1)=BCD(2);
	 *(I+2)=BCD(1);
	 * Stores the binary-coded decimal representation of VX, with the most significant of three
	 * digits at the address in I, the middle digit at I plus 1, and the least significant digit
	 * at I plus 2. (In other words, take the decimal representation of VX, place the hundreds digit
	 * in memory at location in I, the tens digit at location I+1, and the ones digit at location I+2.)
	 */
	protected void setBCD()
	{
		int vx = Byte.toUnsignedInt(V[((opcode & 0x0F00) >> 8)]);
		int hunds = vx / 100;
		int tens = (vx - (hunds * 100)) / 10;
		int ones = (vx - (hunds * 100 + tens * 10));
		memory[I] = (byte) hunds;
		memory[I+1] = (byte) tens;
		memory[I+2] = (byte) ones;
		pc += 2;
	}

	/**
	 * FX55 - reg_dump(Vx,&I) - Stores V0 to VX (including VX) in memory starting at address I.
	 */
	protected void reg_dump()
	{
		for (int i = 0; i < (opcode & 0x0F00); i++)
		{
			memory[I+i] = V[i];
		}
		pc += 2;
	}

	/**
	 * FX65 - reg_load(Vx,&I) - Fills V0 to VX (including VX) with values from memory starting at address I.
	 */
	protected void reg_load()
	{
		for (int i = 0; i < (opcode & 0x0F00); i++)
		{
			V[i] = memory[I+1];
		}
		pc += 2;
	}
}
