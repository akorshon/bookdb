package com.bookdb.util;

import com.ibm.icu.text.Transliterator;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Generates a plain-ASCII, phonetic Latin search key from Bengali text, so a book title like
 * "তোমাকে" is findable by someone typing "tomake". Uses ICU4J's Bengali-Latin transliterator
 * (a standard romanization) and then strips the diacritics it adds for long vowels etc. - e.g.
 * "তোমাকে" transliterates to "tōmākē", which strips down to exactly "tomake". This is
 * intentionally one-directional: the reverse (guessing Bengali spelling from a Latin name like
 * "Humayun Ahmed") produces poor, unnatural results with generic transliteration rules, so
 * author search instead relies on capturing the real Bengali name from the source site
 * (see {@code Author.nameNative}) rather than an algorithmic guess.
 */
public final class Transliteration {

    private static final Transliterator BENGALI_TO_LATIN = Transliterator.getInstance("Bengali-Latin");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private Transliteration() {
    }

    /** Returns a lowercase, diacritic-free Latin phonetic key, or null for blank/non-Bengali input. */
    public static String toLatinSearchKey(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String latin = BENGALI_TO_LATIN.transliterate(text);
        String decomposed = Normalizer.normalize(latin, Normalizer.Form.NFD);
        String stripped = COMBINING_MARKS.matcher(decomposed).replaceAll("");
        return stripped.toLowerCase();
    }
}
