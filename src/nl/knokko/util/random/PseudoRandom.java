/*******************************************************************************
 * The MIT License
 *
 * Copyright (c) 2018 knokko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *  
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
package nl.knokko.util.random;

import java.util.Arrays;

import nl.knokko.util.bits.BitHelper;
import nl.knokko.util.bits.BooleanArrayBitOutput;
import static nl.knokko.util.bits.BitHelper.byteToBinary;

public class PseudoRandom extends Random {

	public static long XOR_TIME = 0;
	public static long SHIFT_TIME1 = 0;
	public static long REPLACE_TIME = 0;
	public static long SHIFT_TIME2 = 0;
	public static long INVERT_TIME = 0;

	private static final int INDEX = 77;
	private static final int SHIFTER = 214;

	private static final boolean[] HARD_MASK = parse(
			"1101000101001000010011100000101000000101100011101001010100000010001000100100111101011100001001111011010110011100001001010010011111100001010010010001101010000110101111011010100011001000100100111100010001100010110011001001000010100101001011110111110011011100");

	public static boolean[] parse(String string) {
		if (string.length() != 256)
			throw new IllegalArgumentException("length must be 256 instead of " + string.length());
		boolean[] result = new boolean[256];
		for (int index = 0; index < 256; index++) {
			char c = string.charAt(index);
			if (c == '1')
				result[index] = true;
			else if (c != '0')
				throw new IllegalArgumentException("Invalid character: " + c + " at index " + index);
		}
		return result;
	}

	private final boolean[] data;

	private Configuration config;

	private int xorCounter;
	private int shiftCounter1;
	private int replaceCounter;
	private int shiftCounter2;
	private int invertCounter;

	private byte shiftAmount;

	public PseudoRandom(Configuration config) {
		this.config = config;
		data = new boolean[256];
		long millis = System.currentTimeMillis();
		long nanos = System.nanoTime();
		seed(millis * millis, nanos * nanos, millis * nanos, millis + nanos);
	}

	public PseudoRandom(String seed, Configuration config) {
		this.config = config;
		int totalLength = seed.length();
		if (totalLength / 16 * 16 != totalLength)
			throw new IllegalArgumentException("Can't divide totalLength (" + totalLength + ") through 16");
		char[] chars = seed.toCharArray();
		int counter = 0;
		BooleanArrayBitOutput helper = new BooleanArrayBitOutput(256);
		for (char c : chars) {
			helper.addChar(c);
			counter++;
			if (counter == 16) {
				counter = 0;
				helper.setWriteIndex(0);
			}
		}
		data = helper.getBackingArray();
	}

	public PseudoRandom(boolean[] data, Configuration config) {
		this.config = config;
		if (data.length == 256)
			this.data = data;
		else
			throw new IllegalArgumentException("Length must be 256, not " + data.length);
	}

	public PseudoRandom(long seed1, Configuration config) {
		this(seed1, seed1 / 3487834, seed1 * 9678538, seed1 - 14396, config);
	}

	public PseudoRandom(long seed1, long seed2, long seed3, long seed4, Configuration config) {
		this.config = config;
		data = new boolean[256];
		seed(seed1, seed2, seed3, seed4);
	}

	public PseudoRandom(int seed1, int seed2, Configuration config) {
		this(seed1, seed1 / 31, seed1 * 97, seed1 - 198345, seed2, seed2 / 31, seed2 * 97, seed2 - 198345, config);
	}

	public PseudoRandom(int seed1, int seed2, int seed3, int seed4, int seed5, int seed6, int seed7, int seed8,
			Configuration config) {
		this.config = config;
		data = new boolean[256];
		seed(seed1, seed2, seed3, seed4, seed5, seed6, seed7, seed8);
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}

	@Override
	public String toString() {
		char[] chars = new char[260];
		chars[0] = 'P';
		chars[1] = 'R';
		chars[2] = '[';
		for (int index = 0; index < data.length; index++) {
			chars[3 + index] = data[index] ? '1' : '0';
		}
		chars[259] = ']';
		return new String(chars);
	}

	private void seed(long seed1, long seed2, long seed3, long seed4) {
		fill(0, seed1);
		fill(64, seed2);
		fill(128, seed3);
		fill(192, seed4);
	}

	private void seed(int seed1, int seed2, int seed3, int seed4, int seed5, int seed6, int seed7, int seed8) {
		setIntAt(0, seed1);
		setIntAt(32, seed2);
		setIntAt(64, seed3);
		setIntAt(96, seed4);
		setIntAt(128, seed5);
		setIntAt(160, seed6);
		setIntAt(192, seed7);
		setIntAt(224, seed8);
	}

	private void fill(int index, long value) {
		byteToBinary(BitHelper.long0(value), data, index);
		byteToBinary(BitHelper.long1(value), data, index + 8);
		byteToBinary(BitHelper.long2(value), data, index + 16);
		byteToBinary(BitHelper.long3(value), data, index + 24);
		byteToBinary(BitHelper.long4(value), data, index + 32);
		byteToBinary(BitHelper.long5(value), data, index + 40);
		byteToBinary(BitHelper.long6(value), data, index + 48);
		byteToBinary(BitHelper.long7(value), data, index + 56);
	}

	public boolean next() {
		int oldIndex = getIndex();
		boolean result = data[oldIndex];
		long startTime = System.nanoTime();
		xor(oldIndex);// only 256 possibilities to try
		XOR_TIME += System.nanoTime() - startTime;
		startTime = System.nanoTime();
		shift1();// only 256 possibilities to try
		SHIFT_TIME1 += System.nanoTime() - startTime;
		startTime = System.nanoTime();
		replace(result);// good luck with reversing this one...
		REPLACE_TIME += System.nanoTime() - startTime;
		startTime = System.nanoTime();
		shift2();// can be reversed by trying all 256 possible previous
					// indices
		SHIFT_TIME2 += System.nanoTime() - startTime;
		startTime = System.nanoTime();
		invert();// the index may or may not have overwritten itself
		INVERT_TIME += System.nanoTime() - startTime;
		setIndex(getAt(getIndex() - 96));// can be reversed by trying all 256 possible previous indices
		return result;
	}

	protected void setAt(int index, boolean[] number) {
		// System.out.println("PseudoRandom.setAt(" + index + "," +
		// Arrays.toString(number) + ")");
		while (index < 0)
			index += 256;
		while (index >= 256)
			index -= 256;
		int copyAmount = 256 - index;
		if (copyAmount >= number.length) {
			// we won't reach the end of our data
			System.arraycopy(number, 0, data, index, number.length);
			// System.out.println(index + "Set data to " + Arrays.toString(data));
		} else {
			// we will reach the end of our data, so we resume at the begin of our data
			System.arraycopy(number, 0, data, index, copyAmount);
			System.arraycopy(number, copyAmount, data, 0, number.length - copyAmount);
		}
	}

	protected void setIntAt(int index, int value) {
		setAt(index, BitHelper.byteToBinary(BitHelper.int0(value)));
		setAt(index + 8, BitHelper.byteToBinary(BitHelper.int1(value)));
		setAt(index + 16, BitHelper.byteToBinary(BitHelper.int2(value)));
		setAt(index + 24, BitHelper.byteToBinary(BitHelper.int3(value)));
	}

	protected void setAt(int index, int value) {
		boolean[] binary = BitHelper.byteToBinary((byte) (value - 128));
		setAt(index, binary);
	}

	protected boolean[] getAt(int index, int length) {
		while (index < 0)
			index += 256;
		while (index >= 256)
			index -= 256;
		boolean[] result = new boolean[length];
		int copyAmount = 256 - index;
		if (copyAmount >= length) {
			// we won't reach the end of the array
			System.arraycopy(data, index, result, 0, length);
		} else {
			// we will reach the end of the array, so we resume at begin of the array
			System.arraycopy(data, index, result, 0, copyAmount);
			System.arraycopy(data, 0, result, copyAmount, length - copyAmount);
		}
		return result;
	}

	protected int getAt(int index) {
		return BitHelper.byteFromBinary(getAt(index, 8)) + 128;
	}

	private int getIndex() {
		return getAt(INDEX);
	}

	private void setIndex(int index) {
		setAt(INDEX, index);
	}

	private void xor(int oldIndex) {
		if (xorCounter <= 0) {
			xor(oldIndex, HARD_MASK);
			xorCounter = config.xorPeriod;
		} else {
			xorCounter--;
			if (data[10]) {
				xorCounter--;
			}
		}
	}

	private void xor(int index, boolean[] mask) {
		int maskIndex = 0;
		for (; maskIndex + index < 256; maskIndex++) {
			if (mask[maskIndex]) {
				data[index + maskIndex] = !data[index + maskIndex];
			}
		}
		for (; maskIndex < 256; maskIndex++) {
			if (mask[maskIndex]) {
				data[index + maskIndex - 256] = !data[index + maskIndex - 256];
			}
		}
	}

	private void shift1() {
		if (shiftCounter1 <= 0) {
			shift(getAt(SHIFTER) + 50);
			shiftCounter1 = config.shiftPeriod1;
		} else {
			shiftCounter1--;
			if (data[7]) {
				shiftCounter1--;
			}
		}
	}

	private void shift2() {
		if (shiftCounter2 <= 0) {
			shift(getAt(getIndex() - 22) + shiftAmount++);
			shiftCounter2 = config.shiftPeriod2;
		} else {
			shiftCounter2--;
			if (data[12]) {
				shiftCounter2--;
			}
		}
	}

	private void shift(int direction) {
		boolean[] backup = Arrays.copyOf(data, 256);
		setAt(direction, backup);
	}

	private void replace(boolean result) {
		if (replaceCounter <= 0) {
			replace(getIndex() + 69, result);
			replaceCounter = config.replacePeriod;
		} else {
			replaceCounter--;
			if (result) {
				--replaceCounter;
			}
		}
	}

	private void replace(int baseIndex, boolean value) {
		int[] firstIndices = new int[32];
		for (int i = 0; i < 32; i++)
			firstIndices[i] = getAt(baseIndex + i * 8);
		int[] secondIndices = new int[256];
		for (int i = 0; i < 32; i++) {
			secondIndices[i * 8 + 0] = getAt(firstIndices[i] + 0);
			secondIndices[i * 8 + 1] = getAt(firstIndices[i] + 23);
			secondIndices[i * 8 + 2] = getAt(firstIndices[i] + 143);
			secondIndices[i * 8 + 3] = getAt(firstIndices[i] + 12);
			secondIndices[i * 8 + 4] = getAt(firstIndices[i] - 74);
			secondIndices[i * 8 + 5] = getAt(firstIndices[i] - 213);
			secondIndices[i * 8 + 6] = getAt(firstIndices[i] + 176);
			secondIndices[i * 8 + 7] = getAt(firstIndices[i] + 58);
		}
		boolean[] copy = Arrays.copyOf(data, 256);
		for (int index = 0; index < 256; index++)
			data[index] = copy[secondIndices[index]];

		/*
		 * Crack 0101010101010101...
		 * 
		 * data[secondIndices[0]] = 0 so data[getAt(firstIndices[0])] = 0 so there are
		 * roughly 128 possibilities for getAt(firstIndices[0])
		 * 
		 * data[secondIndices[1]] = 1 so data[getAt(firstIndices[0] + 23)] = 1 so there
		 * are roughly 128 possibilities for getAt(firstIndices[0] + 23)
		 * 
		 * So you get 8 times roughly 128 possibilities for every firstIndex The
		 * annoying part is that you don't know the baseIndex and the data, so you can't
		 * test the possibilities Besides, you don't know when the replaces happened
		 */
	}

	private void invert() {
		if (invertCounter <= 0) {
			invert(getAt(getIndex() + 17));
			invertCounter = config.invertPeriod;
		} else {
			invertCounter--;
			if (data[3]) {
				invertCounter--;
			}
		}
	}

	private void invert(int index) {
		boolean[] bools = getAt(index, 15);
		for (int i = 0; i < bools.length; i++)
			bools[i] = !bools[i];
		setAt(index, bools);
	}

	public boolean[] getData() {
		return Arrays.copyOf(data, 256);
	}

	public int getReplaceCounter() {
		return replaceCounter;
	}

	@Override
	public boolean isPseudo() {
		return true;
	}

	@Override
	public Random clone() {
		PseudoRandom clone = new PseudoRandom(Arrays.copyOf(data, data.length), config);
		clone.xorCounter = xorCounter;
		clone.shiftCounter1 = shiftCounter1;
		clone.replaceCounter = replaceCounter;
		clone.shiftCounter2 = shiftCounter2;
		clone.invertCounter = invertCounter;
		clone.shiftAmount = shiftAmount;
		return clone;
	}

	public static class Configuration {
		
		/**
		 * The 'legacy' configuration. This is a quite heavy configuration for the random number generator,
		 * where heavy means that generating a lot of random data takes relatively long. This is not really
		 * a bad configuration, but it is 'legacy' because all random number generators before configurations
		 * were added had this 'configuration'.
		 */
		public static final Configuration LEGACY = new Configuration(0, 0, 29, 0, 0);
		
		public static final Configuration MEDIUM = new Configuration(10, 11, 12, 101, 13);
		
		public static final Configuration LIGHT = new Configuration(57, 34, 491, 40, 67);

		private final int xorPeriod;
		private final int shiftPeriod1;
		private final int replacePeriod;
		private final int shiftPeriod2;
		private final int invertPeriod;

		public Configuration(int xorPeriod, int shiftPeriod1, int replacePeriod, int shiftPeriod2, int invertPeriod) {
			this.xorPeriod = xorPeriod;
			this.shiftPeriod1 = shiftPeriod1;
			this.replacePeriod = replacePeriod;
			this.shiftPeriod2 = shiftPeriod2;
			this.invertPeriod = invertPeriod;
		}
	}
}