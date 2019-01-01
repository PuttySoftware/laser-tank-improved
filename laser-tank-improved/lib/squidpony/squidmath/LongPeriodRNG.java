package squidpony.squidmath;

import java.io.Serializable;
import java.util.Arrays;

import squidpony.StringKit;

/**
 * An RNG that has a drastically longer period than the other generators in
 * Sarong, other than IsaacRNG, without sacrificing speed or HTML target
 * compatibility. If you don't already know what the period of an RNG is, this
 * probably isn't needed for your purposes, or many purposes in games at all. It
 * is primarily meant for applications that need to generate massive amounts of
 * random numbers, more than pow(2, 64) (18,446,744,073,709,551,616), without
 * repeating the sequence of generated numbers. An RNG's period refers to the
 * number of numbers it generates given a single seed before the sequence
 * repeats from the beginning. The period of this class is pow(2, 1024) minus 1
 * (179,769,313,486,231,590,772,930,519,078,902,473,361,797,697,894,230,657,273,430,081,157,732,675,805,500,963,132,708,
 * 477,322,407,536,021,120,113,879,871,393,357,658,789,768,814,416,622,492,847,430,639,474,124,377,767,893,424,865,485,
 * 276,302,219,601,246,094,119,453,082,952,085,005,768,838,150,682,342,462,881,473,913,110,540,827,237,163,350,510,684,
 * 586,298,239,947,245,938,479,716,304,835,356,329,624,224,137,215). While that
 * number is preposterously large, there's always some application that seems to
 * need more; if you really need more than that, look into CMWC generators,
 * which can have even larger state and also even larger periods. There isn't
 * one of those in Sarong currently, though there is a possibility of one being
 * added in the future. <br>
 * This class may be particularly useful in conjunction with the shuffle method
 * of RNG; the period of an RNG determines how many possible "shuffles", a.k.a
 * orderings or permutations, can be produced over all calls to a permuting
 * method like shuffle. A LightRNG-backed RNG with a period of pow(2, 64) will
 * only be able to produce all possible "shuffles" for lists or arrays of 20
 * items or less. If a LongPeriodRNG is given to the constructor of an RNG and a
 * large enough state has been given to the LongPeriodRNG (the String or long[]
 * constructors can allow this), then lists or arrays of up to 170 elements can
 * have all possible orderings produced by shuffle(), though it will take
 * near-impossibly-many calls. This class has 128 bytes of state plus more in
 * overhead (compare to the 16-byte-with-overhead LightRNG), but due to its
 * massive period and createMany() static method, you can create a large number
 * of subsequences with rather long periods themselves from a single seed. This
 * uses the xorshift-1024* algorithm, and has competitive speed. Created by
 * Tommy Ettinger on 3/21/2016. Ported from CC0-licensed C code by Sebastiano
 * Vigna, at http://xorshift.di.unimi.it/xorshift1024star.c
 *
 * @author Tommy Ettinger
 */
public class LongPeriodRNG implements RandomnessSource, Serializable {
    public final long[] state = new long[16];
    public int choice;
    private static final long serialVersionUID = 163524490381383244L;
    private static final long jumpTable[] = { 0x84242f96eca9c41dL, 0xa3c65b8776f96855L, 0x5b34a39f070b5837L,
	    0x4489affce4f31a1eL, 0x2ffeeb0a48316f40L, 0xdc2d9891fe68c022L, 0x3659132bb12fea70L, 0xaac17d8efa43cab8L,
	    0xc4cb815590989b13L, 0x5ee975283d71c93bL, 0x691548c86c1bd540L, 0x7910c41d10a1e6a5L, 0x0b5fc64563b3e2a8L,
	    0x047f7684e9fc949dL, 0xb99181f2d8f685caL, 0x284600e3f30e38c3L };

    /**
     * Builds a LongPeriodRNG and initializes this class' 1024 bits of state with a
     * random seed passed into SplitMix64, the algorithm also used by LightRNG. A
     * different algorithm is used in non-constructor code to generate random
     * numbers, which is a recommended technique to generate seeds.
     */
    public LongPeriodRNG() {
	this.reseed();
    }

    /**
     * Builds a LongPeriodRNG and initializes this class' 1024 bits of state with
     * many calls to a SplitMix64-based RNG using a variant on seed produced by
     * running it through PCG-Random's output step (PermutedRNG here).
     *
     * @param seed a 64-bit seed; can be any value.
     */
    public LongPeriodRNG(final long seed) {
	this.reseed(seed);
    }

    public void reseed() {
	final LightRNG lr = new LightRNG((long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000L)
		^ (long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000000L));
	long ts = lr.nextLong();
	if (ts == 0) {
	    ts = 1;
	}
	this.choice = (int) (ts & 15);
	this.state[0] = ts;
	for (int i = 1; i < 16; i++) {
	    // Chosen by trial and error to unevenly reseed 4 times, where i is 2, 5, 10, or
	    // 13
	    if ((6 & i * 1281783497376652987L) == 6) {
		lr.state ^= (long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000L)
			^ (long) ((Math.random() * 2.0 - 1.0) * 0x8000000000000000L);
	    }
	    this.state[i - 1] ^= this.state[i] = lr.nextLong();
	    if (this.state[i - 1] == 0) {
		this.state[i - 1] = 1;
	    }
	}
    }

    /**
     * Reinitializes this class' 1024 bits of state with the given seed passed into
     * SplitMix64, the algorithm also used by LightRNG. A different algorithm is
     * used in actual number generating code, which is a recommended technique to
     * generate seeds.
     *
     * @param seed a 64-bit seed; can be any value.
     */
    public void reseed(final long seed) {
	this.init(seed);
	this.choice = (int) (seed & 15);
    }

    /**
     * Builds a LongPeriodRNG and initializes this class' 1024 bits of state with
     * the given seed, using a different strategy depending on the seed. If seed is
     * null, this uses the same state as any other null seed. If seed is a String
     * with length 15 or less, this generates a 64-bit hash of the seed and uses it
     * in the same way the constructor that takes a long creates 1024 bits of state
     * from a 64-bit seed. If seed is a String with length 16 or more, this splits
     * the string up and generates 16 hashes from progressively smaller substrings
     * of seed. The highest quality states will result from passing this a very long
     * String.
     *
     * @param seed a String seed; can be any value, but produces the best results if
     *             it at least 16 characters long
     */
    public LongPeriodRNG(final String seed) {
	this.reseed(seed);
    }

    /**
     * Reinitializes this class' 1024 bits of state with the given seed, using a
     * different strategy depending on the seed. If seed is null, this uses the same
     * state as any other null seed. If seed is a String with length 15 or less,
     * this generates a 64-bit hash of the seed and uses it in the same way the
     * constructor that takes a long creates 1024 bits of state from a 64-bit seed.
     * If seed is a String with length 16 or more, this splits the string up and
     * generates 16 hashes from progressively smaller substrings of seed. The
     * highest quality states will result from passing this a very long String.
     *
     * @param seed a String seed; can be any value, but produces the best results if
     *             it at least 16 characters long
     */
    public void reseed(final String seed) {
	int len;
	if (seed == null || (len = seed.length()) == 0) {
	    this.init(0x632BE59BD9B4E019L);
	    this.choice = 0;
	} else {
	    if (len < 16) {
		final long h = CrossHash.Falcon.hash64(seed);
		this.init(h);
		this.choice = (int) (h & 15);
	    } else {
		final char[] chars = seed.toCharArray();
		this.state[0] = LongPeriodRNG.validate(CrossHash.Falcon.hash64(chars));
		for (int i = 0; i < 16; i++) {
		    this.state[i] = LongPeriodRNG.validate(CrossHash.Falcon.hash64(chars, i * len >> 4, len));
		}
		this.choice = (int) (this.state[0] & 15);
	    }
	}
    }

    /**
     * Builds a LongPeriodRNG and initializes this class' 1024 bits of state with
     * the given seed as a long array, which may or may not have 16 elements (though
     * it is less wasteful to run this with 16 longs since that is exactly 1024
     * bits). If seed is null, this produces the same state as the String
     * constructor does when given a null seed. If seed has fewer than 16 elements,
     * this repeats earlier elements once it runs out of unused longs. If seed has
     * 16 or more elements, this exclusive-ors elements after the sixteenth with
     * longs it has already placed into the state, causing all elements of the seed
     * to have an effect on the state, and making the 16-element case copy all longs
     * exactly.
     *
     * @param seed a long array seed; can have any number of elements, though 16 is
     *             ideal
     */
    public LongPeriodRNG(final long[] seed) {
	this.reseed(seed);
    }

    /**
     * Reinitializes this class' 1024 bits of state with the given seed as a long
     * array, which may or may not have 16 elements (though it is less wasteful to
     * run this with 16 longs since that is exactly 1024 bits). If seed is null,
     * this produces the same state as the String constructor does when given a null
     * seed. If seed has fewer than 16 elements, this repeats earlier elements once
     * it runs out of unused longs. If seed has 16 or more elements, this
     * exclusive-ors elements after the sixteenth with longs it has already placed
     * into the state, causing all elements of the seed to have an effect on the
     * state, and making the 16-element case copy all longs exactly.
     *
     * @param seed a long array seed; can have any number of elements, though 16 is
     *             ideal
     */
    public void reseed(final long[] seed) {
	int len;
	if (seed == null || (len = seed.length) == 0) {
	    this.init(0x632BE59BD9B4E019L);
	    this.choice = 0;
	} else if (len < 16) {
	    for (int i = 0, s = 0; i < 16; i++, s++) {
		if (s == len) {
		    s = 0;
		}
		this.state[i] ^= seed[s];
		if (this.state[i] == 0) {
		    this.state[i] = 1;
		}
	    }
	    this.choice = (int) (this.state[0] & 15);
	} else {
	    for (int i = 0, s = 0; s < len; s++, i = i + 1 & 15) {
		this.state[i] ^= seed[s];
		if (this.state[i] == 0) {
		    this.state[i] = 1;
		}
	    }
	    this.choice = (int) (this.state[0] & 15);
	}
    }

    private static long validate(final long seed) {
	if (seed == 0) {
	    return 1;
	} else {
	    return seed;
	}
    }

    private void init(long seed) {
	long z;
	seed ^= seed >>> 5 + (seed >>> 59);
	seed = (seed *= 0xAEF17502108EF2D9L) >>> 43 ^ seed;
	for (int i = 0; i < 16; i++) {
	    z = seed += 0x9E3779B97F4A7C15L;
	    z = (z ^ z >>> 30) * 0xBF58476D1CE4E5B9L;
	    z = (z ^ z >>> 27) * 0x94D049BB133111EBL;
	    this.state[i] = z ^ z >>> 31;
	    if (this.state[i] == 0) {
		this.state[i] = 1;
	    }
	}
    }

    public LongPeriodRNG(final LongPeriodRNG other) {
	this.choice = other.choice;
	System.arraycopy(other.state, 0, this.state, 0, 16);
    }

    @Override
    public int next(final int bits) {
	return (int) (this.nextLong() & (1L << bits) - 1);
    }

    /**
     * Can return any long, positive or negative, of any size permissible in a
     * 64-bit signed integer. <br>
     * Written by Sebastiano Vigna, from
     * http://xorshift.di.unimi.it/xorshift1024star.c
     *
     * @return any long, all 64 bits are random
     */
    @Override
    public long nextLong() {
	final long s0 = this.state[this.choice];
	long s1 = this.state[this.choice = this.choice + 1 & 15];
	s1 ^= s1 << 31;
	return (this.state[this.choice] = s1 ^ s0 ^ s1 >>> 11 ^ s0 >>> 30) * 1181783497276652981L;
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong()
     * are called on this object and the copy, both will generate the same sequence
     * of random numbers from the point copy() was called. This just need to copy
     * the state so it isn't shared, usually, and produce a new value with the same
     * exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public RandomnessSource copy() {
	final LongPeriodRNG next = new LongPeriodRNG();
	System.arraycopy(this.state, 0, next.state, 0, 16);
	next.choice = this.choice;
	return next;
    }

    /**
     * This is the jump function for the generator. It is equivalent to 2^512 calls
     * to nextLong(); it can be used to generate 2^512 non-overlapping subsequences
     * for parallel computations. Alters the state of this object. <br>
     * Written by Sebastiano Vigna, from
     * http://xorshift.di.unimi.it/xorshift1024star.c , don't ask how it works.
     */
    public void jump() {
	final long[] t = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	for (int i = 0; i < 16; i++) {
	    for (int b = 0; b < 64; b++) {
		if ((LongPeriodRNG.jumpTable[i] & 1L << b) != 0) {
		    for (int j = 0; j < 16; j++) {
			t[j] ^= this.state[j + this.choice & 15];
		    }
		}
		this.nextLong();
	    }
	}
	for (int j = 0; j < 16; j++) {
	    this.state[j + this.choice & 15] = t[j];
	}
    }

    /**
     * Creates many LongPeriodRNG objects in an array, where each will generate a
     * sequence of pow(2, 512) numbers that will not overlap with other sequences in
     * the array. The number of items in the array is specified by count.
     *
     * @param count the number of LongPeriodRNG objects to generate in the array.
     * @return an array of LongPeriodRNG where none of the RNGs will generate
     *         overlapping sequences.
     */
    public static LongPeriodRNG[] createMany(int count) {
	if (count < 1) {
	    count = 1;
	}
	final LongPeriodRNG origin = new LongPeriodRNG();
	final LongPeriodRNG[] values = new LongPeriodRNG[count];
	for (int i = count - 1; i > 0; i--) {
	    values[i] = new LongPeriodRNG(origin);
	    origin.jump();
	}
	values[0] = origin;
	return values;
    }

    /**
     * Creates many LongPeriodRNG objects in an array, where each will generate a
     * sequence of pow(2, 512) numbers that will not overlap with other sequences in
     * the array. The number of items in the array is specified by count. A seed can
     * be given that will affect all items in the array, but with each item using a
     * different section of the massive period this class supports. Essentially,
     * each LongPeriodRNG in the array will generate a different random sequence
     * relative to any other element of the array, but the sequences are
     * reproducible if the same seed is given to this method a different time
     * (useful for testing).
     *
     * @param count the number of LongPeriodRNG objects to generate in the array.
     * @param seed  the RNG seed that will determine the different sequences the
     *              returned LongPeriodRNG objects produce
     * @return an array of LongPeriodRNG where none of the RNGs will generate
     *         overlapping sequences.
     */
    public static LongPeriodRNG[] createMany(int count, final long seed) {
	if (count < 1) {
	    count = 1;
	}
	final LongPeriodRNG origin = new LongPeriodRNG(seed);
	final LongPeriodRNG[] values = new LongPeriodRNG[count];
	for (int i = count - 1; i > 0; i--) {
	    values[i] = new LongPeriodRNG(origin);
	    origin.jump();
	}
	values[0] = origin;
	return values;
    }

    /**
     * Creates many LongPeriodRNG objects in an array, where each will generate a
     * sequence of pow(2, 512) numbers that will not overlap with other sequences in
     * the array. The number of items in the array is specified by count. A seed can
     * be given that will affect all items in the array, but with each item using a
     * different section of the massive period this class supports. Essentially,
     * each LongPeriodRNG in the array will generate a different random sequence
     * relative to any other element of the array, but the sequences are
     * reproducible if the same seed is given to this method a different time
     * (useful for testing).
     *
     * @param count the number of LongPeriodRNG objects to generate in the array.
     * @param seed  the RNG seed that will determine the different sequences the
     *              returned LongPeriodRNG objects produce
     * @return an array of LongPeriodRNG where none of the RNGs will generate
     *         overlapping sequences.
     */
    public static LongPeriodRNG[] createMany(int count, final String seed) {
	if (count < 1) {
	    count = 1;
	}
	final LongPeriodRNG origin = new LongPeriodRNG(seed);
	final LongPeriodRNG[] values = new LongPeriodRNG[count];
	for (int i = count - 1; i > 0; i--) {
	    values[i] = new LongPeriodRNG(origin);
	    origin.jump();
	}
	values[0] = origin;
	return values;
    }

    /**
     * Creates many LongPeriodRNG objects in an array, where each will generate a
     * sequence of pow(2, 512) numbers that will not overlap with other sequences in
     * the array. The number of items in the array is specified by count. A seed can
     * be given that will affect all items in the array, but with each item using a
     * different section of the massive period this class supports. Essentially,
     * each LongPeriodRNG in the array will generate a different random sequence
     * relative to any other element of the array, but the sequences are
     * reproducible if the same seed is given to this method a different time
     * (useful for testing).
     *
     * @param count the number of LongPeriodRNG objects to generate in the array.
     * @param seed  the RNG seed that will determine the different sequences the
     *              returned LongPeriodRNG objects produce
     * @return an array of LongPeriodRNG where none of the RNGs will generate
     *         overlapping sequences.
     */
    public static LongPeriodRNG[] createMany(int count, final long[] seed) {
	if (count < 1) {
	    count = 1;
	}
	final LongPeriodRNG origin = new LongPeriodRNG(seed);
	final LongPeriodRNG[] values = new LongPeriodRNG[count];
	for (int i = count - 1; i > 0; i--) {
	    values[i] = new LongPeriodRNG(origin);
	    origin.jump();
	}
	values[0] = origin;
	return values;
    }

    @Override
    public String toString() {
	return "LongPeriodRNG with state hash 0x" + StringKit.hexHash(this.state) + "L, choice 0x"
		+ StringKit.hex(this.choice);
    }

    @Override
    public boolean equals(final Object o) {
	if (this == o) {
	    return true;
	}
	if (o == null || this.getClass() != o.getClass()) {
	    return false;
	}
	final LongPeriodRNG that = (LongPeriodRNG) o;
	if (this.choice != that.choice) {
	    return false;
	}
	return Arrays.equals(this.state, that.state);
    }

    @Override
    public int hashCode() {
	return CrossHash.Storm.predefined[this.choice].hash(this.state);
    }
}