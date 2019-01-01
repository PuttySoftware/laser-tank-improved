package squidpony.squidmath;

/**
 * A slight variant on RNG that always uses a stateful RandomessSource and so
 * can have its state set or retrieved using setState() or getState(). Created
 * by Tommy Ettinger on 9/15/2015.
 *
 * @author Tommy Ettinger
 */
public class StatefulRNG extends RNG {
    private static final long serialVersionUID = -2456306898212937163L;

    public StatefulRNG() {
	super(new LightRNG());
    }

    public StatefulRNG(final RandomnessSource random) {
	super(random instanceof StatefulRandomness ? random : new LightRNG(random.nextLong()));
    }

    /**
     * Seeded constructor uses LightRNG, which is of high quality, but low period
     * (which rarely matters for games), and has good speed and tiny state size.
     */
    public StatefulRNG(final long seed) {
	this(new LightRNG(seed));
    }

    /**
     * String-seeded constructor uses the hash of the String as a seed for LightRNG,
     * which is of high quality, but low period (which rarely matters for games),
     * and has good speed and tiny state size.
     *
     * Note: This constructor changed behavior on April 22, 2017, when it was
     * noticed that it was not seeding very effectively (only assigning to 32 bits
     * of seed instead of all 64). If you want to keep the older behavior, you can
     * by replacing {@code new StatefulRNG(text)} with
     * {@code new StatefulRNG(CrossHash.hash(text))} . The new technique assigns to
     * all 64 bits and has less correlation between similar inputs causing similar
     * starting states. It's also faster, but that shouldn't matter in a
     * constructor.
     */
    public StatefulRNG(final CharSequence seedString) {
	this(new LightRNG(CrossHash.hash64(seedString)));
    }

    @Override
    public void setRandomness(final RandomnessSource random) {
	super.setRandomness(random instanceof StatefulRandomness ? random : new LightRNG(random.nextLong()));
    }

    /**
     * Creates a copy of this StatefulRNG; it will generate the same random numbers,
     * given the same calls in order, as this StatefulRNG at the point copy() is
     * called. The copy will not share references with this StatefulRNG.
     *
     * @return a copy of this StatefulRNG
     */
    @Override
    public RNG copy() {
	return new StatefulRNG(this.random.copy());
    }

    /**
     * Get a long that can be used to reproduce the sequence of random numbers this
     * object will generate starting now.
     *
     * @return a long that can be used as state.
     */
    public long getState() {
	return ((StatefulRandomness) this.random).getState();
    }

    /**
     * Sets the state of the random number generator to a given long, which will
     * alter future random numbers this produces based on the state.
     *
     * @param state a long, which typically should not be 0 (some implementations
     *              may tolerate a state of 0, however).
     */
    public void setState(final long state) {
	((StatefulRandomness) this.random).setState(state);
    }

    @Override
    public String toString() {
	return "StatefulRNG{" + Long.toHexString(((StatefulRandomness) this.random).getState()) + "}";
    }
}
