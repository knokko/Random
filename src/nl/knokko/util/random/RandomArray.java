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

import nl.knokko.util.bits.ByteArrayBitInput;

public class RandomArray extends Random {

	private static int ceilDiv(int i1, int i2) {
		int div = i1 / i2;
		if (i2 * div != i1)
			div++;
		return div;
	}

	public static RandomArray createPseudo(byte... bytes) {
		Random[] source = new Random[ceilDiv(bytes.length, 32)];
		bytes = Arrays.copyOf(bytes, 32 * ceilDiv(bytes.length, 32));
		ByteArrayBitInput input = new ByteArrayBitInput(bytes);// if the amount of bytes can't be divided through 32,
																// the remaining bytes will be 0
		for (int index = 0; index < source.length; index++) {
			source[index] = new PseudoRandom(input.readInt(), input.readInt(), input.readInt(), input.readInt(),
					input.readInt(), input.readInt(), input.readInt(), input.readInt());
		}
		return new RandomArray(source);
	}

	private final Random[] source;
	private int index;
	private int counter;

	public RandomArray(Random... sources) {
		source = sources;
		// index starts at 0
		// counter starts at 0
	}

	@Override
	public boolean next() {
		boolean result = source[index].next();
		counter++;
		if (result)
			counter++;
		if (counter == 81 || counter == 82) {
			index = source[index].nextInt(source.length);
			counter = 0;
		}
		return result;
	}

	@Override
	public String toString() {
		return Arrays.toString(source);
	}

	@Override
	public boolean isPseudo() {
		for (Random random : source) {
			if (!random.isPseudo()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Random clone() {
		Random[] array = new Random[source.length];
		for (int index = 0; index < array.length; index++) {
			array[index] = source[index].clone();
		}
		RandomArray clone = new RandomArray(array);
		clone.index = index;
		clone.counter = counter;
		return clone;
	}
}