package nl.knokko.util.random;

public class FakeRandom extends Random {

	@Override
	public boolean next() {
		return true;
	}

	@Override
	public boolean isPseudo() {
		return true;
	}

	@Override
	public Random clone() {
		return new FakeRandom();
	}
}