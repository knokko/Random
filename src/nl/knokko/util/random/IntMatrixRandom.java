package nl.knokko.util.random;

import nl.knokko.util.bits.BitHelper;

public class IntMatrixRandom extends IntBasedRandom {
	
	private final int[] matrixData;
	private final int[] tempBuffer;
	
	private final int length;
	private final int arrayLength;
	
	private int nextIndex;
	
	public IntMatrixRandom(int length, int[] startContents, int startOffset) {
		if (length <= 1) throw new IllegalArgumentException("Length must be at least 2, but is " + length);
		this.length = length;
		this.arrayLength = length * length;
		this.tempBuffer = new int[arrayLength];
		this.matrixData = new int[arrayLength];
		System.arraycopy(startContents, startOffset, tempBuffer, 0, arrayLength);
		System.arraycopy(startContents, startOffset, matrixData, 0, arrayLength);
		// nextIndex = 0 by default
	}
	
	public IntMatrixRandom(int length, long seed) {
		if (length <= 1) throw new IllegalArgumentException("Length must be at least 2, but is " + length);
		this.length = length;
		this.arrayLength = length * length;
		this.tempBuffer = new int[arrayLength];
		this.matrixData = new int[arrayLength];
		for (int index = 0; index < arrayLength; index++) {
			tempBuffer[index] = (int) (seed);
			matrixData[index] = (int) (seed);
			seed += 1234;
			seed *= seed;
			seed /= 3;
		}
	}
	
	public IntMatrixRandom(int length) {
		this(length, System.nanoTime());
		// nextIndex = 0 by default
	}
	
	private void square() {
		for (int x = 0; x < length; x++) {
			for (int y = 0; y < length; y++) {
				int sum = 0;
				for (int i = 0; i < length; i++) {
					sum += matrixData[x + length * i] * matrixData[i + length * y];
				}
				tempBuffer[x + y * length] = sum;
			}
		}
		System.arraycopy(tempBuffer, 0, matrixData, 0, length * length);
	}
	
	private void increment() {
		for (int index = 0; index < arrayLength; index++) {
			tempBuffer[index] += nextIndex;
			matrixData[index] += nextIndex;
		}
		if (++nextIndex == arrayLength) {
			nextIndex = 0;
		}
	}
	
	public int[] getBackingArray() {
		return matrixData;
	}

	@Override
	public int nextInt() {
		square();
		increment();
		byte b0 = 0;
		byte b1 = 1;
		byte b2 = 2;
		byte b3 = 3;
		for (int index = 0; index + 4 <= arrayLength; index++) {
			b0 += BitHelper.int0(matrixData[index]) + BitHelper.int1(matrixData[index + 1])
			+ BitHelper.int2(matrixData[index + 2]) + BitHelper.int3(matrixData[index + 3]);
			b1 += BitHelper.int0(matrixData[index + 1]) + BitHelper.int1(matrixData[index + 2])
			+ BitHelper.int2(matrixData[index + 3]) + BitHelper.int3(matrixData[index]);
			b2 += BitHelper.int0(matrixData[index + 2]) + BitHelper.int1(matrixData[index + 3])
			+ BitHelper.int2(matrixData[index]) + BitHelper.int3(matrixData[index + 1]);
			b3 += BitHelper.int0(matrixData[index + 3]) + BitHelper.int1(matrixData[index])
			+ BitHelper.int2(matrixData[index + 1]) + BitHelper.int3(matrixData[index + 2]);
		}
		return BitHelper.makeInt(b0, b1, b2, b3);
	}

	@Override
	public boolean isPseudo() {
		return true;
	}

	@Override
	public IntMatrixRandom clone() {
		return new IntMatrixRandom(length, matrixData, 0);
	}
}