package squidpony.squidmath;

import java.io.Serializable;

import regexodus.Category;

/**
 * Additional implementations of the {@link CrossHash.IHasher} interface for
 * more specialized uses, like for use in an OrderedSet or OrderedMap with
 * String keys that should use case-insensitive equality/hashing. Created by
 * Tommy Ettinger on 4/15/2017.
 */
public class Hashers {
    private static class CaseInsensitiveStringHasher implements CrossHash.IHasher, Serializable {
	private static final long serialVersionUID = 1L;

	CaseInsensitiveStringHasher() {
	}

	@Override
	public int hash(final Object data) {
	    if (data == null) {
		return 0;
	    }
	    if (!(data instanceof CharSequence)) {
		return data.hashCode();
	    }
	    final CharSequence data2 = (CharSequence) data;
	    long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
	    final int len = data2.length();
	    for (int i = 0; i < len; i++) {
		result += a ^= 0x8329C6EB9E6AD3E3L * Category.caseFold(data2.charAt(i));
	    }
	    return (int) (result * (a | 1L) ^ (result >>> 27 | result << 37));
	}

	@Override
	public boolean areEqual(final Object left, final Object right) {
	    if (left == right) {
		return true;
	    }
	    if (!(left instanceof CharSequence && right instanceof CharSequence)) {
		return false;
	    }
	    final CharSequence l = (CharSequence) left, r = (CharSequence) right;
	    final int llen = l.length(), rlen = r.length();
	    if (llen != rlen) {
		return false;
	    }
	    for (int i = 0; i < llen; i++) {
		if (Category.caseFold(l.charAt(i)) != Category.caseFold(r.charAt(i))) {
		    return false;
		}
	    }
	    return true;
	}
    }

    /**
     * Hashes and equality-checks CharSequences, such as Strings and StringBuilders,
     * using case-insensitive comparison in a cross-platform way.
     */
    public static final CrossHash.IHasher caseInsensitiveStringHasher = new CaseInsensitiveStringHasher();

    private static class CategoryOnlyStringHasher implements CrossHash.IHasher, Serializable {
	private static final long serialVersionUID = 1L;
	public Category category;

	CategoryOnlyStringHasher(final Category category) {
	    this.category = category;
	}

	@Override
	public int hash(final Object data) {
	    if (data == null) {
		return 0;
	    }
	    if (!(data instanceof CharSequence)) {
		return data.hashCode();
	    }
	    final CharSequence data2 = (CharSequence) data;
	    long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
	    final int len = data2.length();
	    char c;
	    for (int i = 0; i < len; i++) {
		if (this.category.contains(c = data2.charAt(i))) {
		    result += a ^= 0x8329C6EB9E6AD3E3L * c;
		}
	    }
	    return (int) (result * (a | 1L) ^ (result >>> 27 | result << 37));
	}

	@Override
	public boolean areEqual(final Object left, final Object right) {
	    if (left == right) {
		return true;
	    }
	    if (!(left instanceof CharSequence && right instanceof CharSequence)) {
		return false;
	    }
	    final CharSequence l = (CharSequence) left, r = (CharSequence) right;
	    final int llen = l.length(), rlen = r.length();
	    char c1, c2;
	    for (int i = 0, j = 0; i < llen && j < rlen;) {
		while (!this.category.contains(c1 = l.charAt(i++))) {
		}
		while (!this.category.contains(c2 = r.charAt(j++))) {
		}
		if (c1 != c2) {
		    return false;
		}
	    }
	    return true;
	}
    }

    private static class NoCategoryStringHasher implements CrossHash.IHasher, Serializable {
	private static final long serialVersionUID = 1L;
	public Category category;

	NoCategoryStringHasher(final Category category) {
	    this.category = category;
	}

	@Override
	public int hash(final Object data) {
	    if (data == null) {
		return 0;
	    }
	    if (!(data instanceof CharSequence)) {
		return data.hashCode();
	    }
	    final CharSequence data2 = (CharSequence) data;
	    long result = 0x9E3779B97F4A7C94L, a = 0x632BE59BD9B4E019L;
	    final int len = data2.length();
	    char c;
	    for (int i = 0; i < len; i++) {
		if (!this.category.contains(c = data2.charAt(i))) {
		    result += a ^= 0x8329C6EB9E6AD3E3L * c;
		}
	    }
	    return (int) (result * (a | 1L) ^ (result >>> 27 | result << 37));
	}

	@Override
	public boolean areEqual(final Object left, final Object right) {
	    if (left == right) {
		return true;
	    }
	    if (!(left instanceof CharSequence && right instanceof CharSequence)) {
		return false;
	    }
	    final CharSequence l = (CharSequence) left, r = (CharSequence) right;
	    final int llen = l.length(), rlen = r.length();
	    char c1, c2;
	    for (int i = 0, j = 0; i < llen && j < rlen;) {
		while (this.category.contains(c1 = l.charAt(i++))) {
		}
		while (this.category.contains(c2 = r.charAt(j++))) {
		}
		if (c1 != c2) {
		    return false;
		}
	    }
	    return true;
	}
    }

    /**
     * Hashes and equality-checks CharSequences, such as Strings and StringBuilders,
     * but only considers letters (that is, characters that are in the Unicode
     * category "L", including A-Z, a-z, most characters used in most non-English
     * languages (katakana glyphs from Japanese count as letters, for instance)),
     * and works in a cross-platform way.
     */
    public static final CrossHash.IHasher letterOnlyStringHasher = new CategoryOnlyStringHasher(Category.L);
    /**
     * Hashes and equality-checks CharSequences, such as Strings and StringBuilders,
     * but only considers valid chars that are valid components of Java identifiers
     * (it does not check that the Strings are valid identifiers, but considers only
     * letters, digits, currency symbols, underscores (and related underscore-like
     * characters), and a few other types of glyph, ignoring whitespace and most
     * punctuation marks), and works in a cross-platform way.
     */
    public static final CrossHash.IHasher identifierOnlyStringHasher = new CategoryOnlyStringHasher(
	    Category.Identifier);
    /**
     * Hashes and equality-checks CharSequences, such as Strings and StringBuilders,
     * but does not consider whitespace (including space, newline, carriage return,
     * tab, and so on), and works in a cross-platform way.
     */
    public static final CrossHash.IHasher noSpaceStringHasher = new NoCategoryStringHasher(Category.Space);
    /**
     * Hashes and equality-checks CharSequences, such as Strings and StringBuilders,
     * but does not consider any number glyphs (Unicode category "N", including 0-9,
     * but also various numbers in other languages, such as the dedicated Roman
     * numeral characters), and works in a cross-platform way.
     */
    public static final CrossHash.IHasher noNumberStringHasher = new NoCategoryStringHasher(Category.N);
    /**
     * Hashes and equality-checks CharSequences, such as Strings and StringBuilders,
     * but does not consider letters (that is, characters that are in the Unicode
     * category "L", including A-Z, a-z, most characters used in most non-English
     * languages (katakana glyphs from Japanese count as letters, for instance)),
     * and works in a cross-platform way.
     */
    public static final CrossHash.IHasher noLetterStringHasher = new NoCategoryStringHasher(Category.L);
}
