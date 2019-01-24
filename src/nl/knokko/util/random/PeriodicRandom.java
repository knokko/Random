package nl.knokko.util.random;

import java.util.Arrays;

public class PeriodicRandom extends Random {
	
	private Random backer;
	private final boolean[] buffer;
	private int period;
	
	private int counter;
	private int index;
	
	public PeriodicRandom(Random backer, int bufferSize, int period) {
		this.backer = backer;
		this.buffer = new boolean[bufferSize];
		this.period = period;
		refresh();
	}
	
	private PeriodicRandom(Random backer, boolean[] buffer, int period, int counter, int index) {
		this.backer = backer;
		this.buffer = buffer;
		this.period = period;
		this.counter = counter;
		this.index = index;
	}
	
	private void refresh() {
		for (int index = 0; index < buffer.length; index++) {
			buffer[index] = backer.next();
		}
	}

	@Override
	public boolean next() {
		if (index < buffer.length) {
			return buffer[index++];
		}
		counter++;
		index = 0;
		if (counter >= period) {
			refresh();
			counter = 0;
		}
		return buffer[0];
	}

	@Override
	public boolean isPseudo() {
		return backer.isPseudo();
	}

	@Override
	public Random clone() {
		return new PeriodicRandom(backer.clone(), Arrays.copyOf(buffer, buffer.length), period, counter, index);
	}
}