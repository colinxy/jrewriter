package jrewriter;

import java.util.*;
import java.util.stream.*;


/**
 * API for matching sequence of bytecode
 * Supports:
 *   Any
 *   Exactly
 *   Or
 *   Pair
 *
 * This experience taught me: never try to write DSL in java
 */

public class BytecodeSeqMatcher {

    // Matcher[] matchers;
    // List<Integer> matched;
    // int pos;

    // public BytecodeSeqMatcher(Matcher... matchers) {
    //     this.matchers = matchers;
    //     matched = new ArrayList<>();
    //     pos = 0;
    // }

    // public boolean match(int bytecode) {
    //     matched.add(bytecode);
    //     pos++;

    //     boolean match = matchers[pos-1].match(matched, pos-1);
    //     if (!match) {
    //         // TODO: use KMP partial match table
    //         // current assume there is no duplicate in m
    //         pos = 0;
    //         matched.subList(pos, matched.size()).clear();
    //     }

    //     return match;
    // }

    // public boolean fullmatch(int bytecode) {
    //     return pos == matchers.length;
    // }


    /**
     * Factory functions
     */
    public static Matcher Any = new Any();

    public static Matcher Exactly(int match) {
        return new Exactly(match);
    }

    public static Matcher Or(int... matchers) {
        return new Or(Arrays.stream(matchers)
                         .mapToObj(m -> new Exactly(m))
                         .toArray(Matcher[]::new));
    }

    public static Matcher Pair(int posBefore, int matchBefore, int matchAfter) {
        return new Pair(posBefore, Exactly(matchBefore), Exactly(matchAfter));
    }

    public static Matcher Pairs(int... matchPairs) {
        if (matchPairs.length % 3 != 0) {
            throw new IllegalArgumentException(
                "Pass in tuples of (posBefore, matchBefore, matchAfter)");
        }
        Matcher[] matchers = new Matcher[matchPairs.length/3];
        for (int i = 0; i < matchPairs.length; i += 3) {
            matchers[i/3] = Pair(matchPairs[i], matchPairs[i+1], matchPairs[i+2]);
        }
        return new Or(matchers);
    }


    /**
     * Matcher classes
     */
    abstract public static class Matcher {
        abstract boolean match(int bytecode);
        abstract boolean match(List<Integer> matchedSeq, int toMatchSeq);
    }

    public static class Any extends Matcher {
        boolean match(int bytecode) {
            return true;
        }

        boolean match(List<Integer> matchedSeq, int toMatchSeq) {
            return true;
        }
    }

    public static class Exactly extends Matcher {
        int match;

        public Exactly(int match) {
            this.match = match;
        }

        boolean match(int bytecode) {
            return match == bytecode;
        }

        boolean match(List<Integer> matchedSeq, int toMatchSeq) {
            return match(matchedSeq.get(toMatchSeq));
        }
    }

    public static class Or extends Matcher {
        Matcher[] matchers;

        public Or(Matcher[] matchers) {
            this.matchers = matchers;
        }

        boolean match(int bytecode) {
            return Arrays.stream(matchers).anyMatch(m -> m.match(bytecode));
        }

        boolean match(List<Integer> matchedSeq, int toMatchSeq) {
            return Arrays.stream(matchers)
                .anyMatch(m -> m.match(matchedSeq, toMatchSeq));
        }
    }

    /**
     * Specify a pair of bytecode instructions should happen with a
     * given distance
     */
    public static class Pair extends Matcher {
        int posBefore;
        Matcher matchBefore;
        Matcher matchCurrent;

        public Pair(int posBefore, Matcher matchBefore, Matcher matchCurrent) {
            if (posBefore <= 0)
                throw new IllegalArgumentException(
                    "posBefore should be positive");

            this.posBefore = posBefore;
            this.matchBefore = matchBefore;
            this.matchCurrent = matchCurrent;
        }

        boolean match(int bytecode) {
            throw new IllegalArgumentException(
                "Use boolean match(List<Integer>, int) instead");
        }

        /**
         * Return true if <code>matchBefore</code> matches
         * <code>matched[current-posBefore]</code>, and
         * <code>matchCurrent</code> matches
         * <code>matched[current]</code>
         *
         * As a result, <code>matchBefore</code> and
         * <code>matchCurrent</code> cannot be Pair
         */
        boolean match(List<Integer> matchedSeq, int toMatchSeq) {
            int bytecode = matchedSeq.get(toMatchSeq),
                bytecodeBefore = matchedSeq.get(toMatchSeq-posBefore);
            return matchBefore.match(bytecodeBefore)
                && matchCurrent.match(bytecode);
        }
    }
}
