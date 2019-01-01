package squidpony.squidgrid.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.iterator.SquidIterators;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * Rectangles in 2D grids. Checkout {@link Utils} for utility methods.
 *
 * @author smelC
 * @see RectangleRoomFinder How to find rectangles in a dungeon
 */
public interface Rectangle extends Zone {
    /**
     * @return The bottom left coordinate of the room.
     */
    Coord getBottomLeft();

    @Override
    /**
     * @return The room's width (from {@link #getBottomLeft()). It is greater or
     *         equal than 0.
     */
    int getWidth();

    @Override
    /**
     * @return The room's height (from {@link #getBottomLeft()). It is greater or
     *         equal than 0.
     */
    int getHeight();

    /**
     * Utilities pertaining to {@link Rectangle}
     *
     * @author smelC
     */
    class Utils {
	/**
	 * A comparator that uses {@link #size(Rectangle)} as the measure.
	 */
	public static final Comparator<Rectangle> SIZE_COMPARATOR = new Comparator<>() {
	    @Override
	    public int compare(final Rectangle o1, final Rectangle o2) {
		return Integer.compare(Utils.size(o1), Utils.size(o2));
	    }
	};

	/**
	 * @param r a Rectangle
	 * @param c a Coord to check against r for presence
	 * @return Whether {@code r} contains {@code c}.
	 */
	public static boolean contains(final Rectangle r, final Coord c) {
	    return c != null && Utils.contains(r, c.x, c.y);
	}

	/**
	 * @param r a Rectangle
	 * @param x x-coordinate of a point to check against r
	 * @param y y-coordinate of a point to check against r
	 * @return Whether {@code r} contains {@code c}.
	 */
	public static boolean contains(final Rectangle r, final int x, final int y) {
	    if (r == null) {
		return false;
	    }
	    final Coord bottomLeft = r.getBottomLeft();
	    final int width = r.getWidth();
	    final int height = r.getHeight();
	    return !(x < bottomLeft.x /* Too much to the left */
		    || bottomLeft.x + width < x /* Too much to the right */
		    || bottomLeft.y < y /* Too low */
		    || y < bottomLeft.y - height); /* Too high */
	}

	/**
	 * @param r  a Rectangle
	 * @param cs a Collection of Coord to check against r; returns true if r
	 *           contains any items in cs
	 * @return {@code true} if {@code r} contains a member of {@code cs}.
	 */
	public static boolean containsAny(final Rectangle r, final Collection<Coord> cs) {
	    for (final Coord c : cs) {
		if (Utils.contains(r, c)) {
		    return true;
		}
	    }
	    return false;
	}

	/**
	 * @param rs an Iterable of Rectangle items to check against c
	 * @param c  a Coord to try to find in any of the Rectangles in rs
	 * @return {@code true} if a member of {@code rs}
	 *         {@link #contains(Rectangle, Coord) contains} {@code c}.
	 */
	public static boolean contains(final Iterable<? extends Rectangle> rs, final Coord c) {
	    for (final Rectangle r : rs) {
		if (Utils.contains(r, c)) {
		    return true;
		}
	    }
	    return false;
	}

	/**
	 * @param r a Rectangle
	 * @return The number of cells that {@code r} covers.
	 */
	public static int size(final Rectangle r) {
	    return r.getWidth() * r.getHeight();
	}

	/**
	 * @param r a Rectangle
	 * @return The center of {@code r}.
	 */
	public static Coord center(final Rectangle r) {
	    final Coord bl = r.getBottomLeft();
	    /*
	     * bl.y - ... : because we're in SquidLib coordinates (0,0) is top left
	     */
	    return Coord.get(bl.x + Math.round(r.getWidth() / 2), bl.y - Math.round(r.getHeight() / 2));
	}

	/**
	 * Use {@link #cellsList(Rectangle)} if you want them all.
	 *
	 * @param r a Rectangle
	 * @return The cells that {@code r} contains, from bottom left to top right;
	 *         lazily computed.
	 */
	public static Iterator<Coord> cells(final Rectangle r) {
	    return new SquidIterators.RectangleFromBottomLeftToTopRight(r.getBottomLeft(), r.getWidth(), r.getHeight());
	}

	/**
	 * Use {@link #cells(Rectangle)} if you may stop before the end of the list,
	 * you'll save some memory.
	 *
	 * @param r
	 * @return The cells that {@code r} contains, from bottom left to top right.
	 */
	public static List<Coord> cellsList(final Rectangle r) {
	    /* Allocate it with the right size, to avoid internal resizings */
	    final List<Coord> result = new ArrayList<>(Utils.size(r));
	    final Iterator<Coord> it = Utils.cells(r);
	    while (it.hasNext()) {
		result.add(it.next());
	    }
	    return result;
	}

	/**
	 * @param d A direction.
	 * @return {@code r} extended to {@code d} by one row and/or column.
	 */
	public static Rectangle extend(final Rectangle r, final Direction d) {
	    final Coord bl = r.getBottomLeft();
	    final int width = r.getWidth();
	    final int height = r.getHeight();
	    switch (d) {
	    case DOWN_LEFT:
		return new Rectangle.Impl(bl.translate(Direction.DOWN_LEFT), width + 1, height + 1);
	    case DOWN_RIGHT:
		return new Rectangle.Impl(bl.translate(Direction.DOWN), width + 1, height + 1);
	    case NONE:
		return r;
	    case UP_LEFT:
		return new Rectangle.Impl(bl.translate(Direction.LEFT), width + 1, height + 1);
	    case UP_RIGHT:
		return new Rectangle.Impl(bl, width + 1, height + 1);
	    case DOWN:
		return new Rectangle.Impl(bl.translate(Direction.DOWN), width, height + 1);
	    case LEFT:
		return new Rectangle.Impl(bl.translate(Direction.LEFT), width + 1, height);
	    case RIGHT:
		return new Rectangle.Impl(bl, width + 1, height);
	    case UP:
		return new Rectangle.Impl(bl, width, height + 1);
	    }
	    throw new IllegalStateException("Unmatched direction in Rectangle.Utils::extend: " + d);
	}

	/**
	 * @param r
	 * @param dir
	 * @return The coord at the corner identified by {@code dir} in {@code r}.
	 */
	public static Coord getCorner(final Rectangle r, final Direction dir) {
	    switch (dir) {
	    case DOWN_LEFT:
		return r.getBottomLeft();
	    case DOWN_RIGHT:
		return r.getBottomLeft().translate(r.getWidth() - 1, 0);
	    case UP_LEFT:
		/* -y because in SquidLib higher y is smaller */
		return r.getBottomLeft().translate(0, -(r.getHeight() - 1));
	    case UP_RIGHT:
		/* -y because in SquidLib higher y is smaller */
		return r.getBottomLeft().translate(r.getWidth() - 1, -(r.getHeight() - 1));
	    case NONE:
		return r.getCenter();
	    case DOWN:
	    case LEFT:
	    case RIGHT:
	    case UP:
		final Coord c1 = Utils.getCorner(r, dir.clockwise());
		final Coord c2 = Utils.getCorner(r, dir.counterClockwise());
		return Coord.get((c1.x + c2.x) / 2, (c1.y + c2.y) / 2);
	    }
	    throw new IllegalStateException("Unmatched direction in Rectangle.Utils::getCorner: " + dir);
	}

	/**
	 * @param r
	 * @param buf An array of (at least) size 4, to hold the 4 corners. It is
	 *            returned, except if {@code null} or too small, in which case a
	 *            fresh array is returned.
	 * @return {@code buf}, if it had length of at least 4, or a new 4-element
	 *         array; it contains this Rectangle's 4 corners
	 */
	public static Coord[] getAll4Corners(final Rectangle r, final Coord[] buf) {
	    final Coord[] result = buf == null || buf.length < 4 ? new Coord[4] : buf;
	    result[0] = Utils.getCorner(r, Direction.DOWN_LEFT);
	    result[1] = Utils.getCorner(r, Direction.DOWN_RIGHT);
	    result[2] = Utils.getCorner(r, Direction.UP_RIGHT);
	    result[3] = Utils.getCorner(r, Direction.UP_LEFT);
	    return result;
	}

	/**
	 * Creates a new Rectangle that is smaller than r by 1 cell from each of r's
	 * edges, to a minimum of a 1x1 cell.
	 *
	 * @param r a Rectangle to shrink
	 * @return the shrunken Rectangle, newly-allocated
	 */
	public static Rectangle shrink(final Rectangle r) {
	    return new Rectangle.Impl(r.getBottomLeft().translate(1, 1), Math.max(1, r.getWidth() - 2),
		    Math.max(1, r.getHeight() - 2));
	}

	/**
	 * @param r
	 * @param cardinal
	 * @param buf      The buffer to fill or {@code null} to let this method
	 *                 allocate.
	 * @return The border of {@code r} at the position {@code cardinal}, i.e. the
	 *         lowest line if {@code r} is {@link Direction#DOWN}, the highest line
	 *         if {@code r} is {@link Direction#UP}, the leftest column if {@code r}
	 *         is {@link Direction#LEFT}, and the rightest column if {@code r} is
	 *         {@link Direction#RIGHT}.
	 */
	public static List<Coord> getBorder(final Rectangle r, final Direction cardinal,
		/* @Nullable */ final List<Coord> buf) {
	    Coord start = null;
	    Direction dir = null;
	    int len = -1;
	    switch (cardinal) {
	    case DOWN:
	    case UP:
		len = r.getWidth();
		dir = Direction.RIGHT;
		start = cardinal == Direction.DOWN ? r.getBottomLeft() : Utils.getCorner(r, Direction.UP_LEFT);
		break;
	    case LEFT:
	    case RIGHT:
		len = r.getHeight();
		dir = Direction.UP;
		start = cardinal == Direction.LEFT ? r.getBottomLeft() : Utils.getCorner(r, Direction.DOWN_RIGHT);
		break;
	    case DOWN_LEFT:
	    case DOWN_RIGHT:
	    case NONE:
	    case UP_LEFT:
	    case UP_RIGHT:
		throw new IllegalStateException(
			"Expected a cardinal direction in Rectangle.Utils::getBorder. Received: " + cardinal);
	    }
	    if (start == null || dir == null) {
		throw new IllegalStateException("Unmatched direction in Rectangle.Utils::Border: " + cardinal);
	    }
	    final List<Coord> result = buf == null ? new ArrayList<>(len) : buf;
	    Coord now = start;
	    for (int i = 0; i < len; i++) {
		buf.add(now);
		now = now.translate(dir);
	    }
	    return result;
	}
    }

    /**
     * @author smelC
     */
    class Impl extends Zone.Skeleton implements Rectangle {
	protected final Coord bottomLeft;
	protected final int width;
	protected final int height;
	private static final long serialVersionUID = -6197401003733967116L;

	public Impl(final Coord bottomLeft, final int width, final int height) {
	    this.bottomLeft = bottomLeft;
	    this.width = width;
	    this.height = height;
	}

	@Override
	public Coord getBottomLeft() {
	    return this.bottomLeft;
	}

	@Override
	public int getWidth() {
	    return this.width;
	}

	@Override
	public int getHeight() {
	    return this.height;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + (this.bottomLeft == null ? 0 : this.bottomLeft.hashCode());
	    result = prime * result + this.height;
	    result = prime * result + this.width;
	    return result;
	}

	@Override
	public boolean equals(final Object obj) {
	    if (this == obj) {
		return true;
	    }
	    if (obj == null) {
		return false;
	    }
	    if (this.getClass() != obj.getClass()) {
		return false;
	    }
	    final Impl other = (Impl) obj;
	    if (this.bottomLeft == null) {
		if (other.bottomLeft != null) {
		    return false;
		}
	    } else if (!this.bottomLeft.equals(other.bottomLeft)) {
		return false;
	    }
	    if (this.height != other.height) {
		return false;
	    }
	    if (this.width != other.width) {
		return false;
	    }
	    return true;
	}

	@Override
	public String toString() {
	    return "Room at " + this.bottomLeft + ", width:" + this.width + ", height:" + this.height;
	}

	// Implementation of Zone:
	@Override
	public boolean isEmpty() {
	    return this.width == 0 || this.height == 0;
	}

	@Override
	public int size() {
	    return this.width * this.height;
	}

	@Override
	public boolean contains(final int x, final int y) {
	    return x >= this.bottomLeft.x && x < this.bottomLeft.x + this.width && y <= this.bottomLeft.y
		    && this.bottomLeft.y - this.height < y;
	}

	@Override
	public boolean contains(final Coord c) {
	    return this.contains(c.x, c.y);
	}

	@Override
	public int x(final boolean smallestOrBiggest) {
	    return this.bottomLeft.x + (smallestOrBiggest ? 0 : this.getWidth() - 1);
	}

	@Override
	public int y(final boolean smallestOrBiggest) {
	    return this.bottomLeft.y - (smallestOrBiggest ? this.getHeight() - 1 : 0);
	}

	@Override
	public Coord getCenter() {
	    return Utils.center(this);
	}

	@Override
	public List<Coord> getAll() {
	    return Utils.cellsList(this);
	}

	@Override
	public Zone translate(final int x, final int y) {
	    return new Impl(this.bottomLeft.translate(x, y), this.width, this.height);
	}

	@Override
	public List<Coord> getInternalBorder() {
	    if (this.width <= 1 || this.height <= 1) {
		return this.getAll();
	    }
	    final int expectedSize = this.width + this.height - 1 + this.width - 1 + this.height - 2;
	    final List<Coord> result = new ArrayList<>(expectedSize);
	    Coord current = Utils.getCorner(this, Direction.DOWN_LEFT);
	    for (int i = 0; i < this.width; i++) {
		assert !result.contains(current);
		result.add(current);
		current = current.translate(Direction.RIGHT);
	    }
	    current = Utils.getCorner(this, Direction.UP_LEFT);
	    for (int i = 0; i < this.width; i++) {
		assert !result.contains(current);
		result.add(current);
		current = current.translate(Direction.RIGHT);
	    }
	    current = Utils.getCorner(this, Direction.DOWN_LEFT);
	    /* Stopping at height - 1 to avoid doublons */
	    for (int i = 0; i < this.height - 1; i++) {
		if (0 < i) {
		    /*
		     * To avoid doublons (with the very first value of 'current' atop this method.
		     */
		    assert !result.contains(current);
		    result.add(current);
		}
		current = current.translate(Direction.UP);
	    }
	    current = Utils.getCorner(this, Direction.DOWN_RIGHT);
	    /* Stopping at height - 1 to avoid doublons */
	    for (int i = 0; i < this.height - 1; i++) {
		if (0 < i) {
		    /*
		     * To avoid doublons (with the very first value of 'current' atop this method.
		     */
		    assert !result.contains(current);
		    result.add(current);
		}
		current = current.translate(Direction.UP);
	    }
	    assert result.size() == expectedSize;
	    return result;
	}

	@Override
	public Collection<Coord> getExternalBorder() {
	    final List<Coord> result = new ArrayList<>((this.width + this.height) * 2);
	    for (final Direction dir : Direction.CARDINALS) {
		Utils.getBorder(this, dir, result);
	    }
	    return result;
	}

	@Override
	public Rectangle extend() {
	    return new Rectangle.Impl(this.bottomLeft.translate(Direction.DOWN_LEFT), this.width + 2, this.height + 2);
	}

	@Override
	public Iterator<Coord> iterator() {
	    /* Do not rely on getAll(), to avoid allocating the list */
	    return Rectangle.Utils.cells(this);
	}
    }
}