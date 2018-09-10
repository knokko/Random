package nl.knokko.util.random;

import java.util.Arrays;

import nl.knokko.util.bits.BitHelper;
import nl.knokko.util.bits.BooleanArrayBitOutput;
import static nl.knokko.util.bits.BitHelper.byteToBinary;

public class PseudoRandom extends Random {
	
	private static final int INDEX = 77;
	private static final int SHIFTER = 214;
	
	private static final boolean[] HARD_MASK = parse(
			"1101000101001000010011100000101000000101100011101001010100000010001000100100111101011100001001111011010110011100001001010010011111100001010010010001101010000110101111011010100011001000100100111100010001100010110011001001000010100101001011110111110011011100"
	);
	
	public static boolean[] parse(String string){
		if(string.length() != 256)
			throw new IllegalArgumentException("length must be 256 instead of " + string.length());
		boolean[] result = new boolean[256];
		for(int index = 0; index < 256; index++){
			char c = string.charAt(index);
			if(c == '1')
				result[index] = true;
			else if(c != '0')
				throw new IllegalArgumentException("Invalid character: "  + c + " at index " + index);
		}
		return result;
	}
	
	protected final boolean[] data;
	
	private byte replaceCounter;
	private byte shiftCounter;
	
	public PseudoRandom(){
		data = new boolean[256];
		long millis = System.currentTimeMillis();
		long nanos = System.nanoTime();
		seed(millis * millis, nanos * nanos, millis * nanos, millis + nanos);
	}
	
	public PseudoRandom(String seed){
		int totalLength = seed.length();
		if(totalLength / 16 * 16 != totalLength) throw new IllegalArgumentException("Can't divide totalLength (" + totalLength + ") through 16");
		char[] chars = seed.toCharArray();
		int counter = 0;
		BooleanArrayBitOutput helper = new BooleanArrayBitOutput(256);
		for(char c : chars){
			helper.addChar(c);
			counter++;
			if(counter == 16){
				counter = 0;
				helper.setWriteIndex(0);
			}
		}
		data = helper.getRawBooleans();
	}
	
	public PseudoRandom(boolean[] data){
		if(data.length == 256)
			this.data = data;
		else
			throw new IllegalArgumentException("Length must be 256, not " + data.length);
	}
	
	public PseudoRandom(long seed1){
		this(seed1, seed1 / 3487834, seed1 * 9678538, seed1 - 14396);
	}
	
	public PseudoRandom(long seed1, long seed2, long seed3, long seed4){
		data = new boolean[256];
		seed(seed1, seed2, seed3, seed4);
	}
	
	public PseudoRandom(int seed1, int seed2){
		this(seed1, seed1 / 31, seed1 * 97, seed1 - 198345, seed2, seed2 / 31, seed2 * 97, seed2 - 198345);
	}
	
	public PseudoRandom(int seed1, int seed2, int seed3, int seed4, int seed5, int seed6, int seed7, int seed8){
		data = new boolean[256];
		seed(seed1, seed2, seed3, seed4, seed5, seed6, seed7, seed8);
	}
	
	@Override
	public String toString(){
		char[] chars = new char[260];
		chars[0] = 'P';
		chars[1] = 'R';
		chars[2] = '[';
		for(int index = 0; index < data.length; index++){
			chars[3 + index] = data[index] ? '1' : '0';
		}
		chars[259] = ']';
		return new String(chars);
	}
	
	private void seed(long seed1, long seed2, long seed3, long seed4){
		fill(0, seed1);
		fill(64, seed2);
		fill(128, seed3);
		fill(192, seed4);
	}
	
	private void seed(int seed1, int seed2, int seed3, int seed4, int seed5, int seed6, int seed7, int seed8){
		setIntAt(0, seed1);
		setIntAt(32, seed2);
		setIntAt(64, seed3);
		setIntAt(96, seed4);
		setIntAt(128, seed5);
		setIntAt(160, seed6);
		setIntAt(192, seed7);
		setIntAt(224, seed8);
	}
	
	private void fill(int index, long value){
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
		xor(oldIndex, HARD_MASK);//only 256 possibilities to try
		shift(getAt(SHIFTER) + 50);//only 256 possibilities to try
		replace(getIndex() + 69, result);//good luck with reversing this one...
		shift(getAt(getIndex() - 22) + shiftCounter++);//can be reversed by trying all 256 possible previous indices
		invert(getAt(getIndex() + 17));//the index may or may not have overwritten itself
		setIndex(getAt(getIndex() - 96));//can be reversed by trying all 256 possible previous indices
		return result;
	}
	
	protected void setAt(int index, boolean[] number){
		//System.out.println("PseudoRandom.setAt(" + index + "," + Arrays.toString(number) + ")");
		while(index < 0)
			index += 256;
		while(index >= 256)
			index -= 256;
		int copyAmount = 256 - index;
		if(copyAmount >= number.length){
			//we won't reach the end of our data
			System.arraycopy(number, 0, data, index, number.length);
			//System.out.println(index + "Set data to " + Arrays.toString(data));
		}
		else {
			//we will reach the end of our data, so we resume at the begin of our data
			System.arraycopy(number, 0, data, index, copyAmount);
			System.arraycopy(number, copyAmount, data, 0, number.length - copyAmount);
		}
	}
	
	protected void setIntAt(int index, int value){
		setAt(index, BitHelper.byteToBinary(BitHelper.int0(value)));
		setAt(index + 8, BitHelper.byteToBinary(BitHelper.int1(value)));
		setAt(index + 16, BitHelper.byteToBinary(BitHelper.int2(value)));
		setAt(index + 24, BitHelper.byteToBinary(BitHelper.int3(value)));
	}
	
	protected void setAt(int index, int value){
		boolean[] binary = BitHelper.byteToBinary((byte) (value - 128));
		setAt(index, binary);
	}
	
	protected boolean[] getAt(int index, int length){
		while(index < 0)
			index += 256;
		while(index >= 256)
			index -= 256;
		boolean[] result = new boolean[length];
		int copyAmount = 256 - index;
		if(copyAmount >= length){
			//we won't reach the end of the array
			System.arraycopy(data, index, result, 0, length);
		}
		else {
			//we will reach the end of the array, so we resume at begin of the array
			System.arraycopy(data, index, result, 0, copyAmount);
			System.arraycopy(data, 0, result, copyAmount, length - copyAmount);
		}
		return result;
	}
	
	protected int getAt(int index){
		return BitHelper.byteFromBinary(getAt(index, 8)) + 128;
	}
	
	private int getIndex(){
		return getAt(INDEX);
	}
	
	private void setIndex(int index){
		setAt(INDEX, index);
	}
	
	private void xor(int index, boolean[] mask){
		int maskIndex = 0;
		for(; maskIndex + index < 256; maskIndex++){
			if(mask[maskIndex])
				data[index + maskIndex] = !data[index + maskIndex];
		}
		for(; maskIndex < 256; maskIndex++){
			if(mask[maskIndex])
				data[index + maskIndex - 256] = !data[index + maskIndex - 256];
		}
	}
	
	private void shift(int direction){
		boolean[] backup = Arrays.copyOf(data, 256);
		setAt(direction, backup);
	}
	
	private void replace(int baseIndex, boolean value){
		if(replaceCounter <= 0){
			int[] firstIndices = new int[32];
			for(int i = 0; i < 32; i++)
				firstIndices[i] = getAt(baseIndex + i * 8);
			int[] secondIndices = new int[256];
			for(int i = 0; i < 32; i++){
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
			for(int index = 0; index < 256; index++)
				data[index] = copy[secondIndices[index]];
			replaceCounter = 29;
		}
		else {
			--replaceCounter;
			if(value)
				--replaceCounter;
		}
		
		/*
		 * Crack 0101010101010101...
		 * 
		 * data[secondIndices[0]] = 0
		 * so data[getAt(firstIndices[0])] = 0
		 * so there are roughly 128 possibilities for getAt(firstIndices[0])
		 * 
		 * data[secondIndices[1]] = 1
		 * so data[getAt(firstIndices[0] + 23)] = 1
		 * so there are roughly 128 possibilities for getAt(firstIndices[0] + 23)
		 * 
		 * So you get 8 times roughly 128 possibilities for every firstIndex
		 * The annoying part is that you don't know the baseIndex and the data, so you can't test the possibilities
		 * Besides, you don't know when the replaces happened
		 */
	}
	
	private void invert(int index){
		boolean[] bools = getAt(index, 15);
		for(int i = 0; i < bools.length; i++)
			bools[i] = !bools[i];
		setAt(index, bools);
	}
	
	public boolean[] getData(){
		return Arrays.copyOf(data, 256);
	}
	
	public byte getReplaceCounter(){
		return replaceCounter;
	}
}