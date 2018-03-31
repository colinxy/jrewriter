package jrewriter;

import java.util.*;
import java.util.stream.*;


/**
 * API (DSL) for matching sequence of bytecode
 * Supports:
 *   Any
 *   Exactly
 *   Or
 *   Pair
 *
 * This experience taught me: never try to write DSL in java
 */

public class BytecodeSeqMatcher {

    /**
     * Factory functions
     *
     * use with
     * import static jrewriter.BytecodeSeqMatcher.Matcher;
     * import static jrewriter.BytecodeSeqMatcher.Skip;
     * ...
     */
    public static Matcher Skip = new Skip();

    public static Matcher Any = new Any();

    public static Matcher Exactly(int match) {
        return new Exactly(match);
    }

    public static Matcher Or(int... matchers) {
        return new Or(Arrays.stream(matchers)
                         .mapToObj(m -> new Exactly(m))
                         .toArray(Matcher[]::new));
    }

    public static Matcher Or(Matcher... matchers) {
        return new Or(matchers);
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
     *
     * do not use directly
     */
    abstract public static class Matcher {
        /** test whether current bytecode matches */
        abstract boolean match(int bytecode);
        /**
         * test whether matchedSeq.get(toMatchSeq) matches;
         * if matches, return position in matchedSeq to match next;
         * otherwise, return -1
         */
        abstract int match(List<Integer> matchedSeq, int toMatchSeq);
    }

    public static class Skip extends Matcher {
        boolean match(int bytecode) {
            return true;
        }

        int match(List<Integer> matchedSeq, int toMatchSeq) {
            return toMatchSeq;
        }
    }

    public static class Any extends Matcher {
        boolean match(int bytecode) {
            return true;
        }

        int match(List<Integer> matchedSeq, int toMatchSeq) {
            return toMatchSeq+1;
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

        int match(List<Integer> matchedSeq, int toMatchSeq) {
            if (match(matchedSeq.get(toMatchSeq)))
                return toMatchSeq+1;
            return -1;
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

        int match(List<Integer> matchedSeq, int toMatchSeq) {
            for (Matcher m : matchers) {
                int toMatchSeqNext = m.match(matchedSeq, toMatchSeq);
                if (toMatchSeqNext != -1) {
                    return toMatchSeqNext;
                }
            }
            return -1;
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

        int match(List<Integer> matchedSeq, int toMatchSeq) {
            int bytecode = matchedSeq.get(toMatchSeq),
                bytecodeBefore = matchedSeq.get(toMatchSeq-posBefore);
            if (matchBefore.match(matchedSeq, toMatchSeq-posBefore) != -1) {
                int toMatchSeqNext = matchCurrent.match(matchedSeq, toMatchSeq);
                if (toMatchSeqNext != -1)
                    return toMatchSeqNext;
            }
            return -1;
        }
    }
}
