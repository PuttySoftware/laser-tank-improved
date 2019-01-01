package squidpony.squidmath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;

import squidpony.ArrayTools;

/**
 * An ordered multi-directional map-like data structure with two sets of keys
 * and one list of values.
 *
 * This is structured so that the two keys, of types A and B, are always
 * associated with each other and the same value, of type Q, but may have their
 * index in the ordering change. You can look up a B key with an A key or index,
 * an A key with a B key or index, and a Q value with an A key, B key, or index.
 * Created by Tommy Ettinger on 10/27/2016.
 */
public class K2V1<A, B, Q> {
    K2<A, B> keys;
    ArrayList<Q> values;

    /**
     * Constructs an empty K2V1 with the default parameters: 32 expected indices and
     * a load factor of 0.5f.
     */
    public K2V1() {
	this(32, 0.5f);
    }

    /**
     * Constructs an empty K2V1 that can hold {@code expected} indices before
     * resizing and has a load factor of 0.5f.
     *
     * @param expected the expected number of items of any single type that this
     *                 will hold; each put counts as one item
     */
    public K2V1(final int expected) {
	this(expected, 0.5f);
    }

    /**
     * Constructs an empty K2V1 that can hold {@code expected} indices before
     * resizing and has load factor {@code f}.
     *
     * @param expected the expected number of items of any single type that this
     *                 will hold; each put counts as one item
     * @param f        the load factor; must be greater than 0 and less than 1, but
     *                 should ideally be between 0.2 and 0.8
     */
    public K2V1(final int expected, final float f) {
	this.keys = new K2<>(expected, f);
	this.values = new ArrayList<>(expected);
    }

    /**
     * Constructs a K2V1 from an A iterable, a B iterable, and a Q iterable, where
     * the A and B items should be unique (if they aren't, each item that would be
     * associated with a duplicate A or B will be skipped). The K2V1 this constructs
     * will only take enough items from all Iterables to exhaust the shortest
     * Iterable, so the lengths can be different between arguments. If there are no
     * duplicate A keys or duplicate B keys (e.g. you can have an A key that is
     * equal to a B key, but not to another A key), then all items will be used in
     * the order the Iterable parameters provide them; otherwise the keys and value
     * that would be associated with a duplicate are skipped.
     *
     * @param aKeys   an Iterable of A keys; if null will be considered empty and
     *                this K2V1 will be empty
     * @param bKeys   an Iterable of B keys; if null will be considered empty and
     *                this K2V1 will be empty
     * @param qValues an Iterable of Q values; if null will be considered empty and
     *                this K2V1 will be empty
     */
    public K2V1(final Iterable<A> aKeys, final Iterable<B> bKeys, final Iterable<Q> qValues) {
	this(32, 0.5f);
	this.putAll(aKeys, bKeys, qValues);
    }

    /**
     * Constructs a K2V1 from another K2V1 with ths same A, B, and Q types. This
     * will have an expected size equal to the current size of other, use the same
     * load factor (f) as other, and will have the same items put into it in the
     * same order.
     *
     * @param other a K2V1 to copy into this; must have the same A, B, and Q types
     */
    public K2V1(final K2V1<A, B, Q> other) {
	this(other == null ? 32 : other.size(), other == null ? 0.5f : other.keys.keysA.f);
	this.putAll(other);
    }

    /**
     * Returns true if this contains the A, key, in its collection of A items, or
     * false otherwise.
     *
     * @param key the A to check the presence of
     * @return true if key is present in this; false otherwise
     */
    public boolean containsA(final A key) {
	return this.keys.containsA(key);
    }

    /**
     * Returns true if this contains the B, key, in its collection of B items, or
     * false otherwise.
     *
     * @param key the B to check the presence of
     * @return true if key is present in this; false otherwise
     */
    public boolean containsB(final B key) {
	return this.keys.containsB(key);
    }

    /**
     * Returns true if this contains at least one Q value, or false otherwise
     *
     * @param value the value to check
     * @return true if value is present at least once in this collection's Q
     *         collection
     */
    public boolean containsQ(final Q value) {
	return this.values.contains(value);
    }

    /**
     * Returns true if index is between 0 (inclusive) and {@link #size()}
     * (exclusive), or false otherwise.
     *
     * @param index the index to check
     * @return true if index is a valid index in the ordering of this K2V1
     */
    public boolean containsIndex(final int index) {
	return this.keys.containsIndex(index);
    }

    /**
     * Given an A object key, finds the position in the ordering which that A has,
     * or -1 if key is not present. Unlike {@link java.util.List#indexOf(Object)},
     * this runs in constant time.
     *
     * @param key the A to find the position of
     * @return the int index of key in the ordering, or -1 if it is not present
     */
    public int indexOfA(final Object key) {
	return this.keys.indexOfA(key);
    }

    /**
     * Given a B object key, finds the position in the ordering which that B has, or
     * -1 if key is not present. Unlike {@link java.util.List#indexOf(Object)}, this
     * runs in constant time.
     *
     * @param key the B to find the position of
     * @return the int index of key in the ordering, or -1 if it is not present
     */
    public int indexOfB(final Object key) {
	return this.keys.indexOfB(key);
    }

    /**
     * Given a Q value, finds the first position in the ordering which contains that
     * value, or -1 if not present. Runs in linear time normally, since this uses
     * {@link ArrayList#indexOf(Object)} to search the values.
     *
     * @param value the value to find the position of, which should be a Q
     * @return the first int index of value in the ordering, or -1 if it is not
     *         present
     */
    public int indexOfQ(final Object value) {
	// noinspection SuspiciousMethodCalls
	return this.values.indexOf(value);
    }

    /**
     * Given an int index, finds the associated A key (using index as a point in the
     * ordering).
     *
     * @param index an int index into this K2V1
     * @return the A object with index for its position in the ordering, or null if
     *         index was invalid
     */
    public A getAAt(final int index) {
	return this.keys.getAAt(index);
    }

    /**
     * Given an int index, finds the associated B key (using index as a point in the
     * ordering).
     *
     * @param index an int index into this K2V1
     * @return the B object with index for its position in the ordering, or null if
     *         index was invalid
     */
    public B getBAt(final int index) {
	return this.keys.getBAt(index);
    }

    /**
     * Given an int index, finds the associated Q value (using index as a point in
     * the ordering).
     *
     * @param index an int index into this K2V1
     * @return the Q value with index for its position in the ordering, or null if
     *         index was invalid
     */
    public Q getQAt(final int index) {
	if (index < 0 || index >= this.keys.keysA.size) {
	    return null;
	}
	return this.values.get(index);
    }

    /**
     * Given an A object, finds the associated B object (it will be at the same
     * point in the ordering).
     *
     * @param key an A object to use as a key
     * @return the B object associated with key, or null if key was not present
     */
    public B getBFromA(final Object key) {
	return this.keys.getBFromA(key);
    }

    /**
     * Given a B object, finds the associated A object (it will be at the same point
     * in the ordering).
     *
     * @param key a B object to use as a key
     * @return the A object associated with key, or null if key was not present
     */
    public A getAFromB(final Object key) {
	return this.keys.getAFromB(key);
    }

    /**
     * Given an A object, finds the associated Q value (it will be at the same point
     * in the ordering).
     *
     * @param key an A object to use as a key
     * @return the Q value associated with key, or null if key was not present
     */
    public Q getQFromA(final Object key) {
	final int idx = this.keys.indexOfA(key);
	if (idx >= 0) {
	    return this.values.get(idx);
	}
	return null;
    }

    /**
     * Given a B object, finds the associated Q value (it will be at the same point
     * in the ordering).
     *
     * @param key a B object to use as a key
     * @return the Q value associated with key, or null if key was not present
     */
    public Q getQFromB(final Object key) {
	final int idx = this.keys.indexOfB(key);
	if (idx >= 0) {
	    return this.values.get(idx);
	}
	return null;
    }

    /**
     * Gets a random A from this K2V1 using the given RNG.
     *
     * @param random generates a random index to get an A with
     * @return a randomly chosen A, or null if this is empty
     */
    public A randomA(final RNG random) {
	return this.keys.randomA(random);
    }

    /**
     * Gets a random B from this K2V1 using the given RNG.
     *
     * @param random generates a random index to get a B with
     * @return a randomly chosen B, or null if this is empty
     */
    public B randomB(final RNG random) {
	return this.keys.randomB(random);
    }

    /**
     * Gets a random Q from this K2V1 using the given RNG.
     *
     * @param random generates a random index to get a Q with
     * @return a randomly chosen Q, or null if this is empty
     */
    public Q randomQ(final RNG random) {
	if (random == null || this.values.isEmpty()) {
	    return null;
	}
	return this.values.get(random.nextIntHasty(this.values.size()));
    }

    /**
     * Changes an existing A key, {@code past}, to another A key, {@code future}, if
     * past exists in this K2V1 and future does not yet exist in this K2V1. This
     * will retain past's point in the ordering for future, so the associated other
     * key(s) will still be associated in the same way.
     *
     * @param past   an A key, that must exist in this K2V1's A keys, and will be
     *               changed
     * @param future an A key, that cannot currently exist in this K2V1's A keys,
     *               but will if this succeeds
     * @return this for chaining
     */
    public K2V1<A, B, Q> alterA(final A past, final A future) {
	this.keys.alterA(past, future);
	return this;
    }

    /**
     * Changes an existing B key, {@code past}, to another B key, {@code future}, if
     * past exists in this K2V1 and future does not yet exist in this K2V1. This
     * will retain past's point in the ordering for future, so the associated other
     * key(s) will still be associated in the same way.
     *
     * @param past   a B key, that must exist in this K2V1's B keys, and will be
     *               changed
     * @param future a B key, that cannot currently exist in this K2V1's B keys, but
     *               will if this succeeds
     * @return this for chaining
     */
    public K2V1<A, B, Q> alterB(final B past, final B future) {
	this.keys.alterB(past, future);
	return this;
    }

    /**
     * Changes the A key at {@code index} to another A key, {@code future}, if index
     * is valid and future does not yet exist in this K2V1. The position in the
     * ordering for future will be the same as index, and the same as the key this
     * replaced, if this succeeds, so the other key(s) at that position will still
     * be associated in the same way.
     *
     * @param index  a position in the ordering to change; must be at least 0 and
     *               less than {@link #size()}
     * @param future an A key, that cannot currently exist in this K2V1's A keys,
     *               but will if this succeeds
     * @return this for chaining
     */
    public K2V1<A, B, Q> alterAAt(final int index, final A future) {
	this.keys.alterAAt(index, future);
	return this;
    }

    /**
     * Changes the B key at {@code index} to another B key, {@code future}, if index
     * is valid and future does not yet exist in this K2V1. The position in the
     * ordering for future will be the same as index, and the same as the key this
     * replaced, if this succeeds, so the other key(s) at that position will still
     * be associated in the same way.
     *
     * @param index  a position in the ordering to change; must be at least 0 and
     *               less than {@link #size()}
     * @param future a B key, that cannot currently exist in this K2V1's B keys, but
     *               will if this succeeds
     * @return this for chaining
     */
    public K2V1<A, B, Q> alterBAt(final int index, final B future) {
	this.keys.alterBAt(index, future);
	return this;
    }

    /**
     * Changes the Q value at {@code index} to another Q value, {@code future}, if
     * index is valid. The position in the ordering for future will be the same as
     * index, and the same as the key this replaced, if this succeeds, so the keys
     * at that position will still be associated in the same way.
     *
     * @param index  a position in the ordering to change; must be at least 0 and
     *               less than {@link #size()}
     * @param future a Q value that will be set at the given index if this succeeds
     * @return this for chaining
     */
    public K2V1<A, B, Q> alterQAt(final int index, final Q future) {
	this.values.set(index, future);
	return this;
    }

    /**
     * Adds an A key, a B key, and a Q value at the same point in the ordering (the
     * end) to this K2V1. Neither aKey nor bKey can be present in this collection
     * before this is called. If you want to change or update an existing key, use
     * {@link #alterA(Object, Object)} or {@link #alterB(Object, Object)}. The
     * value, qValue, has no restrictions.
     *
     * @param aKey   an A key to add; cannot already be present
     * @param bKey   a B key to add; cannot already be present
     * @param qValue a Q value to add; can be present any number of times
     * @return true if this collection changed as a result of this call
     */
    public boolean put(final A aKey, final B bKey, final Q qValue) {
	if (this.keys.put(aKey, bKey)) {
	    this.values.add(qValue);
	    return true;
	}
	return false;
    }

    /**
     * Adds an A key, a B key, and a Q value at the given index in the ordering to
     * this K2V1. Neither a nor b can be present in this collection before this is
     * called. If you want to change or update an existing key, use
     * {@link #alterA(Object, Object)} or {@link #alterB(Object, Object)}. The
     * value, q, has no restrictions. The index this is given should be at least 0
     * and no greater than {@link #size()}.
     *
     * @param index the point in the ordering to place a and b into; later entries
     *              will be shifted forward
     * @param a     an A key to add; cannot already be present
     * @param b     a B key to add; cannot already be present
     * @param q     a Q value to add; can be present any number of times
     * @return true if this collection changed as a result of this call
     */
    public boolean putAt(final int index, final A a, final B b, final Q q) {
	if (this.keys.putAt(index, a, b)) {
	    this.values.add(index, q);
	    return true;
	}
	return false;
    }

    public boolean putAll(final Iterable<A> aKeys, final Iterable<B> bKeys, final Iterable<Q> qValues) {
	if (aKeys == null || bKeys == null || qValues == null) {
	    return false;
	}
	final Iterator<A> aIt = aKeys.iterator();
	final Iterator<B> bIt = bKeys.iterator();
	final Iterator<Q> qIt = qValues.iterator();
	boolean changed = false;
	while (aIt.hasNext() && bIt.hasNext() && qIt.hasNext()) {
	    changed = this.put(aIt.next(), bIt.next(), qIt.next()) || changed;
	}
	return changed;
    }

    /**
     * Puts all unique A and B keys in {@code other} into this K2V1, respecting
     * other's ordering. If an A or a B in other is already present when this would
     * add one, this will not put the A and B keys at that point in the iteration
     * order, and will place the next unique A and B it finds in the arguments at
     * that position instead.
     *
     * @param other another K2V1 collection with the same A and B types
     * @return true if this collection changed as a result of this call
     */
    public boolean putAll(final K2V1<A, B, Q> other) {
	if (other == null) {
	    return false;
	}
	boolean changed = false;
	final int sz = other.size();
	for (int i = 0; i < sz; i++) {
	    changed = this.put(other.getAAt(i), other.getBAt(i), other.getQAt(i)) || changed;
	}
	return changed;
    }

    /**
     * Removes a given A key, if {@code removing} exists in this K2V1's A keys, and
     * also removes any B key or Q value associated with its point in the ordering.
     *
     * @param removing the A key to remove
     * @return this for chaining
     */
    public K2V1<A, B, Q> removeA(final A removing) {
	final int i = this.keys.keysA.removeInt(removing);
	if (i >= 0) {
	    this.keys.keysB.removeAt(i);
	    this.values.remove(i);
	}
	return this;
    }

    /**
     * Removes a given B key, if {@code removing} exists in this K2V1's B keys, and
     * also removes any A key or Q value associated with its point in the ordering.
     *
     * @param removing the B key to remove
     * @return this for chaining
     */
    public K2V1<A, B, Q> removeB(final B removing) {
	final int i = this.keys.keysB.removeInt(removing);
	if (i >= 0) {
	    this.keys.keysA.removeAt(i);
	    this.values.remove(i);
	}
	return this;
    }

    /**
     * Removes a given point in the ordering, if {@code index} is at least 0 and
     * less than {@link #size()}.
     *
     * @param index the position in the ordering to remove
     * @return this for chaining
     */
    public K2V1<A, B, Q> removeAt(final int index) {
	if (index >= 0 && index < this.keys.keysA.size) {
	    this.keys.removeAt(index);
	    this.values.remove(index);
	}
	return this;
    }

    /**
     * Reorders this K2V1 using {@code ordering}, which have the same length as this
     * K2V1's {@link #size()} and can be generated with
     * {@link ArrayTools#range(int)} (which, if applied, would produce no change to
     * the current ordering), {@link RNG#randomOrdering(int)} (which gives a random
     * ordering, and if applied immediately would be the same as calling
     * {@link #shuffle(RNG)}), or made in some other way. If you already have an
     * ordering and want to make a different ordering that can undo the change, you
     * can use {@link ArrayTools#invertOrdering(int[])} called on the original
     * ordering.
     *
     * @param ordering an int array or vararg that should contain each int from 0 to
     *                 {@link #size()} (or less)
     * @return this for chaining
     */
    public K2V1<A, B, Q> reorder(final int... ordering) {
	this.keys.reorder(ordering);
	ArrayTools.reorder(this.values, ordering);
	return this;
    }

    /**
     * Generates a random ordering with rng and applies the same ordering to all
     * keys and values this has; they will maintain their current association to
     * other keys and values but their ordering/indices will change.
     *
     * @param rng an RNG to produce the random ordering this will use
     * @return this for chaining
     */
    public K2V1<A, B, Q> shuffle(final RNG rng) {
	final int[] ordering = rng.randomOrdering(this.keys.keysA.size);
	this.keys.reorder(ordering);
	ArrayTools.reorder(this.values, ordering);
	return this;
    }

    /**
     * Creates a new iterator over the A keys this holds. This can be problematic
     * for garbage collection if called very frequently; it may be better to access
     * items by index (which also lets you access other keys associated with that
     * index) using {@link #getAAt(int)} in a for(int i=0...) loop.
     *
     * @return a newly-created iterator over this K2V1's A keys
     */
    public Iterator<A> iteratorA() {
	return this.keys.iteratorA();
    }

    /**
     * Creates a new iterator over the B keys this holds. This can be problematic
     * for garbage collection if called very frequently; it may be better to access
     * items by index (which also lets you access other keys associated with that
     * index) using {@link #getBAt(int)} in a for(int i=0...) loop.
     *
     * @return a newly-created iterator over this K2V1's B keys
     */
    public Iterator<B> iteratorB() {
	return this.keys.iteratorB();
    }

    /**
     * Creates a new iterator over the Q values this holds. This can be problematic
     * for garbage collection if called very frequently; it may be better to access
     * values by index (which also lets you access other keys associated with that
     * index) using {@link #getQAt(int)} in a for(int i=0...) loop.
     *
     * @return a newly-created iterator over this K2V1's Q values
     */
    public Iterator<Q> iteratorQ() {
	return this.values.iterator();
    }

    /**
     * Gets and caches the A keys as a Collection that implements SortedSet (and so
     * also implements Set). It retains the current ordering.
     *
     * @return the A keys as a SortedSet
     */
    public SortedSet<A> getSetA() {
	return this.keys.getSetA();
    }

    /**
     * Gets and caches the B keys as a Collection that implements SortedSet (and so
     * also implements Set). It retains the current ordering.
     *
     * @return the B keys as a SortedSet
     */
    public SortedSet<B> getSetB() {
	return this.keys.getSetB();
    }

    /**
     * Gets the Q values as a freshly-copied ArrayList of Q; unlike
     * {@link #getSetA()} or {@link #getSetB()}, this does not cache the value list.
     * It retains the current ordering.
     *
     * @return the Q values as an ArrayList
     */
    public ArrayList<Q> getListQ() {
	return new ArrayList<>(this.values);
    }

    /**
     * Gets the size of this collection. That's the number of A keys, which is
     * always the same as the number of B keys, which is always the same as the
     * number of indices, which is also always the same as the size of the values
     * List.
     *
     * @return the current number of indices in this K2V1, which can be thought of
     *         as the number of A keys
     */
    public int size() {
	return this.keys.keysA.size;
    }

    /**
     * I think you can guess what this does.
     *
     * @return true if there are no items in this K2V1; false if there are items
     *         present
     */
    public boolean isEmpty() {
	return this.values.isEmpty();
    }

    public int keyCount() {
	return 2;
    }

    public int valueCount() {
	return 1;
    }
}
