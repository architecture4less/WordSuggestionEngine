package me.jwotoole9141.sugeng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.jwotoole9141.sugeng.AffinityAnalysis.Implication;
import me.jwotoole9141.sugeng.AffinityAnalysis.Result;

/**
 * A word suggestion engine that uses affinity analysis on its bag of words to predict the next word
 * in a sequence.
 */
public class WordSuggester extends BagOfWords {

  /**
   * The default base suggestions used to pad the word suggestion results.
   */
  public static final List<String> DEFAULT_BASE_SUGGESTIONS = List.of("the", "this", "of");

  private double threshold;
  private final List<String> baseSuggestions;
  private final Map<Implication<String>, Integer> implications;
  private final List<Result<String>> analysis;
  private boolean untrained;
  private int messageCount;

  /**
   * Creates an empty word suggester of {@code sequenceLen} order and the given suggestion
   * confidence level. This bag will use {@link #DEFAULT_STOP_WORDS} and {@link
   * #DEFAULT_SANITIZER}.
   *
   * @param sequenceLen       the size of each n-gram
   * @param suggestConfidence the threshold for how correct a suggestion must be
   */
  public WordSuggester(int sequenceLen, double suggestConfidence) {
    this(sequenceLen, suggestConfidence, null, null, null);
  }

  /**
   * Creates an empty word suggester of {@code sequenceLen} order and the given suggestion
   * confidence level. If {@code wordsToIgnore} is null, then {@link #DEFAULT_STOP_WORDS} is used.
   * If {@code wordSanitizer} is null, then {@link #DEFAULT_SANITIZER} is used. If {@code
   * defaultSuggestions} is null, then {@link #DEFAULT_BASE_SUGGESTIONS} is used.
   *
   * @param sequenceLen       the size of each n-gram
   * @param suggestConfidence the threshold for how correct a suggestion must be
   * @param wordsToIgnore     a list of stop-words or null
   * @param wordSanitizer     a function that cleans up words or null
   */
  public WordSuggester(
      int sequenceLen,
      double suggestConfidence,
      List<String> defaultSuggestions,
      Set<String> wordsToIgnore,
      Function<String, String> wordSanitizer) {

    super(sequenceLen, wordsToIgnore, wordSanitizer);
    threshold = suggestConfidence;
    baseSuggestions =
        defaultSuggestions == null ? DEFAULT_BASE_SUGGESTIONS : List.copyOf(defaultSuggestions);
    implications = new HashMap<>();
    analysis = new ArrayList<>();
    untrained = false;
    messageCount = 0;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Each time this is called, the message count is incremented by one and this word suggester is
   * marked as untrained.
   */
  @Override
  public void fillBag(Stream<String> rawLines) {
    super.fillBag(rawLines);
    messageCount += 1;
    untrained = true;
  }

  /**
   * Clears the bag of words and any affinity analysis calculations.
   */
  @Override
  public void clear() {
    super.clear();
    implications.clear();
    analysis.clear();
    messageCount = 0;
    untrained = false;
  }

  /**
   * Gets the base suggestions used to pad word suggestions.
   *
   * @return a list of suggestions.
   */
  public List<String> getBaseSuggestions() {
    return baseSuggestions;
  }

  /**
   * Gets the threshold for how correct a suggestion must be.
   *
   * @return the confidence level
   */
  public double getThreshold() {
    return threshold;
  }

  /**
   * Tests if this suggestion engine should have {@link #train()} called or not.
   *
   * @return true if the bag of words have been updated
   */
  public boolean isUntrained() {
    return untrained;
  }

  /**
   * Performs affinity analysis calculations on the current bag of words.
   */
  public void train() {
    implications.clear();
    analysis.clear();

    // for every gram {a,b,c,...,d} we create the implication ({a,b,c,...} => d)
    for (var entry : getGrams().entrySet()) {
      implications.merge(
          new Implication<>(entry.getKey(), getOrder() - 1),
          entry.getValue(), Integer::sum);
    }

    // perform affinity analysis with our implications...
    analysis.addAll(AffinityAnalysis.analyze(implications, getGrams(), messageCount));
    untrained = false;
  }

  /**
   * Gets a view of this suggester's affinity analysis calculations.
   *
   * @return confidences and supports for the n-grams
   */
  public List<Result<String>> getAnalysis() {
    return Collections.unmodifiableList(analysis);
  }

  /**
   * Creates a list of word suggestions for the given sequence of words.
   * <p>
   * If more words are given than {@link #getOrder()}, then only the needed words are taken from the
   * end. If the number of suggestions is fewer than the length of {@link #getBaseSuggestions()},
   * the results are padded. For a possible suggestion to make it into this list, it must have a
   * confidence level greater than {@link #getThreshold()}.
   *
   * @param text one or more words.
   * @return a list of possible next words.
   */
  public List<String> getSuggestionsFor(String text) {

    // create a premise that represents the given words

    String[] words = text.trim().toLowerCase().split("\\s+");
    Set<String> premise = new LinkedHashSet<>();
    for (int i = words.length - getOrder(); i < words.length; i++) {
      if (i >= 0) {
        premise.add(getSanitizer().apply(words[i]));
      }
    }
    // find all the results with a matching premise & sufficient confidence level
    //  and add their conclusion as a suggestion result

    List<String> results = analysis.stream()
        .filter(r -> NGRAM_COMPARATOR.compare(premise, r.getImplication().getPremise()) == 0)
        .filter(r -> r.getConfidence() > threshold)
        .map(r -> r.getImplication().getConclusion())
        .collect(Collectors.toList());

    // pad the suggestion results if there weren't enough

    Iterator<String> iter = baseSuggestions.iterator();
    while (results.size() < baseSuggestions.size() && iter.hasNext()) {
      results.add(iter.next());
    }

    return results;
  }
}
