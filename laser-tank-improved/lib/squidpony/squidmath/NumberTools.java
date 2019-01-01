package squidpony.squidmath;

/**
 * Various numeric functions that are important to performance but need
 * alternate implementations on GWT to obtain it. Super-sourced on GWT, but most
 * things here are direct calls to JDK methods when on desktop or Android. Some
 * of this code makes use of "creative" bit manipulation of floats and doubles,
 * which can sometimes allow uncommon input-to-output patterns (as in
 * {@link #bounce(float)}), or even can yield a performance boost (compare
 * {@link #zigzag(float)} to using modulus to accomplish the same results). The
 * bit manipulation has good performance on GWT thanks to JS typed arrays, which
 * are well-supported now across all recent browsers and have fallbacks in GWT
 * in the unlikely event of a browser not supporting them.
 */
public class NumberTools {
    /**
     * Identical to {@link Double#doubleToLongBits(double)} on desktop; optimized on
     * GWT. When compiling to JS via GWT, there is no way to distinguish NaN values
     * with different bits but that are still NaN, so this doesn't try to somehow
     * permit that. Uses JS typed arrays on GWT, which are well-supported now across
     * all recent browsers and have fallbacks in GWT in the unlikely event of a
     * browser not supporting them. JS typed arrays support double, but not long, so
     * this needs to compose a long from two ints, which means the
     * double-to/from-long conversions aren't as fast as float-to/from-int
     * conversions.
     *
     * @param value a {@code double} floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static long doubleToLongBits(final double value) {
	return Double.doubleToLongBits(value);
    }

    /**
     * Identical to {@link Double#doubleToLongBits(double)} on desktop (note, not
     * {@link Double#doubleToRawLongBits(double)}); optimized on GWT. When compiling
     * to JS via GWT, there is no way to distinguish NaN values with different bits
     * but that are still NaN, so this doesn't try to somehow permit that. Uses JS
     * typed arrays on GWT, which are well-supported now across all recent browsers
     * and have fallbacks in GWT in the unlikely event of a browser not supporting
     * them. JS typed arrays support double, but not long, so this needs to compose
     * a long from two ints, which means the double-to/from-long conversions aren't
     * as fast as float-to/from-int conversions.
     *
     * @param value a {@code double} floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static long doubleToRawLongBits(final double value) {
	return Double.doubleToLongBits(value);
    }

    /**
     * Identical to {@link Double#longBitsToDouble(long)} on desktop; optimized on
     * GWT. Uses JS typed arrays on GWT, which are well-supported now across all
     * recent browsers and have fallbacks in GWT in the unlikely event of a browser
     * not supporting them. JS typed arrays support double, but not long, so this
     * needs to compose a long from two ints, which means the double-to/from-long
     * conversions aren't as fast as float-to/from-int conversions.
     *
     * @param bits a long.
     * @return the {@code double} floating-point value with the same bit pattern.
     */
    public static double longBitsToDouble(final long bits) {
	return Double.longBitsToDouble(bits);
    }

    /**
     * Converts {@code value} to a long and gets the lower 32 bits of that long, as
     * an int.
     *
     * @param value a {@code double} precision floating-point number.
     * @return the lower half of the bits that represent the floating-point number,
     *         as an int.
     */
    public static int doubleToLowIntBits(final double value) {
	return (int) (Double.doubleToLongBits(value) & 0xffffffffL);
    }

    /**
     * Converts {@code value} to a long and gets the upper 32 bits of that long, as
     * an int.
     *
     * @param value a {@code double} precision floating-point number.
     * @return the upper half of the bits that represent the floating-point number,
     *         as an int.
     */
    public static int doubleToHighIntBits(final double value) {
	return (int) (Double.doubleToLongBits(value) >>> 32);
    }

    /**
     * Converts {@code value} to a long and gets the XOR of its upper and lower
     * 32-bit sections. Useful for numerical code where a 64-bit double needs to be
     * reduced to a 32-bit value with some hope of keeping different doubles giving
     * different ints.
     *
     * @param value a {@code double} precision floating-point number.
     * @return the XOR of the lower and upper halves of the bits that represent the
     *         floating-point number.
     */
    public static int doubleToMixedIntBits(final double value) {
	final long l = Double.doubleToLongBits(value);
	return (int) l ^ (int) (l >>> 32);
    }

    /**
     * Makes a modified version of value that uses the specified bits (up to 12) for
     * its exponent and sign. Meant for some specific cases, like adjusting the
     * exponent on an unknown double to the 1.0 to 2.0 range (which would pass 0x3ff
     * for exponentBits). If you have a double from 1.0 to 2.0, you can subtract 1.0
     * from it to get the often-desirable 0.0-1.0 range. Other common cases are
     * 0x400, which adjusts to between 2.0 and 4.0 (subtracting 3.0 from this gives
     * the -1.0 to 1.0 range, useful for noise), and 0xBFF, which adjusts to between
     * -2.0 and -1.0. For the last case, you might think that -0x3ff would work, but
     * sadly it doesn't. You can use {@code exponentBits |= 0x800} to set the sign
     * bit to negative, or {@code exponentBits &= 0x7ff} for positive.
     *
     * @param value        a double that will have its sign and exponent set to the
     *                     specified bits
     * @param exponentBits the bits to use for the sign and exponent section of the
     *                     returned modification of value
     * @return the double produced by keeping the significand of value but changing
     *         its exponent and sign as given
     */
    public static double setExponent(final double value, final int exponentBits) {
	return Double.longBitsToDouble(Double.doubleToLongBits(value) & 0xfffffffffffffL | (long) exponentBits << 52);
    }

    /**
     * Gets an 8-bit section of the given double {@code value}, using
     * {@code whichByte} to select whether this should return byte 0 (least
     * significant), 1, 2, and so on up to 7 (most significant).
     *
     * @param value     a float
     * @param whichByte an int that will be used to select the byte to take from
     *                  value (any int is allowed, only the bottom 3 bits are used
     *                  to select)
     * @return the selected byte from the given float
     */
    public static byte getSelectedByte(final double value, final int whichByte) {
	return (byte) (Double.doubleToLongBits(value) >>> ((whichByte & 7) << 3));
    }

    /**
     * Like {@link #getSelectedByte(double, int)}, this sets the byte at a selected
     * position in the int representation of a double, then returns the double
     * produced by the bit change. Uses {@code whichByte} to select whether this
     * should set byte 0 (least significant), 1, 2, and so on up to 7 (most
     * significant). {@code newValue} is a byte.
     *
     * @param value     a double
     * @param whichByte an int that will be used to select the byte to take from
     *                  value (any int is allowed, only the bottom 3 bits are used
     *                  to select)
     * @param newValue  a byte that will be placed into the returned double's bits
     *                  at the selected position
     * @return a double that results from changing the bits at the selected position
     *         to match newValue
     */
    public static double setSelectedByte(final double value, final int whichByte, final byte newValue) {
	return Double.longBitsToDouble(Double.doubleToLongBits(value) & ~(255 << ((whichByte & 7) << 3))
		| (newValue & 255) << ((whichByte & 7) << 3));
    }

    /**
     * Very limited-use; takes any double and produces a double in the -1.0 to 1.0
     * range, with similar inputs producing close to a consistent rate of up and
     * down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent
     * sudden "jumps" in noise value.
     *
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static double bounce(final double value) {
	final long s = Double.doubleToLongBits(value);
	return Double.longBitsToDouble((s ^ -((s & 0x8000000000000L) >> 51)) & 0xfffffffffffffL | 0x4010000000000000L)
		- 5.0;
    }

    /**
     * Very limited-use; takes any double and produces a double in the -1.0 to 1.0
     * range, with similar inputs producing close to a consistent rate of up and
     * down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent
     * sudden "jumps" in noise value.
     *
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static float bounce(final float value) {
	final int s = Float.floatToIntBits(value);
	return Float.intBitsToFloat((s ^ -((s & 0x00400000) >> 22)) & 0x007fffff | 0x40800000) - 5f;
    }

    /**
     * Very limited-use; takes the significand bits of a double, represented as a
     * long of which this uses 52 bits, and produces a double in the -1.0 to 1.0
     * range, with similar inputs producing close to a consistent rate of up and
     * down through the range. This is meant for noise, where it may be useful to
     * limit the amount of change between nearby points' noise values and prevent
     * sudden "jumps" in noise value.
     *
     * @param value any long; only the lower 52 bits will be used
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static double bounce(final long value) {
	return Double.longBitsToDouble(
		(value ^ -((value & 0x8000000000000L) >> 51)) & 0xfffffffffffffL | 0x4010000000000000L) - 5.0;
    }

    /**
     * Very limited-use; takes the significand bits of a double, represented as a
     * pair of ints {@code valueLow} and {@code valueHigh}, using all bits in
     * valueLow and the least-significant 20 bits of valueHigh, and produces a
     * double in the -1.0 to 1.0 range, with similar inputs producing close to a
     * consistent rate of up and down through the range. This is meant for noise,
     * where it may be useful to limit the amount of change between nearby points'
     * noise values and prevent sudden "jumps" in noise value.
     *
     * @param valueLow  any int; all bits will be used as the less-significant bits
     *                  of the significand
     * @param valueHigh any int; only the bottom 20 bits will be used as the
     *                  more-significant bits of the significand
     * @return a double from -1.0 (inclusive) to 1.0 (exclusive)
     */
    public static double bounce(final int valueLow, final int valueHigh) {
	final long s = (long) valueHigh << 32 | valueLow;
	return Double.longBitsToDouble((s ^ -((s & 0x8000000000000L) >> 51)) & 0xfffffffffffffL | 0x4010000000000000L)
		- 5.0;
    }

    /**
     * Limited-use; takes any double and produces a double in the -1.0 to 1.0 range,
     * with similar inputs producing close to a consistent rate of up and down
     * through the range. This is meant for noise, where it may be useful to limit
     * the amount of change between nearby points' noise values and prevent sudden
     * "jumps" in noise value. It is very similar to {@link #bounce(double)}, but
     * unlike bounce() this will maintain a continuous rate regardless of the
     * magnitude of its input. An input of any even number should produce something
     * very close to -1.0, any odd number should produce something very close to
     * 1.0, and any number halfway between two incremental integers (like 8.5 or
     * -10.5) should produce 0.0 or a very small fraction.
     *
     * @param value any double
     * @return a double from -1.0 (inclusive) to 1.0 (inclusive)
     */
    public static double zigzag(final double value) {
	final long s = Double.doubleToLongBits(value + (value < 0f ? -2.0 : 2.0)), m = (s >>> 52 & 0x7FFL) - 0x400,
		sm = s << m;
	return Double.longBitsToDouble((sm ^ -((sm & 0x8000000000000L) >> 51)) & 0xfffffffffffffL | 0x4010000000000000L)
		- 5.0;
    }

    /**
     * Limited-use; takes any float and produces a float in the -1f to 1f range,
     * with similar inputs producing close to a consistent rate of up and down
     * through the range. This is meant for noise, where it may be useful to limit
     * the amount of change between nearby points' noise values and prevent sudden
     * "jumps" in noise value. It is very similar to {@link #bounce(float)}, but
     * unlike bounce() this will maintain a continuous rate regardless of the
     * magnitude of its input. An input of any even number should produce something
     * very close to -1f, any odd number should produce something very close to 1f,
     * and any number halfway between two incremental integers (like 8.5f or -10.5f)
     * should produce 0f or a very small fraction.
     *
     * @param value any float
     * @return a float from -1f (inclusive) to 1f (inclusive)
     */
    public static float zigzag(final float value) {
	final int s = Float.floatToIntBits(value + (value < 0f ? -2f : 2f)), m = (s >>> 23 & 0xFF) - 0x80, sm = s << m;
	return Float.intBitsToFloat((sm ^ -((sm & 0x00400000) >> 22)) & 0x007fffff | 0x40800000) - 5f;
    }

    /**
     * Limited-use; takes any float and produces a float in the -1f to 1f range,
     * with a graph of input to output that looks much like a sine wave, curving to
     * have a flat slope when given an integer input and a steep slope when the
     * input is halfway between two integers, smoothly curving at any points between
     * those extremes. This is meant for noise, where it may be useful to limit the
     * amount of change between nearby points' noise values and prevent both sudden
     * "jumps" in noise value and "cracks" where a line takes a sudden jagged
     * movement at an angle. It is very similar to {@link #bounce(float)} and
     * {@link #zigzag(float)}, but unlike bounce() this will maintain not change its
     * frequency of returning max or min values, regardless of the magnitude of its
     * input, and unlike zigzag() this will smooth its path. An input of any even
     * number should produce something very close to -1f, any odd number should
     * produce something very close to 1f, and any number halfway between two
     * incremental integers (like 8.5f or -10.5f) should produce 0f or a very small
     * fraction.
     *
     * @param value any float
     * @return a float from -1f (inclusive) to 1f (inclusive)
     */
    public static double sway(final double value) {
	final long s = Double.doubleToLongBits(value + (value < 0.0 ? -2.0 : 2.0)), m = (s >>> 52 & 0x7FFL) - 0x400,
		sm = s << m;
	final double a = Double.longBitsToDouble(
		(sm ^ -((sm & 0x8000000000000L) >> 51)) & 0xfffffffffffffL | 0x4000000000000000L) - 2.0;
	return a * a * a * (a * (a * 6.0 - 15.0) + 10.0) * 2.0 - 1.0;
    }

    /**
     * Limited-use; takes any float and produces a float in the -1f to 1f range,
     * with a graph of input to output that looks much like a sine wave, curving to
     * have a flat slope when given an integer input and a steep slope when the
     * input is halfway between two integers, smoothly curving at any points between
     * those extremes. This is meant for noise, where it may be useful to limit the
     * amount of change between nearby points' noise values and prevent both sudden
     * "jumps" in noise value and "cracks" where a line takes a sudden jagged
     * movement at an angle. It is very similar to {@link #bounce(float)} and
     * {@link #zigzag(float)}, but unlike bounce() this will maintain not change its
     * frequency of returning max or min values, regardless of the magnitude of its
     * input, and unlike zigzag() this will smooth its path. An input of any even
     * number should produce something very close to -1f, any odd number should
     * produce something very close to 1f, and any number halfway between two
     * incremental integers (like 8.5f or -10.5f) should produce 0f or a very small
     * fraction.
     *
     * @param value any float
     * @return a float from -1f (inclusive) to 1f (inclusive)
     */
    public static float sway(final float value) {
	final int s = Float.floatToIntBits(value + (value < 0f ? -2f : 2f)), m = (s >>> 23 & 0xFF) - 0x80, sm = s << m;
	final float a = Float.intBitsToFloat((sm ^ -((sm & 0x00400000) >> 22)) & 0x007fffff | 0x40000000) - 2f;
	return a * a * a * (a * (a * 6f - 15f) + 10f) * 2f - 1f;
    }

    /**
     * Identical to {@link Float#floatToIntBits(float)} on desktop; optimized on
     * GWT. Uses JS typed arrays on GWT, which are well-supported now across all
     * recent browsers and have fallbacks in GWT in the unlikely event of a browser
     * not supporting them.
     *
     * @param value a floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static int floatToIntBits(final float value) {
	return Float.floatToIntBits(value);
    }

    /**
     * Identical to {@link Float#floatToIntBits(float)} on desktop (note, not
     * {@link Float#floatToRawIntBits(float)}); optimized on GWT. When compiling to
     * JS via GWT, there is no way to distinguish NaN values with different bits but
     * that are still NaN, so this doesn't try to somehow permit that. Uses JS typed
     * arrays on GWT, which are well-supported now across all recent browsers and
     * have fallbacks in GWT in the unlikely event of a browser not supporting them.
     *
     * @param value a floating-point number.
     * @return the bits that represent the floating-point number.
     */
    public static int floatToRawIntBits(final float value) {
	return Float.floatToIntBits(value);
    }

    /**
     * Identical to {@link Float#intBitsToFloat(int)} on desktop; optimized on GWT.
     * Uses JS typed arrays on GWT, which are well-supported now across all recent
     * browsers and have fallbacks in GWT in the unlikely event of a browser not
     * supporting them.
     *
     * @param bits an integer.
     * @return the {@code float} floating-point value with the same bit pattern.
     */
    public static float intBitsToFloat(final int bits) {
	return Float.intBitsToFloat(bits);
    }

    /**
     * Gets an 8-bit section of the given float {@code value}, using
     * {@code whichByte} to select whether this should return byte 0 (least
     * significant), 1, 2, or 3 (most significant).
     *
     * @param value     a float
     * @param whichByte an int that will be used to select the byte to take from
     *                  value (any int is allowed, only the bottom 2 bits are used
     *                  to select)
     * @return the selected byte from the given float
     */
    public static byte getSelectedByte(final float value, final int whichByte) {
	return (byte) (Float.floatToIntBits(value) >>> ((whichByte & 3) << 3));
    }

    /**
     * Like {@link #getSelectedByte(float, int)}, this sets the byte at a selected
     * position in the int representation of a float, then returns the float
     * produced by the bit change. Uses {@code whichByte} to select whether this
     * should set byte 0 (least significant), 1, 2, or 3 (most significant).
     * {@code newValue} is a byte.
     *
     * @param value     a float
     * @param whichByte an int that will be used to select the byte to take from
     *                  value (any int is allowed, only the bottom 2 bits are used
     *                  to select)
     * @param newValue  a byte that will be placed into the returned float's bits at
     *                  the selected position
     * @return a float that results from changing the bits at the selected position
     *         to match newValue
     */
    public static float setSelectedByte(final float value, final int whichByte, final byte newValue) {
	return Float.intBitsToFloat(Float.floatToIntBits(value) & ~(255 << ((whichByte & 3) << 3))
		| (newValue & 255) << ((whichByte & 3) << 3));
    }

    /**
     * Get a pseudo-random long from this with
     * {@code splitMix64(z += 0x9E3779B97F4A7C15L)}, where z is a long to use as
     * state. 0x9E3779B97F4A7C15L can be changed for any odd long if the same number
     * is used across calls.
     *
     * @param state long, must be changed with each call;
     *              {@code splitMix64(z += 0x9E3779B97F4A7C15L)} is recommended
     * @return a pseudo-random long
     */
    public static long splitMix64(long state) {
	state = (state >>> 30 ^ state) * 0xBF58476D1CE4E5B9L;
	state = (state ^ state >>> 27) * 0x94D049BB133111EBL;
	return state ^ state >>> 31;
    }

    /**
     * Generates a pseudo-random double between 0.0 (inclusive) and 1.0 (exclusive)
     * using the given int seed, passing it once through the (very high-quality and
     * rather fast) {@link LightRNG} algorithm, also called SplitMix64. This
     * produces a pair of random ints, which this produces a double from using the
     * equivalent of {@link #longBitsToDouble(long)} or something functionally
     * equivalent on GWT. <br>
     * Consider calling this with {@code NumberTools.randomDouble(++seed)} for an
     * optimal period of 2 to the 32 when repeatedly called, but
     * {@code NumberTools.randomDouble(seed += ODD_INT)} will also work just fine if
     * ODD_INT is any odd-number integer, positive or negative.
     *
     * @param seed any int to be used as a seed
     * @return a pseudo-random double from 0.0 (inclusive) to 1.0 (exclusive)
     */
    public static double randomDouble(final int seed) {
	long state = seed * 0x9E3779B97F4A7C15L;
	state = (state >>> 30 ^ state) * 0xBF58476D1CE4E5B9L;
	state = (state ^ state >>> 27) * 0x94D049BB133111EBL;
	return Double.longBitsToDouble((state ^ state >>> 31) >>> 12 | 0x3ff0000000000000L) - 1.0;
    }

    /**
     * Generates a pseudo-random float between 0.0f (inclusive) and 1.0f (exclusive)
     * using the given int seed, passing it once through the (very high-quality and
     * rather fast) {@link LightRNG} algorithm, also called SplitMix64. This
     * produces a random int, which this produces a float from using
     * {@link #intBitsToFloat(int)} (long)} or something functionally equivalent on
     * GWT. <br>
     * Consider calling this with {@code NumberTools.randomFloat(++seed)} for an
     * optimal period of 2 to the 32 when repeatedly called, but
     * {@code NumberTools.randomFloat(seed += ODD_INT)} will also work just fine if
     * ODD_INT is any odd-number integer, positive or negative.
     *
     * @param seed any int to be used as a seed
     * @return a pseudo-random float from -1.0f (exclusive) to 1.0f (exclusive)
     */
    public static float randomFloat(final int seed) {
	long state = seed * 0x9E3779B97F4A7C15L;
	state = (state >>> 30 ^ state) * 0xBF58476D1CE4E5B9L;
	state = (state ^ state >>> 27) * 0x94D049BB133111EBL;
	return Float.intBitsToFloat((int) (state >>> 41) | 0x3f800000) - 1f;
    }

    /**
     * Generates a pseudo-random float between -1.0f (exclusive) and 1.0f
     * (exclusive) using the given int seed, passing it once through the (very
     * high-quality and rather fast) {@link LightRNG} algorithm, also called
     * SplitMix64. This produces a random int, which this produces a float from
     * using {@link #intBitsToFloat(int)} (long)} or something functionally
     * equivalent on GWT. The sign bit of the result is determined by data that is
     * not used by the float otherwise, and keeps the results almost linear in
     * distribution between -1.0 and 1.0, exclusive for both (0 shows up twice as
     * often as any single other result, but this shouldn't affect the odds very
     * strongly; it's about a 1 in 8 million chance of exactly 0 occurring vs. a 1
     * in 16 million of any other specific float this can produce). <br>
     * Consider calling this with {@code NumberTools.randomSignedFloat(++seed)} for
     * an optimal period of 2 to the 32 when repeatedly called, but
     * {@code NumberTools.randomSignedFloat(seed += ODD_INT)} will also work just
     * fine if ODD_INT is any odd-number integer, positive or negative.
     *
     * @param seed any int to be used as a seed
     * @return a pseudo-random float from -1.0f (exclusive) to 1.0f (exclusive)
     */
    public static float randomSignedFloat(final int seed) {
	long state = seed * 0x9E3779B97F4A7C15L;
	state = (state >>> 30 ^ state) * 0xBF58476D1CE4E5B9L;
	state = (state ^ state >>> 27) * 0x94D049BB133111EBL;
	return (Float.intBitsToFloat((int) (state >>> 40) | 0x3f800000) - 1f) * (state >> 63 | 1L);
    }

    /**
     * Generates a pseudo-random double between -1.0 (exclusive) and 1.0 (exclusive)
     * with a distribution that has a strong central bias (around 0.0). Uses the
     * given int seed, passing it once through the (very high-quality and rather
     * fast) {@link LightRNG} algorithm, also called SplitMix64. This produces a
     * pair of random ints, which this uses to generate a pair of floats between 0.0
     * (inclusive)and 1.0 (exclusive) using the equivalent of
     * {@link #intBitsToFloat(int)} or something functionally equivalent on GWT,
     * multiplies the floats, and sets the sign pseudo-randomly based on an unused
     * bit from earlier. <br>
     * Consider calling this with {@code NumberTools.randomFloatCurved(++seed)} for
     * an optimal period of 2 to the 32 when repeatedly called, but
     * {@code NumberTools.randomFloatCurved(seed += ODD_INT)} will also work just
     * fine if ODD_INT is any odd-number integer, positive or negative.
     *
     * @param seed any int to be used as a seed
     * @return a pseudo-random double from -1.0 (exclusive) to 1.0 (exclusive),
     *         distributed on a curve centered on 0.0
     */
    public static float randomFloatCurved(final int seed) {
	long state = seed * 0x9E3779B97F4A7C15L;
	state = (state >>> 30 ^ state) * 0xBF58476D1CE4E5B9L;
	state = (state ^ state >>> 27) * 0x94D049BB133111EBL;
	state ^= state >>> 31;
	return (Float.intBitsToFloat((int) (state & 0x7FFFFF) | 0x3f800000) - 1f)
		* (Float.intBitsToFloat((int) (state >>> 40) | 0x3f800000) - 1f) * (state >> 63 | 1L);
    }

    /**
     * Given an int as a seed, this uses its least-significant 23 bits to produce a
     * float between 0f (inclusive) and 1f (exclusive). This does not randomize the
     * seed at all, and the upper 9 bits of the seed are ignored.
     *
     * @param seed an int; only the bottom 23 bits will be used
     * @return a float between 0f (inclusive) and 1f (exclusive)
     */
    public static float formFloat(final int seed) {
	return Float.intBitsToFloat(seed & 0x7FFFFF | 0x3f800000) - 1f;
    }

    /**
     * Given an int as a seed, this uses its least-significant 23 bits to produce a
     * float between -1f (inclusive) and 1f (exclusive). This does not randomize the
     * seed at all, and the upper 9 bits of the seed are ignored.
     *
     * @param seed an int; only the bottom 23 bits will be used
     * @return a float between -1f (inclusive) and 1f (exclusive)
     */
    public static float formSignedFloat(final int seed) {
	return Float.intBitsToFloat(seed & 0x7FFFFF | 0x40000000) - 3f;
    }

    /**
     * A different kind of determine-like method that expects to be given a random
     * long and produces a random float with a curved distribution that centers on 0
     * (where it has a bias) and can (rarely) approach -1f and 1f. The distribution
     * for the values is similar to Irwin-Hall, and is frequently near 0 but not
     * too-rarely near -1f or 1f. It cannot produce values greater than or equal to
     * 1f, or less than -1f, but it can produce -1f.
     *
     * @param start a long, usually random, such as one produced by any
     *              RandomnessSource
     * @return a deterministic float between -1f (inclusive) and 1f (exclusive),
     *         that is very likely to be close to 0f
     */
    public static float formCurvedFloat(final long start) {
	return NumberTools.intBitsToFloat((int) start >>> 9 | 0x3F000000)
		+ NumberTools.intBitsToFloat((int) ~start & 0x007FFFFF | 0x3F000000)
		+ NumberTools.intBitsToFloat((int) (start >>> 41) | 0x3F000000)
		+ NumberTools.intBitsToFloat((int) (~start >>> 32) & 0x007FFFFF | 0x3F000000) - 3f;
    }

    /**
     * A different kind of determine-like method that expects to be given random
     * ints and produces a random float with a curved distribution that centers on 0
     * (where it has a bias) and can (rarely) approach -1f and 1f. The distribution
     * for the values is similar to Irwin-Hall, and is frequently near 0 but not
     * too-rarely near -1f or 1f. It cannot produce values greater than or equal to
     * 1f, or less than -1f, but it can produce -1f.
     *
     * @param start1 an int usually random, such as one produced by any
     *               RandomnessSource
     * @param start2 an int usually random, such as one produced by any
     *               RandomnessSource
     * @return a deterministic float between -1f (inclusive) and 1f (exclusive),
     *         that is very likely to be close to 0f
     */
    public static float formCurvedFloat(final int start1, final int start2) {
	return NumberTools.intBitsToFloat(start1 >>> 9 | 0x3F000000)
		+ NumberTools.intBitsToFloat(~start1 & 0x007FFFFF | 0x3F000000)
		+ NumberTools.intBitsToFloat(start2 >>> 9 | 0x3F000000)
		+ NumberTools.intBitsToFloat(~start2 & 0x007FFFFF | 0x3F000000) - 3f;
    }

    /**
     * A different kind of determine-like method that expects to be given a random
     * int and produces a random float with a curved distribution that centers on 0
     * (where it has a bias) and can (rarely) approach -1f and 1f. The distribution
     * for the values is similar to Irwin-Hall, and is frequently near 0 but not
     * too-rarely near -1f or 1f. It cannot produce values greater than or equal to
     * 1f, or less than -1f, but it can produce -1f.
     *
     * @param start an int, usually random, such as one produced by any
     *              RandomnessSource
     * @return a deterministic float between -1f (inclusive) and 1f (exclusive),
     *         that is very likely to be close to 0f
     */
    public static float formCurvedFloat(final int start) {
	return NumberTools.intBitsToFloat(start >>> 9 | 0x3F000000)
		+ NumberTools.intBitsToFloat(start & 0x007FFFFF | 0x3F000000)
		+ NumberTools.intBitsToFloat(start << 18 & 0x007FFFFF ^ ~start >>> 14 | 0x3F000000)
		+ NumberTools.intBitsToFloat(start << 13 & 0x007FFFFF ^ ~start >>> 19 | 0x3F000000) - 3f;
    }
}
