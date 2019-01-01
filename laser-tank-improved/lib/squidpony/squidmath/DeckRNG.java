package squidpony.squidmath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import squidpony.StringKit;

/**
 * An RNG variant that has 16 possible grades of value it can produce and
 * shuffles them like a deck of cards. It repeats grades of value, but not exact
 * values, every 16 numbers requested from it. Grades go in increments of 0.0625
 * from 0.0 to 0.9375, and are added to a random double less than 0.0625 to get
 * the random number for that grade.
 * <p>
 * You can get values from this generator with: {@link #nextDouble()},
 * {@link #nextInt()}, {@link #nextLong()}, and the bounded variants on each of
 * those.
 *
 * Created by Tommy Ettinger on 5/2/2015.
 */
public class DeckRNG extends StatefulRNG implements Serializable {
    private static final long serialVersionUID = 7828346657944720807L;
    private int step;
    private long lastShuffledState;
    private static final double[] baseDeck = new double[] { 0.0, 0.0625, 0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375,
	    0.5, 0.5625, 0.625, 0.6875, 0.75, 0.8125, 0.875, 0.9375 }, deck = new double[16];

    /**
     * Constructs a DeckRNG with a pseudo-random seed from Math.random().
     */
    public DeckRNG() {
	this((long) (Math.random() * ((1L << 50) - 1)));
    }

    /**
     * Construct a new DeckRNG with the given seed.
     *
     * @param seed used to seed the default RandomnessSource.
     */
    public DeckRNG(final long seed) {
	this.lastShuffledState = seed;
	this.random = new LightRNG(seed);
	this.step = 0;
    }

    /**
     * String-seeded constructor uses the hash of the String as a seed for LightRNG,
     * which is of high quality, but low period (which rarely matters for games),
     * and has good speed and tiny state size.
     *
     * @param seedString a String to use as a seed; will be hashed in a uniform way
     *                   across platforms.
     */
    public DeckRNG(final CharSequence seedString) {
	this(CrossHash.hash64(seedString));
    }

    /**
     * Seeds this DeckRNG using the RandomnessSource it is given. Does not assign
     * the RandomnessSource to any fields that would affect future pseudo-random
     * number generation.
     *
     * @param random will be used to generate a new seed, but will not be assigned
     *               as this object's RandomnessSource
     */
    public DeckRNG(final RandomnessSource random) {
	this(random.nextLong());
    }

    /**
     * Generate a random double, altering the result if recently generated results
     * have been leaning away from this class' fairness value.
     *
     * @return a double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    @Override
    public double nextDouble() {
	if (this.step == 0) {
	    this.shuffleInPlace(DeckRNG.deck);
	}
	final double gen = DeckRNG.deck[this.step++];
	this.step %= 16;
	return gen;
    }

    /**
     * This returns a random double between 0.0 (inclusive) and max (exclusive).
     *
     * @return a value between 0 (inclusive) and max (exclusive)
     */
    @Override
    public double nextDouble(final double max) {
	return this.nextDouble() * max;
    }

    /**
     * Returns a value from a even distribution from min (inclusive) to max
     * (exclusive).
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    @Override
    public double between(final double min, final double max) {
	return min + (max - min) * this.nextDouble();
    }

    /**
     * Returns a value between min (inclusive) and max (exclusive).
     *
     * The inclusive and exclusive behavior is to match the behavior of the similar
     * method that deals with floating point values.
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    @Override
    public int between(final int min, final int max) {
	return this.nextInt(max - min) + min;
    }

    /**
     * Returns the average of a number of randomly selected numbers from the
     * provided range, with min being inclusive and max being exclusive. It will
     * sample the number of times passed in as the third parameter.
     *
     * The inclusive and exclusive behavior is to match the behavior of the similar
     * method that deals with floating point values.
     *
     * This can be used to weight RNG calls to the average between min and max.
     *
     * @param min     the minimum bound on the return value (inclusive)
     * @param max     the maximum bound on the return value (exclusive)
     * @param samples the number of samples to take
     * @return the found value
     */
    @Override
    public int betweenWeighted(final int min, final int max, final int samples) {
	int sum = 0;
	for (int i = 0; i < samples; i++) {
	    sum += this.between(min, max);
	}
	return Math.round((float) sum / samples);
    }

    /**
     * Returns a random element from the provided array and maintains object type.
     *
     * @param       <T> the type of the returned object
     * @param array the array to get an element from
     * @return the randomly selected element
     */
    @Override
    public <T> T getRandomElement(final T[] array) {
	if (array.length < 1) {
	    return null;
	}
	return array[this.nextInt(array.length)];
    }

    /**
     * Returns a random element from the provided list. If the list is empty then
     * null is returned.
     *
     * @param      <T> the type of the returned object
     * @param list the list to get an element from
     * @return the randomly selected element
     */
    @Override
    public <T> T getRandomElement(final List<T> list) {
	if (list.size() <= 0) {
	    return null;
	}
	return list.get(this.nextInt(list.size()));
    }

    /**
     * Returns a random element from the provided ShortSet. If the set is empty then
     * an exception is thrown.
     *
     * <p>
     * Requires iterating through a random amount of the elements in set, so
     * performance depends on the size of set but is likely to be decent. This is
     * mostly meant for internal use, the same as ShortSet.
     * </p>
     *
     * @param set the ShortSet to get an element from
     * @return the randomly selected element
     */
    @Override
    public short getRandomElement(final ShortSet set) {
	if (set.size <= 0) {
	    throw new UnsupportedOperationException("ShortSet cannot be empty when getting a random element");
	}
	int n = this.nextInt(set.size);
	short s = 0;
	final ShortSet.ShortSetIterator ssi = set.iterator();
	while (n-- >= 0 && ssi.hasNext) {
	    s = ssi.next();
	}
	ssi.reset();
	return s;
    }

    /**
     * Returns a random element from the provided Collection, which should have
     * predictable iteration order if you want predictable behavior for identical
     * RNG seeds, though it will get a random element just fine for any Collection
     * (just not predictably in all cases). If you give this a Set, it should be a
     * LinkedHashSet or some form of sorted Set like TreeSet if you want predictable
     * results. Any List or Queue should be fine. Map does not implement Collection,
     * thank you very much Java library designers, so you can't actually pass a Map
     * to this, though you can pass the keys or values. If coll is empty, returns
     * null.
     *
     * <p>
     * Requires iterating through a random amount of coll's elements, so performance
     * depends on the size of coll but is likely to be decent, as long as iteration
     * isn't unusually slow. This replaces {@code getRandomElement(Queue)}, since
     * Queue implements Collection and the older Queue-using implementation was
     * probably less efficient.
     * </p>
     *
     * @param      <T> the type of the returned object
     * @param coll the Collection to get an element from; remember, Map does not
     *             implement Collection
     * @return the randomly selected element
     */
    @Override
    public <T> T getRandomElement(final Collection<T> coll) {
	if (coll.size() <= 0) {
	    return null;
	}
	int n = this.nextInt(coll.size());
	T t = null;
	final Iterator<T> it = coll.iterator();
	while (n-- >= 0 && it.hasNext()) {
	    t = it.next();
	}
	return t;
    }

    /**
     * @return a value from the gaussian distribution
     */
    @Override
    public synchronized double nextGaussian() {
	if (this.haveNextNextGaussian) {
	    this.haveNextNextGaussian = false;
	    return this.nextNextGaussian;
	} else {
	    double v1, v2, s;
	    do {
		v1 = 2 * this.nextDouble() - 1; // between -1 and 1
		v2 = 2 * this.nextDouble() - 1; // between -1 and 1
		s = v1 * v1 + v2 * v2;
	    } while (s >= 1 || s == 0);
	    final double multiplier = Math.sqrt(-2 * Math.log(s) / s);
	    this.nextNextGaussian = v2 * multiplier;
	    this.haveNextNextGaussian = true;
	    return v1 * multiplier;
	}
    }

    /**
     * Returns a random integer below the given bound, or 0 if the bound is 0 or
     * negative. Affects the current fortune.
     *
     * @param bound the upper bound (exclusive)
     * @return the found number
     */
    @Override
    public int nextInt(final int bound) {
	if (bound <= 0) {
	    return 0;
	}
	return (int) (this.nextDouble() * bound);
    }

    /**
     * Shuffle an array using the Fisher-Yates algorithm. Not GWT-compatible; use
     * the overload that takes two arrays. <br>
     * https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
     *
     * @param elements an array of T; will not be modified
     * @return a shuffled copy of elements
     */
    @Override
    public <T> T[] shuffle(final T[] elements) {
	final int n = elements.length;
	final T[] array = Arrays.copyOf(elements, n);
	for (int i = 0; i < n; i++) {
	    final int r = i + this.nextIntHasty(n - i);
	    final T t = array[r];
	    array[r] = array[i];
	    array[i] = t;
	}
	return array;
    }

    /**
     * Generates a random permutation of the range from 0 (inclusive) to length
     * (exclusive). Useful for passing to OrderedMap or OrderedSet's reorder()
     * methods.
     *
     * @param length the size of the ordering to produce
     * @return a random ordering containing all ints from 0 to length (exclusive)
     */
    @Override
    public int[] randomOrdering(final int length) {
	final int[] dest = new int[length];
	for (int i = 0; i < length; i++) {
	    final int r = this.nextIntHasty(i + 1);
	    if (r != i) {
		dest[i] = dest[r];
	    }
	    dest[r] = i;
	}
	return dest;
    }

    /**
     * Returns a random non-negative integer below the given bound, or 0 if the
     * bound is 0. Uses a slightly optimized technique. This method is considered
     * "hasty" since it should be faster than nextInt() doesn't check for
     * "less-valid" bounds values. It also has undefined behavior if bound is
     * negative, though it will probably produce a negative number (just how
     * negative is an open question).
     *
     * @param bound the upper bound (exclusive); behavior is undefined if bound is
     *              negative
     * @return the found number
     */
    @Override
    public int nextIntHasty(final int bound) {
	return (int) (this.nextDouble() * bound);
    }

    /**
     * Returns a random integer, which may be positive or negative.
     *
     * @return A random int
     */
    @Override
    public int nextInt() {
	return (int) ((this.nextDouble() * 2.0 - 1.0) * 0x7FFFFFFF);
    }

    /**
     * Returns a random long, which may be positive or negative.
     *
     * @return A random long
     */
    @Override
    public long nextLong() {
	final double nx = this.nextDouble();
	return (long) ((nx * 2.0 - 1.0) * 0x7FFFFFFFFFFFFFFFL);
    }

    /**
     * Returns a random long below the given bound, or 0 if the bound is 0 or
     * negative.
     *
     * @param bound the upper bound (exclusive)
     * @return the found number
     */
    @Override
    public long nextLong(final long bound) {
	if (bound <= 0) {
	    return 0;
	}
	final double nx = this.nextDouble();
	return (long) (nx * bound);
	// return ((long)(nx * bound)) ^ (long)((nx * 0xFFFFFL) % bound) ^ (long)((nx *
	// 0xFFFFF00000L) % bound);
    }

    /**
     *
     * @param bits the number of bits to be returned
     * @return a random int of the number of bits specified.
     */
    @Override
    public int next(int bits) {
	if (bits <= 0) {
	    return 0;
	}
	if (bits > 32) {
	    bits = 32;
	}
	return (int) (this.nextDouble() * (1L << bits));
    }

    @Override
    public Random asRandom() {
	if (this.ran == null) {
	    this.ran = new CustomRandom(new LightRNG(this.getState()));
	}
	return this.ran;
    }

    /**
     * Returns a value between min (inclusive) and max (exclusive).
     * <p/>
     * The inclusive and exclusive behavior is to match the behavior of the similar
     * method that deals with floating point values.
     *
     * @param min the minimum bound on the return value (inclusive)
     * @param max the maximum bound on the return value (exclusive)
     * @return the found value
     */
    @Override
    public long between(final long min, final long max) {
	return this.nextLong(max - min) + min;
    }

    /**
     * Shuffle an array using the Fisher-Yates algorithm.
     *
     * @param elements an array of T; will not be modified
     * @param dest     Where to put the shuffle. It MUST have the same length as
     *                 {@code elements}
     * @return {@code dest}
     * @throws IllegalStateException If {@code dest.length != elements.length}
     */
    @Override
    public <T> T[] shuffle(final T[] elements, final T[] dest) {
	return super.shuffle(elements, dest);
    }

    @Override
    public <T> ArrayList<T> shuffle(final Collection<T> elements) {
	return super.shuffle(elements);
    }

    @Override
    public float nextFloat() {
	return (float) this.nextDouble();
    }

    @Override
    public boolean nextBoolean() {
	return this.nextDouble() >= 0.5;
    }

    @Override
    public RandomnessSource getRandomness() {
	return this.random;
    }

    /**
     * Reseeds this DeckRNG using the RandomnessSource it is given. Does not assign
     * the RandomnessSource to any fields that would affect future pseudo-random
     * number generation.
     *
     * @param random will be used to generate a new seed, but will not be assigned
     *               as this object's RandomnessSource
     */
    @Override
    public void setRandomness(final RandomnessSource random) {
	this.setState((long) random.next(32) << 32 | random.next(32));
    }

    /**
     * Creates a copy of this DeckRNG; it will generate the same random numbers,
     * given the same calls in order, as this DeckRNG at the point copy() is called.
     * The copy will not share references with this DeckRNG.
     *
     * @return a copy of this DeckRNG
     */
    @Override
    public RNG copy() {
	final DeckRNG next = new DeckRNG(this.lastShuffledState);
	next.random = this.random.copy();
	System.arraycopy(DeckRNG.deck, 0, DeckRNG.deck, 0, DeckRNG.deck.length);
	next.step = this.step;
	return next;
    }

    /**
     * Gets a random portion of data (an array), assigns that portion to output (an
     * array) so that it fills as much as it can, and then returns output. Will only
     * use a given position in the given data at most once; does this by shuffling a
     * copy of data and getting a section of it that matches the length of output.
     *
     * Based on http://stackoverflow.com/a/21460179 , credit to Vincent van der
     * Weele; modifications were made to avoid copying or creating a new generic
     * array (a problem on GWT).
     *
     * @param data   an array of T; will not be modified.
     * @param output an array of T that will be overwritten; should always be
     *               instantiated with the portion length
     * @param        <T> can be any non-primitive type.
     * @return an array of T that has length equal to output's length and may
     *         contain null elements if output is shorter than data
     */
    @Override
    public <T> T[] randomPortion(final T[] data, final T[] output) {
	return super.randomPortion(data, output);
    }

    /**
     * Gets a random portion of a List and returns it as a new List. Will only use a
     * given position in the given List at most once; does this by shuffling a copy
     * of the List and getting a section of it.
     *
     * @param data  a List of T; will not be modified.
     * @param count the non-negative number of elements to randomly take from data
     * @return a List of T that has length equal to the smaller of count or
     *         data.length
     */
    @Override
    public <T> List<T> randomPortion(final List<T> data, final int count) {
	return super.randomPortion(data, count);
    }

    /**
     * Gets a random subrange of the non-negative ints from start (inclusive) to end
     * (exclusive), using count elements. May return an empty array if the
     * parameters are invalid (end is less than/equal to start, or start is
     * negative).
     *
     * @param start the start of the range of numbers to potentially use (inclusive)
     * @param end   the end of the range of numbers to potentially use (exclusive)
     * @param count the total number of elements to use; will be less if the range
     *              is smaller than count
     * @return an int array that contains at most one of each number in the range
     */
    @Override
    public int[] randomRange(final int start, final int end, final int count) {
	return super.randomRange(start, end, count);
    }

    /**
     * Shuffle an array using the Fisher-Yates algorithm.
     *
     * @param array an array of double; WILL be modified
     */
    private void shuffleInPlace(final double[] array) {
	this.lastShuffledState = ((LightRNG) this.random).getState();
	final int n = array.length;
	System.arraycopy(DeckRNG.baseDeck, 0, array, 0, n);
	for (int i = 0; i < n; i++) {
	    final int r = i + ((LightRNG) this.random).nextInt(n - i);
	    final double t = array[r];
	    array[r] = array[i];
	    array[i] = ((LightRNG) this.random).nextDouble(0.0625) + t;
	}
    }

    /**
     * Get a long that can be used to reproduce the sequence of random numbers this
     * object will generate starting now.
     *
     * @return a long that can be used as state.
     */
    @Override
    public long getState() {
	return this.lastShuffledState;
    }

    /**
     * Sets the state of the random number generator to a given long, which will
     * alter future random numbers this produces based on the state. Setting the
     * state always causes the "deck" of random grades to be shuffled.
     *
     * @param state any long (this can tolerate states of 0)
     */
    @Override
    public void setState(final long state) {
	((LightRNG) this.random).setState(state);
	this.shuffleInPlace(DeckRNG.deck);
	this.step = 0;
    }

    @Override
    public String toString() {
	return "DeckRNG{state: 0x" + StringKit.hex(this.lastShuffledState) + "L, step: 0x" + StringKit.hex(this.step)
		+ "}";
    }

    @Override
    public boolean equals(final Object o) {
	if (this == o) {
	    return true;
	}
	if (o == null || this.getClass() != o.getClass()) {
	    return false;
	}
	if (!super.equals(o)) {
	    return false;
	}
	final DeckRNG deckRNG = (DeckRNG) o;
	if (this.step != deckRNG.step) {
	    return false;
	}
	return this.lastShuffledState == deckRNG.lastShuffledState;
    }

    @Override
    public int hashCode() {
	int result = this.random.hashCode();
	result = 31 * result + this.step;
	result = 31 * result + (int) (this.lastShuffledState ^ this.lastShuffledState >>> 32);
	return result;
    }

    public int getStep() {
	return this.step;
    }

    public void setStep(final int step) {
	this.step = step;
    }
}