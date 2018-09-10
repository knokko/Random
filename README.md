# Random
Random library for java

This library can be used to generate random numbers in java. 
It contains an abstract java class with an abstract method that is supposed to generate a random boolean.
All other methods of Random use that method to create other types.
The implementations of Random are PseudoRandom, RandomArray and CrazyRandom.

PseudoRandom is much like java.util.Random, but has a larger seed. Like the name states, it's a pseudo random number generator, which means that creating 2 instances with the same seed will generate the same values.
RandomArray can combine multiply Random's to a single Random.
CrazyRandom has a huge seed and is NOT a pseudo random number generator. It uses System.nanoTime(), System.currentTimeMillis() and System.identityHashCode() to make it's results unpredictable.

I have tested this library a little and everything seems to work.

I don't know whether the random number generators of this library are cryptographically secure or not. All I know is that I can't crack it myself.
I only use this library for protecting a simple game, but a real project should use proper hash methods.
