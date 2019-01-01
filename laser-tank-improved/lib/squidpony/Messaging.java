package squidpony;

import java.util.HashMap;

import regexodus.MatchResult;
import regexodus.Matcher;
import regexodus.Pattern;
import regexodus.Replacer;
import regexodus.Substitution;
import regexodus.TextBuffer;

/**
 * Helps handle formation of messages from a template, using correct pronouns
 * and helping handle various idiosyncrasies in English-language text. You call
 * the static method
 * {@link #transform(CharSequence, String, NounTrait, String, NounTrait)} (or
 * one of its overloads) with a template that has specific placeholder glyphs,
 * along with a user name, optional target name, user NounTrait (an enum in this
 * class) to specify how the user should be addressed, including their gender,
 * optional target NounTrait, and possibly extra terms that should be inserted.
 * The placeholder glyphs are usually followed by a specific word that is
 * conjugated in the template for the first-person case, and will be changed to
 * fit the NounTrait for the user or target. For example, you could use "@Name
 * hit$ ^ for ~ damage!" as a message. You could transform it with user "Heero
 * Supra", userTrait NounTrait.SECOND_PERSON_SINGULAR, target "the beast",
 * targetTrait NounTrait.UNSPECIFIED_GENDER, and extra "10" to get the message
 * "You hit the beast for 10 damage!". You could swap the user and target (along
 * with their traits) to get the message "The beast hits you for 10 damage!" You
 * can handle more complex verbs in some cases, such as "@I hurr$$$ to catch
 * up!" can be transformed to "You hurry to catch up!" or "He hurries to catch
 * up!". The rules are fairly simple; @word conjugates a specific word from a
 * list to the correct kind for the user, while ^word does a similar thing but
 * conjugates for the target. Between 1 and 3 $ chars can be used at the end of
 * verbs to conjugate them appropriately for the present tense when the verb is
 * performed by the user (with just $) or alternately the target (if the $ chars
 * are preceded by a ^), while @s, @ss, @sss, ^s, ^ss, or ^sss can be added at
 * the end of nouns to pluralize them if appropriate. Using one $ or s will add
 * s or nothing, as in the case of hit becoming hits, using two $ or s chars
 * will add es or nothing, as in the case of scratch becoming scratches, and
 * using three will add ies or y, as in the case of carry becoming carries. Some
 * unusual pluralization forms are handled; @usi will turn octop@usi into
 * octopus or octopi, and radi@usi into radius or radii, while @fves will turn
 * el@fves into elf or elves, or dwar@fves into dwarf or dwarves. <br>
 * The words you can put after a @ or ^ start with a small list and can be added
 * to with
 * {@link #learnIrregularWord(String, String, String, String, String, String, String)}.
 * The initial list is: name, name_s, i, me, my, mine, myself, am, have, do,
 * haven_t, don_t, or any of these with the first char capitalized (meant for
 * words at the start of sentences). The non-word shortened terms "m" and "ve"
 * can be used for "I'm" and "I've", respectively, as well as "you're" and
 * "you've", plus "he's" for "he is" and "he's" for "he has". Most of the rest
 * conjugate as you would expect; @me will become him, her, it, them, you, or
 * still more forms depending on userTrait. You can also use @ or ^ on its own
 * as an equivalent to @name or ^name. <br>
 * Examples: <br>
 * {@code Messaging.transform("@I @am @my own boss@ss.", "unused", changingTrait)}
 * <ul>
 * <li>When changingTrait is {@code NounTrait.FIRST_PERSON_SINGULAR}, this
 * returns "I am my own boss."</li>
 * <li>When changingTrait is {@code NounTrait.FIRST_PERSON_PLURAL}, this returns
 * "We are our own bosses."</li>
 * <li>When changingTrait is {@code NounTrait.SECOND_PERSON_SINGULAR}, this
 * returns "You are your own boss."</li>
 * <li>When changingTrait is {@code NounTrait.SECOND_PERSON_PLURAL}, this
 * returns "You are your own bosses."</li>
 * <li>When changingTrait is {@code NounTrait.NO_GENDER}, this returns "It is
 * its own boss."</li>
 * <li>When changingTrait is {@code NounTrait.MALE_GENDER}, this returns "He is
 * his own boss."</li>
 * <li>When changingTrait is {@code NounTrait.FEMALE_GENDER}, this returns "She
 * is her own boss."</li>
 * <li>When changingTrait is {@code NounTrait.UNSPECIFIED_GENDER}, this returns
 * "They are their own boss."</li>
 * <li>When changingTrait is {@code NounTrait.ADDITIONAL_GENDER}, this returns
 * "Xe is xis own boss."</li>
 * <li>When changingTrait is {@code NounTrait.SPECIAL_CASE_GENDER}, this returns
 * "Qvqe is qvqis own boss."</li>
 * <li>When changingTrait is {@code NounTrait.GROUP}, this returns "They are
 * their own bosses."</li>
 * </ul>
 * {@code Messaging.transform("@Name spit$ in ^name_s face^s!", userName, userTrait, targetName, targetTrait)}
 * <ul>
 * <li>When userTrait is {@code NounTrait.SECOND_PERSON_SINGULAR}, targetName is
 * {@code "the goblin"}, and targetTrait is {@code NounTrait.MALE_GENDER}, this
 * returns "You spit in the goblin's face!"</li>
 * <li>When userName is {@code "the goblin"}, userTrait is
 * {@code NounTrait.MALE_GENDER}, and targetTrait is
 * {@code NounTrait.SECOND_PERSON_SINGULAR}, this returns "The goblin spits in
 * your face!"</li>
 * <li>When userTrait is {@code NounTrait.SECOND_PERSON_SINGULAR}, targetName is
 * {@code "the goblins"}, and targetTrait is {@code NounTrait.GROUP}, this
 * returns "You spit in the goblins' faces!"</li>
 * <li>When userName is {@code "the goblins"}, userTrait is
 * {@code NounTrait.GROUP}, and targetTrait is
 * {@code NounTrait.SECOND_PERSON_SINGULAR}, this returns "The goblins spit in
 * your face!"</li>
 * </ul>
 * Created by Tommy Ettinger on 10/31/2016.
 */
public class Messaging {
    /**
     * Properties of nouns needed to correctly conjugate those nouns and refer to
     * them with pronouns, such as genders. Includes parts of speech, which only are
     * concerned with whether they refer to a singular noun or a plural noun, and
     * genders for when a gendered pronoun is needed. This provides substantial
     * support for uncommon cases regarding gender and pronoun preferences. That
     * said, gender and pronoun preference can be incredibly hard to handle. The
     * simplest cases are for first- and second-person pronouns; here we have
     * "I/me/my/myself" for {@link #FIRST_PERSON_SINGULAR}, "you/you/your/yourself"
     * for {@link #SECOND_PERSON_SINGULAR}, "we/us/our/ourselves" for
     * {@link #FIRST_PERSON_PLURAL}, and "you/you/your/yourselves" for
     * {@link #SECOND_PERSON_PLURAL}; there are more pronouns this can produce, but
     * they aren't listed here. Third-person pronouns are considerably more
     * challenging because English sporadically considers gender as part of
     * conjugation, but doesn't provide a universally-acceptable set of gendered
     * pronouns. <br>
     * This at least tries to provide pronoun handling for the common cases, such as
     * "you" not needing a gendered pronoun at all (it uses
     * {@link #SECOND_PERSON_SINGULAR}), and supports {@link #MALE_GENDER male},
     * {@link #FEMALE_GENDER female}, {@link #NO_GENDER genderless} (using "it" and
     * related forms; preferred especially for things that aren't alive, and in most
     * cases not recommended for people), {@link #UNSPECIFIED_GENDER "unspecified"}
     * (using "they" in place of "he" or "she"; preferred in some cases when
     * describing someone with a non-specific gender or an unknown gender) pronouns,
     * and {@link #GROUP group} for when a group of individuals, regardless of
     * gender or genders, is referred to with a single pronoun. As mentioned, this
     * has support for some uncommon situations, like {@link #ADDITIONAL_GENDER
     * additional gender} (as in, a gender that is in addition to male and female
     * but that is not genderless, which has a clear use case when describing
     * non-human species, and a more delicate use for humans who use non-binary
     * gender pronouns; hopefully "xe" will be acceptable), and finally a
     * {@link #SPECIAL_CASE_GENDER "special case"} pronoun that is unpronounceable
     * and, if given special processing, can be used as a replacement target for
     * customized pronouns. For the additional gender, the non-binary gendered
     * pronouns are modified from the male pronouns by replacing 'h' with 'x' (he
     * becomes xe, his becomes xis). The "special case" pronouns replace the 'h' in
     * the male pronouns with 'qvq', except for in one case. Where, if the female
     * pronoun were used, it would be "hers", but the male pronoun in that case
     * would be "his", changing the male pronoun would lead to a
     * difficult-to-replace case because "his" is also used in the case where the
     * female pronoun is the usefully distinct "her". Here, the "special case"
     * gender diverges from what it usually does, and uses "qvqims" in place of
     * "his" or "hers". The "special case" pronouns should be replaced before being
     * displayed, since they look like gibberish or a glitch and so are probably
     * confusing out of context.
     *
     */
    public enum NounTrait {
	/**
	 * As in, "I am my own boss." Doesn't reference gender.
	 */
	FIRST_PERSON_SINGULAR,
	/**
	 * As in, "You are your own boss." Doesn't reference gender.
	 */
	SECOND_PERSON_SINGULAR,
	/**
	 * As in, "We are our own bosses." Doesn't reference gender, and applies to
	 * groups.
	 */
	FIRST_PERSON_PLURAL,
	/**
	 * As in, "You are your own bosses." Doesn't reference gender, and applies to
	 * groups.
	 */
	SECOND_PERSON_PLURAL,
	/**
	 * Inanimate objects or beings without gender, as in "It is its own boss."
	 */
	NO_GENDER,
	/**
	 * Male pronoun preference, as in "He is his own boss."
	 */
	MALE_GENDER,
	/**
	 * Female pronoun preference, as in "She is her own boss."
	 */
	FEMALE_GENDER,
	/**
	 * "Singular they" pronoun preference or to be used when preference is unknown,
	 * as in "They are their own boss."
	 */
	UNSPECIFIED_GENDER,
	/**
	 * Third-gender pronoun preference, potentially relevant for cultures with
	 * non-binary gender terms. As in, "Xe is xis own boss."
	 */
	ADDITIONAL_GENDER,
	/**
	 * Unpronounceable words that can be processed specially for more complex cases
	 * of pronoun preference. As in, "Qvqe is qvqis own boss."
	 */
	SPECIAL_CASE_GENDER,
	/**
	 * Any third-person plural, as in "They are their own bosses." Not to be
	 * confused with UNSPECIFIED_GENDER, which is for singular beings, but usually
	 * uses "they" in the same way (not always).
	 */
	GROUP;
	public String nameText(final String term) {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
		return "I";
	    case FIRST_PERSON_PLURAL:
		return "we";
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
		return "you";
	    default:
		return term;
	    }
	}

	public String name_sText(final String term) {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
		return "my";
	    case FIRST_PERSON_PLURAL:
		return "our";
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
		return "your";
	    default:
		if (term.isEmpty()) {
		    return "";
		} else if (term.endsWith("s")) {
		    return term + '\'';
		} else {
		    return term + "'s";
		}
	    }
	}

	public String iText() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
		return "I";
	    case FIRST_PERSON_PLURAL:
		return "we";
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
		return "you";
	    case NO_GENDER:
		return "it";
	    case MALE_GENDER:
		return "he";
	    case FEMALE_GENDER:
		return "she";
	    // case UNSPECIFIED_GENDER: return "they";
	    case ADDITIONAL_GENDER:
		return "xe";
	    case SPECIAL_CASE_GENDER:
		return "qvqe";
	    default:
		return "they";
	    }
	}

	public String meText() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
		return "me";
	    case FIRST_PERSON_PLURAL:
		return "us";
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
		return "you";
	    case NO_GENDER:
		return "it";
	    case MALE_GENDER:
		return "him";
	    case FEMALE_GENDER:
		return "her";
	    // case UNSPECIFIED_GENDER: return "them";
	    case ADDITIONAL_GENDER:
		return "xim";
	    case SPECIAL_CASE_GENDER:
		return "qvqim";
	    default:
		return "them";
	    }
	}

	public String myText() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
		return "my";
	    case FIRST_PERSON_PLURAL:
		return "our";
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
		return "your";
	    case NO_GENDER:
		return "its";
	    case MALE_GENDER:
		return "his";
	    case FEMALE_GENDER:
		return "her";
	    // case UNSPECIFIED_GENDER: return "their";
	    case ADDITIONAL_GENDER:
		return "xis";
	    case SPECIAL_CASE_GENDER:
		return "qvqis";
	    default:
		return "their";
	    }
	}

	public String mineText() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
		return "mine";
	    case FIRST_PERSON_PLURAL:
		return "ours";
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
		return "yours";
	    case NO_GENDER:
		return "its";
	    case MALE_GENDER:
		return "his";
	    case FEMALE_GENDER:
		return "hers";
	    // case UNSPECIFIED_GENDER: return "theirs";
	    case ADDITIONAL_GENDER:
		return "xis";
	    case SPECIAL_CASE_GENDER:
		return "qvqims";
	    default:
		return "theirs";
	    }
	}

	public String myselfText() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
		return "myself";
	    case FIRST_PERSON_PLURAL:
		return "ourselves";
	    case SECOND_PERSON_SINGULAR:
		return "yourself";
	    case SECOND_PERSON_PLURAL:
		return "yourselves";
	    case NO_GENDER:
		return "itself";
	    case MALE_GENDER:
		return "himself";
	    case FEMALE_GENDER:
		return "herself";
	    case UNSPECIFIED_GENDER:
		return "themself";
	    case ADDITIONAL_GENDER:
		return "ximself";
	    case SPECIAL_CASE_GENDER:
		return "qvqimself";
	    default:
		return "themselves";
	    }
	}

	public String sText() {
	    switch (this) {
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_PLURAL:
	    case GROUP:
		return "s";
	    default:
		return "";
	    }
	}

	public String ssText() {
	    switch (this) {
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_PLURAL:
	    case GROUP:
		return "es";
	    default:
		return "";
	    }
	}

	public String sssText() {
	    switch (this) {
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_PLURAL:
	    case GROUP:
		return "ies";
	    default:
		return "y";
	    }
	}

	public String usiText() {
	    switch (this) {
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_PLURAL:
	    case GROUP:
		return "i";
	    default:
		return "us";
	    }
	}

	public String fvesText() {
	    switch (this) {
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_PLURAL:
	    case GROUP:
		return "ves";
	    default:
		return "f";
	    }
	}

	public String $Text() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
	    case UNSPECIFIED_GENDER:
	    case GROUP:
		return "";
	    default:
		return "s";
	    }
	}

	public String $$Text() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
	    case UNSPECIFIED_GENDER:
	    case GROUP:
		return "";
	    default:
		return "es";
	    }
	}

	public String $$$Text() {
	    switch (this) {
	    case FIRST_PERSON_SINGULAR:
	    case FIRST_PERSON_PLURAL:
	    case SECOND_PERSON_SINGULAR:
	    case SECOND_PERSON_PLURAL:
	    case UNSPECIFIED_GENDER:
	    case GROUP:
		return "y";
	    default:
		return "ies";
	    }
	}
    }

    /**
     * Takes message and replaces any of the special terms this recognizes, like @,
     * ^, and $, with the appropriately- conjugated terms for the given user and
     * their associated NounTrait.
     *
     * @param message   the message to transform; should contain "@" or "$" in it,
     *                  at least, to be replaced
     * @param user      the name of the user for cases where it can replace text
     *                  like "@" or "@Name"
     * @param userTrait the {@link NounTrait} enum that determines how user should
     *                  be referred to
     * @return a String resulting from the processing of message
     */
    public static String transform(final CharSequence message, final String user, final NounTrait userTrait) {
	final Replacer ur = new Replacer(Messaging.userPattern, new BeingSubstitution(user, userTrait, true));
	return ur.replace(message);
    }

    /**
     * Takes message and replaces any of the special terms this recognizes, like @,
     * ^, and $, with the appropriately- conjugated terms for the given user, their
     * associated NounTrait, the given target, and their NounTrait.
     *
     * @param message     the message to transform; should contain "@", "^", or "$"
     *                    in it, at least, to be replaced
     * @param user        the name of the user for cases where it can replace text
     *                    like "@" or "@Name"
     * @param userTrait   the {@link NounTrait} enum that determines how user should
     *                    be referred to
     * @param target      the name of the target for cases where it can replace text
     *                    like "^" or "^Name"
     * @param targetTrait the {@link NounTrait} enum that determines how the target
     *                    should be referred to
     * @return a String resulting from the processing of message
     */
    public static String transform(final CharSequence message, final String user, final NounTrait userTrait,
	    final String target, final NounTrait targetTrait) {
	final Replacer tr = new Replacer(Messaging.targetPattern, new BeingSubstitution(target, targetTrait, false)),
		ur = new Replacer(Messaging.userPattern, new BeingSubstitution(user, userTrait, true));
	return ur.replace(tr.replace(message));
    }

    /**
     * Takes message and replaces any of the special terms this recognizes, like @,
     * ^, and $, with the appropriately- conjugated terms for the given user, their
     * associated NounTrait, the given target, and their NounTrait. Also replaces
     * the nth occurrence of "~" with the matching nth item in extra, so the first
     * "~" is replaced with the first item in extra, the second "~" with the second
     * item, and so on until one is exhausted.
     *
     * @param message     the message to transform; should contain "@", "^", "$", or
     *                    "~" in it, at least, to be replaced
     * @param user        the name of the user for cases where it can replace text
     *                    like "@" or "@Name"
     * @param userTrait   the {@link NounTrait} enum that determines how user should
     *                    be referred to
     * @param target      the name of the target for cases where it can replace text
     *                    like "^" or "^Name"
     * @param targetTrait the {@link NounTrait} enum that determines how the target
     *                    should be referred to
     * @param extra       an array or vararg of String where the nth item in extra
     *                    will replace the nth occurrence of "~"
     * @return a String resulting from the processing of message
     */
    public static String transform(final CharSequence message, final String user, final NounTrait userTrait,
	    final String target, final NounTrait targetTrait, final String... extra) {
	final Replacer tr = new Replacer(Messaging.targetPattern, new BeingSubstitution(target, targetTrait, false)),
		ur = new Replacer(Messaging.userPattern, new BeingSubstitution(user, userTrait, true));
	String text = ur.replace(tr.replace(message));
	if (extra != null && extra.length > 0) {
	    final Matcher m = Pattern.compile("~").matcher(text);
	    for (final String element : extra) {
		text = m.replaceFirst(element);
	    }
	}
	return text;
    }

    protected static final Pattern userPattern = Pattern
	    .compile("({at_sign}\\\\@)|({caret_sign}\\\\\\^)|({dollar_sign}\\\\\\$)|({tilde_sign}\\\\~)|"
		    + "({$$$}\\$\\$\\$)|({$$}\\$\\$)|({$}\\$)|({sss}@sss\\b)|({ss}@ss\\b)|({s}@s\\b)|({usi}@usi\\b)|({fves}@fves\\b)|"
		    + "({name_s}@name_s\\b)|({Name_s}@Name_s\\b)|({name}@name\\b)|({Name}@Name\\b)|({i}@i\\b)|({I}@I\\b)|({me}@me\\b)|({Me}@Me\\b)|"
		    + "({myself}@myself\\b)|({Myself}@Myself\\b)|({my}@my\\b)|({My}@My\\b)|({mine}@mine\\b)|({Mine}@Mine\\b)|"
		    + "(?:@({Other}\\p{Lu}\\w*)\\b)|(?:@({other}\\p{Ll}\\w*)\\b)|({=name}@)"),
	    targetPattern = Pattern
		    .compile("({at_sign}\\\\@)|({caret_sign}\\\\\\^)|({dollar_sign}\\\\\\$)|({tilde_sign}\\\\~)|"
			    + "({$$$}\\^\\$\\$\\$)|({$$}\\^\\$\\$)|({$}\\^\\$)|({sss}\\^sss\\b)|({ss}\\^ss\\b)|({s}\\^s\\b)|({usi}\\^usi\\b)|({fves}\\^fves\\b)|"
			    + "({name_s}\\^name_s\\b)|({Name_s}\\^Name_s\\b)|({name}\\^name\\b)|({Name}\\^Name\\b)|({i}\\^i\\b)|({I}\\^I\\b)|({me}\\^me\\b)|({Me}\\^Me\\b)|"
			    + "({myself}\\^myself\\b)|({Myself}\\^Myself\\b)|({my}\\^my\\b)|({My}\\^My\\b)|({mine}\\^mine\\b)|({Mine}\\^Mine\\b)|"
			    + "(?:\\^({Other}\\p{Lu}\\w*)\\b)|(?:\\^({other}\\p{Ll}\\w*)\\b)|({=name}\\^)");
    private static final HashMap<String, String[]> irregular = new HashMap<>(64);

    /**
     * Adds a given {@code word}, which should start with a lower-case letter and
     * use lower-case letters and underscores only, to the dictionary this stores.
     * The 6 additional arguments are used for first person singular ("I am"), first
     * person plural ("we are"), second person singular ("you are"), second person
     * plural ("you are", the same as the last one usually, but not always), third
     * person singular ("he is"), third person plural ("they are").
     *
     * @param word                 the word to learn; must start with a letter and
     *                             use only lower-case letters and underscores
     * @param firstPersonSingular  the conjugated form of the word for first-person
     *                             singular ("I do", "I am")
     * @param firstPersonPlural    the conjugated form of the word for first-person
     *                             plural ("we do", "we are")
     * @param secondPersonSingular the conjugated form of the word for second-person
     *                             singular ("you do", "you are")
     * @param secondPersonPlural   the conjugated form of the word for second-person
     *                             plural ("you do", "you are")
     * @param thirdPersonSingular  the conjugated form of the word for third-person
     *                             singular ("he does", "he is")
     * @param thirdPersonPlural    the conjugated form of the word for third-person
     *                             plural and unspecified-gender singular ("they
     *                             do", "they are")
     */
    public static void learnIrregularWord(final String word, final String firstPersonSingular,
	    final String firstPersonPlural, final String secondPersonSingular, final String secondPersonPlural,
	    final String thirdPersonSingular, final String thirdPersonPlural) {
	Messaging.irregular.put(word,
		new String[] { firstPersonSingular, firstPersonPlural, secondPersonSingular, secondPersonPlural,
			thirdPersonSingular, thirdPersonSingular, thirdPersonSingular, thirdPersonPlural,
			thirdPersonSingular, thirdPersonSingular, thirdPersonPlural });
    }

    static {
	Messaging.learnIrregularWord("m", "'m", "'re", "'re", "'re", "'s", "'re");
	Messaging.learnIrregularWord("am", "am", "are", "are", "are", "is", "are");
	Messaging.learnIrregularWord("ve", "'ve", "'ve", "'ve", "'ve", "'s", "'ve");
	Messaging.learnIrregularWord("have", "have", "have", "have", "have", "has", "have");
	Messaging.learnIrregularWord("haven_t", "haven't", "haven't", "haven't", "haven't", "hasn't", "haven't");
	Messaging.learnIrregularWord("do", "do", "do", "do", "do", "does", "do");
	Messaging.learnIrregularWord("don_t", "don't", "don't", "don't", "don't", "doesn't", "don't");
    }

    protected static class BeingSubstitution implements Substitution {
	public String term;
	public NounTrait trait;
	public boolean finisher;

	public BeingSubstitution() {
	    this.term = "Joe";
	    this.trait = NounTrait.MALE_GENDER;
	    this.finisher = true;
	}

	public BeingSubstitution(final String term, final NounTrait trait, final boolean finish) {
	    this.term = term == null ? "Nullberoth of the North" : term;
	    this.trait = trait == null ? NounTrait.UNSPECIFIED_GENDER : trait;
	    this.finisher = finish;
	}

	public static void appendCapitalized(final String s, final TextBuffer dest) {
	    dest.append(Character.toUpperCase(s.charAt(0)));
	    if (s.length() > 1) {
		dest.append(s.substring(1, s.length()));
	    }
	}

	@Override
	public void appendSubstitution(final MatchResult match, final TextBuffer dest) {
	    if (match.isCaptured("at_sign")) {
		dest.append(this.finisher ? "@" : "\\@");
	    } else if (match.isCaptured("caret_sign")) {
		dest.append(this.finisher ? "^" : "\\^");
	    } else if (match.isCaptured("dollar_sign")) {
		dest.append(this.finisher ? "$" : "\\$");
	    } else if (match.isCaptured("tilde_sign")) {
		dest.append(this.finisher ? "~" : "\\~");
	    } else if (match.isCaptured("name")) {
		dest.append(this.trait.nameText(this.term));
	    } else if (match.isCaptured("Name")) {
		BeingSubstitution.appendCapitalized(this.trait.nameText(this.term), dest);
	    } else if (match.isCaptured("name_s")) {
		dest.append(this.trait.name_sText(this.term));
	    } else if (match.isCaptured("Name_s")) {
		BeingSubstitution.appendCapitalized(this.trait.name_sText(this.term), dest);
	    } else if (match.isCaptured("i")) {
		dest.append(this.trait.iText());
	    } else if (match.isCaptured("I")) {
		BeingSubstitution.appendCapitalized(this.trait.iText(), dest);
	    } else if (match.isCaptured("me")) {
		dest.append(this.trait.meText());
	    } else if (match.isCaptured("Me")) {
		BeingSubstitution.appendCapitalized(this.trait.meText(), dest);
	    } else if (match.isCaptured("my")) {
		dest.append(this.trait.myText());
	    } else if (match.isCaptured("My")) {
		BeingSubstitution.appendCapitalized(this.trait.myText(), dest);
	    } else if (match.isCaptured("mine")) {
		dest.append(this.trait.mineText());
	    } else if (match.isCaptured("Mine")) {
		BeingSubstitution.appendCapitalized(this.trait.mineText(), dest);
	    } else if (match.isCaptured("myself")) {
		dest.append(this.trait.myselfText());
	    } else if (match.isCaptured("Myself")) {
		BeingSubstitution.appendCapitalized(this.trait.myselfText(), dest);
	    } else if (match.isCaptured("s")) {
		dest.append(this.trait.sText());
	    } else if (match.isCaptured("ss")) {
		dest.append(this.trait.ssText());
	    } else if (match.isCaptured("sss")) {
		dest.append(this.trait.sssText());
	    } else if (match.isCaptured("usi")) {
		dest.append(this.trait.usiText());
	    } else if (match.isCaptured("fves")) {
		dest.append(this.trait.fvesText());
	    } else if (match.isCaptured("$")) {
		dest.append(this.trait.$Text());
	    } else if (match.isCaptured("$$")) {
		dest.append(this.trait.$$Text());
	    } else if (match.isCaptured("$$$")) {
		dest.append(this.trait.$$$Text());
	    } else if (match.isCaptured("other")) {
		final String[] others = Messaging.irregular.get(match.group("other"));
		if (others != null && others.length == 11) {
		    dest.append(others[this.trait.ordinal()]);
		} else {
		    match.getGroup(0, dest);
		}
	    } else if (match.isCaptured("Other")) {
		final String[] others = Messaging.irregular.get(match.group("Other").toLowerCase());
		if (others != null && others.length == 11) {
		    BeingSubstitution.appendCapitalized(others[this.trait.ordinal()], dest);
		} else {
		    match.getGroup(0, dest);
		}
	    } else {
		match.getGroup(0, dest);
	    }
	}
    }
}