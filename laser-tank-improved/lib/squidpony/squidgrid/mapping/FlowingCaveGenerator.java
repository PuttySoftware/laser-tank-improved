package squidpony.squidgrid.mapping;

import java.util.ArrayList;

import squidpony.squidgrid.mapping.styled.DungeonBoneGen;
import squidpony.squidgrid.mapping.styled.TilesetType;
import squidpony.squidmath.CellularAutomaton;
import squidpony.squidmath.GreasedRegion;
import squidpony.squidmath.RNG;

/**
 * An IDungeonGenerator that distorts and smooths an ordinary dungeon map to
 * make it appear like a cave complex. This usually exhibits the complex
 * connectivity that dungeons made with a {@link TilesetType} like
 * {@link TilesetType#DEFAULT_DUNGEON} have, but shouldn't have noticeable
 * room/corridor areas, and should appear as all one cave. <br>
 * An example map this can produce: <br>
 *
 * <pre>
 *   ┌─────┐ ┌───────┬─┐ ┌───┐                ┌────┐   ┌────┐   ┌────┐    ┌──┐                  ┌───┐   ┌─────┐
 *  ┌┘.....└─┘.......│.└─┘...└─┐    ┌─┐    ┌──┘....│   │....└──┬┘....└┬───┘..└──┐  ┌───┐       ┌┘...│  ┌┘.....└┐
 *  │..........................└┐ ┌─┘.└┐   │.......└┐  │.......│......│.........└─┬┘...│  ┌───┬┘....│  │.......│
 *  └┐.........┌┐...............└─┘....└┐ ┌┘........│ ┌┘....│.....................│...─┤┌─┘...│.....│  │.......│
 *   │........┌┘│.......................└─┘.........└┐│.....│.....┌┐...................││...........│  │.......│
 *   │........│┌┘...............................#....││.....│.....│└┐..................└┘...........│  │.......└┐
 *   │.......┌┼┘.....┌┐............┌─┐..............┌┘│.....├─┐...│ │...............................│  │........│
 *   │......┌┘│....┌─┘└┐.........┌─┘ └──────┐.......│ │....┌┘ │...└─┤......#.......│................│  └┐.......│
 *   │.....┌┘ └┐.┌─┘   └┐.......┌┘          │......┌┘ │...┌┘  └─┐...│....│........┌┤........│.......│  ┌┘......┌┘
 *   └┐...─┤   └─┘      │......─┤  ┌──┐     │......│  │...│    ┌┘.......─┘.......┌┘└─┐....─┬┤.......└┐ │.......│
 *   ┌┘....└┐  ┌─┐  ┌───┘.......└──┘..└┐    └┐.....│ ┌┘...└┐ ┌─┘.................│  ┌┘.....│└┐.......└─┘.......└┐
 *  ┌┘......└──┘.└┐┌┘......─┐..........└─┐  ┌┘.....└─┘.....│┌┘...................│ ┌┘......│ │..................│
 * ┌┘.............└┘........│............└┬─┘..│...........└┘.....│.........───..└┬┘.......│ │....┌┐............│
 * │........................│......┌─┐....│....├┐...............──┘...............│........└─┘....│└┐...........│
 * │...┌─┐........................┌┘ └┐.......┌┘└┐................................................│ └┐..........│
 * │..┌┘ └┐.....................┌─┘  ┌┘.......│  │...............................................┌┘  │..........│
 * └──┘   └┐...................┌┘   ┌┘........│  └─┐...│...─┐...........│........................│   └┐.........│
 *         │..................┌┘    │.........│ ┌─┐└───┴┐...│...........├─┐........│.............└─┐  │.........│
 *     ┌─┐ └──┐.....┌──┐......│    ┌┘...┌───┐.└─┘.└┐    └┐..├┐..........│ └┐......┌┤...............│  └─┐.......│
 *     │.└─┐  └┐...┌┘  └┐.....└┐   │....│  ┌┘......└┐    │..│└───┐.....┌┘  └┐....┌┘├─.....┌──┐.....│    │......┌┘
 *     │...└┐  └┐..│   ┌┘......└─┬─┘...─┤  │........└┐ ┌─┘.─┤    │.....│    │....│ │......└┐ └┐....│    └┐....─┤
 *     └┐...│  ┌┘..│   │.........│......│ ┌┘.........└─┘....└─┐ ┌┘.....│   ┌┘....├─┘.......└┐ │....│     │.....│
 *      └┐..│ ┌┘...│   │................│┌┘...................│ │......└───┘.....│..........│ │....└─┐   │...┌─┘
 *       │..├─┘...─┤  ┌┘................├┘....................└─┘................│..........└┬┘......└─┐┌┘...│
 *       ├─.│......│ ┌┘..<........│...#.│...........................┌┬─......................│.........└┘...┌┘
 *       │..│......│┌┘............│................................─┴┘......................................│
 *      ┌┘......┌──┘│....................┌─┐.....................................┌┐.........................│
 *      │....┌──┘   └─┐..................│ └─┐.┌─┐....┌─┐....┌─┐.........┌─┐.....│└─┐......┌─┐....┌──┐......│
 *      │...┌┘        └─┐...........┌────┘   └─┘┌┴─┬──┘ └────┘ │........┌┘ │....┌┘  │.....┌┘ └────┘  └──┐...│
 *      │..┌┘           └┐.....┌─┐..└┐          │..│   ┌──┐    └┐......┌┘ ┌┘....└┐  └┐....│    ┌────┐   │...└┐
 *     ┌┘..└┐ ┌─────┐   ┌┘....┌┘ └┐..└┐        ┌┘..│ ┌─┘..└─┐   └┐.....│  │......└───┘....└┐  ┌┘....└┐  └┐...│
 *    ┌┘....└─┘.....└┐ ┌┘.....└┐ ┌┘...└┐       │...│┌┘......│    │.....│  └┐..........#....│ ┌┘......└┐  │...│
 *   ┌┘..........│...└┬┘.......└┐│.....└─┐   ┌─┘...││.......├─┐  │.....│   │...............└─┘........└┐ └┐..└┐
 *  ┌┘.......┌───┴┐...│.........││.......│  ┌┘....┌┘│.......│.└──┼┬─...│  ┌┘...........................└┐ │...│
 * ┌┘.......┌┘    │.............└┘.......└┐ │...┌─┘ │...│.#......└┘....│ ┌┘........................│....└─┴─..└┐
 * │........│    ┌┘.......................│ │..┌┘   └┐..│..............└─┘......──.................│...........│
 * │........│   ┌┘..........┌─┐..........┌┘ │..│     └┐.............................┌─┐........................└┐
 * │........│  ┌┘.....┌─────┘ └──┐.......│  │..│      │.........................┌───┘ ├─...┌────┐...............│
 * │........│  │.....┌┘          └┐.....┌┘  └┐.└┐     └┐......┌─┬─┐....┌──┐.....│    ┌┘...┌┘    │....┌─┐........│
 * └┐......┌┘  │.....│    ┌────┐  │.....│   ┌┘..└┐     └┐....┌┘┌┘.│....│  └┐....│   ┌┘....│     │....│ │........│
 *  │....┌─┘   │.....└┐ ┌─┘....│  └┐...─┤  ┌┘....│      └┐...└┬┘.......│   │....└┐ ┌┘.....│  ┌──┘....└┬┘........│
 *  │....└─┐   └┐.....└─┘......└┐  │....└──┘.....└─┐  ┌─┬┘....│........└┐ ┌┘.....└┐│......└──┘........│.........│
 *  └┐.....└┐ ┌─┴─..............│ ┌┘...............└──┘.│...............│┌┘.......└┘......................┌┐....│
 *   │......└─┘.................└┐│.....................................└┘...............................─┤└┐...│
 *  ┌┘.........│.................└┘.........................│................┌──┐.#.#.....................│ └┐.┌┘
 *  │..........│..........................................──┼──┐.....#......┌┘  │....................│....└┐ ├─┤
 *  └┐.........│.....................┌─┐....................│  └─┐..........│   └─┐................#.├─┐...└─┘.└┐
 *   │....┌┐.........┌─┐.........│..┌┘ └─┐..................│    │...┌───┐..└┐    │..................│ │........│
 *   └┐...│└─┐.....┌─┘ └┐......┌─┴┐.└┐   └─┐..........│.....│    │...└┐  └┐..└─┐  └─┐....┌──┐.......┌┘ └┐.......│
 *   ┌┘..┌┘  └┐...┌┘    └┐.....│  │..│ ┌─┐ └┐.......┌─┤....┌┘    └┐...└─┬─┘....└─┐  └┐...└┐ └┐......│   │......┌┘
 *   │...│    │...└───┐  │.....└┐┌┘..│┌┘.└┐ └┐.....┌┘ │....│      ├─....│........│   └┐...└┐ └┐.....└┐ ┌┘......│
 *  ┌┘..┌┘   ┌┘.......│  │......└┘...││...└┐ │.....└┐┌┘....└┐┌────┘.........┌─┐..└┐   └┐...└┐ └┐.....└─┘.......└┐
 * ┌┘..┌┘    │........│  └┐..........└┘....└─┘......└┘......└┘..............│ └┐..└┐   │....│ ┌┘................│
 * │...│    ┌┘....┌───┘   └┐...............................................─┤  └┐..└┐  └┐...└─┤.................│
 * │..┌┘   ┌┘....┌┘        │..........................................┌┐....│   │...└┐  │.....│.................│
 * │.┌┘   ┌┘.....└┐        │..........................................│└─┐.┌┘  ┌┘....└─┐│.......................│
 * │.└┐  ┌┘.......└───┐   ┌┘.......┌┐......┌─┐..................│.....│  └─┘   │.......└┘.....>.........#.......│
 * │..└┬─┘............│┌──┘......┌─┘└┐.....│ └┐..─┐.............│.....│        │.............┌─┐...............┌┘
 * │...│..┌──┐........││........┌┘   └┐...┌┘  └┐..├───┐..............┌┘        │..┌─┐......┌─┘ ├─....┌┐........│
 * │......│  └┐......┌┘└┐......┌┘     └───┘    └──┘   └┐....┌──┐....┌┘       ┌─┴─┬┘ │.....┌┘   │....┌┘└┐......┌┘
 * └─┐....│   ├─.....└┐ │......└┐     ┌──┐             │....│  └┐..┌┘       ┌┘...└┐ │.....│   ┌┘....│  └┐.....│
 *   ├─..┌┘  ┌┘.......│ └┐......└┐   ┌┘..│             └┐...│   └──┘       ┌┘.....└─┘.....└─┐┌┘.....└┐  │.....│
 * ┌─┘...└┐ ┌┘........└┐ │.......└───┘...│              │..┌┘              │................└┘.......│  └┐....└─┐
 * │......└─┘..........│ └─┐.............└─┬───┐       ┌┘.┌┘               │.........................└┐ ┌┘......│
 * │...................└┐  └───┐...........│...└───────┘..└┐               │.......┌─┐................│ │.......│
 * └┐...................│      │..........┌┴┐..............└┐              ├─......│ └┐...............└─┘.......│
 *  └┐.#................│      │..........│ └┐..............└┐         ┌───┘.....┌─┘  │.........................│
 *   └┐...............┌─┘      │.........┌┘  └┐.....┌───┐....│         │.........│    │......│....┌──────┐.....┌┘
 *    └┐.┌───┐........│        └───┐....┌┘    └─┐.┌─┘   └─┐.┌┘         └┐......┌─┘    └─┐...┌┴────┘      └─┐..┌┘
 *     └─┘   └────────┘            └────┘       └─┘       └─┘           └──────┘        └───┘              └──┘
 * </pre>
 *
 * Created by Tommy Ettinger on 8/18/2017.
 */
public class FlowingCaveGenerator implements IDungeonGenerator {
    public DungeonBoneGen gen;
    public int width;
    public int height;
    public TilesetType type;
    public RNG rng;
    protected CellularAutomaton ca;

    /**
     * Default constructor that makes a 60x60 cave map with a random seed.
     */
    public FlowingCaveGenerator() {
	this(60, 60);
    }

    /**
     * Makes a cave map with the specified dimensions and a random seed.
     *
     * @param width  the width of the dungeon map(s) to generate
     * @param height the height of the dungeon map(s) to generate
     */
    public FlowingCaveGenerator(final int width, final int height) {
	this.width = Math.max(3, width);
	this.height = Math.max(3, height);
	this.type = TilesetType.DEFAULT_DUNGEON;
	this.rng = new RNG();
	this.gen = new DungeonBoneGen(this.rng);
	this.ca = new CellularAutomaton(this.width, this.height);
    }

    /**
     *
     * @param width  the width of the dungeon map(s) to generate
     * @param height the height of the dungeon map(s) to generate
     * @param type   a TilesetType enum value; {@link TilesetType#DEFAULT_DUNGEON}
     *               is used if null or unspecified
     * @param rng    a random number generator to use when generating the caves; if
     *               null this will use a default RNG
     */
    public FlowingCaveGenerator(final int width, final int height, final TilesetType type, final RNG rng) {
	this.width = Math.max(3, width);
	this.height = Math.max(3, height);
	this.type = type == null ? TilesetType.DEFAULT_DUNGEON : type;
	this.rng = rng == null ? new RNG() : rng;
	this.gen = new DungeonBoneGen(this.rng);
	this.ca = new CellularAutomaton(this.width, this.height);
    }

    /**
     * Generates a dungeon or other map as a 2D char array. Any implementation may
     * allow its own configuration and customization of how dungeons are generated,
     * but each must provide this as a sane default. Thus uses the convention of '#'
     * representing a wall and '.' representing a bare floor.
     *
     * @return a 2D char array representing a cave system with '#' for walls and '.'
     *         for floors
     */
    @Override
    public char[][] generate() {
	return this.generate(this.type);
    }

    /**
     * Generates a flowing cave dungeon with a different {@link TilesetType} than
     * this generator was made with. The default type is
     * {@link TilesetType#DEFAULT_DUNGEON} if unspecified in the constructor.
     *
     * @param type a TilesetType enum value
     * @return a 2D char array for the cave system
     */
    public char[][] generate(final TilesetType type) {
	this.gen.generate(type, this.width, this.height);
	this.ca.remake(this.gen.region);
	this.gen.region.and(this.ca.runBasicSmoothing()).deteriorate(this.rng, 0.9);
	this.gen.region.and(this.ca.runBasicSmoothing()).deteriorate(this.rng, 0.9);
	this.ca.current.remake(this.gen.region.deteriorate(this.rng, 0.9));
	this.gen.region.or(this.ca.runBasicSmoothing());
	this.gen.region.remake(this.gen.region.removeEdges().largestPart());
	return this.gen.region.intoChars(this.gen.getDungeon(), '.', '#');
    }

    /**
     * Generates a flowing cave dungeon with a different {@link TilesetType} than
     * this generator was made with, and specifying a chance to keep the original
     * walls of rooms before the flowing smoothing step is performed.
     * {@code roomChance} can be between 0.0 and 1.0, and if a room (identified with
     * a similar technique to {@link RoomFinder}, but not using it directly) is
     * randomly selected to be preserved (the probability per room is roomChance),
     * then most of its walls will be kept in-place, generally with more right
     * angles than the caves will have. It may be best to keep roomChance above 0.5
     * if you want the effect to be noticeable. Starting with
     * {@link TilesetType#DEFAULT_DUNGEON} is a good choice for {@code type}.
     *
     * @param type       a TilesetType enum value
     * @param roomChance the chance, from 0.0 to 1.0, to preserve each room, keeping
     *                   its walls where they start
     * @return a 2D char array for the cave system
     */
    public char[][] generate(final TilesetType type, final double roomChance) {
	this.gen.generate(type, this.width, this.height);
	final ArrayList<GreasedRegion> rooms = this.gen.region.copy().retract8way().flood8way(this.gen.region, 1)
		.split();
	this.ca.remake(this.gen.region);
	this.gen.region.and(this.ca.runBasicSmoothing()).deteriorate(this.rng, 0.9);
	this.gen.region.and(this.ca.runBasicSmoothing()).deteriorate(this.rng, 0.9);
	this.ca.current.remake(this.gen.region.deteriorate(this.rng, 0.9));
	this.gen.region.or(this.ca.runBasicSmoothing());
	for (int i = 0; i < rooms.size(); i++) {
	    if (this.rng.nextDouble() < roomChance) {
		this.gen.region.andNot(rooms.get(i).fringe8way().deteriorate(this.rng, 0.81));
	    }
	}
	this.gen.region.remake(this.gen.region.removeEdges());
	this.gen.region.insertSeveral(DungeonUtility
		.ensurePath(this.gen.region.intoChars(this.gen.getDungeon(), '.', '#'), this.rng, '.', '#'));
	return this.gen.region.largestPart().intoChars(this.gen.getDungeon(), '.', '#');
    }

    /**
     * Gets the most recently-produced dungeon as a 2D char array, usually produced
     * by calling {@link #generate()} or some similar method present in a specific
     * implementation. This normally passes a direct reference and not a copy, so
     * you can normally modify the returned array to propagate changes back into
     * this IDungeonGenerator.
     *
     * @return the most recently-produced dungeon/map as a 2D char array
     */
    @Override
    public char[][] getDungeon() {
	return this.gen.getDungeon();
    }
}
