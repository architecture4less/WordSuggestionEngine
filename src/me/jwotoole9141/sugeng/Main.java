package me.jwotoole9141.sugeng;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;

/**
 * A driver to test the {@link WordSuggester} class.
 *
 * @author Jared O'Toole
 */
public class Main {

  /**
   * Creates a word suggester, loads {@code messages.txt} into it, and then enters a loop so the
   * user can test out word suggestions. The program exits when the user enters a blank line.
   *
   * @param args unused command line args
   */
  public static void main(String[] args) {

    // create the word suggestion engine for bi-grams, no stop-words, and 65% confidence level

    WordSuggester suggester = new WordSuggester(2, 0.65, null, Set.of(), null);
    try {
      Files.lines(Paths.get("res/messages.txt")).forEachOrdered(suggester::load);
    } catch (IOException ex) {
      System.out.println("Couldn't load word suggestion data: " + ex.getMessage());
      return;
    }
    suggester.train();

    // show suggestions for user input

    Scanner input = new Scanner(System.in);
    while (true) {

      System.out.println("Enter some text to see available suggestions.\n(leave blank to exit)");
      System.out.print(">>> ");
      String text = input.nextLine().trim();

      if (text.isBlank()) {
        break;
      }
      System.out.println("[\"" + String.join("\", \"", suggester.getSuggestionsFor(text)) + "\"]");
      System.out.println();
    }

    // show all the calculations on exit

    System.out.println("The affinity analysis calculations:");
    suggester.getAnalysis().forEach(r -> System.out.println("- " + r));
  }
}
