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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;

import nl.knokko.util.bits.BitHelper;
import nl.knokko.util.bits.BitInput;
import nl.knokko.util.bits.BitOutput;
import nl.knokko.util.bits.BitOutputStream;
import nl.knokko.util.bits.ByteArrayBitInput;
import nl.knokko.util.random.PseudoRandom.Configuration;

import static nl.knokko.util.bits.BitHelper.byteFromBinary;

/**
 * I tried to program a cryptographically strong random number generator and this is the result. It
 * should not be used in any serious environment because it is not proven to be secure. I simply don't
 * know whether or not this is really secure, all I know is that I can't 'predict' it myself.
 * 
 * This class uses a seed, but not only a seed. It also makes use of System.currentTimeMillis(),
 * System.nanoTime() and System.identityHashcode() to manipulate its seed which makes it NOT a true
 * pseudo random number generator.
 * @author knokko
 *
 */
public class CrazyRandom extends Random {
	
	/**
	 * Loads the CrazyRandom seed that is stored within the file and returns an instance of CrazyRandom
	 * with that seed. The start index of the instance will be random.
	 * @param file The file where the seed is stored
	 * @return An instance of CrazyRandom with the seed that is stored in the file
	 * @throws IOException If an IO error occurred while reading the file
	 * @throws RuntimeException If the file doesn't have enough bytes for a seed
	 */
	public static CrazyRandom fromFile(File file) throws IOException {
		BitInput input = ByteArrayBitInput.fromFile(file);
		boolean[] state = input.readBooleans(32000);
		input.terminate();
		int index = (int) (System.currentTimeMillis() % (MAX_INDEX + 1));
		return new CrazyRandom(state, index);
	}

	/**
	 * Quickly creates a CrazyRandom instance that is not very strong, but this
	 * doesn't require much time or crazy operations.
	 * 
	 * @return An acceptable CrazyRandom instance
	 */
	public static CrazyRandom createWeak() {
		PseudoRandom simple = new PseudoRandom(PseudoRandom.Configuration.LEGACY);
		CrazyRandom crazy = new CrazyRandom(simple.nextBooleans(LENGTH), simple.nextInt(MAX_INDEX));
		crazy.superMix();
		return crazy;
	}

	/**
	 * Attempts to create a strong CrazyRandom instance. This method will search the
	 * file system for files to read and uses the contents of some random files to
	 * seed itself. If the readable files are not good enough, an IOException will
	 * be thrown instead.
	 * 
	 * @return A strong CrazyRandom instance
	 * @throws IOException if there are no readable files or if an IO exception
	 *                     occurs while reading a file that should be readable
	 */
	public static CrazyRandom createStrong() throws IOException {
		int readableFileIndex = 0;
		FileLengthPair[] readableFiles = new FileLengthPair[10000];

		// Let's see what we can get...
		File[] roots = File.listRoots();
		if (roots != null && roots.length > 0) {

			// Nice, we can access root files
			for (File root : roots) {
				readableFileIndex = addFiles(readableFiles, readableFileIndex, root);
			}
		} else {

			// Ok, we will have to do it with local files...
			File current = new File(".");
			try {

				// At least we can get the absolute file and thus 'climb upwards'
				current = current.getAbsoluteFile();
				while (readableFileIndex < readableFiles.length) {
					readableFileIndex = addFiles(readableFiles, readableFileIndex, current);
					current = current.getParentFile();
					if (current == null) {
						break;
					}
				}
			} catch (SecurityException sec) {

				// It looks like we will have to stay local
				readableFileIndex = addFiles(readableFiles, readableFileIndex, current);
			}
		}

		byte[] bytes;

		// By now, the array should either be full or contain all readable files
		if (readableFileIndex == 0) {
			throw new IOException("Not a single readable file was found");
		} else if (readableFileIndex < 10) {

			// Don't be picky, use all files we got!
			int totalLength = 0;
			for (FileLengthPair pair : readableFiles) {
				totalLength += pair.length;
			}

			// Now fill all the bytes into the array
			bytes = new byte[totalLength];
			int byteIndex = 0;
			for (FileLengthPair pair : readableFiles) {
				DataInputStream input = new DataInputStream(Files.newInputStream(pair.file.toPath()));
				input.readFully(bytes, byteIndex, pair.length);
				byteIndex += pair.length;
				input.close();
			}

		} else {

			// Use a simple Random to select which files we will use
			Random selector = new PseudoRandom(PseudoRandom.Configuration.LEGACY);
			
			File[] selected = new File[Math.min(30, readableFileIndex)];
			int totalLength = 0;
			for (int index = 0; index < selected.length; index++) {
				int nextFileIndex = selector.nextInt(readableFileIndex - index);
				FileLengthPair next = readableFiles[nextFileIndex];
				readableFiles[nextFileIndex] = readableFiles[readableFileIndex - 1];
				readableFiles[readableFileIndex - 1] = next;
				selected[index] = next.file;
				totalLength += next.length;
			}
			
			bytes = new byte[totalLength];
		}

		// Create some random number generators

		// addFiles guarantees the minimal file length is MIN_USABLE_FILE_LENGTH
		// which ensure that the file and thus the byte array contains enough bytes to
		// do this:
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		Random initial = new PseudoRandom(buffer.getLong(), buffer.getLong(), buffer.getLong(), buffer.getLong(),
				PseudoRandom.Configuration.LEGACY);

		int randomAmount = 50 + initial.nextInt(32);
		Random[] array = new Random[randomAmount];
		for (int index = 0; index < randomAmount; index++) {
			buffer.position(initial.nextInt(bytes.length - 32));
			array[index] = new PseudoRandom(buffer.getLong(), buffer.getLong(), buffer.getLong(), buffer.getLong(),
					PseudoRandom.Configuration.LEGACY);
		}

		// Use the random number generators
		boolean[] seed = new boolean[LENGTH];
		for (Random current : array) {
			for (int index = 0; index < LENGTH; index++) {
				seed[index] ^= current.next();
			}
		}

		// Let's do something with the remaining bytes...
		int seedIndex = 0;
		for (byte b : bytes) {
			seed[seedIndex] = seed[seedIndex] ^ (b % 2 == 0);
			if (seedIndex >= LENGTH) {
				seedIndex -= LENGTH;
			}
		}

		return new CrazyRandom(seed, initial.nextInt(MAX_INDEX));
	}

	private static class FileLengthPair implements Comparable<FileLengthPair> {

		private final File file;
		private final int length;

		private FileLengthPair(File file, long length) {
			this.file = file;
			this.length = (int) length;
		}

		@Override
		public int compareTo(FileLengthPair other) {
			if (length < other.length) {
				return -1;
			}
			if (length > other.length) {
				return 1;
			}
			return 0;
		}
	}

	private static final int MIN_USABLE_FILE_LENGTH = 32;
	private static final int MAX_USABLE_FILE_LENGTH = 100000;

	private static int addFiles(FileLengthPair[] files, int oldIndex, File current) {
		if (oldIndex >= files.length) {
			return oldIndex;
		}
		if (current.isDirectory()) {

			// This is a directory, so let's try to add all files within it
			try {
				File[] children = current.listFiles();
				if (children != null) {

					int newIndex = oldIndex;

					// Ok, try to add all children
					for (File child : children) {
						newIndex = addFiles(files, newIndex, child);

						// Array is filled, so we are done
						if (newIndex >= files.length) {
							return newIndex;
						}
					}

					return newIndex;
				} else {

					// Ok, this must be some weird directory
					return oldIndex;
				}
			} catch (SecurityException sec) {

				// Alright, let's stay out of this one
				return oldIndex;
			}
		} else if (current.isFile()) {
			try {
				long length = current.length();
				if (current.canRead() && length >= MIN_USABLE_FILE_LENGTH && length <= MAX_USABLE_FILE_LENGTH) {

					// We can read it, so add to the array and increase index
					files[oldIndex] = new FileLengthPair(current, length);
					return oldIndex + 1;
				} else {
					return oldIndex;
				}
			} catch (SecurityException sec) {

				// I guess this means we are not allowed to read it
				return oldIndex;
			}
		} else {

			// This is some special file that is not a normal file or directory
			return oldIndex;
		}
	}

	private static final int LENGTH = 32000;
	private static final int MAX_INDEX = LENGTH - 1025;

	/**
	 * length must be 32000
	 */
	private final boolean[] state;

	private int index;
	private short counter;
	
	/**
	 * Creates a new CrazyRandom instance with the given state/seed and start index. The given state
	 * will be used directly by this instance and thus modifications to that same boolean array could
	 * affect the numbers generated by this CrazyRandom. (Also, this CrazyRandom will modify the state
	 * all the time.)
	 * @param state The state/seed of the random number generator
	 * @param index The start index of the random number generator
	 */
	public CrazyRandom(boolean[] state, int index) {
		if (state.length != LENGTH)
			throw new IllegalArgumentException("Length of state must be " + LENGTH);
		this.state = state;
		this.index = index;
	}
	
	/**
	 * Saves the seed of this CrazyRandom to the given file. Later, a similar CrazyRandom can be loaded
	 * from the seed using CrazyRandom.fromFile. Notice however that this is NOT a pseudo random number
	 * generator, so the loaded instance won't produce the same numbers as this one.
	 * @param file The file to save the current seed in
	 * @throws IOException If an IO exception occurs while trying to save the seed
	 */
	public void saveToFile(File file) throws IOException {
		BitOutput output = new BitOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())));
		output.addBooleans(state);
		output.terminate();
	}

	@Override
	public boolean next() {
		boolean result = state[index];
		counter++;
		if (result)
			counter++;
		if ((counter / 45) * 45 == counter) {
			weakMix();
			clearTrace();
		} else if ((counter / 123) * 123 == counter) {
			mediumMix();
			clearTrace();
		} else if ((counter / 421) * 421 == counter) {
			strongMix();
			clearTrace();
		} else if (counter == 2998 || counter == 2999) {
			superMix();
			clearTrace();
			counter = 0;
		} else {
			index--;
			if (index <= 0)
				index = MAX_INDEX;
		}
		return result;
	}

	protected void clearTrace() {
		PseudoRandom temp = new PseudoRandom(readLong(index), readLong(index + 256), readLong(index + 512),
				readLong(index + 768), Configuration.LEGACY);
		addLong(index, temp.nextLong());
		addLong(index + 256, temp.nextLong());
		addLong(index + 512, temp.nextLong());
		addLong(index + 768, temp.nextLong());
		// System.out.println(index);
		index += temp.nextInt(MAX_INDEX + 1);
		// System.out.println(index);
		if (index > MAX_INDEX)
			index -= MAX_INDEX;
		// System.out.println(index);
		invert(index, 1024);
	}

	protected void superMix() {
		Random first = new PseudoRandom(System.nanoTime(), System.identityHashCode(this), System.currentTimeMillis(),
				System.identityHashCode(System.out), Configuration.LEGACY);
		RandomArray second = RandomArray.createPseudo(Configuration.LEGACY, first.nextBytes(640));
		for (int count = 0; count < 1000; count++)
			addLong(second.nextInt(MAX_INDEX + 1), second.nextLong());
	}

	protected void strongMix() {
		Random first = new PseudoRandom(System.nanoTime(), readLong(index), readLong(index + 256),
				readLong(index + 512), Configuration.LEGACY);
		RandomArray second = RandomArray.createPseudo(Configuration.LEGACY, first.nextBytes(128));
		for (int count = 0; count < 100; count++)
			addLong(second.nextInt(MAX_INDEX + 1), second.nextLong());
	}

	protected void mediumMix() {
		Random random = new PseudoRandom(System.nanoTime(), Configuration.LEGACY);
		for (int count = 0; count < 40; count++)
			writeLong(random.nextInt(MAX_INDEX + 1), random.nextLong());
	}

	protected void weakMix() {
		Random random = new PseudoRandom(System.nanoTime(), Configuration.LEGACY);
		for (int count = 0; count < 10; count++)
			writeLong(random.nextInt(MAX_INDEX + 1), random.nextLong());

		int swapLength = 5000 + random.nextInt(10000);
		int swapIndex = random.nextInt(LENGTH - swapLength);
		for (int index = 0; index < swapLength; index++) {
			state[index + swapIndex] = !state[index + swapIndex];
		}
	}

	protected long readLong(int index) {
		return BitHelper.makeLong(byteFromBinary(state, index), byteFromBinary(state, index + 8),
				byteFromBinary(state, index + 16), byteFromBinary(state, index + 24), byteFromBinary(state, index + 32),
				byteFromBinary(state, index + 40), byteFromBinary(state, index + 48),
				byteFromBinary(state, index + 56));
	}

	protected void addLong(int index, long value) {
		writeLong(index, readLong(index) + value);
	}

	protected void writeLong(int index, long value) {
		BitHelper.byteToBinary(BitHelper.long0(value), state, index);
		BitHelper.byteToBinary(BitHelper.long1(value), state, index + 8);
		BitHelper.byteToBinary(BitHelper.long2(value), state, index + 16);
		BitHelper.byteToBinary(BitHelper.long3(value), state, index + 24);
		BitHelper.byteToBinary(BitHelper.long4(value), state, index + 32);
		BitHelper.byteToBinary(BitHelper.long5(value), state, index + 40);
		BitHelper.byteToBinary(BitHelper.long6(value), state, index + 48);
		BitHelper.byteToBinary(BitHelper.long7(value), state, index + 56);
	}

	protected void invert(int index, int amount) {
		int bound = index + amount;
		for (; index < bound; index++)
			state[index] = !state[index];
	}

	@Override
	public boolean isPseudo() {
		return false;
	}

	@Override
	public CrazyRandom clone() {
		CrazyRandom clone = new CrazyRandom(Arrays.copyOf(state, state.length), index);
		clone.counter = counter;
		return clone;
	}
}