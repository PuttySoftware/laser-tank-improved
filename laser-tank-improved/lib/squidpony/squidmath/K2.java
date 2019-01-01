package squidpony.squidmath;

import java.util.Iterator;
import java.util.SortedSet;

import squidpony.ArrayTools;
import squidpony.annotation.Beta;

/**
 * An ordered bidirectional map-like data structure, with unique A keys and
 * unique B keys updated together like a map that can be queried by A keys, B
 * keys, or int indices. Does not implement any interfaces that you would expect
 * for a data structure, because almost every method it has needs to specify
 * whether it applies to A or B items, but you can get collections that
 * implement SortedSet of its A or B keys. <br>
 * Called K2 because it has 2 key sets; other collections can have other keys or
 * have values, like K2V1. Created by Tommy Ettinger on 10/25/2016.
 */
@Beta
public class K2<A, B> {
    Arrangement<A> keysA;
    Arrangement<B> keysB;

    /**
     * Constructs an empty K2.
     */
    public K2() {
	this(Arrangement.DEFAULT_INITIAL_SIZE, Arrangement.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a K2 with the expected number of indices to hold (the number of A
     * and number of B items is always the same, and this will be more efficient if
     * expected is greater than that number).
     *
     * @param expected
     */
    public K2(final int expected) {
	this(expected, Arrangement.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a K2 with the expected number of indices to hold (the number of A
     * and number of B items is always the same, and this will be more efficient if
     * expected is greater than that number) and the load factor to use, between
     * 0.1f and 0.8f usually (using load factors higher than 0.8f can cause
     * problems).
     *
     * @param expected the amount of indices (the count of A items is the same as
     *                 the count of B items) this should hold
     * @param f        the load factor, probably between 0.1f and 0.8f
     */
    public K2(final int expected, final float f) {
	this.keysA = new Arrangement<>(expected, f);
	this.keysB = new Arrangement<>(expected, f);
    }

    /**
     * Constructs a K2 with the expected number of indices to hold (the number of A
     * and number of B items are always equal, and this will be more efficient if
     * expected is greater than that number), the load factor to use, between 0.1f
     * and 0.8f usually (using load factors higher than 0.8f can cause problems),
     * and two IHasher implementations, such as {@link CrossHash#generalHasher},
     * that will be used to hash and compare for equality with A keys and B keys,
     * respectively. Specifying an IHasher is usually needed if your keys are arrays
     * (there are existing implementations for 1D arrays of all primitive types,
     * CharSequence, and Object in CrossHash), or if you want hashing by identity
     * and reference equality (which would use {@link CrossHash#identityHasher}, and
     * might be useful if keys are mutable). Other options are possible with custom
     * IHashers, like hashing Strings but ignoring, or only considering, a certain
     * character for the hash and equality checks.
     *
     * @param expected the amount of indices (the count of A items is the same as
     *                 the count of B items) this should hold
     * @param f        the load factor, probably between 0.1f and 0.8f
     * @param hasherA  an implementation of CrossHash.IHasher meant for A keys
     * @param hasherB  an implementation of CrossHash.IHasher meant for B keys
     */
    public K2(final int expected, final float f, final CrossHash.IHasher hasherA, final CrossHash.IHasher hasherB) {
	this.keysA = new Arrangement<>(expected, f, hasherA);
	this.keysB = new Arrangement<>(expected, f, hasherB);
    }

    /**
     * Constructs a K2 from a pair of Iterables that will be processed in pairs,
     * adding a unique A from aKeys if and only if it can also add a unique B from
     * bKeys, otherwise skipping that pair.
     *
     * @param aKeys an Iterable of A that should all be unique
     * @param bKeys an Iterable of B that should all be unique
     */
    public K2(final Iterable<? extends A> aKeys, final Iterable<? extends B> bKeys) {
	this.keysA = new Arrangement<>();
	this.keysB = new Arrangement<>();
	if (aKeys != null && bKeys != null) {
	    this.putAll(aKeys, bKeys);
	}
    }

    public K2(final K2<? extends A, ? extends B> other) {
	if (other == null) {
	    this.keysA = new Arrangement<>();
	    this.keysB = new Arrangement<>();
	} else {
	    this.keysA = new Arrangement<>(other.keysA);
	    this.keysB = new Arrangement<>(other.keysB);
	}
    }

    public K2(final Arrangement<A> aItems, final Arrangement<B> bItems) {
	if (aItems == null || bItems == null) {
	    this.keysA = new Arrangement<>();
	    this.keysB = new Arrangement<>();
	} else {
	    final int amt = Math.min(aItems.size, bItems.size);
	    this.keysA = aItems.take(amt);
	    this.keysB = bItems.take(amt);
	}
    }

    /**
     * Returns true if this contains the A, key, in its collection of A items.
     *
     * @param key the A to check the presence of
     * @return true if key is present in this; false otherwise
     */
    public boolean containsA(final A key) {
	return this.keysA.containsKey(key);
    }

    /**
     * Returns true if this contains the B, key, in its collection of B items.
     *
     * @param key the B to check the presence of
     * @return true if key is present in this; false otherwise
     */
    public boolean containsB(final B key) {
	return this.keysB.containsKey(key);
    }

    /**
     * Returns true if index is between 0 (inclusive) and {@link #size()}
     * (exclusive), or false otherwise.
     *
     * @param index the index to check
     * @return true if index is a valid index in the ordering of this K2
     */
    public boolean containsIndex(final int index) {
	return this.keysA.containsValue(index);
    }

    /**
     * Given an A object key, finds the position in the ordering that A has, or -1
     * if key is not present. Unlike {@link java.util.List#indexOf(Object)}, this
     * runs in constant time.
     *
     * @param key the A to find the position of
     * @return the int index of key in the ordering, or -1 if it is not present
     */
    public int indexOfA(final Object key) {
	return this.keysA.getInt(key);
    }

    /**
     * Given a B object key, finds the position in the ordering that B has, or -1 if
     * key is not present. Unlike {@link java.util.List#indexOf(Object)}, this runs
     * in constant time.
     *
     * @param key the B to find the position of
     * @return the int index of key in the ordering, or -1 if it is not present
     */
    public int indexOfB(final Object key) {
	return this.keysB.getInt(key);
    }

    /**
     * Given an int index, finds the associated A key (using index as a point in the
     * ordering).
     *
     * @param index an int index into this K2
     * @return the A object with index for its position in the ordering, or null if
     *         index was invalid
     */
    public A getAAt(final int index) {
	return this.keysA.keyAt(index);
    }

    /**
     * Given an int index, finds the associated B key (using index as a point in the
     * ordering).
     *
     * @param index an int index into this K2
     * @return the B object with index for its position in the ordering, or null if
     *         index was invalid
     */
    public B getBAt(final int index) {
	return this.keysB.keyAt(index);
    }

    /**
     * Given an A object, finds the associated B object (it will be at the same
     * point in the ordering).
     *
     * @param key an A object to use as a key
     * @return the B object associated with key, or null if key was not present
     */
    public B getBFromA(final Object key) {
	return this.keysB.keyAt(this.keysA.getInt(key));
    }

    /**
     * Given a B object, finds the associated A object (it will be at the same point
     * in the ordering).
     *
     * @param key a B object to use as a key
     * @return the A object associated with key, or null if key was not present
     */
    public A getAFromB(final Object key) {
	return this.keysA.keyAt(this.keysB.getInt(key));
    }

    /**
     * Gets a random A from this K2 using the given RNG.
     *
     * @param random generates a random index to get an A with
     * @return a randomly chosen A, or null if this is empty
     */
    public A randomA(final RNG random) {
	return this.keysA.randomKey(random);
    }

    /**
     * Gets a random B from this K2 using the given RNG.
     *
     * @param random generates a random index to get a B with
     * @return a randomly chosen B, or null if this is empty
     */
    public B randomB(final RNG random) {
	return this.keysB.randomKey(random);
    }

    /**
     * Changes an existing A key, {@code past}, to another A key, {@code future}, if
     * past exists in this K2 and future does not yet exist in this K2. This will
     * retain past's point in the ordering for future, so the associated other
     * key(s) will still be associated in the same way.
     *
     * @param past   an A key, that must exist in this K2's A keys, and will be
     *               changed
     * @param future an A key, that cannot currently exist in this K2's A keys, but
     *               will if this succeeds
     * @return this for chaining
     */
    public K2<A, B> alterA(final A past, final A future) {
	if (this.keysA.containsKey(past) && !this.keysA.containsKey(future)) {
	    this.keysA.alter(past, future);
	}
	return this;
    }

    /**
     * Changes an existing B key, {@code past}, to another B key, {@code future}, if
     * past exists in this K2 and future does not yet exist in this K2. This will
     * retain past's point in the ordering for future, so the associated other
     * key(s) will still be associated in the same way.
     *
     * @param past   a B key, that must exist in this K2's B keys, and will be
     *               changed
     * @param future a B key, that cannot currently exist in this K2's B keys, but
     *               will if this succeeds
     * @return this for chaining
     */
    public K2<A, B> alterB(final B past, final B future) {
	if (this.keysB.containsKey(past) && !this.keysB.containsKey(future)) {
	    this.keysB.alter(past, future);
	}
	return this;
    }

    /**
     * Changes the A key at {@code index} to another A key, {@code future}, if index
     * is valid and future does not yet exist in this K2. The position in the
     * ordering for future will be the same as index, and the same as the key this
     * replaced, if this succeeds, so the other key(s) at that position will still
     * be associated in the same way.
     *
     * @param index  a position in the ordering to change; must be at least 0 and
     *               less than {@link #size()}
     * @param future an A key, that cannot currently exist in this K2's A keys, but
     *               will if this succeeds
     * @return this for chaining
     */
    public K2<A, B> alterAAt(final int index, final A future) {
	if (!this.keysA.containsKey(future) && index >= 0 && index < this.keysA.size) {
	    this.keysA.alter(this.keysA.keyAt(index), future);
	}
	return this;
    }

    /**
     * Changes the B key at {@code index} to another B key, {@code future}, if index
     * is valid and future does not yet exist in this K2. The position in the
     * ordering for future will be the same as index, and the same as the key this
     * replaced, if this succeeds, so the other key(s) at that position will still
     * be associated in the same way.
     *
     * @param index  a position in the ordering to change; must be at least 0 and
     *               less than {@link #size()}
     * @param future a B key, that cannot currently exist in this K2's B keys, but
     *               will if this succeeds
     * @return this for chaining
     */
    public K2<A, B> alterBAt(final int index, final B future) {
	if (!this.keysB.containsKey(future) && index >= 0 && index < this.keysB.size) {
	    this.keysB.alter(this.keysB.keyAt(index), future);
	}
	return this;
    }

    /**
     * Adds an A key and a B key at the same point in the ordering (the end) to this
     * K2. Neither parameter can be present in this collection before this is
     * called. If you want to change or update an existing key, use
     * {@link #alterA(Object, Object)} or {@link #alterB(Object, Object)}.
     *
     * @param a an A key to add; cannot already be present
     * @param b a B key to add; cannot already be present
     * @return true if this collection changed as a result of this call
     */
    public boolean put(final A a, final B b) {
	if (!this.keysA.containsKey(a) && !this.keysB.containsKey(b)) {
	    this.keysA.add(a);
	    this.keysB.add(b);
	    return true;
	}
	return false;
    }

    /**
     * Puts all unique A and B keys in {@code aKeys} and {@code bKeys} into this K2
     * at the end. If an A in aKeys or a B in bKeys is already present when this
     * would add one, this will not put the A and B keys at that point in the
     * iteration order, and will place the next unique A and B it finds in the
     * arguments at that position instead.
     *
     * @param aKeys an Iterable or Collection of A keys to add; should all be unique
     *              (like a Set)
     * @param bKeys an Iterable or Collection of B keys to add; should all be unique
     *              (like a Set)
     * @return true if this collection changed as a result of this call
     */
    public boolean putAll(final Iterable<? extends A> aKeys, final Iterable<? extends B> bKeys) {
	if (aKeys == null || bKeys == null) {
	    return false;
	}
	final Iterator<? extends A> aIt = aKeys.iterator();
	final Iterator<? extends B> bIt = bKeys.iterator();
	boolean changed = false;
	while (aIt.hasNext() && bIt.hasNext()) {
	    changed = this.put(aIt.next(), bIt.next()) || changed;
	}
	return changed;
    }

    /**
     * Puts all unique A and B keys in {@code other} into this K2, respecting
     * other's ordering. If an A or a B in other is already present when this would
     * add one, this will not put the A and B keys at that point in the iteration
     * order, and will place the next unique A and B it finds in the arguments at
     * that position instead.
     *
     * @param other another K2 collection with the same A and B types
     * @return true if this collection changed as a result of this call
     */
    public boolean putAll(final K2<? extends A, ? extends B> other) {
	if (other == null) {
	    return false;
	}
	boolean changed = false;
	final int sz = other.size();
	for (int i = 0; i < sz; i++) {
	    changed = this.put(other.getAAt(i), other.getBAt(i)) || changed;
	}
	return changed;
    }

    /**
     * Adds an A key and a B key at the given index in the ordering to this K2.
     * Neither a nor b can be present in this collection before this is called. If
     * you want to change or update an existing key, use
     * {@link #alterA(Object, Object)} or {@link #alterB(Object, Object)}. The index
     * this is given should be at least 0 and no greater than {@link #size()}.
     *
     * @param index the point in the ordering to place a and b into; later entries
     *              will be shifted forward
     * @param a     an A key to add; cannot already be present
     * @param b     a B key to add; cannot already be present
     * @return true if this collection changed as a result of this call
     */
    public boolean putAt(final int index, final A a, final B b) {
	if (!this.keysA.containsKey(a) && !this.keysB.containsKey(b)) {
	    this.keysA.addAt(index, a);
	    this.keysB.addAt(index, b);
	    return true;
	}
	return false;
    }

    /**
     * Removes a given A key, if {@code removing} exists in this K2's A keys, and
     * also removes any keys associated with its point in the ordering.
     *
     * @param removing the A key to remove
     * @return this for chaining
     */
    public K2<A, B> removeA(final A removing) {
	this.keysB.removeAt(this.keysA.removeInt(removing));
	return this;
    }

    /**
     * Removes a given B key, if {@code removing} exists in this K2's B keys, and
     * also removes any keys associated with its point in the ordering.
     *
     * @param removing the B key to remove
     * @return this for chaining
     */
    public K2<A, B> removeB(final B removing) {
	this.keysA.removeAt(this.keysB.removeInt(removing));
	return this;
    }

    /**
     * Removes a given point in the ordering, if {@code index} is at least 0 and
     * less than {@link #size()}.
     *
     * @param index the position in the ordering to remove
     * @return this for chaining
     */
    public K2<A, B> removeAt(final int index) {
	this.keysA.removeAt(index);
	this.keysB.removeAt(index);
	return this;
    }

    /**
     * Reorders this K2 using {@code ordering}, which have the same length as this
     * K2's {@link #size()} and can be generated with {@link ArrayTools#range(int)}
     * (which, if applied, would produce no change to the current ordering),
     * {@link RNG#randomOrdering(int)} (which gives a random ordering, and if
     * applied immediately would be the same as calling {@link #shuffle(RNG)}), or
     * made in some other way. If you already have an ordering and want to make a
     * different ordering that can undo the change, you can use
     * {@link ArrayTools#invertOrdering(int[])} called on the original ordering.
     *
     * @param ordering an int array or vararg that should contain each int from 0 to
     *                 {@link #size()} (or less)
     * @return this for chaining
     */
    public K2<A, B> reorder(final int... ordering) {
	this.keysA.reorder(ordering);
	this.keysB.reorder(ordering);
	return this;
    }

    /**
     * Generates a random ordering with rng and applies the same ordering to all
     * kinds of keys this has; they will maintain their current association to other
     * keys but their ordering/indices will change.
     *
     * @param rng an RNG to produce the random ordering this will use
     * @return this for chaining
     */
    public K2<A, B> shuffle(final RNG rng) {
	final int[] ordering = rng.randomOrdering(this.keysA.size);
	this.keysA.reorder(ordering);
	this.keysB.reorder(ordering);
	return this;
    }

    /**
     * Creates a new iterator over the A keys this holds. This can be problematic
     * for garbage collection if called very frequently; it may be better to access
     * items by index (which also lets you access other keys associated with that
     * index) using {@link #getAAt(int)} in a for(int i=0...) loop.
     *
     * @return a newly-created iterator over this K2's A keys
     */
    public Iterator<A> iteratorA() {
	return this.keysA.iterator();
    }

    /**
     * Creates a new iterator over the B keys this holds. This can be problematic
     * for garbage collection if called very frequently; it may be better to access
     * items by index (which also lets you access other keys associated with that
     * index) using {@link #getBAt(int)} in a for(int i=0...) loop.
     *
     * @return a newly-created iterator over this K2's B keys
     */
    public Iterator<B> iteratorB() {
	return this.keysB.iterator();
    }

    /**
     * Gets and caches the A keys as a Collection that implements SortedSet (and so
     * also implements Set).
     *
     * @return the A keys as a SortedSet
     */
    public SortedSet<A> getSetA() {
	return this.keysA.keySet();
    }

    /**
     * Gets and caches the B keys as a Collection that implements SortedSet (and so
     * also implements Set).
     *
     * @return the B keys as a SortedSet
     */
    public SortedSet<B> getSetB() {
	return this.keysB.keySet();
    }

    /**
     * To be called sparingly, since this allocates a new OrderedSet instead of
     * reusing one.
     *
     * @return the A keys as an OrderedSet
     */
    public OrderedSet<A> getOrderedSetA() {
	return this.keysA.keysAsOrderedSet();
    }

    /**
     * To be called sparingly, since this allocates a new OrderedSet instead of
     * reusing one.
     *
     * @return the B keys as an OrderedSet
     */
    public OrderedSet<B> getOrderedSetB() {
	return this.keysB.keysAsOrderedSet();
    }

    public int keyCount() {
	return 2;
    }

    public int valueCount() {
	return 0;
    }

    public int size() {
	return this.keysA.size;
    }

    public boolean isEmpty() {
	return this.keysA.isEmpty();
    }
}