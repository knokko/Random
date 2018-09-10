package nl.knokko.util.random;

import java.util.Arrays;

import nl.knokko.util.bits.ByteArrayBitInput;

public class RandomArray extends Random {
	
	private static int ceilDiv(int i1, int i2){
		int div = i1 / i2;
		if(i2 * div != i1)
			div++;
		return div;
	}
	
	public static RandomArray createPseudo(byte...bytes){
		Random[] source = new Random[ceilDiv(bytes.length, 32)];
		bytes = Arrays.copyOf(bytes, 32 * ceilDiv(bytes.length, 32));
		ByteArrayBitInput input = new ByteArrayBitInput(bytes);//if the amount of bytes can't be divided through 32, the remaining bytes will be 0
		for(int index = 0; index < source.length; index++){
			source[index] = new PseudoRandom(input.readInt(),input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt(), input.readInt());
		}
		return new RandomArray(source);
	}
	
	private final Random[] source;
	private int index;
	private int counter;

	public RandomArray(Random...sources) {
		source = sources;
		//index starts at 0
		//counter starts at 0
	}

	@Override
	public boolean next() {
		boolean result = source[index].next();
		counter++;
		if(result)
			counter++;
		if(counter == 81 || counter == 82){
			index = source[index].nextInt(source.length);
			counter = 0;
		}
		return result;
	}
	
	@Override
	public String toString(){
		return Arrays.toString(source);
	}
}