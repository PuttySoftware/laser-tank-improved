package squidpony.squidgrid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import squidpony.annotation.GwtIncompatible;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidmath.Coord;
import squidpony.squidmath.CoordPacker;
import squidpony.squidmath.OrderedMap;
import squidpony.squidmath.ShortVLA;

/**
 * A combined FOV calculator, partial LOS calculator, FOV/LOS compressor, and
 * tool to store/query/extract compressed FOV/LOS data. It operates on one level
 * map at a time and stores FOV maps for all cells in a memory-efficient way,
 * though it is likely to take too long to process large maps to be useful on
 * those unless run before the player gets to that map. (Large here means more
 * than 10,000 total cells, or 100 width * 100 height, but this rough upper
 * bound is based on the capability of the machine running the calculations, and
 * should be expected to be much lower on, for instance, older dual-core phones
 * than newer quad-core desktops). There are a few ways to ensure FOVCache is
 * done processing a map by the time a player gets to that map; the recommended
 * approach for games with clearly defined, separate levels is to generate the
 * first level as part of game startup, run cacheAll() immediately afterward
 * (this will start calculation on a second thread), and when the map needs to
 * be displayed (because gameplay has started and any character creation steps
 * are done), to call the caching method's counterpart, awaitCache(), which will
 * cause gameplay to be essentially frozen until the cache completes (you could
 * display a loading message before calling it). The next part is more
 * interesting; you should generate the second level immediately after the
 * awaiting method finishes, before the player has approached the second level,
 * and create another FOVCache object using the second level as its map, then
 * call cacheAll() on the second level's FOVCache. This will calculate the
 * cache, as before, on another thread, and you should call the appropriate
 * awaiting method when the player is entering the second level (descending or
 * ascending a staircase, for example). This time, as long as the player spent
 * more than a few seconds on the first level for most maps, the cache should be
 * pre-calculated and awaiting should take no time. When the game is closed or
 * the player quits, you should call destroy() on this FOVCache to avoid threads
 * possibly lingering after the game should have ended. <br>
 * The FOV calculation this class performs includes a post-processing stage that
 * guarantees symmetry for both LOS and FOV. This works by checking every cell
 * that is within the maximum radius for each non-wall cell, and if any cell A
 * can see cell B, but cell B can not yet see A, then B's cached FOV map will be
 * altered so it can see A. The other post-processing step provides distant
 * lighting; if lights have been passed to the constructor as a Map of Coord
 * keys to Integer values, then the Coords in that Map that are walkable squares
 * will emit light up to a radius equal to their value in that Map. Cells with
 * distant lighting will always be in FOV if they could be seen at up to radius
 * equal to maxLOSRadius, which defaults to 62 and is calculated for every
 * FOVCache as an LOS cache. Calculating distant lighting adds a somewhat
 * substantial amount of time to each caching attempt, estimated at tripling the
 * amount of time used in cases where there are very many lights in a large
 * dungeon (in a 100x100 dungeon, with one light per 5x5 area when the center of
 * that area is walkable, for example), but the lighting is expected to be much
 * less of a performance hindrance on smaller maps (80x40, 60x60, anything
 * smaller, etc.) or when there are simply less lights to process (because
 * distant lighting is meant to go beyond nearby cells, it needs to run through
 * essentially all lights for every cell it processes, and even though adding
 * the lit area to FOV is very efficient and does not require recalculating FOV,
 * having lots of lights means lots of work per cell). <br>
 * This class extends FOV and can be used as a replacement for FOV in some
 * cases. Generally, FOVCache provides methods that allow faster manipulation
 * and checks of certain values (such as a simple case of whether a cell can be
 * seen from another cell at a given FOV radius), but will fall back to
 * Shadowcasting FOV (without using the cache) if any calls to FOV methods are
 * made that have not had their needed information cached. Uncached calls to FOV
 * will not have some of the niceties FOVCache can provide, like distant lights.
 * If different light levels are needed (which Shadowcasting does not provide),
 * you can call Graded variants on the FOV methods, which will fall back to a
 * Ripple FOV instead and will have values between 0.0 and 1.0 instead of only
 * those two extremes. <br>
 * Conservation of memory usage is a primary concern for this class; storing a
 * full 2D array for every cell on a map that is even moderately large uses
 * outrageous amounts of RAM, and attempting that naive approach on a 256x256
 * map would use more than 4 GB of RAM for purely the data from storing bytes or
 * booleans, not including the JVM's overhead of between 12 and 19 bytes for
 * every array. Using smaller maps helps both this class and any other approach
 * (less cells to store FOV for), and at least for FOVCache, using smaller
 * maxRadius values can reduce memory usage as well. For a normal 100x100 map,
 * storing one byte[][] for every cell, and storing a 2D array of those (which
 * has a minimal effect on memory consumption vs. a 1D array in this case), the
 * resulting byte[][][][] will use 112,161,616 bytes of RAM, approximately 110
 * MB; this would still need an additional step of processing when used to limit
 * a requested FOV map stored in it to the appropriate vision range. To
 * contrast, the current version of FOVCache on the same size of map, caching 12
 * separate FOV radii, uses approximately 6.2 MB. Tests run on ten 100x100
 * dungeons ran the gamut between 6,049,760 and 6,404,336 bytes (the layout of
 * the dungeon doesn't affect memory usage of the naive case, but it does have a
 * small effect on the compressed version). To actually use the compressed maps
 * does take an additional processing step, but careful benchmarking indicates
 * running FOV for a roughly 12 radius (Radius.SQUARE kind) area takes twice as
 * long as simply extracting a cached FOV map, and the advantage for the cache
 * is greater for larger FOV radii (but the cache also uses slightly more
 * memory). This compares against the fastest FOV type, Shadowcasting, but to
 * get distance information from an FOV you need to use either the customized
 * FOV algorithm in this class (Slope Shadowcasting), or to use the Ripple FOV
 * type. Ripple does respect translucent objects, which neither shadowcasting
 * nor this class' slope shadowcasting does, but getting 16 FOV levels from the
 * cache for every walkable cell on a 100x100 dungeon map takes approximately 19
 * ms while running Ripple FOV for the same set of cells takes over 700 ms.
 * Benchmarks are conducted using JMH, a tool developed by the OpenJDK team, in
 * a Maven module called squidlib-performance that is not distributed with
 * SquidLib but is available if you download the SquidLib source code.
 *
 * @see squidpony.squidmath.CoordPacker has various utilities for operating on
 *      compressed data of this kind. Created by Tommy Ettinger on 10/7/2015.
 * @author Tommy Ettinger
 */
@GwtIncompatible
public class FOVCache extends FOV {
    protected int maxRadius, maxLOSRadius;
    protected int width;
    protected int height;
    protected int mapLimit;
    protected int limit;
    protected double[][] resMap;
    protected Radius radiusKind;
    protected short[][][] cache;
    protected short[][][] tmpCache;
    protected short[][] losCache;
    protected boolean complete, qualityComplete, refreshComplete;
    protected FOV fov, gradedFOV;
    protected short[][] ALL_WALLS;
    protected short[] wallMap;
    protected double[][] atan2Cache, directionAngles;
    protected short[][] distanceCache;
    protected Coord[][] waves;
    protected final int NUM_THREADS;
    private final ExecutorService executor;
    protected double fovPermissiveness;
    protected OrderedMap<Coord, Integer> lights;
    protected Coord[] lightSources;
    protected int[] lightBrightnesses;
    private double[][] levels;
    protected double decay;
    private Thread performanceThread = null, qualityThread = null;
    private static final double HALF_PI = Math.PI * 0.5, QUARTER_PI = Math.PI * 0.25125, SLIVER_PI = Math.PI * 0.05,
	    PI2 = Math.PI * 2;

    /**
     * Create an FOVCache for a given map (as a char[][]), caching all FOV radii
     * from 0 up to and including maxRadius, using the given Radius enum to
     * determine FOV shape. Upon calling cacheAllPerformance() or cacheAll(),
     * (collectively, the caching methods), the object this creates will run a
     * medium-quality, fairly permissive FOV calculation for every cell on the map
     * using 8 threads, and if cacheAll() was called, will then ensure symmetry (if
     * cell A can see cell B, then it will make cell B able to see cell A even if it
     * couldn't in an earlier step). At the same time as the first caching method
     * call, this will calculate Line of Sight at maxLOSRadius (here it is given a
     * default of 62) for all cells. Walls will always have no cells in their FOV or
     * LOS.
     *
     * @param map        a char[][] as returned by SquidLib's map generators
     * @param maxRadius  the longest radius that will possibly need to be cached for
     *                   FOV; LOS is separate
     * @param radiusKind a Radius enum that determines the shape of each FOV area
     */
    public FOVCache(final char[][] map, final int maxRadius, final Radius radiusKind) {
	if (map == null || map.length == 0) {
	    throw new UnsupportedOperationException("The map used by FOVCache must not be null or empty");
	}
	this.NUM_THREADS = 8;
	this.executor = Executors.newFixedThreadPool(this.NUM_THREADS);
	this.width = map.length;
	this.height = map[0].length;
	if (this.width > 256 || this.height > 256) {
	    throw new UnsupportedOperationException("Map size is too large to efficiently cache, aborting");
	}
	this.mapLimit = this.width * this.height;
	if (maxRadius <= 0 || maxRadius >= 63) {
	    throw new UnsupportedOperationException("FOV radius is incorrect. Must be 0 < maxRadius < 63");
	}
	this.fov = new FOV(FOV.SHADOW);
	this.gradedFOV = new FOV(FOV.RIPPLE);
	this.resMap = DungeonUtility.generateResistances(map);
	this.maxRadius = Math.max(1, maxRadius);
	this.maxLOSRadius = 62;
	this.decay = 1.0 / maxRadius;
	this.radiusKind = radiusKind;
	this.fovPermissiveness = 0.9;
	this.lights = new OrderedMap<>();
	this.cache = new short[this.mapLimit][][];
	this.tmpCache = new short[this.mapLimit][][];
	this.losCache = new short[this.mapLimit][];
	this.ALL_WALLS = new short[maxRadius + 1][];
	for (int i = 0; i < maxRadius + 1; i++) {
	    this.ALL_WALLS[i] = CoordPacker.ALL_WALL;
	}
	this.limit = 0x10000;
	if (this.height <= 128) {
	    this.limit >>= 1;
	    if (this.width <= 128) {
		this.limit >>= 1;
		if (this.width <= 64) {
		    this.limit >>= 1;
		    if (this.height <= 64) {
			this.limit >>= 1;
			if (this.height <= 32) {
			    this.limit >>= 1;
			    if (this.width <= 32) {
				this.limit >>= 1;
			    }
			}
		    }
		}
	    }
	}
	this.preloadMeasurements();
	this.complete = false;
    }

    /**
     * Create an FOVCache for a given map (as a char[][]), caching all FOV radii
     * from 0 up to and including maxRadius, using the given Radius enum to
     * determine FOV shape. Upon calling cacheAllPerformance() or cacheAll(),
     * (collectively, the caching methods), the object this creates will run a
     * medium-quality, fairly permissive FOV calculation for every cell on the map
     * using a number of threads equal to threadCount, and if cacheAll() was called,
     * will then ensure symmetry (if cell A can see cell B, then it will make cell B
     * able to see cell A even if it couldn't in an earlier step). At the same time
     * as the first caching method call, this will calculate Line of Sight at
     * maximum range (given by maxLOSRadius) for all cells. Walls will always have
     * no cells in their FOV or in their LOS.
     *
     * @param map          a char[][] as returned by SquidLib's map generators
     * @param maxRadius    the longest radius that will possibly need to be cached
     *                     for FOV; LOS is separate
     * @param maxLOSRadius the longest radius that will possibly need to be cached
     *                     for LOS, must be less than 63
     * @param radiusKind   a Radius enum that determines the shape of each FOV area
     * @param threadCount  how many threads to use during the full-map calculations
     */
    public FOVCache(final char[][] map, final int maxRadius, final int maxLOSRadius, final Radius radiusKind,
	    final int threadCount) {
	if (map == null || map.length == 0) {
	    throw new UnsupportedOperationException("The map used by FOVCache must not be null or empty");
	}
	this.NUM_THREADS = threadCount;
	this.executor = Executors.newFixedThreadPool(this.NUM_THREADS);
	this.width = map.length;
	this.height = map[0].length;
	if (this.width > 256 || this.height > 256) {
	    throw new UnsupportedOperationException("Map size is too large to efficiently cache, aborting");
	}
	this.mapLimit = this.width * this.height;
	if (maxRadius <= 0 || maxRadius >= 63) {
	    throw new UnsupportedOperationException("FOV radius is incorrect. Must be 0 < maxRadius < 63");
	}
	if (maxLOSRadius <= 0 || maxLOSRadius >= 63) {
	    throw new UnsupportedOperationException("LOS radius is incorrect. Must be 0 < maxLOSRadius < 63");
	}
	this.fov = new FOV(FOV.SHADOW);
	this.gradedFOV = new FOV(FOV.RIPPLE);
	this.resMap = DungeonUtility.generateResistances(map);
	this.maxRadius = Math.max(1, maxRadius);
	this.maxLOSRadius = Math.max(1, maxLOSRadius);
	this.decay = 1.0 / maxRadius;
	this.radiusKind = radiusKind;
	this.fovPermissiveness = 0.9;
	this.lights = new OrderedMap<>();
	this.cache = new short[this.mapLimit][][];
	this.tmpCache = new short[this.mapLimit][][];
	this.losCache = new short[this.mapLimit][];
	this.ALL_WALLS = new short[maxRadius][];
	for (int i = 0; i < maxRadius; i++) {
	    this.ALL_WALLS[i] = CoordPacker.ALL_WALL;
	}
	this.limit = 0x10000;
	if (this.height <= 128) {
	    this.limit >>= 1;
	    if (this.width <= 128) {
		this.limit >>= 1;
		if (this.width <= 64) {
		    this.limit >>= 1;
		    if (this.height <= 64) {
			this.limit >>= 1;
			if (this.height <= 32) {
			    this.limit >>= 1;
			    if (this.width <= 32) {
				this.limit >>= 1;
			    }
			}
		    }
		}
	    }
	}
	this.preloadMeasurements();
	this.complete = false;
    }

    /**
     * Create an FOVCache for a given map (as a char[][]), caching all FOV radii
     * from 0 up to and including maxRadius, using the given Radius enum to
     * determine FOV shape. Upon calling cacheAllPerformance() or cacheAll(),
     * (collectively, the caching methods), the object this creates will run a
     * medium-quality, fairly permissive FOV calculation for every cell on the map
     * using a number of threads equal to threadCount, and if cacheAll() was called,
     * will then ensure symmetry (if cell A can see cell B, then it will make cell B
     * able to see cell A even if it couldn't in an earlier step). At the same time
     * as the first caching method call, this will calculate Line of Sight at
     * maximum range (given by maxLOSRadius) for all cells. Walls will always have
     * no cells in their FOV or in their LOS. This constructor also allows you to
     * initialize light sources in the level using the lights parameter; any Coord
     * keys should correspond to walkable cells (or they will be ignored), and the
     * values will be the range those cells should light up..
     *
     * @param map          a char[][] as returned by SquidLib's map generators
     * @param maxRadius    the longest radius that will possibly need to be cached
     *                     for FOV; LOS is separate
     * @param maxLOSRadius the longest radius that will possibly need to be cached
     *                     for LOS, must be less than 63
     * @param radiusKind   a Radius enum that determines the shape of each FOV area
     * @param threadCount  how many threads to use during the full-map calculations
     * @param lights       a Map of Coords (which should be walkable) to the radii
     *                     they should light (not for moving lights)
     */
    public FOVCache(final char[][] map, final int maxRadius, final int maxLOSRadius, final Radius radiusKind,
	    final int threadCount, final Map<Coord, Integer> lights) {
	if (map == null || map.length == 0) {
	    throw new UnsupportedOperationException("The map used by FOVCache must not be null or empty");
	}
	this.NUM_THREADS = threadCount;
	this.executor = Executors.newFixedThreadPool(this.NUM_THREADS);
	this.width = map.length;
	this.height = map[0].length;
	if (this.width > 256 || this.height > 256) {
	    throw new UnsupportedOperationException("Map size is too large to efficiently cache, aborting");
	}
	this.mapLimit = this.width * this.height;
	if (maxRadius <= 0 || maxRadius >= 63) {
	    throw new UnsupportedOperationException("FOV radius is incorrect. Must be 0 < maxRadius < 63");
	}
	if (maxLOSRadius <= 0 || maxLOSRadius >= 63) {
	    throw new UnsupportedOperationException("LOS radius is incorrect. Must be 0 < maxLOSRadius < 63");
	}
	this.fov = new FOV(FOV.SHADOW);
	this.gradedFOV = new FOV(FOV.RIPPLE);
	this.resMap = DungeonUtility.generateResistances(map);
	this.maxRadius = Math.max(1, maxRadius);
	this.maxLOSRadius = Math.max(1, maxLOSRadius);
	this.decay = 1.0 / maxRadius;
	this.radiusKind = radiusKind;
	this.fovPermissiveness = 0.9;
	this.lights = new OrderedMap<>(lights);
	this.cache = new short[this.mapLimit][][];
	this.tmpCache = new short[this.mapLimit][][];
	this.losCache = new short[this.mapLimit][];
	this.ALL_WALLS = new short[maxRadius][];
	for (int i = 0; i < maxRadius; i++) {
	    this.ALL_WALLS[i] = CoordPacker.ALL_WALL;
	}
	this.limit = 0x10000;
	if (this.height <= 128) {
	    this.limit >>= 1;
	    if (this.width <= 128) {
		this.limit >>= 1;
		if (this.width <= 64) {
		    this.limit >>= 1;
		    if (this.height <= 64) {
			this.limit >>= 1;
			if (this.height <= 32) {
			    this.limit >>= 1;
			    if (this.width <= 32) {
				this.limit >>= 1;
			    }
			}
		    }
		}
	    }
	}
	this.preloadMeasurements();
	this.complete = false;
    }

    private void preloadLights() {
	final Iterator<Coord> it = this.lights.keySet().iterator();
	Coord pos;
	while (it.hasNext()) {
	    pos = it.next();
	    if (this.resMap[pos.x][pos.y] >= 1.0) {
		it.remove();
	    }
	}
	this.lightSources = this.lights.keySet().toArray(new Coord[this.lights.size()]);
	this.lightBrightnesses = new int[this.lights.size()];
	for (int i = 0; i < this.lightSources.length; i++) {
	    this.lightBrightnesses[i] = this.lights.get(this.lightSources[i]);
	}
    }

    private void preloadMeasurements() {
	this.levels = new double[this.maxRadius + 1][this.maxRadius + 1];
	// levels[maxRadius][maxRadius] = 1.0;
	for (int i = 1; i <= this.maxRadius; i++) {
	    System.arraycopy(CoordPacker.generateLightLevels(i), 0, this.levels[i], this.maxRadius - i + 1, i);
	}
	final boolean[][] walls = new boolean[this.width][this.height];
	for (int i = 0; i < this.width; i++) {
	    for (int j = 0; j < this.height; j++) {
		walls[i][j] = this.resMap[i][j] >= 1.0;
	    }
	}
	this.wallMap = CoordPacker.pack(walls);
	this.preloadLights();
	this.atan2Cache = new double[this.maxRadius * 2 + 1][this.maxRadius * 2 + 1];
	this.distanceCache = new short[this.maxRadius * 2 + 1][this.maxRadius * 2 + 1];
	this.waves = new Coord[this.maxRadius + 1][];
	this.waves[0] = new Coord[] { Coord.get(this.maxRadius, this.maxRadius) };
	final ShortVLA[] positionsAtDistance = new ShortVLA[this.maxRadius + 1];
	for (int i = 0; i < this.maxRadius + 1; i++) {
	    positionsAtDistance[i] = new ShortVLA(i * 8 + 1);
	}
	short tmp, inverse_tmp;
	for (int i = 0; i <= this.maxRadius; i++) {
	    for (int j = 0; j <= this.maxRadius; j++) {
		tmp = this.distance(i, j);
		inverse_tmp = (short) (this.maxRadius + 1 - tmp / 2);
		this.atan2Cache[this.maxRadius + i][this.maxRadius + j] = Math.atan2(j, i);
		if (this.atan2Cache[this.maxRadius + i][this.maxRadius + j] < 0) {
		    this.atan2Cache[this.maxRadius + i][this.maxRadius + j] += FOVCache.PI2;
		}
		if (tmp > 0) {
		    this.atan2Cache[this.maxRadius - i][this.maxRadius + j] = Math.atan2(j, -i);
		    if (this.atan2Cache[this.maxRadius - i][this.maxRadius + j] < 0) {
			this.atan2Cache[this.maxRadius - i][this.maxRadius + j] += FOVCache.PI2;
		    }
		    this.atan2Cache[this.maxRadius + i][this.maxRadius - j] = Math.atan2(-j, i);
		    if (this.atan2Cache[this.maxRadius + i][this.maxRadius - j] < 0) {
			this.atan2Cache[this.maxRadius + i][this.maxRadius - j] += FOVCache.PI2;
		    }
		    this.atan2Cache[this.maxRadius - i][this.maxRadius - j] = Math.atan2(-j, -i);
		    if (this.atan2Cache[this.maxRadius - i][this.maxRadius - j] < 0) {
			this.atan2Cache[this.maxRadius - i][this.maxRadius - j] += FOVCache.PI2;
		    }
		}
		if (tmp / 2 <= this.maxRadius && inverse_tmp > 0) {
		    this.distanceCache[this.maxRadius + i][this.maxRadius + j] = inverse_tmp;
		    if (tmp > 0) {
			this.distanceCache[this.maxRadius - i][this.maxRadius + j] = inverse_tmp;
			this.distanceCache[this.maxRadius + i][this.maxRadius - j] = inverse_tmp;
			this.distanceCache[this.maxRadius - i][this.maxRadius - j] = inverse_tmp;
			positionsAtDistance[tmp / 2]
				.add(CoordPacker.zEncode((short) (this.maxRadius + i), (short) (this.maxRadius + j)));
			if (i > 0) {
			    positionsAtDistance[tmp / 2].add(
				    CoordPacker.zEncode((short) (this.maxRadius - i), (short) (this.maxRadius + j)));
			}
			if (j > 0) {
			    positionsAtDistance[tmp / 2].add(
				    CoordPacker.zEncode((short) (this.maxRadius + i), (short) (this.maxRadius - j)));
			}
			if (i > 0 && j > 0) {
			    positionsAtDistance[tmp / 2].add(
				    CoordPacker.zEncode((short) (this.maxRadius - i), (short) (this.maxRadius - j)));
			}
		    } else {
			positionsAtDistance[0].add(CoordPacker.zEncode((short) this.maxRadius, (short) this.maxRadius));
		    }
		}
	    }
	}
	final short[][] positionsZ = new short[this.maxRadius + 1][];
	for (int i = 0; i <= this.maxRadius; i++) {
	    positionsZ[i] = positionsAtDistance[i].toArray();
	    this.waves[i] = new Coord[positionsZ[i].length];
	    for (int j = 0; j < this.waves[i].length; j++) {
		this.waves[i][j] = CoordPacker.zDecode(positionsZ[i][j]);
	    }
	}
	this.directionAngles = new double[3][3];
	this.directionAngles[0][0] = Math.atan2(1, 1);
	this.directionAngles[0][1] = Math.atan2(0, 1);
	this.directionAngles[0][2] = Math.atan2(-1, 1) + FOVCache.PI2;
	this.directionAngles[1][0] = Math.atan2(1, 0);
	this.directionAngles[1][1] = 0;
	this.directionAngles[1][2] = Math.atan2(-1, 0) + FOVCache.PI2;
	this.directionAngles[2][0] = Math.atan2(1, -1);
	this.directionAngles[2][1] = Math.atan2(0, -1);
	this.directionAngles[2][2] = Math.atan2(-1, -1) + FOVCache.PI2;
    }

    /**
     * Packs FOV for a point as a center, and returns it to be stored.
     *
     * @param index an int that stores the x,y center of FOV as calculated by: x + y
     *              * width
     * @return a multi-packed series of progressively wider FOV radii
     */
    protected long storeCellFOV(final int index) {
	final long startTime = System.currentTimeMillis();
	this.cache[index] = this.calculatePackedSlopeShadowFOV(index % this.width, index / this.width);
	// cache[index] = calculatePackedExternalFOV(index % width, index / width);
	return System.currentTimeMillis() - startTime;
    }

    /**
     * Packs FOV for a point as a center, and returns it to be stored.
     *
     * @param index an int that stores the x,y center of FOV as calculated by: x + y
     *              * width
     * @return a multi-packed series of progressively wider FOV radii
     */
    protected long storeCellLOS(final int index) {
	final long startTime = System.currentTimeMillis();
	this.losCache[index] = this.calculatePackedLOS(index % this.width, index / this.width);
	return System.currentTimeMillis() - startTime;
    }

    /**
     * Uses previously cached FOV and makes it symmetrical. Also handles distant
     * lights
     *
     * @param index an int that stores the x,y center of FOV as calculated by: x + y
     *              * width
     * @return a multi-packed series of progressively wider FOV radii
     */
    protected long storeCellSymmetry(final int index) {
	final long startTime = System.currentTimeMillis();
	this.tmpCache[index] = this.improveQuality(index % this.width, index / this.width);
	return System.currentTimeMillis() - startTime;
    }

    /**
     * Packs FOV for the given viewer's X and Y as a center, and returns the packed
     * data to be stored.
     *
     * @param viewerX an int less than 256 and less than width
     * @param viewerY an int less than 256 and less than height
     * @return a packed FOV map for radius equal to maxLOSRadius
     */
    public short[] calculatePackedLOS(final int viewerX, final int viewerY) {
	if (viewerX < 0 || viewerY < 0 || viewerX >= this.width || viewerY >= this.height) {
	    return CoordPacker.ALL_WALL;
	}
	if (this.resMap[viewerX][viewerY] >= 1.0) {
	    return CoordPacker.ALL_WALL;
	}
	return CoordPacker
		.pack(this.fov.calculateFOV(this.resMap, viewerX, viewerY, this.maxLOSRadius, this.radiusKind));
    }

    /**
     * Packs FOV for the given viewer's X and Y as a center, and returns the packed
     * data to be stored.
     *
     * @param viewerX an int less than 256 and less than width
     * @param viewerY an int less than 256 and less than height
     * @return a multi-packed series of progressively wider FOV radii
     */
    public short[][] calculatePackedSlopeShadowFOV(final int viewerX, final int viewerY) {
	if (viewerX < 0 || viewerY < 0 || viewerX >= this.width || viewerY >= this.height) {
	    return this.ALL_WALLS;
	}
	if (this.resMap[viewerX][viewerY] >= 1.0) {
	    return this.ALL_WALLS;
	}
	return CoordPacker.packMulti(this.slopeShadowFOV(viewerX, viewerY), this.maxRadius + 1);
    }

    /**
     * Packs FOV for the given viewer's X and Y as a center, and returns the packed
     * data to be stored.
     *
     * @param viewerX an int less than 256 and less than width
     * @param viewerY an int less than 256 and less than height
     * @return a multi-packed series of progressively wider FOV radii
     */
    public short[][] calculatePackedWaveFOV(final int viewerX, final int viewerY) {
	if (viewerX < 0 || viewerY < 0 || viewerX >= this.width || viewerY >= this.height) {
	    return this.ALL_WALLS;
	}
	if (this.resMap[viewerX][viewerY] >= 1.0) {
	    return this.ALL_WALLS;
	}
	return CoordPacker.packMulti(this.waveFOV(viewerX, viewerY), this.maxRadius + 1);
    }

    public short[][] getCacheEntry(final int x, final int y) {
	return this.cache[x + y * this.width];
    }

    public short[] getCacheEntry(final int x, final int y, final int radius) {
	return this.cache[x + y * this.width][this.maxRadius - radius];
    }

    public short[] getLOSEntry(final int x, final int y) {
	return this.losCache[x + y * this.width];
    }

    public boolean queryCache(final int visionRange, final int viewerX, final int viewerY, final int targetX,
	    final int targetY) {
	return CoordPacker.queryPacked(this.cache[viewerX + viewerY * this.width][this.maxRadius - visionRange],
		targetX, targetY);
    }

    public boolean isCellVisible(final int visionRange, final int viewerX, final int viewerY, final int targetX,
	    final int targetY) {
	return CoordPacker.queryPacked(this.cache[viewerX + viewerY * this.width][this.maxRadius - visionRange],
		targetX, targetY)
		|| CoordPacker.queryPacked(this.cache[targetX + targetY * this.width][this.maxRadius - visionRange],
			viewerX, viewerY);
    }

    public boolean queryLOS(final int viewerX, final int viewerY, final int targetX, final int targetY) {
	return CoordPacker.queryPacked(this.losCache[viewerX + viewerY * this.width], targetX, targetY);
    }

    private long arrayMemoryUsage(final int length, final long bytesPerItem) {
	return ((bytesPerItem * length + 12 - 1) / 8 + 1) * 8L;
    }

    private int arrayMemoryUsageJagged(final short[][] arr) {
	int ctr = 0;
	for (final short[] element : arr) {
	    ctr += this.arrayMemoryUsage(element.length, 2);
	}
	return ((ctr + 12 - 1) / 8 + 1) * 8;
    }

    public long approximateMemoryUsage() {
	long ctr = 0;
	for (final short[][] element : this.cache) {
	    ctr += this.arrayMemoryUsageJagged(element);
	}
	ctr = ((ctr + 12L - 1L) / 8L + 1L) * 8L;
	ctr += ((this.arrayMemoryUsageJagged(this.losCache) + 12L - 1L) / 8L + 1L) * 8L;
	return ctr;
    }

    /*
     * //needs rewrite, must store the angle a ray traveled at to get around an
     * obstacle, and propagate it to the end of //the ray. It should check if the
     * angle theta for a given point is too different from the angle in angleMap.
     * private byte[][] waveFOVWIP(int viewerX, int viewerY) { byte[][] gradientMap
     * = new byte[width][height]; double[][] angleMap = new double[2 * maxRadius +
     * 1][2 * maxRadius + 1]; for (int i = 0; i < angleMap.length; i++) {
     * Arrays.fill(angleMap[i], -20); } gradientMap[viewerX][viewerY] = (byte)(2 *
     * maxRadius); Direction[] dirs = (radiusKind == Radius.DIAMOND || radiusKind ==
     * Radius.OCTAHEDRON) ? Direction.CARDINALS : Direction.OUTWARDS; int cx, cy,
     * ccwAdjX, ccwAdjY, cwAdjX, cwAdjY, ccwGridX, ccwGridY, cwGridX, cwGridY; Coord
     * pt; double theta, angleCW, angleCCW; byte dist; boolean blockedCCW,
     * blockedCW, isStraightCCW; for(int w = 0; w < waves.length; w++) { for(int c =
     * 0; c < waves[w].length; c++) { pt = waves[w][c]; cx = viewerX - maxRadius +
     * pt.x; cy = viewerY - maxRadius + pt.y; if(cx < width && cx >= 0 && cy <
     * height && cy >= 0) { theta = atan2Cache[pt.x][pt.y]; dist =
     * (byte)(distanceCache[pt.x][pt.y ]);
     *
     * if(w <= 0) { gradientMap[cx][cy] = dist; } else { switch ((int)
     * Math.floor(theta / QUARTER_PI)) {
     *
     * //positive x, postive y (lower on screen), closer to x-axis case 0: cwAdjX =
     * pt.x - 1; cwAdjY = pt.y; angleCW = directionAngles[0][1]; isStraightCCW =
     * false; ccwAdjX = pt.x - 1; ccwAdjY = pt.y - 1; angleCCW =
     * directionAngles[0][0]; break; //positive x, postive y (lower on screen),
     * closer to y-axis case 1: cwAdjX = pt.x - 1; cwAdjY = pt.y - 1; angleCW =
     * directionAngles[0][0]; ccwAdjX = pt.x; ccwAdjY = pt.y - 1; angleCCW =
     * directionAngles[1][0]; isStraightCCW = true; break; //negative x, postive y
     * (lower on screen), closer to y-axis case 2: cwAdjX = pt.x; cwAdjY = pt.y - 1;
     * angleCW = directionAngles[1][0]; isStraightCCW = false; ccwAdjX = pt.x + 1;
     * ccwAdjY = pt.y - 1; angleCCW = directionAngles[2][0]; break; //negative x,
     * postive y (lower on screen), closer to x-axis case 3: cwAdjX = pt.x + 1;
     * cwAdjY = pt.y - 1; angleCW = directionAngles[2][0]; ccwAdjX = pt.x + 1;
     * ccwAdjY = pt.y; angleCCW = directionAngles[2][1]; isStraightCCW = true;
     * break;
     *
     * //negative x, negative y (higher on screen), closer to x-axis case 4: cwAdjX
     * = pt.x + 1; cwAdjY = pt.y + 1; angleCW = directionAngles[2][2]; ccwAdjX =
     * pt.x + 1; ccwAdjY = pt.y; angleCCW = directionAngles[2][1]; isStraightCCW =
     * false; break; //negative x, negative y (higher on screen), closer to y-axis
     * case 5: cwAdjX = pt.x + 1; cwAdjY = pt.y + 1; angleCW =
     * directionAngles[2][2]; ccwAdjX = pt.x; ccwAdjY = pt.y + 1; angleCCW =
     * directionAngles[1][2]; isStraightCCW = true; break; //positive x, negative y
     * (higher on screen), closer to y-axis case 6: cwAdjX = pt.x; cwAdjY = pt.y +
     * 1; angleCW = directionAngles[1][2]; isStraightCCW = false; ccwAdjX = pt.x -
     * 1; ccwAdjY = pt.y + 1; angleCCW = directionAngles[0][2]; break; //positive x,
     * negative y (higher on screen), closer to x-axis default: cwAdjX = pt.x - 1;
     * cwAdjY = pt.y + 1; angleCW = directionAngles[0][2]; ccwAdjX = pt.x - 1;
     * ccwAdjY = pt.y; angleCCW = directionAngles[0][1]; isStraightCCW = true;
     * break; } /* angleCCW = (((Math.abs(atan2Cache[ccwAdjX][ccwAdjY] - angleCCW) >
     * Math.PI) ? atan2Cache[ccwAdjX][ccwAdjY] + angleCCW + PI2 :
     * atan2Cache[ccwAdjX][ccwAdjY] + angleCCW) 0.5) % PI2; //(angleCCW +
     * atan2Cache[ccwAdjX][ccwAdjY]) * 0.5; angleCW =
     * (((Math.abs(atan2Cache[cwAdjX][cwAdjY] - angleCW) > Math.PI) ?
     * atan2Cache[cwAdjX][cwAdjY] + angleCW + PI2 : atan2Cache[cwAdjX][cwAdjY] +
     * angleCW) 0.5) % PI2; //(angleCW + atan2Cache[cwAdjX][cwAdjY]) * 0.5;
     *
     * / angleCCW = atan2Cache[ccwAdjX][ccwAdjY]; //(angleCCW +
     * atan2Cache[ccwAdjX][ccwAdjY]) * 0.5; angleCW = atan2Cache[cwAdjX][cwAdjY];
     * //(angleCW + atan2Cache[cwAdjX][cwAdjY]) * 0.5;
     *
     *
     * cwGridX = cwAdjX + viewerX - maxRadius; ccwGridX = ccwAdjX + viewerX -
     * maxRadius; cwGridY = cwAdjY + viewerY - maxRadius; ccwGridY = ccwAdjY +
     * viewerY - maxRadius;
     *
     * blockedCW = cwGridX >= width || cwGridY >= height || cwGridX < 0 || cwGridY <
     * 0 || resMap[cwGridX][cwGridY] > 0.5 || angleMap[cwAdjX][cwAdjY] >= PI2;
     * blockedCCW = ccwGridX >= width || ccwGridY >= height || ccwGridX < 0 ||
     * ccwGridY < 0 || resMap[ccwGridX][ccwGridY] > 0.5 ||
     * angleMap[ccwAdjX][ccwAdjY] >= PI2;
     *
     * if (blockedCW && blockedCCW) { angleMap[pt.x][pt.y] = PI2; continue; } if
     * (theta % (HALF_PI - 0.00125) < 0.005) if (isStraightCCW) { if (blockedCCW) {
     * angleMap[pt.x][pt.y] = PI2; gradientMap[cx][cy] = dist; continue; } else
     * angleMap[pt.x][pt.y] = theta; } else { if (blockedCW) { angleMap[pt.x][pt.y]
     * = PI2; gradientMap[cx][cy] = dist; continue; } else angleMap[pt.x][pt.y] =
     * theta; } else if(theta % (QUARTER_PI - 0.0025) < 0.005) if (isStraightCCW) {
     * if (blockedCW) { angleMap[pt.x][pt.y] = PI2; gradientMap[cx][cy] = dist;
     * continue; } else angleMap[pt.x][pt.y] = theta; } else { if (blockedCCW) {
     * angleMap[pt.x][pt.y] = PI2; gradientMap[cx][cy] = dist; continue; } else
     * angleMap[pt.x][pt.y] = theta; } else { if (blockedCW) { angleMap[pt.x][pt.y]
     * = angleMap[ccwAdjX][ccwAdjY]; // angleMap[pt.x][pt.y] =
     * Math.max(angleMap[ccwAdjX][ccwAdjY], // (theta - (angleCCW - theta + PI2) %
     * PI2 * 0.5 + PI2) % PI2); // (theta - angleCCW > Math.PI) // ? (theta -
     * (angleCCW - theta + PI2) * 0.5) % PI2 // : theta - (angleCCW - theta + PI2) %
     * PI2 * 0.5; //angleMap[pt.x][pt.y] = angleCCW;
     *
     * // (((Math.abs(theta - angleCCW) > Math.PI) // ? theta + angleCCW + PI2 // :
     * theta + angleCCW) // * 0.5) % PI2; //angleMap[cwAdjX - viewerX +
     * maxRadius][cwAdjY - viewerY + maxRadius]; //Math.abs(angleMap[cwAdjX -
     * viewerX + maxRadius][cwAdjY - viewerY + maxRadius] -
     *
     * //angleMap[pt.x][pt.y] = Math.abs(theta - angleCCW) + // angleMap[cwAdjX -
     * viewerX + maxRadius][cwAdjY - viewerY + maxRadius];
     *
     * } else if (blockedCCW) { angleMap[pt.x][pt.y] = angleMap[cwAdjX][cwAdjY]; //
     * angleMap[pt.x][pt.y] = Math.max(angleMap[cwAdjX][cwAdjY], // (theta + (theta
     * - angleCW + PI2) % PI2 * 0.5 + PI2) % PI2); // (angleCW - theta > Math.PI) //
     * ? theta + (theta - angleCW + PI2) % PI2 * 0.5 // : (theta + (theta - angleCW
     * + PI2) * 0.5) % PI2; //angleCW;
     *
     * //angleMap[ccwAdjX - viewerX + maxRadius][ccwAdjY - viewerY + maxRadius];
     * //angleMap[ccwAdjX - viewerX + maxRadius][ccwAdjY - viewerY + maxRadius] }
     * else { double cwTemp = angleMap[cwAdjX][cwAdjY], ccwTemp =
     * angleMap[ccwAdjX][ccwAdjY]; if(cwTemp < 0) cwTemp =
     * (atan2Cache[cwAdjX][cwAdjY]); if(ccwTemp < 0) ccwTemp =
     * (atan2Cache[ccwAdjX][ccwAdjY]); if(cwTemp != atan2Cache[cwAdjX][cwAdjY] &&
     * ccwTemp != atan2Cache[ccwAdjX][ccwAdjY]) angleMap[pt.x][pt.y] = 0.5 * (cwTemp
     * + ccwTemp); else if(ccwTemp != atan2Cache[ccwAdjX][ccwAdjY])
     * angleMap[pt.x][pt.y] = ccwTemp; else if(cwTemp != atan2Cache[cwAdjX][cwAdjY])
     * angleMap[pt.x][pt.y] = cwTemp; else angleMap[pt.x][pt.y] = theta; } /*
     *
     * else if (!blockedCW) angleMap[pt.x][pt.y] = (angleMap[cwAdjX][cwAdjY] !=
     * atan2Cache[cwAdjX][cwAdjY]) ? angleMap[cwAdjX][cwAdjY] : theta; else
     * angleMap[pt.x][pt.y] = (angleMap[ccwAdjX][ccwAdjY] !=
     * atan2Cache[ccwAdjX][ccwAdjY]) ? angleMap[ccwAdjX][ccwAdjY] : theta; / }
     * if(Math.abs(angleMap[pt.x][pt.y] - theta) <= 0.001 || resMap[pt.x][pt.y] >
     * 0.5) gradientMap[cx][cy] = dist; else angleMap[pt.x][pt.y] = PI2 * 2; } } } }
     *
     * return gradientMap; }
     */
    public byte[][] waveFOV(final int viewerX, final int viewerY) {
	final byte[][] gradientMap = new byte[this.width][this.height];
	final double[][] angleMap = new double[2 * this.maxRadius + 1][2 * this.maxRadius + 1];
	gradientMap[viewerX][viewerY] = (byte) (1 + this.maxRadius);
	int cx, cy, nearCWx, nearCWy, nearCCWx, nearCCWy;
	Coord pt;
	double theta, angleCW, angleCCW, straight;
	byte dist;
	boolean blockedCCW, blockedCW;
	for (int w = 0; w < this.waves.length; w++) {
	    for (int c = 0; c < this.waves[w].length; c++) {
		pt = this.waves[w][c];
		cx = viewerX - this.maxRadius + pt.x;
		cy = viewerY - this.maxRadius + pt.y;
		if (cx < this.width && cx >= 0 && cy < this.height && cy >= 0) {
		    theta = this.atan2Cache[pt.x][pt.y];
		    dist = (byte) this.distanceCache[pt.x][pt.y];
		    if (w <= 0) {
			gradientMap[cx][cy] = dist;
		    } else {
			switch ((int) Math.floor(theta / FOVCache.QUARTER_PI)) {
			// positive x, postive y, closer to x-axis
			case 0:
			    nearCCWx = pt.x - 1;
			    nearCCWy = pt.y;
			    angleCCW = this.directionAngles[0][1]; // atan2Cache[nearCCWx][nearCCWy];
			    straight = angleCCW;
			    nearCWx = pt.x - 1;
			    nearCWy = pt.y - 1;
			    angleCW = this.directionAngles[0][0]; // atan2Cache[nearCWx][nearCWy];
			    break;
			// positive x, postive y, closer to y-axis
			case 1:
			    nearCWx = pt.x;
			    nearCWy = pt.y - 1;
			    angleCW = this.directionAngles[1][0];
			    straight = angleCW;
			    nearCCWx = pt.x - 1;
			    nearCCWy = pt.y - 1;
			    angleCCW = this.directionAngles[0][0];
			    break;
			// negative x, postive y, closer to y-axis
			case 2:
			    nearCCWx = pt.x;
			    nearCCWy = pt.y - 1;
			    angleCCW = this.directionAngles[1][0];
			    straight = angleCCW;
			    nearCWx = pt.x + 1;
			    nearCWy = pt.y - 1;
			    angleCW = this.directionAngles[2][0];
			    break;
			// negative x, postive y, closer to x-axis
			case 3:
			    nearCCWx = pt.x + 1;
			    nearCCWy = pt.y;
			    angleCCW = this.directionAngles[2][1];
			    straight = angleCCW;
			    nearCWx = pt.x + 1;
			    nearCWy = pt.y - 1;
			    angleCW = this.directionAngles[2][0];
			    break;
			// negative x, negative y, closer to x-axis
			case 4:
			    nearCWx = pt.x + 1;
			    nearCWy = pt.y;
			    angleCW = -this.directionAngles[2][1];
			    straight = angleCW;
			    nearCCWx = pt.x + 1;
			    nearCCWy = pt.y + 1;
			    angleCCW = this.directionAngles[2][2];
			    break;
			// negative x, negative y, closer to y-axis
			case 5:
			    nearCWx = pt.x;
			    nearCWy = pt.y + 1;
			    angleCW = this.directionAngles[1][2];
			    straight = angleCW;
			    nearCCWx = pt.x + 1;
			    nearCCWy = pt.y + 1;
			    angleCCW = this.directionAngles[2][2];
			    break;
			// positive x, negative y, closer to y-axis
			case 6:
			    nearCCWx = pt.x;
			    nearCCWy = pt.y + 1;
			    angleCCW = this.directionAngles[1][2];
			    straight = angleCCW;
			    nearCWx = pt.x - 1;
			    nearCWy = pt.y + 1;
			    angleCW = this.directionAngles[0][2];
			    break;
			// positive x, negative y, closer to x-axis
			default:
			    nearCWx = pt.x - 1;
			    nearCWy = pt.y;
			    angleCW = this.directionAngles[0][1];
			    straight = angleCW;
			    nearCCWx = pt.x - 1;
			    nearCCWy = pt.y + 1;
			    angleCCW = this.directionAngles[0][2];
			    break;
			}
			nearCCWx += viewerX - this.maxRadius;
			nearCWx += viewerX - this.maxRadius;
			nearCCWy += viewerY - this.maxRadius;
			nearCWy += viewerY - this.maxRadius;
			blockedCCW = this.resMap[nearCCWx][nearCCWy] > 0.5
				|| angleMap[nearCCWx - viewerX + this.maxRadius][nearCCWy - viewerY
					+ this.maxRadius] >= FOVCache.PI2;
			blockedCW = this.resMap[nearCWx][nearCWy] > 0.5
				|| angleMap[nearCWx - viewerX + this.maxRadius][nearCWy - viewerY
					+ this.maxRadius] >= FOVCache.PI2;
			if (theta == 0 || theta == Math.PI || Math.abs(theta) - FOVCache.HALF_PI < 0.005
				&& Math.abs(theta) - FOVCache.HALF_PI > -0.005) {
			    angleMap[pt.x][pt.y] = straight == angleCCW
				    ? blockedCCW ? FOVCache.PI2
					    : angleMap[nearCCWx - viewerX + this.maxRadius][nearCCWy - viewerY
						    + this.maxRadius]
				    : blockedCW ? FOVCache.PI2
					    : angleMap[nearCWx - viewerX + this.maxRadius][nearCWy - viewerY
						    + this.maxRadius];
			} else {
			    if (blockedCW && blockedCCW) {
				angleMap[pt.x][pt.y] = FOVCache.PI2;
				continue;
			    }
			    if (blockedCW) {
				angleMap[pt.x][pt.y] = Math.abs(theta - angleCCW) + FOVCache.SLIVER_PI;
				// angleMap[nearCCWx - viewerX + maxRadius][nearCCWy - viewerY + maxRadius];
				// Math.abs(angleMap[nearCCWx - viewerX + maxRadius][nearCCWy - viewerY +
				// maxRadius] -
				// angleMap[pt.x][pt.y] = Math.abs(theta - angleCCW) +
				// angleMap[nearCCWx - viewerX + maxRadius][nearCCWy - viewerY + maxRadius];
			    } else if (blockedCCW) {
				angleMap[pt.x][pt.y] = Math.abs(angleCW - theta) + FOVCache.SLIVER_PI;
				// angleMap[nearCWx - viewerX + maxRadius][nearCWy - viewerY + maxRadius];
				// angleMap[nearCWx - viewerX + maxRadius][nearCWy - viewerY + maxRadius]
			    }
			    if (!blockedCW) {
				angleMap[pt.x][pt.y] += 0.5 * angleMap[nearCWx - viewerX + this.maxRadius][nearCWy
					- viewerY + this.maxRadius];
			    }
			    if (!blockedCCW) {
				angleMap[pt.x][pt.y] += 0.5 * angleMap[nearCCWx - viewerX + this.maxRadius][nearCCWy
					- viewerY + this.maxRadius];
			    }
			}
			if (angleMap[pt.x][pt.y] <= this.fovPermissiveness) {
			    gradientMap[cx][cy] = dist;
			} else {
			    angleMap[pt.x][pt.y] = FOVCache.PI2;
			}
		    }
		}
	    }
	}
	return gradientMap;
    }

    public byte[][] slopeShadowFOV(final int viewerX, final int viewerY) {
	final byte[][] lightMap = new byte[this.width][this.height];
	lightMap[viewerX][viewerY] = (byte) (1 + this.maxRadius);
	for (final Direction d : Direction.DIAGONALS) {
	    this.slopeShadowCast(1, 1.0, 0.0, 0, d.deltaX, d.deltaY, 0, viewerX, viewerY, lightMap);
	    this.slopeShadowCast(1, 1.0, 0.0, d.deltaX, 0, 0, d.deltaY, viewerX, viewerY, lightMap);
	}
	return lightMap;
    }

    private byte[][] slopeShadowCast(final int row, double start, final double end, final int xx, final int xy,
	    final int yx, final int yy, final int viewerX, final int viewerY, byte[][] lightMap) {
	double newStart = 0;
	if (start < end) {
	    return lightMap;
	}
	final int width = lightMap.length;
	final int height = lightMap[0].length;
	boolean blocked = false;
	int dist;
	for (int distance = row; distance <= this.maxRadius && !blocked; distance++) {
	    final int deltaY = -distance;
	    for (int deltaX = -distance; deltaX <= 0; deltaX++) {
		final int currentX = viewerX + deltaX * xx + deltaY * xy;
		final int currentY = viewerY + deltaX * yx + deltaY * yy;
		final double leftSlope = (deltaX - 0.5f) / (deltaY + 0.5f);
		final double rightSlope = (deltaX + 0.5f) / (deltaY - 0.5f);
		if (!(currentX >= 0 && currentY >= 0 && currentX < width && currentY < height
			&& currentX - viewerX + this.maxRadius >= 0 && currentX - viewerX <= this.maxRadius
			&& currentY - viewerY + this.maxRadius >= 0 && currentY - viewerY <= this.maxRadius)
			|| start < rightSlope) {
		    continue;
		} else if (end > leftSlope) {
		    break;
		}
		dist = this.distanceCache[currentX - viewerX + this.maxRadius][currentY - viewerY + this.maxRadius];
		// check if it's within the lightable area and light if needed
		if (dist <= this.maxRadius) {
		    lightMap[currentX][currentY] = (byte) dist;
		}
		if (blocked) { // previous cell was a blocking one
		    if (this.resMap[currentX][currentY] >= 0.5) {// hit a wall
			newStart = rightSlope;
		    } else {
			blocked = false;
			start = newStart;
		    }
		} else {
		    if (this.resMap[currentX][currentY] >= 0.5 && distance < this.maxRadius) {// hit a wall within sight
											      // line
			blocked = true;
			lightMap = this.slopeShadowCast(distance + 1, start, leftSlope, xx, xy, yx, yy, viewerX,
				viewerY, lightMap);
			newStart = rightSlope;
		    }
		}
	    }
	}
	return lightMap;
    }

    public short[][] improveQuality(final int viewerX, final int viewerY) {
	if (!this.complete) {
	    throw new IllegalStateException(
		    "cacheAllPerformance() must be called before improveQuality() to fill the cache.");
	}
	if (viewerX < 0 || viewerY < 0 || viewerX >= this.width || viewerY >= this.height) {
	    return this.ALL_WALLS;
	}
	if (this.resMap[viewerX][viewerY] >= 1.0) {
	    return this.ALL_WALLS;
	}
	final short myHilbert = (short) CoordPacker.posToHilbert(viewerX, viewerY);
	final ShortVLA packing = new ShortVLA(128);
	final short[][] packed = new short[this.maxRadius + 1][], cached = this.cache[viewerX + viewerY * this.width];
	final short[] losCached = this.losCache[viewerX + viewerY * this.width];
	/// *
	// short[] perimeter = allPackedHilbert(fringe(losCached, 2, width, height));
	int xr = Math.max(0, viewerX - 1 - this.maxLOSRadius), yr = Math.max(0, viewerY - 1 - this.maxLOSRadius),
		wr = Math.min(this.width - 1 - viewerX, this.maxLOSRadius * 2 + 3),
		hr = Math.min(this.height - 1 - viewerY, this.maxLOSRadius * 2 + 3);
	short[] perimeter = CoordPacker.rectangleHilbert(xr, yr, wr, hr);
	short p_x, p_y;
	for (final short element : perimeter) {
	    p_x = CoordPacker.hilbertX[element & 0xffff];
	    p_y = CoordPacker.hilbertY[element & 0xffff];
	    if (CoordPacker.queryPackedHilbert(this.losCache[p_x + p_y * this.width], myHilbert)) {
		packing.add(element);
	    }
	}
	// */
	/*
	 * boolean[][] losUnpacked = unpack(losCached, width, height); for (int x =
	 * Math.max(0, viewerX - 62); x <= Math.min(viewerX + 62, width - 1); x++) { for
	 * (int y = Math.max(0, viewerY - 62); y <= Math.min(viewerY + 62, height - 1);
	 * y++) { if (losUnpacked[x][y]) continue; if(losCache[x + y * width] ==
	 * ALL_WALL) continue; if (distance(x - viewerX, y - viewerY) / 2 > 62)
	 * continue; if (queryPackedHilbert(losCache[x + y * width], myHilbert))
	 * packing.add((short) posToHilbert(x, y)); } }
	 */
	this.losCache[viewerX + viewerY * this.width] = CoordPacker.insertSeveralPacked(losCached, packing.asInts());
	for (int l = 0; l <= this.maxRadius; l++) {
	    packing.clear();
	    xr = Math.max(0, viewerX - l);
	    yr = Math.max(0, viewerY - l);
	    wr = Math.min(this.width - viewerX + l, l * 2 + 1);
	    hr = Math.min(this.height - viewerY + l, l * 2 + 1);
	    perimeter = CoordPacker.rectangleHilbert(xr, yr, wr, hr);
	    // short p_x, p_y;
	    for (final short element : perimeter) {
		if (CoordPacker.queryPackedHilbert(cached[this.maxRadius - l], element)) {
		    packing.add(element);
		    continue;
		}
		p_x = CoordPacker.hilbertX[element & 0xffff];
		p_y = CoordPacker.hilbertY[element & 0xffff];
		if (this.cache[p_x + p_y * this.width] == this.ALL_WALLS) {
		    continue;
		}
		if (this.distance(p_x - viewerX, p_y - viewerY) / 2 > l) {
		    continue;
		}
		if (CoordPacker.queryPackedHilbert(this.cache[p_x + p_y * this.width][this.maxRadius - l], myHilbert)) {
		    packing.add(element);
		}
	    }
	    /*
	     * knownSeen = cached[maxRadius - l]; for (int x = Math.max(0, viewerX - l); x
	     * <= Math.min(viewerX + l, width - 1); x++) { for (int y = Math.max(0, viewerY
	     * - l); y <= Math.min(viewerY + l, height - 1); y++) { if(cache[x + y * width]
	     * == ALL_WALLS) continue; if (distance(x - viewerX, y - viewerY) / 2 > l)
	     * continue; short i = (short) posToHilbert(x, y); if
	     * (queryPackedHilbert(knownSeen, i)) continue; if (queryPackedHilbert(cache[x +
	     * y * width][maxRadius - l], myHilbert)) packing.add(i); } }
	     */
	    packed[this.maxRadius - l] = CoordPacker.packSeveral(packing.asInts());
	    Coord light;
	    for (int i = 0; i < this.lightSources.length; i++) {
		light = this.lightSources[i];
		packed[this.maxRadius
			- l] = CoordPacker
				.unionPacked(packed[this.maxRadius - l],
					CoordPacker
						.differencePacked(
							CoordPacker.intersectPacked(losCached,
								this.cache[light.x
									+ light.y * this.width][this.maxRadius
										- this.lightBrightnesses[i]]),
							this.wallMap));
	    }
	}
	return packed;
    }

    /**
     * Runs FOV calculations on another thread, without interrupting this one.
     * Before using the cache, you should call awaitCachePerformance() to ensure
     * this method has finished on its own thread, but be aware that this will cause
     * the thread that calls awaitCachePerformance() to essentially freeze until FOV
     * calculations are over.
     */
    private void cacheAllPerformance() {
	if (this.performanceThread != null || this.complete) {
	    return;
	}
	this.performanceThread = new Thread(new PerformanceUnit());
	this.performanceThread.start();
    }

    /**
     * Runs FOV calculations on another thread, without interrupting this one, then
     * performs additional quality tweaks and adds any distant lights, if there were
     * any in the constructor. Before using the cache, you should call awaitCache()
     * to ensure this method has finished on its own thread, but be aware that this
     * will cause the thread that calls awaitCache() to essentially freeze until FOV
     * calculations are over.
     */
    public void cacheAll() {
	if (this.qualityThread != null || this.qualityComplete) {
	    return;
	}
	this.qualityThread = new Thread(new QualityUnit());
	this.qualityThread.start();
    }

    /**
     * If FOV calculations from cacheAll() are being done on another thread, calling
     * this method will make the current thread wait for the FOV calculations'
     * thread to finish, "freezing" the current thread until it does. This ensures
     * the cache will be complete with the additional quality improvements such as
     * distant lights after this method returns true, and if this method returns
     * false, then something has gone wrong.
     *
     * @return true if cacheAll() has successfully completed, false otherwise.
     */
    public boolean awaitCache() {
	if (this.qualityThread == null && !this.qualityComplete) {
	    this.cacheAll();
	}
	if (this.qualityComplete) {
	    return true;
	}
	try {
	    this.qualityThread.join();
	} catch (final InterruptedException e) {
	    return false;
	}
	return this.qualityComplete;
    }

    /**
     * Runs FOV calculations for any cells that were changed as a result of newMap
     * being different from the map passed to the FOVCache constructor. It runs
     * these on another thread, without interrupting this one. Before using the
     * cache, you should call awaitRefresh() to ensure this method has finished on
     * its own thread, but be aware that this will cause the thread that calls
     * awaitRefresh() to essentially freeze until FOV calculations are over.
     */
    public void refreshCache(final char[][] newMap) {
	this.performanceThread = new Thread(new RefreshUnit(newMap));
	this.performanceThread.start();
    }

    /**
     * If FOV calculations from refreshCache() are being done on another thread,
     * calling this method will make the current thread wait for the FOV
     * calculations' thread to finish, "freezing" the current thread until it does.
     * This ensures the cache will be complete with the updates from things like
     * opening a door after this method returns true, and if this method returns
     * false, then something has gone wrong.
     *
     * @return true if refreshCache() has successfully completed, false otherwise.
     */
    public boolean awaitRefresh(final char[][] newMap) {
	if (!this.performanceThread.isAlive() && !this.refreshComplete) {
	    this.refreshCache(newMap);
	}
	if (this.refreshComplete) {
	    return true;
	}
	try {
	    this.performanceThread.join();
	} catch (final InterruptedException e) {
	    return false;
	}
	if (this.refreshComplete) {
	    this.refreshComplete = false;
	    return true;
	} else {
	    return false;
	}
    }

    private short distance(final int xPos, final int yPos) {
	final int x = Math.abs(xPos), y = Math.abs(yPos);
	switch (this.radiusKind) {
	case CIRCLE:
	case SPHERE: {
	    if (x == y) {
		return (short) (3 * x);
	    } else if (x < y) {
		return (short) (3 * x + 2 * (y - x));
	    } else {
		return (short) (3 * y + 2 * (x - y));
	    }
	}
	case DIAMOND:
	case OCTAHEDRON:
	    return (short) (2 * (x + y));
	default:
	    return (short) (2 * Math.max(x, y));
	}
    }

    /**
     * Calculates the Field Of View for the provided map from the given x, y
     * coordinates. Returns a light map where the values are either 1.0 or 0.0.
     * Takes a double[][] resistance map that will be disregarded. <br>
     * The starting point for the calculation is considered to be at the center of
     * the origin cell. Radius determinations are based on the radiusKind given in
     * construction. The light will be treated as having radius 62, regardless of
     * the maxRadius passed to the constructor; this should in most cases be
     * suitable when limitless light is desired. If the cache has not been fully
     * constructed, this will compute a new FOV map using Shadow FOV instead of
     * using the cache, and the result will not be cached.
     *
     * @param resistanceMap the grid of cells to calculate on
     * @param startx        the horizontal component of the starting location
     * @param starty        the vertical component of the starting location
     * @return the computed light grid
     */
    @Override
    public double[][] calculateFOV(final double[][] resistanceMap, final int startx, final int starty) {
	if (this.qualityComplete || this.complete) {
	    return CoordPacker.unpackDouble(this.losCache[startx + starty * this.width], this.width, this.height);
	} else {
	    return this.fov.calculateFOV(this.resMap, startx, starty, this.maxRadius, this.radiusKind);
	}
    }
    /*
     * Calculates the Field Of View for the provided map from the given x, y
     * coordinates. Returns a light map where the values are either 1.0 or 0.0.
     * Takes a double radius to extend out to (rounded to the nearest int) and a
     * double[][] resistance map that will be disregarded. <br> The starting point
     * for the calculation is considered to be at the center of the origin cell.
     * Radius determinations are based on the radiusKind given in construction. The
     * light will be treated as having the given radius, but if that is higher than
     * the maximum possible radius stored by this FOVCache, it will compute a new
     * FOV map using Shadow FOV instead of using the cache. If the cache has not
     * been fully constructed, this will compute a new FOV map using Shadow FOV
     * instead of using the cache, and the result will not be cached.
     *
     * @param resistanceMap the grid of cells to calculate on
     *
     * @param startx the horizontal component of the starting location
     *
     * @param starty the vertical component of the starting location
     *
     * @param radius the distance the light will extend to
     *
     * @return the computed light grid
     */
    /*
     * @Override public double[][] calculateFOV(double[][] resistanceMap, int
     * startx, int starty, double radius) { if((qualityComplete || complete) &&
     * radius >= 0 && radius <= maxRadius) return unpackDouble(cache[startx + starty
     * * width][maxRadius - (int) Math.round(radius)], width, height); else return
     * fov.calculateFOV(resMap, startx, starty, radius, radiusKind); }
     */
    /*
     * Calculates the Field Of View for the provided map from the given x, y
     * coordinates. Returns a light map where the values are either 1.0 or 0.0.
     * Takes a double radius to extend out to (rounded to the nearest int), a Radius
     * enum that should match the Radius this FOVCache was constructed with, and a
     * double[][] resistance map that will be disregarded. <br> The starting point
     * for the calculation is considered to be at the center of the origin cell.
     * Radius determinations are based on the radiusTechnique passed to this method,
     * but will only use the cache if the Radius given is the same kind as the one
     * given in construction. The light will be treated as having the given radius,
     * but if that is higher than the maximum possible radius stored by this
     * FOVCache, it will compute a new FOV map using Shadow FOV instead of using the
     * cache. If the cache has not been fully constructed, this will compute a new
     * FOV map using Shadow FOV instead of using the cache, and the result will not
     * be cached.
     *
     * @param resistanceMap the grid of cells to calculate on
     *
     * @param startX the horizontal component of the starting location
     *
     * @param startY the vertical component of the starting location
     *
     * @param radius the distance the light will extend to
     *
     * @param radiusTechnique provides a means to calculate the radius as desired
     *
     * @return the computed light grid
     */
    /*
     * @Override public double[][] calculateFOV(double[][] resistanceMap, int
     * startX, int startY, double radius, Radius radiusTechnique) {
     * if((qualityComplete || complete) && radius >= 0 && radius <= maxRadius &&
     * radiusKind.equals2D(radiusTechnique)) return unpackDouble(cache[startX +
     * startY * width][maxRadius - (int) Math.round(radius)], width, height); else
     * return fov.calculateFOV(resMap, startX, startY, radius, radiusTechnique); }
     */

    /*
     * Calculates the conical Field Of View for the provided map from the given x, y
     * coordinates. Returns a light map where the values are either 1.0 or 0.0.
     * Takes a double radius to extend out to (rounded to the nearest int), a Radius
     * enum that should match the Radius this FOVCache was constructed with, a
     * double[][] resistance map that will be disregarded, an angle to center the
     * conical FOV on (in degrees), and the total span in degrees for the FOV to
     * cover. <br> The starting point for the calculation is considered to be at the
     * center of the origin cell. Radius determinations are based on the
     * radiusTechnique passed to this method, but will only use the cache if the
     * Radius given is the same kind as the one given in construction. The light
     * will be treated as having the given radius, but if that is higher than the
     * maximum possible radius stored by this FOVCache, it will compute a new FOV
     * map using Shadow FOV instead of using the cache. A conical section of FOV is
     * lit by this method if span is greater than 0.
     *
     * If the cache has not been fully constructed, this will compute a new FOV map
     * using Shadow FOV instead of using the cache, and the result will not be
     * cached.
     *
     * @param resistanceMap the grid of cells to calculate on
     *
     * @param startX the horizontal component of the starting location
     *
     * @param startY the vertical component of the starting location
     *
     * @param radius the distance the light will extend to
     *
     * @param radiusTechnique provides a means to calculate the radius as desired
     *
     * @param angle the angle in degrees that will be the center of the FOV cone, 0
     * points right
     *
     * @param span the angle in degrees that measures the full arc contained in the
     * FOV cone
     *
     * @return the computed light grid
     */
    /*
     * @Override public double[][] calculateFOV(double[][] resistanceMap, int
     * startX, int startY, double radius, Radius radiusTechnique, double angle,
     * double span) { if((qualityComplete || complete) && radius >= 0 && radius <=
     * maxRadius && radiusKind.equals2D(radiusTechnique)) return
     * unpackDoubleConical(cache[startX + startY * width][maxRadius - (int)
     * Math.round(radius)], width, height, startX, startY, angle, span); else return
     * fov.calculateFOV(resMap, startX, startY, radius, radiusTechnique, angle,
     * span); }
     */
    /**
     * Calculates the Field Of View for the provided map from the given x, y
     * coordinates. Returns a light map where the values range from 1.0 (center of
     * the FOV) to 0.0 (not seen), with values between those two extremes for the
     * rest of the seen area. Takes a double radius to extend out to (rounded to the
     * nearest int), and a double[][] resistance map that will be disregarded. <br>
     * The starting point for the calculation is considered to be at the center of
     * the origin cell. Radius determinations are based on the radiusKind given in
     * construction. The light will be treated as having the given radius, but if
     * that is higher than the maximum possible radius stored by this FOVCache, it
     * will compute a new FOV map using Ripple FOV instead of using the cache. If
     * the cache has not been fully constructed, this will compute a new FOV map
     * using Ripple FOV instead of using the cache, and the result will not be
     * cached.
     *
     * @param resistanceMap the grid of cells to calculate on
     * @param startx        the horizontal component of the starting location
     * @param starty        the vertical component of the starting location
     * @param radius        the distance the light will extend to
     * @return the computed light grid
     */
    @Override
    public double[][] calculateFOV(final double[][] resistanceMap, final int startx, final int starty,
	    final double radius) {
	if ((this.qualityComplete || this.complete) && radius > 0 && radius <= this.maxRadius) {
	    return CoordPacker.unpackMultiDoublePartial(this.cache[startx + starty * this.width], this.width,
		    this.height, this.levels[(int) Math.round(radius)], (int) Math.round(radius));
	} else {
	    return this.gradedFOV.calculateFOV(this.resMap, startx, starty, radius, this.radiusKind);
	}
    }

    /**
     * Calculates the Field Of View for the provided map from the given x, y
     * coordinates. Returns a light map where the values range from 1.0 (center of
     * the FOV) to 0.0 (not seen), with values between those two extremes for the
     * rest of the seen area. Takes a double radius to extend out to (rounded to the
     * nearest int), a Radius enum that should match the Radius this FOVCache was
     * constructed with, and a double[][] resistance map that will be disregarded.
     * <br>
     * The starting point for the calculation is considered to be at the center of
     * the origin cell. Radius determinations are based on the radiusTechnique
     * passed to this method, but will only use the cache if the Radius given is the
     * same kind as the one given in construction. The light will be treated as
     * having the given radius, but if that is higher than the maximum possible
     * radius stored by this FOVCache, it will compute a new FOV map using Ripple
     * FOV instead of using the cache. If the cache has not been fully constructed,
     * this will compute a new FOV map using Ripple FOV instead of using the cache,
     * and the result will not be cached.
     *
     * @param resistanceMap   the grid of cells to calculate on
     * @param startX          the horizontal component of the starting location
     * @param startY          the vertical component of the starting location
     * @param radius          the distance the light will extend to
     * @param radiusTechnique provides a means to calculate the radius as desired
     * @return the computed light grid
     */
    @Override
    public double[][] calculateFOV(final double[][] resistanceMap, final int startX, final int startY,
	    final double radius, final Radius radiusTechnique) {
	if ((this.qualityComplete || this.complete) && radius > 0 && radius <= this.maxRadius
		&& this.radiusKind.equals2D(radiusTechnique)) {
	    return CoordPacker.unpackMultiDoublePartial(this.cache[startX + startY * this.width], this.width,
		    this.height, this.levels[(int) Math.round(radius)], (int) Math.round(radius));
	} else {
	    return this.gradedFOV.calculateFOV(this.resMap, startX, startY, radius, radiusTechnique);
	}
    }

    /**
     * Calculates the conical Field Of View for the provided map from the given x, y
     * coordinates. Returns a light map where the values range from 1.0 (center of
     * the FOV) to 0.0 (not seen), with values between those two extremes for the
     * rest of the seen area. Takes a double radius to extend out to (rounded to the
     * nearest int), a Radius enum that should match the Radius this FOVCache was
     * constructed with, a double[][] resistance map that will be disregarded, an
     * angle to center the conical FOV on (in degrees), and the total span in
     * degrees for the FOV to cover. <br>
     * The starting point for the calculation is considered to be at the center of
     * the origin cell. Radius determinations are based on the radiusTechnique
     * passed to this method, but will only use the cache if the Radius given is the
     * same kind as the one given in construction. The light will be treated as
     * having the given radius, but if that is higher than the maximum possible
     * radius stored by this FOVCache, it will compute a new FOV map using Ripple
     * FOV instead of using the cache. A conical section of FOV is lit by this
     * method if span is greater than 0.
     *
     * If the cache has not been fully constructed, this will compute a new FOV map
     * using Ripple FOV instead of using the cache, and the result will not be
     * cached.
     *
     * @param resistanceMap   the grid of cells to calculate on
     * @param startX          the horizontal component of the starting location
     * @param startY          the vertical component of the starting location
     * @param radius          the distance the light will extend to
     * @param radiusTechnique provides a means to calculate the radius as desired
     * @param angle           the angle in degrees that will be the center of the
     *                        FOV cone, 0 points right
     * @param span            the angle in degrees that measures the full arc
     *                        contained in the FOV cone
     * @return the computed light grid
     */
    @Override
    public double[][] calculateFOV(final double[][] resistanceMap, final int startX, final int startY,
	    final double radius, final Radius radiusTechnique, final double angle, final double span) {
	if ((this.qualityComplete || this.complete) && radius > 0 && radius <= this.maxRadius
		&& this.radiusKind.equals2D(radiusTechnique)) {
	    return CoordPacker.unpackMultiDoublePartialConical(this.cache[startX + startY * this.width], this.width,
		    this.height, this.levels[(int) Math.round(radius)], (int) Math.round(radius), startX, startY, angle,
		    span);
	} else {
	    return this.gradedFOV.calculateFOV(this.resMap, startX, startY, radius, radiusTechnique, angle, span);
	}
    }

    /**
     * Calculates an array of Coord positions that can be seen along the line from
     * the given start point and end point. Does not order the array. Uses the
     * pre-computed LOS cache to determine obstacles, and tends to draw a thicker
     * line than Bresenham lines will. This uses the same radiusKind the FOVCache
     * was created with, but the line this draws doesn't necessarily travel along
     * valid directions for creatures (in particular, Radius.DIAMOND should only
     * allow orthogonal movement, but if you request a 45-degree line, the LOS will
     * have Coords on a perfect diagonal, though they won't travel through walls
     * that occupy a thin perpendicular diagonal).
     *
     * @param startX the x position of the starting point; must be within bounds of
     *               the map
     * @param startY the y position of the starting point; must be within bounds of
     *               the map
     * @param endX   the x position of the endpoint; does not need to be within
     *               bounds (will stop LOS at the edge)
     * @param endY   the y position of the endpoint; does not need to be within
     *               bounds (will stop LOS at the edge)
     * @return a Coord[], unordered, that can be seen along the line of sight;
     *         limited to maxLOSRadius, default 62
     */
    public Coord[] calculateLOS(final int startX, final int startY, final int endX, final int endY) {
	if (!this.complete || startX < 0 || startX >= this.width || startY < 0 || startY >= this.height) {
	    return new Coord[0];
	}
	final int max = this.distance(endX - startX, endY - startY);
	final ArrayList<Coord> path = new ArrayList<>(max / 2 + 1);
	final short[] losCached = this.losCache[startX + startY * this.width];
	if (losCached.length == 0) {
	    return new Coord[0];
	}
	boolean on = false;
	int idx = 0, xt, yt;
	short x = 0, y = 0;
	path.add(Coord.get(startX, startY));
	if (startX == endX && startY == endY) {
	    return path.toArray(new Coord[0]);
	}
	final double angle = Math.atan2(endY - startY, endX - startX);
	final double x2 = Math.sin(angle) * 0.5, y2 = Math.cos(angle) * 0.5;
	final boolean[][] mask = new boolean[this.width][this.height];
	for (int d = 2; d <= max; d++) {
	    xt = startX + (int) (x2 * d + 0.5);
	    yt = startY + (int) (y2 * d + 0.5);
	    if (xt < 0 || xt >= this.width || yt < 0 || yt >= this.height) {
		break;
	    }
	    mask[xt][yt] = true;
	}
	for (int p = 0; p < losCached.length; p++, on = !on) {
	    if (on) {
		for (final int toSkip = idx + (losCached[p] & 0xffff); idx < toSkip && idx < this.limit; idx++) {
		    x = CoordPacker.hilbertX[idx];
		    y = CoordPacker.hilbertY[idx];
		    // this should never be possible, removing tentatively
		    // if(x >= width || y >= height)
		    // continue;
		    if (mask[x][y]) {
			path.add(Coord.get(x, y));
		    }
		}
	    } else {
		idx += losCached[p] & 0xffff;
	    }
	}
	return path.toArray(new Coord[0]);
    }

    /**
     * Calculates an array of Coord positions that can be seen along the line from
     * the given start point and end point. Sorts the array, starting from the
     * closest Coord to start and ending close to end. Uses the pre-computed LOS
     * cache to determine obstacles, and tends to draw a thicker line than Bresenham
     * lines will. This uses the same radiusKind the FOVCache was created with, but
     * the line this draws doesn't necessarily travel along valid directions for
     * creatures (in particular, Radius.DIAMOND should only allow orthogonal
     * movement, but if you request a 45-degree line, the LOS will have Coords on a
     * perfect diagonal, though they won't travel through walls that occupy a thin
     * perpendicular diagonal).
     *
     * @param startX the x position of the starting point; must be within bounds of
     *               the map
     * @param startY the y position of the starting point; must be within bounds of
     *               the map
     * @param endX   the x position of the endpoint; does not need to be within
     *               bounds (will stop LOS at the edge)
     * @param endY   the y position of the endpoint; does not need to be within
     *               bounds (will stop LOS at the edge)
     * @return a Coord[], sorted, that can be seen along the line of sight; limited
     *         to max LOS range, 62
     */
    public Coord[] sortedLOS(final int startX, final int startY, final int endX, final int endY) {
	final Coord[] path = this.calculateLOS(startX, startY, endX, endY);
	final SortedMap<Double, Coord> sorted = new TreeMap<>();
	double modifier = 0.0001;
	Coord c;
	for (int i = 0; i < path.length; i++, modifier += 0.0001) {
	    c = path[i];
	    sorted.put(this.distance(c.x, c.y) + modifier, c);
	}
	return sorted.values().toArray(new Coord[sorted.size()]);
    }

    /**
     * Given a path as a List of Coords (such as one produced by
     * DijkstraMap.getPath()), this method will look up the FOV for the given
     * fovRange at each Coord, and returns an array of packed FOV maps where each
     * map is the union of the FOV centered on a Coord in path with all FOVs
     * centered on previous Coords in path. The purpose of this is mainly to have an
     * efficient way to show the progressively expanding seen area of a character
     * who moves multiple tiles. It may be desirable to add the entire path's
     * cumulative FOV (stored in the last element of the returned short[][]) to the
     * history of what a character has seen, removing the path's FOV from the
     * currently visible cells either after the character has finished their action,
     * or even immediately after moving if, for instance, the movement was part of a
     * rapid leap that wouldn't let the character spot details while moving (but the
     * general layout provided by the seen-cell history could suffice). This method
     * never unpacks any packed data.
     *
     * @param path     a List of Coords that will be added, in order, to multiple
     *                 packed FOVs for the path so far
     * @param fovRange the radius the creature or thing taking the path can see (or
     *                 possibly light up).
     * @return a packed short[][], each short[] encoding the FOV around an
     *         additional Coord merged with those before
     */
    public short[][] pathFOVPacked(final List<Coord> path, final int fovRange) {
	if (!this.complete) {
	    throw new IllegalStateException("Cache is not yet constructed");
	}
	if (fovRange > this.maxRadius) {
	    throw new UnsupportedOperationException("Given fovRange parameter exceeds maximum cached range");
	}
	final short[][] fovSteps = new short[path.size()][];
	int idx = 0;
	for (final Coord c : path) {
	    if (c.x < 0 || c.y < 0 || c.x >= this.width || c.y >= this.height) {
		throw new ArrayIndexOutOfBoundsException("Along given path, encountered an invalid Coord: " + c);
	    }
	    if (idx == 0) {
		fovSteps[idx] = this.cache[c.x + c.y * this.width][this.maxRadius - fovRange];
	    } else {
		fovSteps[idx] = CoordPacker.unionPacked(fovSteps[idx - 1],
			this.cache[c.x + c.y * this.width][this.maxRadius - fovRange]);
	    }
	    idx++;
	}
	return fovSteps;
    }

    /**
     * Given a path as a List of Coords (such as one produced by
     * DijkstraMap.getPath()), this method will look up the FOV for the given
     * fovRange at each Coord, and returns an array of full FOV maps where each map
     * is the union of the FOV centered on a Coord in path with all FOVs centered on
     * previous Coords in path. The purpose of this is mainly to have a way to show
     * the progressively expanding seen area of a character who moves multiple
     * tiles. It may be desirable to add the entire path's cumulative FOV (stored in
     * the last element of the returned double[][][]) to the history of what a
     * character has seen, removing the path's FOV from the currently visible cells
     * either after the character has finished their action, or even immediately
     * after moving if, for instance, the movement was part of a rapid leap that
     * wouldn't let the character spot details while moving (but the general layout
     * provided by the seen-cell history could suffice). This method computes the
     * union of the FOV without unpacking, but then unpacks each step along the path
     * into a double[][] of 1.0 and 0.0 values.
     *
     * @param path     a List of Coords that will be added, in order, to multiple
     *                 FOVs for the path so far
     * @param fovRange the radius the creature or thing taking the path can see (or
     *                 possibly light up).
     * @return a packed double[][][]; each double[][] is the FOV around an
     *         additional Coord merged with those before
     */
    public double[][][] pathFOV(final List<Coord> path, final int fovRange) {
	final short[][] compressed = this.pathFOVPacked(path, fovRange);
	final double[][][] fovSteps = new double[compressed.length][][];
	for (int i = 0; i < compressed.length; i++) {
	    fovSteps[i] = CoordPacker.unpackDouble(compressed[i], this.width, this.height);
	}
	return fovSteps;
    }

    /**
     * In games that have multiple characters who should share one FOV map, this
     * method should provide optimal performance when collecting several cached FOV
     * maps into one packed map. It takes a Map of Coord keys to Integer values, and
     * since it does not modify its parameter, nor does it need a particular
     * iteration order, it doesn't perform a defensive copy of the team parameter.
     * Each Coord key should correspond to the position of a character, and each
     * Integer value should be the FOV range of that character. This returns a
     * short[] as a packed FOV map for all characters in team as a collective.
     *
     * @param team a Map of Coord keys for characters' positions to Integer values
     *             for the FOV range of each character
     * @return a packed FOV map that can be used with other packed data using
     *         CoordPacker.
     */
    public short[] teamFOVPacked(final Map<Coord, Integer> team) {
	if (!this.complete) {
	    throw new IllegalStateException("Cache is not yet constructed");
	}
	short[] packing = new short[0];
	int idx = 0;
	Coord c;
	int range;
	for (final Map.Entry<Coord, Integer> kv : team.entrySet()) {
	    c = kv.getKey();
	    range = kv.getValue();
	    if (c.x < 0 || c.y < 0 || c.x >= this.width || c.y >= this.height) {
		throw new ArrayIndexOutOfBoundsException("Among team, encountered an invalid Coord: " + c);
	    }
	    if (idx == 0) {
		packing = this.cache[c.x + c.y * this.width][this.maxRadius - range];
	    } else {
		packing = CoordPacker.unionPacked(packing, this.cache[c.x + c.y * this.width][this.maxRadius - range]);
	    }
	    idx++;
	}
	return packing;
    }

    /**
     * In games that have multiple characters who should share one FOV map, this
     * method should provide optimal performance when collecting several cached FOV
     * maps into one full map. It takes a Map of Coord keys to Integer values, and
     * since it does not modify its parameter, nor does it need a particular
     * iteration order, it doesn't perform a defensive copy of the team parameter.
     * Each Coord key should correspond to the position of a character, and each
     * Integer value should be the FOV range of that character. This returns a
     * double[][] as a full FOV map, with values of either 1.0 or 0.0, for all
     * characters in team as a collective.
     *
     * @param team a Map of Coord keys for characters' positions to Integer values
     *             for the FOV range of each character
     * @return a double[][] FOV map with 1.0 and 0.0 as values, combining all
     *         characters' FOV maps.
     */
    public double[][] teamFOV(final Map<Coord, Integer> team) {
	return CoordPacker.unpackDouble(this.teamFOVPacked(team), this.width, this.height);
    }

    /**
     * When you have some packed on/off data and want to make sure it does not
     * include walls, you can use this. It does not need to unpack the given packed
     * short[] to get the subset of it without walls. Of course, this needs the
     * FOVCache to be
     *
     * @param packed a short[] produced by some CoordPacker method or obtained by
     *               getLOSEntry() or getCacheEntry().
     * @return another packed array containing only the non-wall "on" cells in
     *         packed
     */
    public short[] removeWalls(final short[] packed) {
	return CoordPacker.differencePacked(packed, this.wallMap);
    }

    public int getMaxRadius() {
	return this.maxRadius;
    }

    public Radius getRadiusKind() {
	return this.radiusKind;
    }

    public int getWidth() {
	return this.width;
    }

    public int getHeight() {
	return this.height;
    }

    @GwtIncompatible
    protected class PerformanceUnit implements Runnable {
	/**
	 * When an object implementing interface <code>Runnable</code> is used to create
	 * a thread, starting the thread causes the object's <code>run</code> method to
	 * be called in that separately executing thread.
	 * <p>
	 * The general contract of the method <code>run</code> is that it may take any
	 * action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override
	public void run() {
	    final ArrayList<ArrayList<LOSUnit>> losUnits = new ArrayList<>(24);
	    final ArrayList<ArrayList<FOVUnit>> fovUnits = new ArrayList<>(24);
	    for (int p = 0; p < 24; p++) {
		losUnits.add(new ArrayList<LOSUnit>(FOVCache.this.mapLimit / 20));
		fovUnits.add(new ArrayList<FOVUnit>(FOVCache.this.mapLimit / 20));
	    }
	    for (int i = 0, p = 0; i < FOVCache.this.mapLimit; i++, p = (p + 1) % 24) {
		losUnits.get(p).add(new LOSUnit(i));
		fovUnits.get(p).add(new FOVUnit(i));
	    }
	    // long totalTime = System.currentTimeMillis(), threadTime = 0L;
	    for (int p = 23; p >= 0; p--) {
		try {
		    final List<Future<Long>> invoke = FOVCache.this.executor.invokeAll(losUnits.get(p));
		    for (final Future<Long> future : invoke) {
			future.get();
			// long t = future.get();
			// threadTime += t;
			// System.out.println(t);
		    }
		} catch (InterruptedException | ExecutionException e) {
		    e.printStackTrace();
		}
		losUnits.remove(p);
		System.gc();
	    }
	    for (int p = 23; p >= 0; p--) {
		try {
		    final List<Future<Long>> invoke = FOVCache.this.executor.invokeAll(fovUnits.get(p));
		    for (final Future<Long> future : invoke) {
			future.get();
			// long t = future.get();
			// threadTime += t;
			// System.out.println(t);
		    }
		} catch (InterruptedException | ExecutionException e) {
		    e.printStackTrace();
		}
		fovUnits.remove(p);
		System.gc();
	    }
	    // totalTime = System.currentTimeMillis() - totalTime;
	    // System.out.println("Total real time elapsed: " + totalTime);
	    // System.out.println("Total CPU time elapsed, on " + NUM_THREADS + " threads: "
	    // + threadTime);
	    /*
	     * long totalRAM = 0; for (int c = 0; c < width * height; c++) { long ctr = 0,
	     * losCtr = 0; for (int i = 0; i < cache[c].length; i++) { ctr += (((2 *
	     * cache[c][i].length + 12 - 1) / 8) + 1) * 8L; } totalRAM += (((ctr + 12 - 1) /
	     * 8) + 1) * 8;
	     *
	     * losCtr = (((2 * losCache[c].length + 12 - 1) / 8) + 1) * 8L; totalRAM +=
	     * (((losCtr + 12 - 1) / 8) + 1) * 8; }
	     * System.out.println("Total memory used by cache: " + totalRAM);
	     */
	    FOVCache.this.complete = true;
	}
    }

    @GwtIncompatible
    protected class QualityUnit implements Runnable {
	/**
	 * When an object implementing interface <code>Runnable</code> is used to create
	 * a thread, starting the thread causes the object's <code>run</code> method to
	 * be called in that separately executing thread.
	 * <p>
	 * The general contract of the method <code>run</code> is that it may take any
	 * action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override
	public void run() {
	    // long totalTime = System.currentTimeMillis(), threadTime = 0L;
	    if (!FOVCache.this.complete) {
		FOVCache.this.cacheAllPerformance();
		try {
		    FOVCache.this.performanceThread.join();
		} catch (final InterruptedException e) {
		    return;
		}
	    }
	    final ArrayList<ArrayList<SymmetryUnit>> symUnits = new ArrayList<>(4);
	    for (int p = 0; p < 4; p++) {
		symUnits.add(new ArrayList<SymmetryUnit>(FOVCache.this.mapLimit / 3));
	    }
	    for (int i = 0, p = 0; i < FOVCache.this.mapLimit; i++, p = (p + 1) % 4) {
		symUnits.get(p).add(new SymmetryUnit(i));
	    }
	    for (int p = 3; p >= 0; p--) {
		try {
		    final List<Future<Long>> invoke = FOVCache.this.executor.invokeAll(symUnits.get(p));
		    for (final Future<Long> future : invoke) {
			// threadTime +=
			future.get();
			// System.out.println(t);
		    }
		} catch (InterruptedException | ExecutionException e) {
		    e.printStackTrace();
		}
		symUnits.remove(p);
	    }
	    FOVCache.this.cache = FOVCache.this.tmpCache;
	    FOVCache.this.qualityComplete = true;
	    /*
	     * totalTime = System.currentTimeMillis() - totalTime;
	     * System.out.println("Total real time elapsed : " + totalTime);
	     * System.out.println("Total CPU time elapsed, on " + NUM_THREADS + " threads: "
	     * + threadTime);
	     *
	     * long totalRAM = 0; for (int c = 0; c < width * height; c++) { long ctr = 0,
	     * losCtr = 0; for (int i = 0; i < cache[c].length; i++) { ctr += (((2 *
	     * cache[c][i].length + 12 - 1) / 8) + 1) * 8L; } totalRAM += (((ctr + 12 - 1) /
	     * 8) + 1) * 8;
	     *
	     * losCtr = (((2 * losCache[c].length + 12 - 1) / 8) + 1) * 8L; totalRAM +=
	     * (((losCtr + 12 - 1) / 8) + 1) * 8; }
	     * System.out.println("Total memory used by cache: " + totalRAM);
	     *
	     * System.out.println("FOV Map stored for every cell, booleans or bytes, "+width
	     * +"x"+height+": " + ((((((((((((height + 12 - 1) / 8) + 1) * 8L * width + 12 -
	     * 1) / 8) + 1) * 8L height + 12 - 1) / 8) + 1) * 8L * width + 12 - 1) / 8) + 1)
	     * * 8L);
	     */
	}
    }

    @GwtIncompatible
    protected class RefreshUnit implements Runnable {
	protected double[][] res;

	protected RefreshUnit(final char[][] newMap) {
	    this.res = DungeonUtility.generateResistances(newMap);
	}

	/**
	 * When an object implementing interface <code>Runnable</code> is used to create
	 * a thread, starting the thread causes the object's <code>run</code> method to
	 * be called in that separately executing thread.
	 * <p>
	 * The general contract of the method <code>run</code> is that it may take any
	 * action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override
	public void run() {
	    System.arraycopy(FOVCache.this.cache, 0, FOVCache.this.tmpCache, 0, FOVCache.this.tmpCache.length);
	    short[] needsChange = new short[0];
	    for (int i = 0; i < FOVCache.this.width; i++) {
		for (int j = 0; j < FOVCache.this.height; j++) {
		    if (FOVCache.this.resMap[i][j] != this.res[i][j]) {
			needsChange = CoordPacker.unionPacked(needsChange,
				FOVCache.this.losCache[i + j * FOVCache.this.width]);
		    }
		}
	    }
	    needsChange = CoordPacker.differencePacked(needsChange, FOVCache.this.wallMap);
	    FOVCache.this.resMap = this.res;
	    final Coord[] changingCoords = CoordPacker.allPacked(needsChange);
	    final List<LOSUnit> losUnits = new ArrayList<>(changingCoords.length);
	    final List<FOVUnit> fovUnits = new ArrayList<>(changingCoords.length);
	    final List<SymmetryUnit> symUnits = new ArrayList<>(changingCoords.length);
	    for (int i = 0, idx; i < changingCoords.length; i++) {
		idx = changingCoords[i].x + changingCoords[i].y * FOVCache.this.width;
		losUnits.add(new LOSUnit(idx));
		fovUnits.add(new FOVUnit(idx));
		symUnits.add(new SymmetryUnit(idx));
	    }
	    try {
		final List<Future<Long>> invoke = FOVCache.this.executor.invokeAll(losUnits);
		for (final Future<Long> future : invoke) {
		    // threadTime +=
		    future.get();
		    // System.out.println(t);
		}
	    } catch (InterruptedException | ExecutionException e) {
		e.printStackTrace();
	    }
	    try {
		final List<Future<Long>> invoke = FOVCache.this.executor.invokeAll(fovUnits);
		for (final Future<Long> future : invoke) {
		    // threadTime +=
		    future.get();
		    // System.out.println(t);
		}
	    } catch (InterruptedException | ExecutionException e) {
		e.printStackTrace();
	    }
	    try {
		final List<Future<Long>> invoke = FOVCache.this.executor.invokeAll(symUnits);
		for (final Future<Long> future : invoke) {
		    // threadTime +=
		    future.get();
		    // System.out.println(t);
		}
	    } catch (InterruptedException | ExecutionException e) {
		e.printStackTrace();
	    }
	    FOVCache.this.cache = FOVCache.this.tmpCache;
	    FOVCache.this.refreshComplete = true;
	}
    }

    @GwtIncompatible
    protected class FOVUnit implements Callable<Long> {
	protected int index;

	public FOVUnit(final int index) {
	    this.index = index;
	}

	/**
	 * Computes a result, or throws an exception if unable to do so.
	 *
	 * @return computed result
	 * @throws Exception if unable to compute a result
	 */
	@Override
	public Long call() throws Exception {
	    return FOVCache.this.storeCellFOV(this.index);
	}
    }

    @GwtIncompatible
    protected class LOSUnit implements Callable<Long> {
	protected int index;

	public LOSUnit(final int index) {
	    this.index = index;
	}

	/**
	 * Computes a result, or throws an exception if unable to do so.
	 *
	 * @return computed result
	 * @throws Exception if unable to compute a result
	 */
	@Override
	public Long call() throws Exception {
	    return FOVCache.this.storeCellLOS(this.index);
	}
    }

    @GwtIncompatible
    protected class SymmetryUnit implements Callable<Long> {
	protected int index;

	public SymmetryUnit(final int index) {
	    this.index = index;
	}

	/**
	 * Computes a result, or throws an exception if unable to do so.
	 *
	 * @return computed result
	 * @throws Exception if unable to compute a result
	 */
	@Override
	public Long call() throws Exception {
	    return FOVCache.this.storeCellSymmetry(this.index);
	}
    }

    /**
     * Shuts down any threads that may prevent the game from closing properly. It is
     * recommended you call this at the end of the program to avoid threads
     * lingering too long. You don't have to do anything special after you call
     * this, other than not using this FOVCache any more.
     */
    public void destroy() {
	this.performanceThread.interrupt();
	this.qualityThread.interrupt();
	this.executor.shutdown();
    }
}
