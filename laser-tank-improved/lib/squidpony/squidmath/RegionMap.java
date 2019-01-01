package squidpony.squidmath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * A small extension of OrderedMap that specifically handles {@code short[]}
 * regions as produced by {@link CoordPacker}. The methods
 * {@link #allAt(int, int)}, {@link #containsRegion(short[])}, and
 * {@link #regionsContaining(int, int)} are added here, and the minor extra work
 * needed to handle array keys in OrderedMap is taken care of automatically.
 * {@link #toString()} also produces nicer output by default for this usage,
 * with the keys printed in a usable way. Created by Tommy Ettinger on
 * 11/24/2016.
 */
public class RegionMap<V> extends OrderedMap<short[], V> implements Serializable {
    private static final long serialVersionUID = 2L;

    public RegionMap(final int expected, final float f) {
	super(expected, f);
	this.hasher = CrossHash.shortHasher;
    }

    /**
     * Creates a new RegionMap with 0.75f as load factor.
     *
     * @param expected the expected number of elements in the RegionMap.
     */
    public RegionMap(final int expected) {
	this(expected, OrderedMap.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new RegionMap with initial expected 16 entries and 0.75f as load
     * factor.
     */
    public RegionMap() {
	this(OrderedMap.DEFAULT_INITIAL_SIZE, OrderedMap.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new RegionMap copying a given one.
     *
     * @param m a {@link Map} to be copied into the new RegionMap.
     * @param f the load factor.
     */
    public RegionMap(final Map<short[], ? extends V> m, final float f) {
	this(m.size(), f);
	this.putAll(m);
    }

    /**
     * Creates a new RegionMap with 0.75f as load factor copying a given one.
     *
     * @param m a {@link Map} to be copied into the new RegionMap.
     */
    public RegionMap(final Map<short[], ? extends V> m) {
	this(m, OrderedMap.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a new RegionMap using the elements of two parallel arrays.
     *
     * @param keyArray   the array of keys of the new RegionMap.
     * @param valueArray the array of corresponding values in the new RegionMap.
     * @param f          the load factor.
     * @throws IllegalArgumentException if <code>k</code> and <code>v</code> have
     *                                  different lengths.
     */
    public RegionMap(final short[][] keyArray, final V[] valueArray, final float f) {
	this(keyArray.length, f);
	if (keyArray.length != valueArray.length) {
	    throw new IllegalArgumentException("The key array and the value array have different lengths ("
		    + keyArray.length + " and " + valueArray.length + ")");
	}
	for (int i = 0; i < keyArray.length; i++) {
	    this.put(keyArray[i], valueArray[i]);
	}
    }

    /**
     * Creates a new RegionMap using the elements of two parallel arrays.
     *
     * @param keyColl   the collection of keys of the new RegionMap.
     * @param valueColl the collection of corresponding values in the new RegionMap.
     * @param f         the load factor.
     * @throws IllegalArgumentException if <code>k</code> and <code>v</code> have
     *                                  different lengths.
     */
    public RegionMap(final Collection<short[]> keyColl, final Collection<V> valueColl, final float f) {
	this(keyColl.size(), f);
	if (keyColl.size() != valueColl.size()) {
	    throw new IllegalArgumentException("The key array and the value array have different lengths ("
		    + keyColl.size() + " and " + valueColl.size() + ")");
	}
	final Iterator<short[]> ki = keyColl.iterator();
	final Iterator<V> vi = valueColl.iterator();
	while (ki.hasNext() && vi.hasNext()) {
	    this.put(ki.next(), vi.next());
	}
    }

    /**
     * Creates a new RegionMap with 0.75f as load factor using the elements of two
     * parallel arrays.
     *
     * @param keyArray   the array of keys of the new RegionMap.
     * @param valueArray the array of corresponding values in the new RegionMap.
     * @throws IllegalArgumentException if <code>k</code> and <code>v</code> have
     *                                  different lengths.
     */
    public RegionMap(final short[][] keyArray, final V[] valueArray) {
	this(keyArray, valueArray, OrderedMap.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Gets a List of all values associated with regions containing a given x,y
     * point.
     *
     * @param x the x coordinate of the point in question
     * @param y the y coordinate of the point in question
     * @return an ArrayList of all V values corresponding to regions containing the
     *         given x,y point.
     */
    public ArrayList<V> allAt(final int x, final int y) {
	final ArrayList<V> found = new ArrayList<>(this.size);
	final OrderedSet<short[]> regions = CoordPacker.findManyPacked(x, y, this.keySet());
	final int count = regions.size;
	for (int i = 0; i < count; i++) {
	    found.add(this.get(regions.getAt(i)));
	}
	return found;
    }

    /**
     * Checks if a region, stored as packed data (possibly from CoordPacker or this
     * class) overlaps with regions stored in this object as keys. Returns true if
     * there is any overlap, false otherwise
     *
     * @param region the packed region to check for overlap with regions this stores
     *               values for
     * @return true if the region overlaps at all, false otherwise
     */
    public boolean containsRegion(final short[] region) {
	return CoordPacker.regionsContain(region, this.keySet());
    }

    /**
     * Gets a List of all regions containing a given x,y point.
     *
     * @param x the x coordinate of the point in question
     * @param y the y coordinate of the point in question
     * @return an ArrayList of all regions in this data structure containing the
     *         given x,y point.
     */
    public OrderedSet<short[]> regionsContaining(final int x, final int y) {
	return CoordPacker.findManyPacked(x, y, this.keySet());
    }

    public String toString(final String separator) {
	return this.toString(separator, false);
    }

    @Override
    public String toString() {
	return this.toString(", ", true);
    }

    private String toString(final String separator, final boolean braces) {
	if (this.size == 0) {
	    return braces ? "{}" : "";
	}
	final StringBuilder buffer = new StringBuilder(32);
	if (braces) {
	    buffer.append('{');
	}
	final short[][] keyTable = this.key;
	final V[] valueTable = this.value;
	int i = keyTable.length;
	while (i-- > 0) {
	    final short[] key = keyTable[i];
	    if (key == null) {
		continue;
	    }
	    buffer.append("Packed Region:");
	    buffer.append(CoordPacker.encodeASCII(key));
	    buffer.append('=');
	    buffer.append(valueTable[i]);
	    break;
	}
	while (i-- > 0) {
	    final short[] key = keyTable[i];
	    if (key == null) {
		continue;
	    }
	    buffer.append(separator);
	    buffer.append("Packed Region:");
	    buffer.append(CoordPacker.encodeASCII(key));
	    buffer.append('=');
	    buffer.append(valueTable[i]);
	}
	if (braces) {
	    buffer.append('}');
	}
	return buffer.toString();
    }
}