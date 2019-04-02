package nl.knokko.util.random;

public abstract class IntBasedRandom extends Random {
	
	private static final int MAX_POWER_VALUE = Integer.MAX_VALUE / 2 + 1;
	
	private int current;
	private int currentPowerValue;

	@Override
	public boolean next() {
		if (currentPowerValue == 0) {
			
			// Set currentPowerValue to 2^30
			currentPowerValue = MAX_POWER_VALUE;
			current = nextInt();
			if (current < 0) {
				current++;
				current += Integer.MAX_VALUE;
				return false;
			} else {
				return true;
			}
		} else {
			boolean result;
			if (current >= currentPowerValue) {
				current -= currentPowerValue;
				result = true;
			} else {
				result = false;
			}
			
			// Observe that 1 / 2 = 0 with integer division
			currentPowerValue /= 2;
			return result;
		}
	}
	
	@Override
	public abstract int nextInt();
}