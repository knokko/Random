package nl.knokko.util.random;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import nl.knokko.util.bits.BitHelper;
import nl.knokko.util.bits.BitInput;
import nl.knokko.util.bits.BitInputStream;
import nl.knokko.util.bits.BitOutput;
import nl.knokko.util.bits.BitOutputStream;
import static nl.knokko.util.bits.BitHelper.byteFromBinary;

public class CrazyRandom extends Random {
	
	public static CrazyRandom fromFile(File file) throws IOException {
		BitInput input = new BitInputStream(new FileInputStream(file));
		boolean[] state = input.readBooleans(32000);
		input.terminate();
		PseudoRandom temp = new PseudoRandom(System.nanoTime());
		int index = temp.nextInt(MAX_INDEX + 1);
		return new CrazyRandom(state, index);
	}
	
	private static final int LENGTH = 32000;
	private static final int MAX_INDEX = LENGTH - 1025;
	
	/**
	 * length must be 32000
	 */
	private final boolean[] state;
	
	private int index;
	private short counter;

	public CrazyRandom(boolean[] state, int index) {
		if(state.length != LENGTH)
			throw new IllegalArgumentException("Length of state must be " + LENGTH);
		this.state = state;
		this.index = index;
	}
	
	public void saveToFile(File file) throws IOException {
		BitOutput output = new BitOutputStream(new FileOutputStream(file));
		output.addBooleans(state);
		output.terminate();
	}

	@Override
	public boolean next() {
		boolean result = state[index];
		counter++;
		if(result)
			counter++;
		if((counter / 45) * 45 == counter){
			weakMix();
			clearTrace();
		}
		else if((counter / 123) * 123 == counter){
			mediumMix();
			clearTrace();
		}
		else if((counter / 421) * 421 == counter){
			strongMix();
			clearTrace();
		}
		else if(counter == 2998 || counter == 2999){
			superMix();
			clearTrace();
			counter = 0;
		}
		else {
			index--;
			if(index <= 0)
				index = MAX_INDEX;
		}
		return result;
	}
	
	protected void clearTrace(){
		PseudoRandom temp = new PseudoRandom(readLong(index), readLong(index + 256), readLong(index + 512), readLong(index + 768));
		addLong(index, temp.nextLong());
		addLong(index + 256, temp.nextLong());
		addLong(index + 512, temp.nextLong());
		addLong(index + 768, temp.nextLong());
		//System.out.println(index);
		index += temp.nextInt(MAX_INDEX + 1);
		//System.out.println(index);
		if(index > MAX_INDEX)
			index -= MAX_INDEX;
		//System.out.println(index);
		invert(index, 1024);
	}
	
	protected void superMix(){
		Random first = new PseudoRandom(System.nanoTime(), System.identityHashCode(this), System.currentTimeMillis(), System.identityHashCode(System.out));
		RandomArray second = RandomArray.createPseudo(first.nextBytes(640));
		for(int count = 0; count < 1000; count++)
			addLong(second.nextInt(MAX_INDEX + 1), second.nextLong());
	}
	
	protected void strongMix(){
		Random first = new PseudoRandom(System.nanoTime(), readLong(index), readLong(index + 256), readLong(index + 512));
		RandomArray second = RandomArray.createPseudo(first.nextBytes(128));
		for(int count = 0; count < 100; count++)
			addLong(second.nextInt(MAX_INDEX + 1), second.nextLong());
	}
	
	protected void mediumMix(){
		Random random = new PseudoRandom(System.nanoTime());
		for(int count = 0; count < 40; count++)
			writeLong(random.nextInt(MAX_INDEX + 1), random.nextLong());
	}
	
	protected void weakMix(){
		Random random = new PseudoRandom(System.nanoTime());
		for(int count = 0; count < 10; count++)
			writeLong(random.nextInt(MAX_INDEX + 1), random.nextLong());
	}
	
	protected long readLong(int index){
		return BitHelper.makeLong(byteFromBinary(state, index), byteFromBinary(state, index + 8), byteFromBinary(state, index + 16), byteFromBinary(state, index + 24), byteFromBinary(state, index + 32), byteFromBinary(state, index + 40), byteFromBinary(state, index + 48), byteFromBinary(state, index + 56));
	}
	
	protected void addLong(int index, long value){
		writeLong(index, readLong(index) + value);
	}
	
	protected void writeLong(int index, long value){
		BitHelper.byteToBinary(BitHelper.long0(value), state, index);
		BitHelper.byteToBinary(BitHelper.long1(value), state, index + 8);
		BitHelper.byteToBinary(BitHelper.long2(value), state, index + 16);
		BitHelper.byteToBinary(BitHelper.long3(value), state, index + 24);
		BitHelper.byteToBinary(BitHelper.long4(value), state, index + 32);
		BitHelper.byteToBinary(BitHelper.long5(value), state, index + 40);
		BitHelper.byteToBinary(BitHelper.long6(value), state, index + 48);
		BitHelper.byteToBinary(BitHelper.long7(value), state, index + 56);
	}
	
	protected void invert(int index, int amount){
		int bound = index + amount;
		for(; index < bound; index++)
			state[index] = !state[index];
	}
}