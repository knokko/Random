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

import nl.knokko.util.bits.BitHelper;

public abstract class Random {

	public static byte getRequiredBits(long number) {
		byte b = 0;
		while (BitHelper.get2Power(b) <= number)
			b++;
		return b;
	}

	/**
	 * All implementations of Random must override this method. All other methods of
	 * Random will use next() calls to generate its value. This method will be
	 * called for every bit that is necessary for the result. The chance to generate
	 * false should be equal to the chance to generate true
	 * 
	 * @return the next random boolean
	 */
	public abstract boolean next();

	/**
	 * This method does the same as next(), but its name is more clear.
	 * 
	 * @return the next random boolean
	 */
	public boolean nextBoolean() {
		return next();
	}

	/**
	 * This method generates the next random byte. It can generate every byte value
	 * with equal chance to get every byte.
	 * 
	 * @return the next random byte
	 */
	public byte nextByte() {
		return BitHelper.byteFromBinary(next(), next(), next(), next(), next(), next(), next(), next());
	}

	/**
	 * This method generates the next random short. It can generate every short
	 * value with equal chance to get every short.
	 * 
	 * @return the next random short
	 */
	public short nextShort() {
		return BitHelper.makeShort(nextByte(), nextByte());
	}

	/**
	 * This method generates the next random char. It can generate every char value
	 * with equal chance to get every char.
	 * 
	 * @return the next random character
	 */
	public char nextChar() {
		return BitHelper.makeChar(nextByte(), nextByte());
	}

	/**
	 * This method generates the next random int. It can generate every int value
	 * with equal chance to get every int.
	 * 
	 * @return the next random integer
	 */
	public int nextInt() {
		return BitHelper.makeInt(nextByte(), nextByte(), nextByte(), nextByte());
	}

	/**
	 * This method generates the next random long. It can generate every long value
	 * with equal chance to get every long.
	 * 
	 * @return the next random long
	 */
	public long nextLong() {
		return BitHelper.makeLong(nextByte(), nextByte(), nextByte(), nextByte(), nextByte(), nextByte(), nextByte(),
				nextByte());
	}

	/**
	 * This method generates the next random float. The result can be any float
	 * value, including NaN values, infinity values and 0 values. The chance for
	 * every possible float value should be equal. This method simple returns
	 * Float.intBitsToFloat(nextInt().
	 * 
	 * @return the next random float
	 */
	public float nextTrueFloat() {
		return Float.intBitsToFloat(nextInt());
	}

	/**
	 * This method generates a random float in the range 0 (inclusive) to 1
	 * (exclusive). All possible float values in this range can be returned with
	 * equal chance.
	 * 
	 * @return a random float between 0 (inclusive) and 1 (exclusive)
	 */
	public float nextFloat() {
		byte b0 = nextByte();
		byte b1 = nextByte();
		byte b2 = BitHelper.byteFromBinary(next(), next(), next(), next(), next(), next(), next(), true);
		byte b3 = BitHelper.byteFromBinary(false, next(), next(), next(), next(), next(), next(), true);
		return Float.intBitsToFloat(BitHelper.makeInt(b0, b1, b2, b3));
	}

	/**
	 * This method generates the next random double. The result can be any double
	 * value, including NaN values, infinity values and 0 values. The chance for
	 * every possible double value should be equal. This method simple returns
	 * Double.longBitsToDouble(nextLong().
	 * 
	 * @return the next random double
	 */
	public double nextTrueDouble() {
		return Double.longBitsToDouble(nextLong());
	}

	/**
	 * This method generates a random double in the range 0 (inclusive) to 1
	 * (exclusive). The result can be any double value in this range with equal
	 * chance.
	 * 
	 * @return a random double between 0 (inclusive) and 1 (exclusive)
	 */
	public double nextDouble() {
		byte b0 = nextByte();
		byte b1 = nextByte();
		byte b2 = nextByte();
		byte b3 = nextByte();
		byte b4 = nextByte();
		byte b5 = nextByte();
		// now, the annoying part...
		byte b6;
		byte b7 = BitHelper.byteFromBinary(false, next(), next(), next(), next(), next(), next(), true);
		if (b7 < 63) {
			// the easier part
			b6 = nextByte();
		} else {
			// the really annoying part
			// when b7 is 63, b6 mustn't be between -16 and -1
			do {
				b6 = nextByte();
			} while (b6 >= -16 && b6 <= -1);
		}
		return Double.longBitsToDouble(BitHelper.makeLong(b0, b1, b2, b3, b4, b5, b6, b7));
	}

	public int nextInt(int bound) {
		byte bits = getRequiredBits(bound - 1);
		long result;
		do {
			result = BitHelper.numberFromBinary(nextBooleans(bits), bits, false);
		} while (result >= bound);
		return (int) result;
	}

	public long nextLong(long bound) {
		byte bits = getRequiredBits(bound - 1);
		long result;
		do {
			result = BitHelper.numberFromBinary(nextBooleans(bits), bits, false);
		} while (result >= bound);
		return result;
	}

	/**
	 * This method creates a boolean[] with the given size and fills it with random
	 * booleans.
	 * 
	 * @param size the size of the boolean[] that will be created and returned
	 * @return a boolean[] with specified size that is filled with random booleans
	 */

	public boolean[] nextBooleans(int size) {
		boolean[] result = new boolean[size];
		for (int index = 0; index < size; index++)
			result[index] = next();
		return result;
	}

	public byte[] nextBytes(int amount) {
		byte[] result = new byte[amount];
		for (int index = 0; index < amount; index++)
			result[index] = nextByte();
		return result;
	}

	public char[] nextChars(int amount) {
		char[] result = new char[amount];
		for (int index = 0; index < amount; index++)
			result[index] = nextChar();
		return result;
	}

	public int[] nextInts(int amount) {
		int[] result = new int[amount];
		for (int index = 0; index < amount; index++)
			result[index] = nextInt();
		return result;
	}

	public float[] nextFloats(int amount) {
		float[] result = new float[amount];
		for (int index = 0; index < amount; index++) {
			result[index] = nextFloat();
		}
		return result;
	}

	public long[] nextLongs(int amount) {
		long[] result = new long[amount];
		for (int index = 0; index < amount; index++)
			result[index] = nextLong();
		return result;
	}

	public double[] nextDoubles(int amount) {
		double[] result = new double[amount];
		for (int index = 0; index < amount; index++) {
			result[index] = nextDouble();
		}
		return result;
	}
}