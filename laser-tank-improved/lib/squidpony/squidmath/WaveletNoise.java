/*
 * Copyright 2014-2017 Steven T Sell (ssell@vertexfragment.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package squidpony.squidmath;

/**
 * Wavelet Noise, a kind of multi-dimensional noise that is technically
 * unrelated to classic Perlin or Simplex Noise, developed by Pixar to solve
 * some difficult issues of 2D textures being displayed in 3D scenes. A good
 * source for the benefits this has is
 * <a href="http://graphics.pixar.com/library/WaveletNoise/paper.pdf">The
 * original Pixar paper</a>. This is in SquidLib to experiment with using it for
 * higher-dimensional noise. It is unusual in that it generates a large number
 * of pseudo-random floats when a WaveletNoise object is constructed, and uses
 * the same block of random numbers (the default is 48 * 48 * 48 numbers in a
 * cube) in different ways as different areas are sampled. It has
 * somewhat-noticeable axis-aligned bias, and isn't as fast as something like
 * SeededNoise or WhirlingNoise. These flaws may be corrected at some point. It
 * currently doesn't implement Noise2D or Noise3D, but it should be able to
 * soon. <br>
 * This code is originally from the Apache-licensed OcularEngine project, fixing
 * small issues (float distribution from the RNG):
 * https://github.com/ssell/OcularEngine/blob/master/OcularCore/src/Math/Noise/WaveletNoise.cpp
 * . <br>
 * Created by Tommy Ettinger on 9/8/2017.
 */
public class WaveletNoise implements Noise.Noise1D, Noise.Noise2D, Noise.Noise3D {
    private static int mod(final int x, final int n) {
	final int m = x % n;
	return m < 0 ? m + n : m;
    }

    private final int m_Dimensions;
    private final int m_NoiseSize;
    private final float[] m_Noise, temp1, temp2, noise;
    private final int[] f = new int[3], c = new int[3], mid = new int[3];
    private final float[][] w = new float[3][3];
    private static final float[] downCoefficients = { 0.000334f, -0.001528f, 0.000410f, 0.003545f, -0.000938f,
	    -0.008233f, 0.002172f, 0.019120f, -0.005040f, -0.044412f, 0.011655f, 0.103311f, -0.025936f, -0.243780f,
	    0.033979f, 0.655340f, 0.655340f, 0.033979f, -0.243780f, -0.025936f, 0.103311f, 0.011655f, -0.044412f,
	    -0.005040f, 0.019120f, 0.002172f, -0.008233f, -0.000938f, 0.003546f, 0.000410f, -0.001528f, 0.000334f },
	    upCoefficients = { 0.25f, 0.75f, 0.75f, 0.25f };
    public float m_Scale;
    public int seed = 0x1337BEEF;
    public static final WaveletNoise instance = new WaveletNoise();

    public WaveletNoise() {
	this(48);
    }

    public WaveletNoise(final int dimensions) // may fix this at 48; it's the length/height/width of all axes
    {
	this.m_Dimensions = dimensions + (dimensions & 1); // make m_Dimensions an even number, increasing if needed
	this.m_NoiseSize = this.m_Dimensions * this.m_Dimensions * this.m_Dimensions;
	this.m_Noise = new float[this.m_NoiseSize];
	this.temp1 = new float[this.m_NoiseSize];
	this.temp2 = new float[this.m_NoiseSize];
	this.noise = new float[this.m_NoiseSize];
	this.m_Scale = 6.0f;
	this.generate();
    }

    @Override
    public double getNoise(final double x) {
	return this.getRawNoise((float) x * this.m_Scale, 0f, 0f, this.seed);
    }

    @Override
    public double getNoiseWithSeed(final double x, final int seed) {
	return this.getRawNoise((float) x * this.m_Scale, 0f, 0f, seed);
    }

    @Override
    public double getNoise(final double x, final double y) {
	return this.getRawNoise((float) x * this.m_Scale, (float) y * this.m_Scale, 0f, this.seed);
    }

    @Override
    public double getNoiseWithSeed(final double x, final double y, final int seed) {
	return this.getRawNoise((float) x * this.m_Scale, (float) y * this.m_Scale, 0f, seed);
    }

    @Override
    public double getNoise(final double x, final double y, final double z) {
	return this.getRawNoise((float) x * this.m_Scale, (float) y * this.m_Scale, (float) z * this.m_Scale,
		this.seed);
    }

    @Override
    public double getNoiseWithSeed(final double x, final double y, final double z, final int seed) {
	return this.getRawNoise((float) x * this.m_Scale, (float) y * this.m_Scale, (float) z * this.m_Scale, seed);
    }

    public float getNoise(final float x) {
	return this.getRawNoise(x * this.m_Scale, 0f, 0f, this.seed);
    }

    public float getNoise(final float x, final float y) {
	return this.getRawNoise(x * this.m_Scale, y * this.m_Scale, 0f, this.seed);
    }

    public float getNoise(final float x, final float y, final float z) {
	return this.getRawNoise(x * this.m_Scale, y * this.m_Scale, z * this.m_Scale, this.seed);
    }

    public void setScale(final float scale) {
	this.m_Scale = scale;
    }

    /**
     * Like {@link Math#floor}, but returns an int. Doesn't consider weird floats
     * like INFINITY and NaN.
     *
     * @param t the float to find the floor for
     * @return the floor of t, as an int
     */
    public static int fastFloor(final float t) {
	return t >= 0 ? (int) t : (int) t - 1;
    }

    /**
     * Like {@link Math#ceil(double)}, but returns an int. Doesn't consider weird
     * floats like INFINITY and NaN.
     *
     * @param t the float to find the ceiling for
     * @return the ceiling of t, as an int
     */
    private static int fastCeil(final float t) {
	return t >= 0 ? -(int) -t + 1 : -(int) -t;
    }

    /**
     * The basis for all getNoise methods in this class; takes x, y, and z
     * coordinates as floats, plus a seed that will alter the noise effectively by
     * just moving the section this samples in an unrelated way to changing x, y,
     * and z normally. Returns a float between -1.0f (inclusive, in theory) and 1.0f
     * exclusive.
     *
     * @param x    x position
     * @param y    y position
     * @param z    z position
     * @param seed seed as an int to modify the noise produced
     * @return a float between -1.0f (inclusive) and 1.0f (exclusive)
     */
    public float getRawNoise(float x, float y, float z, final int seed) {
	final int n = this.m_Dimensions;
	final int[] f = this.f;
	final int[] c = this.c;
	final int[] mid = this.mid;
	final float[][] w = this.w;
	float t;
	float result = 0.0f;
	final float rnd = (ThrustRNG.determine(seed) >> 41) * 0x1p-15f;
	x += rnd;
	y += rnd;
	z += rnd;
	// ---------------------------------------------------
	// Evaluate quadratic B-spline basis functions
	mid[0] = WaveletNoise.fastCeil(x - 0.5f); // Math.round(x);
	t = mid[0] - (x - 0.5f);
	w[0][0] = t * t * 0.5f;
	w[0][2] = (1.0f - t) * (1.0f - t) * 0.5f;
	w[0][1] = 1.0f - w[0][0] - w[0][2];
	mid[1] = WaveletNoise.fastCeil(y - 0.5f); // Math.round(y);
	t = mid[1] - (y - 0.5f);
	w[1][0] = t * t * 0.5f;
	w[1][2] = (1.0f - t) * (1.0f - t) * 0.5f;
	w[1][1] = 1.0f - w[1][0] - w[1][2];
	mid[2] = WaveletNoise.fastCeil(z - 0.5f); // Math.round(z);
	t = mid[2] - (z - 0.5f);
	w[2][0] = t * t * 0.5f;
	w[2][2] = (1.0f - t) * (1.0f - t) * 0.5f;
	w[2][1] = 1.0f - w[2][0] - w[2][2];
	float weight;
	for (f[2] = -1; f[2] <= 1; f[2]++) {
	    for (f[1] = -1; f[1] <= 1; f[1]++) {
		for (f[0] = -1; f[0] <= 1; f[0]++) {
		    weight = 1.0f;
		    for (int i = 0; i < 3; i++) {
			c[i] = WaveletNoise.mod(mid[i] + f[i], n);
			weight *= w[i][f[i] + 1];
		    }
		    result += weight * this.m_Noise[c[2] * n * n + c[1] * n + c[0]];
		}
	    }
	}
//        if(result < -1f || result >= 1f)
//            System.out.println("BAD: result "+ result + ", x " + x + ", y " + y + ", z" + z + ", mid[0] " + mid[0] + ", mid[1] " + mid[1] + ", mid[2] " + mid[2]);
	return result;
    }

    /**
     * Makes a float array that can be passed as bands to
     * {@link #getBandedNoise(float, float, float, int, float[])}. This takes an
     * array or varargs of floats, which are used in order as weights for
     * successively doubled frequencies for the noise. It may be good to experiment
     * with this; {@code 0.5f, 1.1f, 1.9f, 1.2f, 0.6f} will be different from
     * {@code 2f, 0.8f, 0f, 0.4f, 1.3f}. Lengths for the weights array of 10 or more
     * can yield nice results, but take longer to compute. Weights of 0 do not
     * require calculating any noise when they are used.
     *
     * @param weights an array or varargs of float, where each float, in order,
     *                corresponds to a weight for a higher frequency of noise
     * @return a float array that can be passed as bands to
     *         {@link #getBandedNoise(float, float, float, int, float[])}
     */
    public static float[] prepareBands(final float... weights) {
	final int len = weights.length;
	final float[] bands = new float[len + 1];
	float variance = 0f, t;
	for (int i = 0; i < len; i++) {
	    t = bands[i] = Math.max(0f, weights[i]);
	    variance += t * t;
	}
	bands[len] = variance != 0f ? (float) (1.0 / Math.sqrt(variance * len * 0.11)) : 1f;
	return bands;
    }

    /**
     * A 3D noise function that allows different frequencies to be mixed in unusual
     * ways, similarly to octaves in other noise functions but allowing arbitrary
     * weights for frequencies. This takes x, y, and z coordinates as floats, a seed
     * that will alter the noise calls like it does in
     * {@link #getRawNoise(float, float, float, int)}, and a float array of bands.
     * The bands must be readied by {@link #prepareBands(float...)}, which creates a
     * specially formatted float array that can (and probably should) be reused; the
     * items given to prepareBands are each weights for successively doubled
     * frequencies. Returns a float between -1.0f (inclusive, in theory) and 1.0f
     * exclusive.
     *
     * @param x     x position
     * @param y     y position
     * @param z     z position
     * @param seed  seed as an int to modify the noise produced
     * @param bands a float array that was created by
     *              {@link #prepareBands(float...)}; holds frequency band data
     * @return a float between -1.0f (inclusive) and 1.0f (exclusive)
     */
    public float getBandedNoise(final float x, final float y, final float z, final int seed, final float[] bands) {
	float result = 0, ax, ay, az, t;
	final int len = bands.length - 1;
	for (int b = 0; b < len; b++) {
	    if ((t = bands[b]) == 0) {
		continue;
	    }
	    ax = x * (2 << b);
	    ay = y * (2 << b);
	    az = z * (2 << b);
	    result += t * this.getRawNoise(ax, ay, az, seed);
	}
	return result * bands[len];
//        if(result < -1f || result >= 1f)
//            System.out.println("BAD: result "+ result + ", x " + x + ", y " + y + ", z" + z);
//        return result;
    }

    /**
     * Only needs to be called if you change the {@link #seed} field and want the
     * cube of random values re-created.
     */
    public void generate() {
	int x;
	int y;
	int z;
	int i;
	final float[] temp1 = this.temp1;
	final float[] temp2 = this.temp2;
	final float[] noise = this.noise;
	// ---------------------------------------------------
	// Step 1: Fill the tile with random numbers on range [-1.0, 1.0)
	for (i = 0; i < this.m_NoiseSize; i++) {
	    noise[i] = // (ThrustRNG.determine(seed + i * 421L) >> 41) * 0x1p-22f;
		    NumberTools.formCurvedFloat(ThrustRNG.determine(this.seed + i * 181L));
	}
	// ---------------------------------------------------
	// Step 2 & 3: Downsample and then Upsample
	for (y = 0; y < this.m_Dimensions; y++) {
	    for (z = 0; z < this.m_Dimensions; z++) {
		i = y * this.m_Dimensions + z * this.m_Dimensions * this.m_Dimensions;
		this.downsample(noise, temp1, i, this.m_Dimensions, 1);
		this.upsample(temp1, temp2, i, this.m_Dimensions, 1);
	    }
	}
	for (x = 0; x < this.m_Dimensions; x++) {
	    for (z = 0; z < this.m_Dimensions; z++) {
		i = x + z * this.m_Dimensions * this.m_Dimensions;
		this.downsample(temp2, temp1, i, this.m_Dimensions, this.m_Dimensions);
		this.upsample(temp1, temp2, i, this.m_Dimensions, this.m_Dimensions);
	    }
	}
	for (x = 0; x < this.m_Dimensions; x++) {
	    for (y = 0; y < this.m_Dimensions; y++) {
		i = x + y * this.m_Dimensions;
		this.downsample(temp2, temp1, i, this.m_Dimensions, this.m_Dimensions * this.m_Dimensions);
		this.upsample(temp1, temp2, i, this.m_Dimensions, this.m_Dimensions * this.m_Dimensions);
	    }
	}
	// ---------------------------------------------------
	// Step 4: Substract out the coarse-scale constribution (original - (downsample
	// upsample))
	for (i = 0; i < this.m_NoiseSize; i++) {
	    noise[i] -= temp2[i];
	}
	// ---------------------------------------------------
	// Step 5: Avoid even/odd variance difference by adding odd-offset version of
	// noise to itself
	final int offset = this.m_Dimensions >> 1 | 1;
	for (i = 0, x = 0; x < this.m_Dimensions; x++) {
	    for (y = 0; y < this.m_Dimensions; y++) {
		for (z = 0; z < this.m_Dimensions; z++) {
		    temp1[i++] = noise[WaveletNoise.mod(x + offset, this.m_Dimensions)
			    + WaveletNoise.mod(y + offset, this.m_Dimensions) * this.m_Dimensions
			    + WaveletNoise.mod(z + offset, this.m_Dimensions) * this.m_Dimensions * this.m_Dimensions];
		}
	    }
	}
	for (i = 0; i < this.m_NoiseSize; i++) {
	    this.m_Noise[i] = noise[i] + temp1[i];// + 1.3125f);
	}
    }

    protected void downsample(final float[] from, final float[] to, final int idx, final int n, final int stride) {
	int tindex;
	int findex;
	int cindex;
	for (int i = 0; i < n >> 1; i++) {
	    tindex = i * stride + idx;
	    to[tindex] = 0.0f;
	    for (int j = 2 * i - 16; j < 2 * i + 16; j++) {
		cindex = 16 + j - 2 * i;
		findex = WaveletNoise.mod(j, n) * stride + idx;
		to[tindex] += WaveletNoise.downCoefficients[cindex] * from[findex];
	    }
	}
    }

    protected void upsample(final float[] from, final float[] to, final int idx, final int n, final int stride) {
	// int cindex;
	// int tindex;
	// int findex;
	for (int i = 0; i < n; i++) {
	    to[i * stride + idx] = WaveletNoise.upCoefficients[2 + (i & 1)]
		    * from[WaveletNoise.mod(i >> 1, n >> 1) * stride + idx]
		    + WaveletNoise.upCoefficients[2 + i - 2 * ((i >> 1) + 1)]
			    * from[WaveletNoise.mod((i >> 1) + 1, n >> 1) * stride + idx];
	    /*
	     * for (int j = (i >> 1); j <= ((i >> 1) + 1); j++) { cindex = 2 + (i - (2 *
	     * j)); findex = mod(j, (n >> 1)) * stride + idx;
	     *
	     * to[tindex] += coefficients[cindex] * from[findex]; }
	     */
	}
    }
}
