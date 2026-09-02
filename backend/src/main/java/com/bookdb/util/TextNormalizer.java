package com.bookdb.util;

import java.text.Normalizer;

/**
 * Normalizes text to Unicode NFD. Bengali (and other Indic-script) text can represent the same
 * visible character multiple ways - e.g. "য়" as the single precomposed codepoint U+09DF, or as
 * "য" + "়" (U+09AF followed by the combining nukta mark U+09BC) - depending on the input method
 * or system that produced it. Both render identically but are different byte sequences, which
 * silently breaks exact/LIKE-based database search unless everything is normalized to one form
 * consistently: scraped text going in, and the user's search query coming back out.
 *
 * <p>NFD (fully decomposed), not NFC, is the form that actually works here: most Bengali nukta
 * letters - including U+09DF - are on Unicode's composition exclusion list, so NFC normalization
 * cannot recompose the decomposed form back into the precomposed one. Decomposition mappings
 * apply regardless of that exclusion, so NFD reliably reduces both variants to the same bytes.
 * This has no visible effect - any Unicode-aware renderer displays both forms identically.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String text) {
        return text == null ? null : Normalizer.normalize(text, Normalizer.Form.NFD);
    }
}
