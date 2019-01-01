package squidpony.panel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import squidpony.annotation.Beta;

/**
 * A {@link String} divided in chunks of different colors. Use the
 * {@link Iterable} interface to get the pieces.
 *
 * @author smelC
 *
 * @param <T> The type of colors;
 */
@Beta
public interface IColoredString<T> extends Iterable<IColoredString.Bucket<T>> {
    /**
     * A convenience alias for {@code append(c, null)}.
     *
     * @param c the char to append
     */
    void append(/* @Nullable */ char c);

    /**
     * Mutates {@code this} by appending {@code c} to it.
     *
     * @param c     The text to append.
     * @param color {@code text}'s color. Or {@code null} to let the panel decide.
     */
    void append(char c, /* @Nullable */T color);

    /**
     * A convenience alias for {@code append(text, null)}.
     *
     * @param text
     */
    void append(/* @Nullable */ String text);

    /**
     * Mutates {@code this} by appending {@code text} to it. Does nothing if
     * {@code text} is {@code null}.
     *
     * @param text  The text to append.
     * @param color {@code text}'s color. Or {@code null} to let the panel decide.
     */
    void append(/* @Nullable */String text, /* @Nullable */T color);

    /**
     * Mutates {@code this} by appending {@code i} to it.
     *
     * @param i     The int to append.
     * @param color {@code text}'s color. Or {@code null} to let the panel decide.
     */
    void appendInt(int i, /* @Nullable */T color);

    /**
     * Mutates {@code this} by appending {@code f} to it.
     *
     * @param f     The float to append.
     * @param color {@code text}'s color. Or {@code null} to let the panel decide.
     */
    void appendFloat(float f, /* @Nullable */T color);

    /**
     * Mutates {@code this} by appending {@code other} to it.
     *
     * @param other
     */
    void append(IColoredString<T> other);

    /**
     * Replace color {@code old} by {@code new_} in all buckets of {@code this}.
     *
     * @param old  The color to replace.
     * @param new_ The replacing color.
     */
    void replaceColor(/* @Nullable */ T old, /* @Nullable */ T new_);

    /**
     * Set {@code color} in all buckets.
     *
     * @param color
     */
    void setColor(/* @Nullable */ T color);

    /**
     * Deletes all content after index {@code len} (if any).
     *
     * @param len
     */
    void setLength(int len);

    /**
     * @return {@code true} if {@link #present()} is {@code ""}.
     */
    boolean isEmpty();

    /**
     * @param width A positive integer
     * @return {@code this} split in pieces that would fit in a display with
     *         {@code width} columns (if all words in {@code this} are smaller or
     *         equal in length to {@code width}, otherwise wrapping will fail for
     *         these words).
     */
    List<IColoredString<T>> wrap(int width);

    /**
     * This method does NOT guarantee that the result's length is {@code width}. It
     * is impossible to do correct justifying if {@code this}'s length is greater
     * than {@code width} or if {@code this} has no space character.
     *
     * @param width
     * @return A variant of {@code this} where spaces have been introduced
     *         in-between words, so that {@code this}'s length is as close as
     *         possible to {@code width}. Or {@code this} itself if unaffected.
     */
    IColoredString<T> justify(int width);

    /**
     * Empties {@code this}.
     */
    void clear();

    /**
     * This method is typically more efficient than {@link #colorAt(int)}.
     *
     * @return The color of the last bucket, if any.
     */
    /* @Nullable */ T lastColor();

    /**
     * @param index
     * @return The color at {@code index}, if any.
     * @throws NoSuchElementException If {@code index} equals or is greater to
     *                                {@link #length()}.
     */
    /* @Nullable */ T colorAt(int index);

    /**
     * @param index
     * @return The character at {@code index}, if any.
     * @throws NoSuchElementException If {@code index} equals or is greater to
     *                                {@link #length()}.
     */
    char charAt(int index);

    /**
     * @return The length of text.
     */
    int length();

    /**
     * @return The text that {@code this} represents.
     */
    String present();

    /**
     * Given some way of converting from a T value to an in-line markup tag, returns
     * a string representation of this IColoredString with in-line markup
     * representing colors.
     *
     * @param markup an IMarkup implementation
     * @return a String with markup inserted inside.
     */
    String presentWithMarkup(IMarkup<T> markup);

    /**
     * A basic implementation of {@link IColoredString}.
     *
     * @author smelC
     *
     * @param <T> The type of colors
     */
    class Impl<T> implements IColoredString<T> {
	protected final LinkedList<Bucket<T>> fragments;

	/**
	 * An empty instance.
	 */
	public Impl() {
	    this.fragments = new LinkedList<>();
	}

	/**
	 * An instance initially containing {@code text} (with {@code color}).
	 *
	 * @param text  The text that {@code this} should contain.
	 * @param color The color of {@code text}.
	 */
	public Impl(final String text, /* @Nullable */final T color) {
	    this();
	    this.append(text, color);
	}

	/**
	 * A static constructor, to avoid having to write {@code <T>} in the caller.
	 *
	 * @return {@code new Impl(s, t)}.
	 */
	public static <T> IColoredString.Impl<T> create() {
	    return new IColoredString.Impl<>("", null);
	}

	/**
	 * A convenience method, equivalent to {@code create(s, null)}.
	 *
	 * @param s
	 * @return {@code create(s, null)}
	 */
	public static <T> IColoredString.Impl<T> create(final String s) {
	    return Impl.create(s, null);
	}

	/**
	 * A static constructor, to avoid having to write {@code <T>} in the caller.
	 *
	 * @return {@code new Impl(s, t)}.
	 */
	public static <T> IColoredString.Impl<T> create(final String s, /* @Nullable */ final T t) {
	    return new IColoredString.Impl<>(s, t);
	}

	public static <T> IColoredString.Impl<T> clone(final IColoredString<T> toClone) {
	    final IColoredString.Impl<T> result = new IColoredString.Impl<>();
	    result.append(toClone);
	    return result;
	}

	/**
	 * @param one
	 * @param two
	 * @return Whether {@code one} represents the same content as {@code two}.
	 */
	/*
	 * Method could be smarter, i.e. return true more often, by doing some
	 * normalization. It is unnecessary if you only create instances of
	 * IColoredString.Impl.
	 */
	public static <T> boolean equals(final IColoredString<T> one, final IColoredString<T> two) {
	    if (one == two) {
		return true;
	    }
	    final Iterator<IColoredString.Bucket<T>> oneIt = one.iterator();
	    final Iterator<IColoredString.Bucket<T>> twoIt = two.iterator();
	    while (true) {
		if (oneIt.hasNext()) {
		    if (twoIt.hasNext()) {
			final Bucket<T> oneb = oneIt.next();
			final Bucket<T> twob = twoIt.next();
			if (!Impl.equals(oneb.getText(), twob.getText())) {
			    return false;
			}
			if (!Impl.equals(oneb.getColor(), twob.getColor())) {
			    return false;
			}
			continue;
		    } else {
			/* 'this' not terminated, but 'other' is. */
			return false;
		    }
		} else {
		    if (twoIt.hasNext()) {
			/* 'this' terminated, but not 'other'. */
			return false;
		    } else {
			/* Both terminated */
			break;
		    }
		}
	    }
	    return true;
	}

	@Override
	public void append(final char c) {
	    this.append(c, null);
	}

	@Override
	public void append(final char c, final T color) {
	    this.append(String.valueOf(c), color);
	}

	@Override
	public void append(final String text) {
	    this.append(text, null);
	}

	@Override
	public void append(final String text, final T color) {
	    if (text == null || text.isEmpty()) {
		return;
	    }
	    if (this.fragments.isEmpty()) {
		this.fragments.add(new Bucket<>(text, color));
	    } else {
		final Bucket<T> last = this.fragments.getLast();
		if (Impl.equals(last.color, color)) {
		    /* Append to the last bucket, to avoid extending the list */
		    final Bucket<T> novel = last.append(text);
		    this.fragments.removeLast();
		    this.fragments.addLast(novel);
		} else {
		    this.fragments.add(new Bucket<>(text, color));
		}
	    }
	}

	@Override
	public void appendInt(final int i, final T color) {
	    this.append(String.valueOf(i), color);
	}

	@Override
	public void appendFloat(final float f, final T color) {
	    final int i = Math.round(f);
	    this.append(i == f ? String.valueOf(i) : String.valueOf(f), color);
	}

	@Override
	/* KISS implementation */
	public void append(final IColoredString<T> other) {
	    for (final IColoredString.Bucket<T> ofragment : other) {
		this.append(ofragment.getText(), ofragment.getColor());
	    }
	}

	@Override
	public void replaceColor(/* @Nullable */ final T old, /* @Nullable */ final T new_) {
	    if (Impl.equals(old, new_)) {
		/* Nothing to do */
		return;
	    }
	    final ListIterator<Bucket<T>> it = this.fragments.listIterator();
	    while (it.hasNext()) {
		final Bucket<T> bucket = it.next();
		if (Impl.equals(bucket.color, old)) {
		    /* Replace */
		    it.remove();
		    it.add(new Bucket<>(bucket.getText(), new_));
		}
		/* else leave untouched */
	    }
	}

	@Override
	public void setColor(final T color) {
	    final ListIterator<Bucket<T>> it = this.fragments.listIterator();
	    while (it.hasNext()) {
		final Bucket<T> next = it.next();
		if (!Impl.equals(color, next.getColor())) {
		    it.remove();
		    it.add(next.setColor(color));
		}
	    }
	}

	public void append(final Bucket<T> bucket) {
	    this.fragments.add(new Bucket<>(bucket.getText(), bucket.getColor()));
	}

	@Override
	public void setLength(final int len) {
	    int l = 0;
	    final ListIterator<IColoredString.Bucket<T>> it = this.fragments.listIterator();
	    while (it.hasNext()) {
		final IColoredString.Bucket<T> next = it.next();
		final String ftext = next.text;
		final int flen = ftext.length();
		final int nextl = l + flen;
		if (nextl < len) {
		    /* Nothing to do */
		    l += flen;
		    continue;
		} else if (nextl == len) {
		    /* Delete all next fragments */
		    while (it.hasNext()) {
			it.next();
			it.remove();
		    }
		    /* We'll exit the outer loop right away */
		} else {
		    assert len < nextl;
		    /* Trim this fragment */
		    final IColoredString.Bucket<T> trimmed = next.setLength(len - l);
		    /* Replace this fragment */
		    it.remove();
		    it.add(trimmed);
		    /* Delete all next fragments */
		    while (it.hasNext()) {
			it.next();
			it.remove();
		    }
		    /* We'll exit the outer loop right away */
		}
	    }
	}

	@Override
	public List<IColoredString<T>> wrap(final int width) {
	    if (width <= 0) {
		/* Really, you should not rely on this behavior */
		System.err.println("Cannot wrap string in empty display");
		final List<IColoredString<T>> result = new LinkedList<>();
		result.add(this);
		return result;
	    }
	    final List<IColoredString<T>> result = new ArrayList<>();
	    if (this.isEmpty()) {
		/*
		 * Catch this case early on, as empty lines are eaten below (see code after the
		 * while). Checking emptyness is cheap anyway.
		 */
		result.add(this);
		return result;
	    }
	    IColoredString<T> current = Impl.create();
	    int curlen = 0;
	    final Iterator<Bucket<T>> it = this.iterator();
	    while (it.hasNext()) {
		final Bucket<T> next = it.next();
		final String bucket = next.getText();
		final String[] split = bucket.split(" ");
		final T color = next.color;
		for (int i = 0; i < split.length; i++) {
		    if (i == split.length - 1 && bucket.endsWith(" ")) {
			/*
			 * Do not loose trailing space that got eaten by 'bucket.split'.
			 */
			split[i] = split[i] + " ";
		    }
		    final String chunk = split[i];
		    final int chunklen = chunk.length();
		    final boolean addLeadingSpace = 0 < curlen && 0 < i;
		    if (curlen + chunklen + (addLeadingSpace ? 1 : 0) <= width) {
			if (addLeadingSpace) {
			    /*
			     * Do not forget space on which chunk got split. If the space is offscreen, it's
			     * harmless, hence not checking it.
			     */
			    current.append(' ', null);
			    curlen++;
			}
			/* Can add it */
			current.append(chunk, color);
			/* Extend size */
			curlen += chunklen;
		    } else {
			/* Need to wrap */
			/* Flush content so far */
			if (!current.isEmpty()) {
			    result.add(current);
			}
			/*
			 * else: line was prepared, but did not contain anything
			 */
			if (chunklen <= width) {
			    curlen = chunklen;
			    current = Impl.create();
			    current.append(chunk, color);
			    /* Reinit size */
			    curlen = chunklen;
			} else {
			    /*
			     * This word is too long. Adding it and preparing a new line immediately.
			     */
			    /* Add */
			    result.add(new Impl<>(chunk, color));
			    /* Prepare for next rolls */
			    current = Impl.create();
			    /* Reinit size */
			    curlen = 0;
			}
		    }
		}
	    }
	    if (!current.isEmpty()) {
		/* Flush rest */
		result.add(current);
	    }
	    return result;
	}

	@Override
	/*
	 * smelC: not the cutest result (we should add spaces both from the left and the
	 * right, instead of just from the left), but better than nothing.
	 */
	public IColoredString<T> justify(final int width) {
	    final int length = this.length();
	    if (width <= length) {
		/*
		 * If width==length, we're good. If width<length, we cannot adjust
		 */
		return this;
	    }
	    int totalDiff = width - length;
	    assert 0 < totalDiff;
	    if (width <= totalDiff * 3) {
		/* Too much of a difference, it would look very weird. */
		return this;
	    }
	    final IColoredString.Impl<T> result = Impl.create();
	    ListIterator<IColoredString.Bucket<T>> it = this.fragments.listIterator();
	    final int nbb = this.fragments.size();
	    final int[] bucketToNbSpaces = new int[nbb];
	    /* The number of buckets that can contribute to justifying */
	    int totalNbSpaces = 0;
	    /* The index of the last bucket that has spaces */
	    int lastHopeIndex = -1;
	    {
		int i = 0;
		while (it.hasNext()) {
		    final Bucket<T> next = it.next();
		    final int nbs = this.nbSpaces(next.getText());
		    totalNbSpaces += nbs;
		    bucketToNbSpaces[i] = nbs;
		    i++;
		}
		if (totalNbSpaces == 0) {
		    /* Cannot do anything */
		    return this;
		}
		for (int j = bucketToNbSpaces.length - 1; 0 <= j; j--) {
		    if (0 < bucketToNbSpaces[j]) {
			lastHopeIndex = j;
			break;
		    }
		}
		/* Holds because we ruled out 'totalNbSpaces == 0' before */
		assert 0 <= lastHopeIndex;
	    }
	    final int toAddPerSpace = totalNbSpaces == 0 ? 0 : totalDiff / totalNbSpaces;
	    int totalRest = totalDiff - toAddPerSpace * totalNbSpaces;
	    assert 0 <= totalRest;
	    int bidx = -1;
	    it = this.fragments.listIterator();
	    while (it.hasNext() && 0 < totalDiff) {
		bidx++;
		final Bucket<T> next = it.next();
		final String bucket = next.getText();
		final int blength = bucket.length();
		final int localNbSpaces = bucketToNbSpaces[bidx];
		if (localNbSpaces == 0) {
		    /* Cannot change it */
		    result.append(next);
		    continue;
		}
		int localDiff = localNbSpaces * toAddPerSpace;
		assert localDiff <= totalDiff;
		final int nb = localDiff / localNbSpaces;
		int localRest = localDiff - nb * localNbSpaces;
		if (localRest == 0 && 0 < totalRest) {
		    /*
		     * Take one for the group. This avoids flushing all spaces needed in the 'last
		     * hope' cases below.
		     */
		    localRest = 1;
		}
		assert 0 <= localRest;
		assert localRest <= totalRest;
		String novel = "";
		int eatenSpaces = 1;
		for (int i = 0; i < blength; i++) {
		    final char c = bucket.charAt(i);
		    novel += c;
		    if (c == ' ' && (0 < localDiff || 0 < totalDiff || 0 < localRest || 0 < totalRest)) {
			/* Can (and should) add an extra space */
			for (int j = 0; j < nb && 0 < localDiff; j++) {
			    novel += " ";
			    localDiff--;
			    totalDiff--;
			}
			if (0 < localRest || 0 < totalRest) {
			    if (eatenSpaces == localNbSpaces) {
				/* I'm the last hope for this bucket */
				for (int j = 0; j < localRest; j++) {
				    novel += " ";
				    localRest--;
				    totalRest--;
				}
				if (bidx == lastHopeIndex) {
				    /* I'm the last hope globally */
				    while (0 < totalRest) {
					novel += " ";
					totalRest--;
				    }
				}
			    } else {
				if (0 < localRest && 0 < totalRest) {
				    /* Not the last hope: take one only */
				    novel += " ";
				    localRest--;
				    totalRest--;
				}
			    }
			}
			eatenSpaces++;
		    }
		}
		/* I did my job */
		assert localRest == 0;
		/* If I was the hope, I did my job */
		assert bidx != lastHopeIndex || totalRest == 0;
		result.append(novel, next.getColor());
	    }
	    while (it.hasNext()) {
		result.append(it.next());
	    }
	    return result;
	}

	@Override
	public void clear() {
	    this.fragments.clear();
	}

	@Override
	public int length() {
	    int result = 0;
	    for (final Bucket<T> fragment : this.fragments) {
		result += fragment.getText().length();
	    }
	    return result;
	}

	@Override
	/* This implementation is resilient to empty buckets */
	public boolean isEmpty() {
	    for (final Bucket<?> bucket : this.fragments) {
		if (bucket.text == null || bucket.text.isEmpty()) {
		    continue;
		} else {
		    return false;
		}
	    }
	    return true;
	}

	@Override
	public T lastColor() {
	    return this.fragments.isEmpty() ? null : this.fragments.getLast().color;
	}

	@Override
	public T colorAt(final int index) {
	    final ListIterator<IColoredString.Bucket<T>> it = this.fragments.listIterator();
	    int now = 0;
	    while (it.hasNext()) {
		final IColoredString.Bucket<T> next = it.next();
		final String ftext = next.text;
		final int flen = ftext.length();
		final int nextl = now + flen;
		if (index < nextl) {
		    return next.color;
		}
		now += flen;
	    }
	    throw new NoSuchElementException("Color at index " + index + " in " + this);
	}

	@Override
	public char charAt(final int index) {
	    final ListIterator<IColoredString.Bucket<T>> it = this.fragments.listIterator();
	    int now = 0;
	    while (it.hasNext()) {
		final IColoredString.Bucket<T> next = it.next();
		final String ftext = next.text;
		final int flen = ftext.length();
		final int nextl = now + flen;
		if (index < nextl) {
		    return ftext.charAt(index - now);
		}
		now += flen;
	    }
	    throw new NoSuchElementException("Character at index " + index + " in " + this);
	}

	@Override
	public String present() {
	    final StringBuilder result = new StringBuilder();
	    for (final Bucket<T> fragment : this.fragments) {
		result.append(fragment.text);
	    }
	    return result.toString();
	}

	/**
	 * Given some way of converting from a T value to an in-line markup tag, returns
	 * a string representation of this IColoredString with in-line markup
	 * representing colors.
	 *
	 * @param markup an IMarkup implementation
	 * @return a String with markup inserted inside.
	 */
	@Override
	public String presentWithMarkup(final IMarkup<T> markup) {
	    final StringBuilder result = new StringBuilder();
	    boolean open = false;
	    for (final Bucket<T> fragment : this.fragments) {
		if (fragment.color != null) {
		    if (open) {
			result.append(markup.closeMarkup());
		    }
		    result.append(markup.getMarkup(fragment.color));
		    open = true;
		} else {
		    if (open) {
			result.append(markup.closeMarkup());
		    }
		    open = false;
		}
		result.append(fragment.text);
	    }
	    return result.toString();
	}

	@Override
	public Iterator<Bucket<T>> iterator() {
	    return this.fragments.iterator();
	}

	@Override
	public String toString() {
	    return this.present();
	}

	protected static boolean equals(final Object o1, final Object o2) {
	    if (o1 == null) {
		return o2 == null;
	    } else {
		return o1.equals(o2);
	    }
	}

	private int nbSpaces(final String s) {
	    final int bd = s.length();
	    int result = 0;
	    for (int i = 0; i < bd; i++) {
		final char c = s.charAt(i);
		if (c == ' ') {
		    result++;
		}
	    }
	    return result;
	}

	/** Some tests */
	public static void main(final String[] args) {
	    final IColoredString<Object> salutCestHgames = IColoredString.Impl.create();
	    salutCestHgames.append("Salut", new Object());
	    salutCestHgames.append(" ", new Object());
	    salutCestHgames.append("c'est", new Object());
	    salutCestHgames.append(" ", new Object());
	    salutCestHgames.append("hgames", new Object());
	    for (int i = 0; i < salutCestHgames.length(); i++) {
		System.out.println(salutCestHgames.charAt(i));
	    }
	}
    }

    /**
     * A piece of a {@link IColoredString}: a text and its color.
     *
     * @author smelC
     *
     * @param <T> The type of colors;
     */
    class Bucket<T> {
	protected final String text;
	protected final/* @Nullable */T color;

	public Bucket(final String text, /* @Nullable */final T color) {
	    this.text = text == null ? "" : text;
	    this.color = color;
	}

	/**
	 * @param text
	 * @return An instance whose text is {@code this.text + text}. Color is
	 *         unchanged.
	 */
	public Bucket<T> append(final String text) {
	    if (text == null || text.isEmpty()) {
		/* Let's save an allocation */
		return this;
	    } else {
		return new Bucket<>(this.text + text, this.color);
	    }
	}

	public Bucket<T> setLength(final int l) {
	    final int here = this.text.length();
	    if (here <= l) {
		return this;
	    } else {
		return new Bucket<>(this.text.substring(0, l), this.color);
	    }
	}

	public Bucket<T> setColor(final T t) {
	    return this.color == t ? this : new Bucket<>(this.text, t);
	}

	/**
	 * @return The text that this bucket contains.
	 */
	public String getText() {
	    return this.text;
	}

	/**
	 * @return The color of {@link #getText()}. Or {@code null} if none.
	 */
	public/* @Nullable */T getColor() {
	    return this.color;
	}

	@Override
	public String toString() {
	    if (this.color == null) {
		return this.text;
	    } else {
		return this.text + "(" + this.color + ")";
	    }
	}
    }
}