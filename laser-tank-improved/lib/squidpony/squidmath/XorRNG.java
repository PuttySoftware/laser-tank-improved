/*
Written in 2015 by Sebastiano Vigna (vigna@acm.org)

To the extent possible under law, the author has dedicated all copyright
and related and neighboring rights to this software to the public domain
worldwide. This software is distributed without any warranty.

See <http://creativecommons.org/publicdomain/zero/1.0/>. */
package squidpony.squidmath;

import squidpony.StringKit;

/**
 * A port of Sebastiano Vigna's XorShift 128+ generator. Should be very fast and
 * produce high-quality output. Original version at
 * http://xorshift.di.unimi.it/xorshift128plus.c Written in 2015 by Sebastiano
 * Vigna (vigna@acm.org)
 *
 * @author Sebastiano Vigna
 * @author Tommy Ettinger
 */
public class XorRNG implements RandomnessSource {
    private static final long DOUBLE_MASK = (1L << 53) - 1;
    private static final double NORM_53 = 1. / (1L << 53);
    private static final long FLOAT_MASK = (1L << 24) - 1;
    private static final double NORM_24 = 1. / (1L << 24);
    private static final long serialVersionUID = 1263134736171610359L;
    private long state0, state1;

    /**
     * Creates a new generator seeded using Math.random.
     */
    public XorRNG() {
	this((long) (Math.random() * Long.MAX_VALUE));
    }

    public XorRNG(final long seed) {
	this.setSeed(seed);
    }

    @Override
    public int next(final int bits) {
	return (int) (this.nextLong() & (1L << bits) - 1);
    }

    @Override
    public long nextLong() {
	long s1 = this.state0;
	final long s0 = this.state1;
	this.state0 = s0;
	s1 ^= s1 << 23; // a
	return (this.state1 = s1 ^ s0 ^ s1 >>> 17 ^ s0 >>> 26) + s0; // b, c
    }

    public int nextInt() {
	return (int) this.nextLong();
    }

    public int nextInt(final int n) {
	if (n <= 0) {
	    throw new IllegalArgumentException();
	}
	return (int) ((this.nextLong() >>> 1) % n);
    }

    public long nextLong(final long n) {
	if (n <= 0) {
	    throw new IllegalArgumentException();
	}
	for (;;) {
	    final long bits = this.nextLong() >>> 1;
	    final long value = bits % n;
	    if (bits - value + n - 1 >= 0) {
		return value;
	    }
	}
    }

    public double nextDouble() {
	return (this.nextLong() & XorRNG.DOUBLE_MASK) * XorRNG.NORM_53;
    }

    public float nextFloat() {
	return (float) ((this.nextLong() & XorRNG.FLOAT_MASK) * XorRNG.NORM_24);
    }

    public boolean nextBoolean() {
	return this.nextLong() < 0L;
    }

    public void nextBytes(final byte[] bytes) {
	int i = bytes.length, n = 0;
	while (i != 0) {
	    n = Math.min(i, 8);
	    for (long bits = this.nextLong(); n-- != 0; bits >>>= 8) {
		bytes[--i] = (byte) bits;
	    }
	}
    }

    private long avalanche(long k) {
	k ^= k >>> 33;
	k *= 0xff51afd7ed558ccdL;
	k ^= k >>> 33;
	k *= 0xc4ceb9fe1a85ec53L;
	k ^= k >>> 33;
	return k;
    }

    /**
     * Sets the seed of this generator. Passing this 0 will just set it to -1
     * instead.
     *
     * @param seed the number to use as the seed
     */
    public void setSeed(final long seed) {
	this.state0 = this.avalanche(seed == 0 ? -1 : seed);
	this.state1 = this.avalanche(this.state0);
	this.state0 = this.avalanche(this.state1);
    }

    @Override
    public String toString() {
	return "XorRNG with state hash 0x" + StringKit.hexHash(this.state0, this.state1) + 'L';
    }

    /**
     * Produces a copy of this RandomnessSource that, if next() and/or nextLong()
     * are called on this object and the copy, both will generate the same sequence
     * of random numbers from the point copy() was called. This just needs to copy
     * the state so it isn't shared, usually, and produce a new value with the same
     * exact state.
     *
     * @return a copy of this RandomnessSource
     */
    @Override
    public RandomnessSource copy() {
	final XorRNG next = new XorRNG(this.state0);
	next.state0 = this.state0;
	next.state1 = this.state1;
	return next;
    }
}