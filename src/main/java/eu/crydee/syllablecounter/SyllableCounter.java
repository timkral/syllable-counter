package eu.crydee.syllablecounter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fallback syllable counter.
 *
 * Based on the work of Thomas Jakobsen (thomj05@student.uia.no) and Thomas
 * Skardal (thomas04@student.uia.no) for the NLTK readability plugin.
 *
 * https://code.google.com/p/nltk/source/browse/trunk/nltk_contrib/nltk_contrib/readability/syllables_en.py
 *
 * Their work is itself based on the algorithm in Greg Fast's perl module
 * Lingua::EN::Syllable.
 */
public class SyllableCounter {

    private static int cacheSize;

    private final static Map<String, Integer> exceptions, cache;

    private final static List<Pattern> subSyl, addSyl;

    private final static Set<Character> vowels;

    static {
        cacheSize = 20000;

        cache = new HashMap<>();

        exceptions = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                SyllableCounter.class.getResourceAsStream("/eu/crydee/syllablecounter/english-exceptions.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] fields = line.split("	");
                    if (fields.length != 2) {
                        System.err.println("couldn't parse the exceptions "
                                + "file. Didn't find 2 fields in one of the "
                                + "lines.");
                    }
                    int nSyllables = Integer.parseInt(fields[0]);
                    String word = fields[1];
                    exceptions.put(word, nSyllables);
                }
            }
        } catch (IOException ex) {
            System.err.println("couldn't open the exceptions file.");
            ex.printStackTrace(System.err);
        }

        subSyl = Arrays.asList("cial", "tia", "cius", "cious", "gui", "ion",
                "iou", "sia$", ".ely$").stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());

        addSyl = Arrays.asList("ia", "riet", "dien", "iu", "io", "ii",
                "[aeiouy]bl$", "mbl$",
                "[aeiou]{3}",
                "^mc", "ism$",
                "(.)(?!\\1)([aeiouy])\\2l$",
                "[^l]llien",
                "^coad.", "^coag.", "^coal.", "^coax.",
                "(.)(?!\\1)[gq]ua(.)(?!\\2)[aeiou]",
                "dnt$").stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());
        vowels = new HashSet<>();
        vowels.add('a');
        vowels.add('e');
        vowels.add('i');
        vowels.add('o');
        vowels.add('u');
        vowels.add('y');
    }

    /**
     *
     * @param cacheSize the new size of the cache. Negative to disable caching.
     * Won't clean a cache that was bigger than the new size before.
     */
    public static void setCacheSize(int cacheSize) {
        SyllableCounter.cacheSize = cacheSize;
    }

    /**
     * Only entry point of this library. Method to count the number of syllables
     * of a word using a fallback method as documented at the class level of
     * this documentation.
     *
     * @param word the word you want to count the syllables of.
     * @return the number of syllables of the word.
     */
    public static int count(String word) {
        if (word == null) {
            return 0;
        } else if (word.length() == 1) {
            return 1;
        }

        word = word.toLowerCase(Locale.ENGLISH);

        if (exceptions.containsKey(word)) {
            return exceptions.get(word);
        }

        if (cacheSize > 0 && cache.containsKey(word)) {
            return cache.get(word);
        }

        if (word.charAt(word.length() - 1) == 'e') {
            word = word.substring(0, word.length() - 1);
        }

        int count = 0;
        boolean prevIsVowel = false;
        for (char c : word.toCharArray()) {
            boolean isVowel = vowels.contains(c);
            if (isVowel && !prevIsVowel) {
                ++count;
            }
            prevIsVowel = isVowel;
        }
        for (Pattern pattern : addSyl) {
            if (pattern.matcher(word).find()) {
                ++count;
            }
        }
        for (Pattern pattern : subSyl) {
            if (pattern.matcher(word).find()) {
                --count;
            }
        }

        if (cacheSize > 0 && cache.size() < cacheSize) {
            cache.put(word, count);
        }

        return count;
    }
}
