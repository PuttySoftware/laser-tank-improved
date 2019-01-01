package squidpony;

import java.io.Reader;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;

import regexodus.Matcher;
import regexodus.Pattern;
import regexodus.REFlags;
import squidpony.annotation.Beta;
import squidpony.squidmath.CrossHash;
import squidpony.squidmath.IntVLA;

/**
 * A simple format parser for String-based configuration or data files where
 * JSON is overkill. Supports only one type, String, but allows each String to
 * have arbitrary nested levels of String children as if in sub-lists. You can
 * interpret the Strings however you want, and quoting each String isn't
 * necessary if they are just one word ("bare words" are allowed). <br>
 * The main way of using this is to get an ObText.ItemIterator value using
 * {@link #iterator()}, which acts as a normal Iterator over the top-level
 * Strings (not children of anything), but to call its
 * {@link ItemIterator#hasChild()} method when you expect potential child
 * elements, then {@link ItemIterator#children()} to get another ItemIterator
 * over the child elements if you want to explore deeper. <br>
 * This implements Collection of String but is (mostly) unmodifiable; you can
 * call {@link #parse(CharSequence)} to append the results of parsing more
 * formatted text, or call {@link #clear()} to remove all data.
 * {@link #add(Object)} and {@link #remove(Object)} are not implemented and
 * throw exceptions. A quirk of how this implements Collection is that it only
 * considers the top-level Strings to be part of the Collection for length and
 * for {@link #contains(Object)}, and will ignore child strings unless you
 * access them via {@link ItemIterator#children()} on an item that has at least
 * one child. <br>
 * Format example:
 *
 * <pre>
 * hello world
 * 'how are you today?' [just great thanks]
 * hooray!
 *
 * complexity?
 * [it is possible [yes this is a good example]
 * 'escapes like \[\'\] all work'
 * ]
 *
 * comments are allowed // like this
 * comments can have different forms # like this
 * // block comments like in c are allowed
 * / * but because this example is in javadoc, this example is not actually a comment * /
 * // remove the spaces between each slash and asterisk to make the last line a comment.
 * /[delimit/or block comments with delimiters/delimit]/
 *
 * '''
 * raw strings (heredocs) look like this normally.
 *     they permit characters without escapes, ]][][[ \/\/\ ,
 *     except for triple quotes.
 *     they keep newlines and indentation intact,
 * except for up to one newline ignored adjacent to each triple quote.
 * '''
 *
 * [[different[
 * if you may need triple quotes
 *     in the raw string, use a different syntax that allows delimiters.
 * here, the delimiter is '''different''', just to be different.]different]]
 * </pre>
 *
 * <br>
 * Inspired strongly by STOB, http://stobml.org/ , but no code is shared and the
 * format is slightly different. The main differences are that ObText supports
 * nested block comments using the syntax
 * {@code /[delimiter/contents/delimiter]/} where delimiter may be empty but
 * must match on both sides, and contents is the body of the comment. ObText
 * uses Python- like "heredoc" syntax for raw strings surrounded by
 * triple-apostrophes '''like so''' with optional initial and final newlines in
 * the raw string ignored. An alternate raw string syntax is present that allows
 * delimiters, using the syntax {@code [[delimiter[contents]delimiter]]}, where
 * again delimiter may be empty and contents is the body of the raw string. We
 * use square brackets in place of STOB's curly braces to mark children
 * associated with a string.
 */
@Beta
public class ObText extends AbstractCollection<String> {
    public static final Pattern pattern = Pattern.compile(
	    "(?>'''(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?({=s}.*?)(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?''')"
		    + "|(?>\\[\\[({=q}[^\\[\\]]*)\\[(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?({=s}.*?)(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?\\]{\\q}\\]\\])"
		    + "|(?>({=q}[\"'])({=s}.*?)(?<!\\\\){\\q})" + "|(?>(?>//|#)(?>\\V*))" + "|(?>/\\*(?:.*?)\\*/)"
		    + "|(?>/\\[({=q}\\S*)/(?:.*?)/{\\q}\\]/)" + "|({=s}[^\\s\\[\\]\"'#\\\\]+)" + "|({=o}\\[)"
		    + "|({=c}\\])",
	    REFlags.DOTALL | REFlags.UNICODE),
	    patternRelaxed = Pattern.compile(
		    "(?>'''(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?({=s}.*?)(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?''')"
			    + "|(?>\\[\\[({=q}[^\\[\\]]*)\\[(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?({=s}.*?)(?:[\n\u000C\f\r\u0085\u2028\u2029]|\r\n)?\\]{\\q}\\]\\])"
			    + "|(?>({=q}[\"'])({=s}.*?)(?<!\\\\){\\q})" +
			    // "|(?>(?>//|#)(?>\\V*))" +
			    // "|(?>/\\*(?:.*?)\\*/)" +
			    // "|(?>/\\[({=q}\\S*)/(?:.*?)/{\\q}\\]/)" +
			    "|({=s}[^\\s\\[\\]\"'\\\\]+)",
		    REFlags.DOTALL | REFlags.UNICODE);
    public static final int stringId = ObText.pattern.groupId("s"), openId = ObText.pattern.groupId("o"),
	    closeId = ObText.pattern.groupId("c");
    protected static final Pattern illegalBareWord = Pattern.compile("[\\s\\[\\]\"'#\\\\]|(?:/[/\\*])"),
	    reallyIllegalBareWord = Pattern.compile("[\\s\\[\\]\"'\\\\]"),
	    needsRaw = Pattern.compile("(?<!\\\\)[\\[\\]]|\\\\$");
    protected static final Matcher m = ObText.pattern.matcher();
    protected static final Matcher bare = ObText.illegalBareWord.matcher(), raw = ObText.needsRaw.matcher(),
	    reallyBare = ObText.reallyIllegalBareWord.matcher();
    protected final ArrayList<String> strings = new ArrayList<>(64);
    protected final IntVLA neighbors = new IntVLA(64);
    private final IntVLA nesting = new IntVLA(16);
    protected int length = 0;

    public ObText() {
    }

    public ObText(final CharSequence text) {
	this.parse(text);
    }

    /**
     * Parses the given text (a String or other CharSequence) and appends it into
     * this ObText.
     *
     * @param text a CharSequence (such as a String) using ObText formatting, as
     *             described in this class' JavaDocs
     * @return this ObText object after appending the parsed text, for chaining
     */
    public ObText parse(final CharSequence text) {
	ObText.m.setTarget(text);
	this.nesting.clear();
	int t = -1;
	while (ObText.m.find()) {
	    if (ObText.m.isCaptured(ObText.stringId)) {
		this.strings.add(ObText.m.group(ObText.stringId));
		this.neighbors.add(1);
		if (this.nesting.isEmpty()) {
		    this.length++;
		}
	    } else if (ObText.m.isCaptured(ObText.openId)) {
		this.nesting.add(this.neighbors.size - 1);
	    } else if (ObText.m.isCaptured(ObText.closeId)) {
		this.neighbors.incr(t = this.nesting.pop(), this.neighbors.size - t - 1);
		if (t < this.neighbors.size - 1) {
		    this.neighbors.set(this.neighbors.size - 1, 0);
		}
	    }
	}
	return this;
    }

    @Override
    public void clear() {
	this.strings.clear();
	this.neighbors.clear();
	this.length = 0;
    }

    @Override
    public int size() {
	return this.length;
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public ItemIterator iterator() {
	return new ItemIterator();
    }

    /**
     * A one-way iterator through this ObText's String items. Note that this can get
     * an iterator into a child sequence with {@link #children()}, which should only
     * be called after checking that a child exists with {@link #hasChild()}.
     * {@link #remove()} is not supported, but {@link #next()} and
     * {@link #hasNext()} are, of course.
     */
    public class ItemIterator implements Iterator<String> {
	ItemIterator() {
	}

	ItemIterator(final int i) {
	    this.current = i % ObText.this.neighbors.size;
	    this.index = this.current;
	}

	int index = 0, current = -1;

	/**
	 * Returns {@code true} if the iteration has more elements. (In other words,
	 * returns {@code true} if {@link #next} would return an element rather than
	 * throwing an exception.)
	 *
	 * @return {@code true} if the iteration has more elements
	 */
	@Override
	public boolean hasNext() {
	    return this.index < ObText.this.neighbors.size
		    && (this.current < 0 || ObText.this.neighbors.get(this.current) > 0);
	}

	/**
	 * Returns {@code true} if the ObText.Item has any child elements. (In other
	 * words, returns {@code true} if {@link #children} would return an ItemIterator
	 * rather than throwing an exception.)
	 *
	 * @return {@code true} if this has any children
	 */
	public boolean hasChild() {
	    return this.index < ObText.this.neighbors.size - 1 && (this.current < 0 && ObText.this.neighbors.get(0) > 1
		    || ObText.this.neighbors.get(this.current) > 1);
	}

	/**
	 * Returns the next element in the iteration.
	 *
	 * @return the next element in the iteration
	 * @throws java.util.NoSuchElementException if the iteration has no more
	 *         elements
	 */
	@Override
	public String next() {
	    if (this.current < 0) {
		if (ObText.this.strings.isEmpty()) {
		    throw new java.util.NoSuchElementException("No more sibling items in ObText object");
		}
		this.current = 0;
		this.index = ObText.this.neighbors.get(0);
		return ObText.this.strings.get(0);
	    }
	    if (this.index >= ObText.this.neighbors.size || ObText.this.neighbors.get(this.current) <= 0) {
		throw new java.util.NoSuchElementException("No more sibling items in ObText object");
	    }
	    this.index = ObText.this.neighbors.get(this.index) + (this.current = this.index);
	    return ObText.this.strings.get(this.current);
	}

	/**
	 * Returns the first child of this ObText.Item and descends into the sequence of
	 * child elements.
	 *
	 * @return the first child of this item
	 * @throws java.util.NoSuchElementException if the iteration has no children
	 */
	public ItemIterator children() {
	    if (this.current < 0) {
		if (ObText.this.neighbors.size <= 0 || ObText.this.neighbors.get(0) == 1) {
		    throw new java.util.NoSuchElementException("No current children in ObText object");
		}
		return new ItemIterator(1);
	    }
	    if (this.current >= ObText.this.neighbors.size - 2 || ObText.this.neighbors.get(this.current) == 1) {
		throw new java.util.NoSuchElementException("No current children in ObText object");
	    }
	    return new ItemIterator(this.current + 1);
	}

	@Override
	public void remove() {
	    throw new UnsupportedOperationException("remove() not supported");
	}
    }

    // Used to generate randomized delimiters using up to 9 non-English letters.
    // call while assigning your state with randomChars(state +=
    // 0x9E3779B97F4A7C15L, myChars)
    // that assumes you have a 9-element char[] called myChars
    // as long as z/state is deterministic (i.e. based on a hash), this should be
    // too
    private static void randomChars(long z, final char[] mut) {
	z = (z ^ z >>> 30) * 0xBF58476D1CE4E5B9L;
	z = (z ^ z >>> 27) * 0x94D049BB133111EBL;
	z ^= z >>> 31;
	mut[0] = ArrayTools.letters[(int) (128 + (z & 127))];
	mut[1] = ArrayTools.letters[(int) (128 + (z >>> 7 & 127))];
	mut[2] = ArrayTools.letters[(int) (128 + (z >>> 14 & 127))];
	mut[3] = ArrayTools.letters[(int) (128 + (z >>> 21 & 127))];
	mut[4] = ArrayTools.letters[(int) (128 + (z >>> 28 & 127))];
	mut[5] = ArrayTools.letters[(int) (128 + (z >>> 35 & 127))];
	mut[6] = ArrayTools.letters[(int) (128 + (z >>> 42 & 127))];
	mut[7] = ArrayTools.letters[(int) (128 + (z >>> 49 & 127))];
	mut[8] = ArrayTools.letters[(int) (128 + (z >>> 56 & 127))];
    }

    public static void appendQuoted(final StringBuilder sb, final String text) {
	ObText.appendQuoted(sb, text, ObText.reallyBare);
    }

    public static void appendQuotedObText(final StringBuilder sb, final String text) {
	ObText.appendQuoted(sb, text, ObText.bare);
    }

    protected static void appendQuoted(final StringBuilder sb, final String text, final Matcher bareFinder) {
	if (text == null || text.isEmpty()) {
	    sb.append("''");
	    return;
	}
	bareFinder.setTarget(text);
	if (!bareFinder.find()) {
	    sb.append(text);
	} else {
	    ObText.raw.setTarget(text);
	    if (ObText.raw.find()) {
		if (text.contains("'''")) {
		    long state = CrossHash.Wisp.hash64(text);
		    final char[] myChars = new char[9];
		    int count;
		    do {
			ObText.randomChars(state += 0x9E3779B97F4A7C15L, myChars);
			count = StringKit.containsPart(text, myChars, "]", "]]");
		    } while (count == 12);
		    sb.append("[[").append(myChars, 0, count).append("[\n").append(text).append("\n]")
			    .append(myChars, 0, count).append("]]");
		} else {
		    sb.append("'''\n").append(text).append("\n'''");
		}
	    } else if (!text.contains("'")) {
		sb.append('\'').append(text).append('\'');
	    } else {
		if (text.contains("\"")) {
		    if (text.contains("'''")) {
			long state = CrossHash.Wisp.hash64(text);
			final char[] myChars = new char[9];
			int count;
			do {
			    ObText.randomChars(state += 0x9E3779B97F4A7C15L, myChars);
			    count = StringKit.containsPart(text, myChars);
			} while (count == 9);
			sb.append("[[").append(myChars, 0, count).append("[\n").append(text).append("\n]")
				.append(myChars, 0, count).append("]]");
		    } else {
			sb.append("'''\n").append(text).append("\n'''");
		    }
		} else {
		    sb.append('"').append(text).append('"');
		}
	    }
	}
    }

    @Override
    public String toString() {
	return "ObText object: [[[[\n" + this.serializeToString() + "\n]]]]";
    }

    @Override
    public boolean equals(final Object o) {
	if (this == o) {
	    return true;
	}
	if (o == null || this.getClass() != o.getClass()) {
	    return false;
	}
	final ObText o2 = (ObText) o;
	if (!this.strings.equals(o2.strings)) {
	    return false;
	}
	return this.neighbors.equals(o2.neighbors);
    }

    @Override
    public int hashCode() {
	return CrossHash.Wisp.hash(this.strings) + CrossHash.Wisp.hash(this.neighbors.items);
    }

    public String serializeToString() {
	final StringBuilder sb = new StringBuilder(100);
	ObText.iterate(sb, this.iterator());
	return sb.toString();
    }

    /**
     * Deserializes an ObText that was serialized by {@link #serializeToString()} or
     * {@link #toString()}, and will ignore the prefix and suffix that toString
     * appends for readability (these are "ObText object: [[[[ " and " ]]]]", for
     * reference). This is otherwise the same as calling the constructor
     * {@link #ObText(CharSequence)}.
     *
     * @param data a String that is usually produced by serializeToString or
     *             toString on an ObText
     * @return a new ObText produced by parsing data (disregarding any prefix or
     *         suffix from toString() )
     */
    public static ObText deserializeFromString(final String data) {
	if (data.startsWith("ObText object: [[[[\n")) {
	    return new ObText(data.substring(20, data.length() - 5));
	}
	return new ObText(data);
    }

    private static void iterate(final StringBuilder sb, final ObText.ItemIterator it) {
	while (it.hasNext()) {
	    ObText.appendQuotedObText(sb, it.next());
	    sb.append('\n');
	    if (it.hasChild()) {
		sb.append("[\n");
		ObText.iterate(sb, it.children());
		sb.append("]\n");
	    }
	}
    }

    /**
     * Can be used to help reading sequences of Strings with ObText-style quotation
     * marking their boundaries. This returns a {@link ContentMatcher} object that
     * you must call setTarget on before using it. The argument(s) to setTarget
     * should be the text that might contain quotes, heredoc-style quotes, or just
     * bare words. Calling {@link ContentMatcher#find()} will try to find the next
     * String, returning false if there's nothing left or returning true and
     * advancing the search if a String was found. The String might be a special
     * term in some cases, like "[" and "]" without quotes being syntax in ObText
     * that don't contain usable Strings. That's why, after a String was found with
     * find(), you should check {@link ContentMatcher#hasMatch()} to verify that a
     * match was successful, and if that's true, then you can call
     * {@link ContentMatcher#getMatch()} to get the un-quoted contents of the next
     * String in the target.
     *
     * @return a {@link ContentMatcher} that must have one of its setTarget()
     *         methods called before it can be used
     */
    public static ContentMatcher makeMatcher() {
	return new ContentMatcher();
    }

    /**
     * Can be used to help reading sequences of Strings with ObText-style quotation
     * marking their boundaries. This returns a {@link ContentMatcher} object that
     * is already configured to read from {@code text}. The {@code text} should
     * contain Strings that may be surrounded by quotes, heredoc-style quotes, or
     * just bare words. Calling {@link ContentMatcher#find()} will try to find the
     * next String, returning false if there's nothing left or returning true and
     * advancing the search if a String was found. The String might be a special
     * term in some cases, like "[" and "]" without quotes being syntax in ObText
     * that don't contain usable Strings. That's why, after a String was found with
     * find(), you should check {@link ContentMatcher#hasMatch()} to verify that a
     * match was successful, and if that's true, then you can call
     * {@link ContentMatcher#getMatch()} to get the un-quoted contents of the next
     * String in the target.
     *
     * @param text the target String that should probably have at least one
     *             sub-string that might be quoted
     * @return a {@link ContentMatcher} that can be used immediately by calling
     *         {@link ContentMatcher#find()}
     */
    public static ContentMatcher makeMatcher(final CharSequence text) {
	return new ContentMatcher(text);
    }

    /**
     * Can be used to help reading sequences of Strings with ObText-style quotation
     * marking their boundaries, but no comments (which allows some additional
     * characters to be used in bare words, like '#'). This returns a
     * {@link ContentMatcher} object that is already configured to read from
     * {@code text}. The {@code text} should contain Strings that may be surrounded
     * by quotes, heredoc-style quotes, or just bare words. Calling
     * {@link ContentMatcher#find()} will try to find the next String, returning
     * false if there's nothing left or returning true and advancing the search if a
     * String was found. Unlike the ContentMatcher produced by
     * {@link #makeMatcher(CharSequence)}, you can call
     * {@link ContentMatcher#getMatch()} after any successful call to
     * {@link ContentMatcher#find()}, which will get the un-quoted contents of the
     * next String in the target.
     *
     * @param text the target String that should probably have at least one
     *             sub-string that might be quoted
     * @return a {@link ContentMatcher} that can be used immediately by calling
     *         {@link ContentMatcher#find()}
     */
    public static ContentMatcher makeMatcherNoComments(final CharSequence text) {
	return new ContentMatcher(text, ObText.patternRelaxed);
    }

    public static class ContentMatcher extends Matcher {
	/**
	 * Constructs a ContentMatcher that will need to have its target set with
	 * {@link #setTarget(CharSequence)} or one of its overloads. The target should
	 * contain multiple substrings that may have quotation around them; this class
	 * is meant to skip the quotation in ObText's style.
	 */
	public ContentMatcher() {
	    super(ObText.pattern);
	}

	/**
	 * Constructs a ContentMatcher that already has its target set to {@code text}.
	 *
	 * @param text the CharSequence, such as a String, to find possibly-quoted
	 *             Strings in.
	 */
	public ContentMatcher(final CharSequence text) {
	    super(ObText.pattern, text);
	}

	/**
	 * Constructs a ContentMatcher that already has its target set to {@code text}
	 * and uses an alternate Pattern.
	 */
	ContentMatcher(final CharSequence text, final Pattern altPattern) {
	    super(altPattern, text);
	}

	/**
	 * Supplies a text to search in/match with. Resets current search position to
	 * zero.
	 *
	 * @param text - a data
	 * @see Matcher#setTarget(Matcher, int)
	 * @see Matcher#setTarget(CharSequence, int, int)
	 * @see Matcher#setTarget(char[], int, int)
	 * @see Matcher#setTarget(Reader, int)
	 */
	@Override
	public void setTarget(final CharSequence text) {
	    super.setTarget(text);
	}

	/**
	 * Supplies a text to search in/match with, as a part of String. Resets current
	 * search position to zero.
	 *
	 * @param text  - a data source
	 * @param start - where the target starts
	 * @param len   - how long is the target
	 * @see Matcher#setTarget(Matcher, int)
	 * @see Matcher#setTarget(CharSequence)
	 * @see Matcher#setTarget(char[], int, int)
	 * @see Matcher#setTarget(Reader, int)
	 */
	@Override
	public void setTarget(final CharSequence text, final int start, final int len) {
	    super.setTarget(text, start, len);
	}

	/**
	 * Supplies a text to search in/match with, as a part of char array. Resets
	 * current search position to zero.
	 *
	 * @param text  - a data source
	 * @param start - where the target starts
	 * @param len   - how long is the target
	 * @see Matcher#setTarget(Matcher, int)
	 * @see Matcher#setTarget(CharSequence)
	 * @see Matcher#setTarget(CharSequence, int, int)
	 * @see Matcher#setTarget(Reader, int)
	 */
	@Override
	public void setTarget(final char[] text, final int start, final int len) {
	    super.setTarget(text, start, len);
	}

	/**
	 * Returns true if {@link #find()} has returned true and the found text is a
	 * usable String (not some syntax). If this returns true, you can reasonably get
	 * a (possibly empty) String using {@link #getMatch()}.
	 *
	 * @return true if there is a usable String found that can be obtained with
	 *         {@link #getMatch()}
	 */
	public boolean hasMatch() {
	    return this.isCaptured(ObText.stringId);
	}

	/**
	 * Returns the contents of the latest String successfully found with
	 * {@link #find()}, without quotation. You should typically call
	 * {@link #hasMatch()} even if find() has returned true, to ensure there is a
	 * valid String that can be acquired (this will return an empty String if
	 * hasMatch() returns false, but an empty String is also potentially a valid
	 * result in a successful match, so it should be distinguished).
	 *
	 * @return the contents of the latest String successfully found with
	 *         {@link #find()}
	 */
	public String getMatch() {
	    return this.group(ObText.stringId);
	}
    }
}