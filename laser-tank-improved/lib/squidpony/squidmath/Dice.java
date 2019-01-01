package squidpony.squidmath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import regexodus.Matcher;
import regexodus.Pattern;
import squidpony.annotation.Beta;

/**
 * Class for emulating various traditional RPG-style dice rolls.
 *
 * Based on code from the Blacken library.
 *
 * @author yam655
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
@Beta
public class Dice implements Serializable {
    private static final long serialVersionUID = -488902743486431146L;
    private static final Pattern guessPattern = Pattern
	    .compile("\\s*(\\d+)?\\s*(?:([:])\\s*(\\d+))??\\s*(?:([d:])\\s*(\\d+))?\\s*(?:([+-/*])\\s*(\\d+))?\\s*");
    private RNG rng;

    /**
     * Creates a new dice roller that uses a random RNG seed for an RNG that it
     * owns.
     */
    public Dice() {
	this.rng = new RNG();
    }

    /**
     * Creates a new dice roller that uses the given RNG, which can be seeded before
     * it's given here. The RNG will be shared, not copied, so requesting a random
     * number from the same RNG in another place may change the value of the next
     * die roll this makes, and dice rolls this makes will change the state of the
     * shared RNG.
     *
     * @param rng an RNG that can be seeded; will be shared (dice rolls will change
     *            the RNG state outside here)
     */
    public Dice(final RNG rng) {
	this.rng = rng;
    }

    /**
     * Creates a new dice roller that will use its own RNG, seeded with the given
     * seed.
     *
     * @param seed a long to use as a seed for a new RNG (can also be an int, short,
     *             or byte)
     */
    public Dice(final long seed) {
	this.rng = new RNG(seed);
    }

    /**
     * Creates a new dice roller that will use its own RNG, seeded with the given
     * seed.
     *
     * @param seed a String to use as a seed for a new RNG
     */
    public Dice(final String seed) {
	this.rng = new RNG(seed);
    }

    /**
     * Sets the random number generator to be used.
     *
     * This method does not need to be called before using the methods of this
     * class.
     *
     * @param rng the source of randomness
     */
    public void setRandom(final RNG rng) {
	this.rng = rng;
    }

    /**
     * Rolls the given number of dice with the given number of sides and returns the
     * total of the best n dice.
     *
     * @param n     number of best dice to total
     * @param dice  total number of dice to roll
     * @param sides number of sides on the dice
     * @return sum of best n out of <em>dice</em><b>d</b><em>sides</em>
     */
    public int bestOf(final int n, final int dice, final int sides) {
	final int rolls = Math.min(n, dice);
	final ArrayList<Integer> results = new ArrayList<>(dice);
	for (int i = 0; i < dice; i++) {
	    results.add(this.rollDice(1, sides));
	}
	return this.bestOf(rolls, results);
    }

    /**
     * Totals the highest n numbers in the pool.
     *
     * @param n    the number of dice to be totaled
     * @param pool the dice to pick from
     * @return the sum
     */
    public int bestOf(final int n, final List<Integer> pool) {
	final int rolls = Math.min(n, pool.size());
	Collections.sort(pool);
	int ret = 0;
	for (int i = 0; i < rolls; i++) {
	    ret += pool.get(pool.size() - 1 - i);
	}
	return ret;
    }

    /**
     * Find the best n totals from the provided number of dice rolled according to
     * the roll group string.
     *
     * @param n     number of roll groups to total
     * @param dice  number of roll groups to roll
     * @param group string encoded roll grouping
     * @return the sum
     */
    public int bestOf(final int n, final int dice, final String group) {
	final int rolls = Math.min(n, dice);
	final ArrayList<Integer> results = new ArrayList<>(dice);
	for (int i = 0; i < rolls; i++) {
	    results.add(this.rollGroup(group));
	}
	return this.bestOf(rolls, results);
    }

    /**
     * Emulate a dice roll and return the sum.
     *
     * @param n     number of dice to sum
     * @param sides number of sides on the rollDice
     * @return sum of rollDice
     */
    public int rollDice(final int n, final int sides) {
	int ret = 0;
	for (int i = 0; i < n; i++) {
	    ret += this.rng.nextInt(sides) + 1;
	}
	return ret;
    }

    /**
     * Get a list of the independent results of n rolls of dice with the given
     * number of sides.
     *
     * @param n     number of dice used
     * @param sides number of sides on each die
     * @return list of results
     */
    public List<Integer> independentRolls(final int n, final int sides) {
	final List<Integer> ret = new ArrayList<>(n);
	for (int i = 0; i < n; i++) {
	    ret.add(this.rng.nextInt(sides) + 1);
	}
	return ret;
    }

    /**
     * Turn the string to a randomized number.
     *
     * The following types of strings are supported "42": simple absolute string
     * "10:20": simple random range (inclusive between 10 and 20) "d6": synonym for
     * "1d6" "3d6": sum of 3 6-sided dice "3:4d6": best 3 of 4 6-sided dice
     *
     * The following types of suffixes are supported "+4": add 4 to the value "-3":
     * subtract 3 from the value "*100": multiply value by 100 "/8": divide value by
     * 8
     *
     * @param group string encoded roll grouping
     * @return random number
     */
    public int rollGroup(final String group) {// TODO -- rework to tokenize and allow multiple chained operations
	final Matcher mat = Dice.guessPattern.matcher(group);
	int ret = 0;
	if (mat.matches()) {
	    final String num1 = mat.group(1); // number constant
	    final String wmode = mat.group(2); // between notation
	    final String wnum = mat.group(3); // number constant
	    final String mode = mat.group(4); // db
	    final String num2 = mat.group(5); // number constant
	    final String pmode = mat.group(6); // math operation
	    final String pnum = mat.group(7); // number constant
	    final int a = num1 == null ? 0 : Integer.parseInt(num1);
	    final int b = num2 == null ? 0 : Integer.parseInt(num2);
	    final int w = wnum == null ? 0 : Integer.parseInt(wnum);
	    if (num1 != null && num2 != null) {
		if (wnum != null) {
		    if (":".equals(wmode)) {
			if ("d".equals(mode)) {
			    ret = this.bestOf(a, w, b);
			}
		    }
		} else if ("d".equals(mode)) {
		    ret = this.rollDice(a, b);
		} else if (":".equals(mode)) {
		    ret = this.rng.between(a, b + 1);
		}
	    } else if (num1 != null) {
		if (":".equals(wmode)) {
		    ret = this.rng.between(a, w + 1);
		} else {
		    ret = a;
		}
	    } else if (num2 != null) {
		if (mode != null) {
		    switch (mode) {
		    case "d":
			ret = this.rollDice(1, b);
			break;
		    case ":":
			ret = this.rng.between(0, b + 1);
			break;
		    }
		}
	    }
	    if (pmode != null) {
		final int p = pnum == null ? 0 : Integer.parseInt(pnum);
		switch (pmode) {
		case "+":
		    ret += p;
		    break;
		case "-":
		    ret -= p;
		    break;
		case "*":
		    ret *= p;
		    break;
		case "/":
		    ret /= p;
		    break;
		}
	    }
	}
	return ret;
    }
}
