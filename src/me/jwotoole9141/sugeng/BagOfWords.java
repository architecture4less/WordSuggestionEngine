package me.jwotoole9141.sugeng;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A bag of n-grams representing sequences of words loaded from plain text.
 *
 * @author Jared O'Toole
 */
public class BagOfWords {

    /**
     * The default set of words ignored when loading an n-gram.
     */
    // this is an unmodifiable collection
    public static final Set<String> DEFAULT_STOP_WORDS = Set.of("the", "in", "a", "an", "to", "am", "is");

    /**
     * The default sanitizer, which cleans up a given word by trimming it
     * and ignoring every character outside the regex "[^a-zA-Z0-9-_]".
     */
    public static final Function<String, String> DEFAULT_SANITIZER = w -> w.trim().replaceAll("[^a-zA-Z0-9-_]", "");

    /**
     * The function used for case-insensitive comparison of ordered sets of words (n-grams).
     */
    protected static final Comparator<Set<String>> NGRAM_COMPARATOR =
            Comparator.comparing(set -> String.join("", set).toLowerCase());

    private final int order;
    private final Set<String> stopWords;
    private final Function<String, String> sanitizer;
    private TreeMap<String, Integer> words;
    private TreeMap<Set<String>, Integer> grams;
    private int gramCount;
    private int wordCount;

    /**
     * Creates an empty bag of words of {@code sequenceLen} order.
     * This bag will use {@link #DEFAULT_STOP_WORDS} and {@link #DEFAULT_SANITIZER}.
     *
     * @param sequenceLen the size of each n-gram
     */
    public BagOfWords(int sequenceLen) {
        this(sequenceLen, null, null);
    }

    /**
     * Creates an empty bag of words of {@code sequenceLen} order.
     * If {@code wordsToIgnore} is null, then {@link #DEFAULT_STOP_WORDS} is used.
     * If {@code wordSanitizer} is null, then {@link #DEFAULT_SANITIZER} is used.
     *
     * @param sequenceLen   the size of each n-gram
     * @param wordsToIgnore a list of stop-words or null
     * @param wordSanitizer a function that cleans up words or null
     */
    public BagOfWords(int sequenceLen, Set<String> wordsToIgnore, Function<String, String> wordSanitizer) {
        if (sequenceLen < 1) {
            throw new IllegalArgumentException("sequenceLen cannot be less than 1");
        }
        order = sequenceLen;
        stopWords = wordsToIgnore == null ? DEFAULT_STOP_WORDS : Set.copyOf(wordsToIgnore);
        sanitizer = wordSanitizer == null ? DEFAULT_SANITIZER : wordSanitizer;
        grams = new TreeMap<>(NGRAM_COMPARATOR);
        gramCount = 0;
        wordCount = 0;
    }

    /**
     * Creates a deep copy of the given bag of words.
     *
     * @param other the original bag of words
     */
    public BagOfWords(final BagOfWords other) {
        order = other.order;
        stopWords = other.stopWords;
        sanitizer = other.sanitizer;
        words = new TreeMap<>(other.words);
        grams = new TreeMap<>(other.grams.comparator());
        other.grams.forEach((key, value) -> grams.put(new LinkedHashSet<>(key), value));
        gramCount = other.gramCount;
        wordCount = other.wordCount;
    }

    /**
     * Gets the number of words in each n-gram of the bag.
     *
     * @return the size of each n-gram
     */
    public int getOrder() {
        return order;
    }

    /**
     * Gets the set of words ignored when filling the bag.
     *
     * @return the ignored words list
     */
    public Set<String> getStopWords() {
        return stopWords;
    }

    /**
     * Gets the function that is used to clean up words before they are placed into the bag.
     *
     * @return the word cleaner
     */
    public Function<String, String> getSanitizer() {
        // make sure it is the anti-viral kind!
        return sanitizer;
    }

    /**
     * Fills the bag with all of the words found in the given text.
     *
     * @param text some text containing words
     */
    public void load(String text) {
        fillBag(text.lines());
    }

    /**
     * Fills the bag with all of the words found in the given file.
     * You may specify the number of lines to skip in case the file has a header.
     *
     * @param textFile  the path to a plain text file
     * @param skipLines the number of header lines to skip
     * @throws IOException the file could not be found or read
     */
    public void loadFrom(Path textFile, int skipLines) throws IOException {
        fillBag(Files.lines(textFile).skip(skipLines));
    }

    /**
     * Puts new n-grams into the bag using the given words.
     * <p>
     * These words are sanitized using the bag's {@link #getSanitizer() sanitizer},
     * filtered using the bag's {@link #getStopWords() stopWords},
     * and converted into n-grams with the bag's {@link #getOrder order}.
     * Blank words are ignored.
     * If fewer words are given than this bag's order, nothing is put into the bag.
     *
     * @param rawLines lines of new words for the bag.
     */
    public void fillBag(Stream<String> rawLines) {

        // the MapReduce SPLIT step
        List<String> words = rawLines
                .map(l -> l.trim().split("\\s+"))
                .flatMap(Arrays::stream)
                .map(sanitizer)
                .filter(w -> !w.isBlank() && !stopWords.contains(w))
                .collect(Collectors.toList());

        if (words.size() < order) {
            return;  // maybe pad the n-gram instead?
        }

        // the MapReduce SHUFFLE and REDUCE steps
        for (int i = 0; i <= words.size() - order; i++) {
            Set<String> gram = new LinkedHashSet<>(words.subList(i, i + order));
            grams.merge(Collections.unmodifiableSet(gram), 1, Integer::sum);
            gramCount++;
        }

        wordCount += words.size();
    }

    /**
     * Clears this bag of all its words and n-grams.
     */
    public void clear() {
        words.clear();
        grams.clear();
        gramCount = 0;
        wordCount = 0;
    }

    /**
     * Gets the total number of words in this bag.
     *
     * @return the number of words counted
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * Gets the total number of n-grams in this bag.
     *
     * @return the number of n-grams counted
     */
    public int getGramCount() {
        return gramCount;
    }

    /**
     * Gets a view of this bag's unique words, which is kept sorted alphabetically.
     *
     * @return a view of the words with their counts
     */
    public Map<String, Integer> getWords() {
        return Collections.unmodifiableMap(words);
    }

    /**
     * Gets a view of this bag's unique n-grams, which is kept sorted alphabetically.
     * Each n-gram is an ordered set.
     *
     * @return a view of the n-grams with their counts
     */
    public Map<Set<String>, Integer> getGrams() {
        return Collections.unmodifiableMap(grams);
    }

    /**
     * Creates a new n-gram counter for this bag, sorted by count in reverse order.
     * Each n-gram is an ordered set.
     *
     * @return a view of the n-grams
     */
    public Map<Set<String>, Integer> getGramsByCount() {
        // if the counter sorted by amount is needed often,
        // then maybe cache it and update here only if outdated.
        return Collections.unmodifiableMap(grams.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new)));
    }
}
