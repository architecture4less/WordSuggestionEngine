package me.jwotoole9141.sugeng;

import java.util.*;

/**
 * An assortment of classes and functions for performing
 * affinity analysis calculations.
 *
 * @author Jared O'Toole
 */
public class AffinityAnalysis {

    private AffinityAnalysis() {}

    /**
     * Computes the confidence and support of the given implications.
     *
     * @param implications unique implications and their counts
     * @param grams        unique n-grams and their counts
     * @param numActions   the total number of "transactions"
     * @param <T>          the type of the elements
     * @return the confidences and supports of each implication
     */
    public static <T> Set<Result<T>> analyze(
            Map<Implication<T>, Integer> implications, Map<Set<T>, Integer> grams, int numActions) {

        Set<Result<T>> results = new HashSet<>();

        for (Implication<T> implication : implications.keySet()) {
            int implicationCount = implications.get(implication);
            int groupCount = grams.get(implication.getUnion());

            /* confidence = ratio of "times item was bought" to  "times group was bought"
                 it represents how often an item exists in its group and not outside of it

               support = ratio of "times group was bought" to "number of transactions"
                 it represents how likely the item was bought at all */

            double confidence = implicationCount / (double) groupCount;
            double support = groupCount / (double) numActions;

            results.add(new Result<>(implication, confidence, support));
        }
        return results;
    }

    /**
     * A logical implication between a premise and a conclusion.
     *
     * @param <T> the type of the elements
     */
    public static class Implication<T> {

        private final Set<T> union;
        private final Set<T> premise;
        private final T conclusion;

        /**
         * Creates an implication by partitioning a given n-gram.
         * The n-gram should be an ordered set for predictable results.
         * It is also recommended the n-gram is unmodifiable.
         *
         * @param gram            the n-gram to create an implication for
         * @param conclusionIndex the index of the conclusion within the n-gram
         */
        public Implication(Set<T> gram, int conclusionIndex) {
            int t = conclusionIndex % gram.size();
            List<T> seq = new ArrayList<>(gram);
            Set<T> subset = new LinkedHashSet<>();

            for (int i = 0; i < gram.size(); i++) {
                if (i != t) {
                    subset.add(seq.get(i));
                }
            }
            this.union = gram;
            this.conclusion = seq.get(t);
            this.premise = Collections.unmodifiableSet(subset);
        }

        /**
         * Gets the set union of this implication's premise and conclusion.
         * This is equal to the n-gram this implication was produced from.
         *
         * @return this implication's n-gram
         */
        public Set<T> getUnion() {
            return union;
        }

        /**
         * Gets the premise of this implication.
         *
         * @return the antecedent
         */
        public Set<T> getPremise() {
            return premise;
        }

        /**
         * Gets the conclusion of this implication.
         *
         * @return the consequent
         */
        public T getConclusion() {
            return conclusion;
        }

        /**
         * Tests if this implication has the same n-gram,
         * premise, and conclusion as another implication.
         *
         * @param other the implication to test against
         * @return true if this is equal to other
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Implication<?>)) {
                return false;
            }
            Implication<?> that = (Implication<?>) other;
            return union.equals(that.union) &&
                    premise.equals(that.premise) &&
                    conclusion.equals(that.conclusion);
        }

        /**
         * Generates a hash code for this implication.
         *
         * @return a hash value
         */
        @Override
        public int hashCode() {
            return Objects.hash(union, conclusion);
        }

        /**
         * Creates a string to represent this implication.
         *
         * @return a descriptive string
         */
        @Override
        public String toString() {
            return "Implication{" +
                    "premise=" + premise +
                    ", conclusion=" + conclusion +
                    '}';
        }
    }

    /**
     * An implication with its confidence and support values
     * from an affinity analysis calculation.
     *
     * @param <T> the type of the elements
     */
    public static class Result<T> implements Comparable<Result<T>> {

        private final Implication<T> implication;
        private final double confidence;
        private final double support;

        /**
         * Creates a result for some implication.
         *
         * @param implication the implication
         * @param confidence  its confidence percentage
         * @param support     its support percentage
         */
        public Result(Implication<T> implication, double confidence, double support) {
            this.implication = implication;
            this.confidence = confidence;
            this.support = support;
        }

        /**
         * Gets the implication for this result.
         *
         * @return the implication
         */
        public Implication<T> getImplication() {
            return implication;
        }

        /**
         * Gets the confidence level for this result.
         * This is the ratio between the premise's count and its n-gram's count.
         *
         * @return percentage
         */
        public double getConfidence() {
            return confidence;
        }

        /**
         * Gets the support level for this result.
         * This is the ratio between the n-gram's count and the total rows of data.
         *
         * @return percentage
         */
        public double getSupport() {
            return support;
        }

        /**
         * Compares this result to another based on confidence.
         *
         * @param other another result
         * @return less than zero than other, vice versa, or zero if equivalent.
         */
        @Override
        public int compareTo(Result<T> other) {
            return Double.compare(this.confidence, other.confidence);
        }

        /**
         * Tests if this result is equal to another result,
         * requiring the same implication, confidence, and support.
         *
         * @param other the result to test against
         * @return true if this is equal to other
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Result<?>)) {
                return false;
            }
            Result<?> result = (Result<?>) other;
            return Double.compare(result.confidence, confidence) == 0 &&
                    Double.compare(result.support, support) == 0 &&
                    implication.equals(result.implication);
        }

        /**
         * Generates a hash code for this result.
         *
         * @return a hash value
         */
        @Override
        public int hashCode() {
            return Objects.hash(implication, confidence, support);
        }

        /**
         * Creates a string to represent this result.
         *
         * @return a descriptive string
         */
        @Override
        public String toString() {
            return "Result{" +
                    "implication=" + implication +
                    ", confidence=" + confidence +
                    ", support=" + support +
                    '}';
        }
    }
}
