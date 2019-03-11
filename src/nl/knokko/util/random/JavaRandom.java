package nl.knokko.util.random;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A subclass of nl.knokko.util.Random that uses an instance of java.util.Random to produce random numbers.
 * This class can be used to compare my random number generators with the random number generator of java.
 * @author knokko
 *
 */
public class JavaRandom extends Random {
	
	private final java.util.Random backingRandom;
	
	public JavaRandom() {
		this(new java.util.Random());
	}
	
	public JavaRandom(java.util.Random backingRandom) {
		this.backingRandom = backingRandom;
	}

	@Override
	public boolean next() {
		return backingRandom.nextBoolean();
	}

	@Override
	public boolean isPseudo() {
		return true;
	}

	@Override
	public Random clone() {
		
		// Copy the backing random...
		java.util.Random randomCopy;
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bo);
			oos.writeObject(backingRandom);
			oos.close();
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()));
			randomCopy = (java.util.Random) (ois.readObject());
		} catch (IOException ioex) {
			throw new Error("Shouldn't happen", ioex);
		} catch (ClassNotFoundException cnfe) {
			throw new Error("Shoulnd't happen", cnfe);
		}
		
		// And finally return the copy
		return new JavaRandom(randomCopy);
	}
}