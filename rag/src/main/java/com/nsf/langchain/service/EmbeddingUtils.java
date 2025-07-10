package com.nsf.langchain.service;

import java.util.Set;

public class EmbeddingUtils {

    private static final Set<String> SKIPPED_EXTENSIONS = Set.of(
        ".jar", ".class", ".exe", ".png", ".jpg", ".jpeg", ".gif",
        ".zip", ".tar", ".gz", ".min.js", ".map"
    );

    private static final Set<String> TEXTUAL_EXTENSIONS = Set.of(
        ".java", ".py", ".js", ".ts", ".cpp", ".c", ".go", ".rs",
        ".html", ".css", ".md", ".txt", ".xml", ".yml", ".yaml", ".json", ""
    );

    public enum EmbedDecision {
        EMBED_DIRECTLY,
        EMBED_CHUNKED,
        SKIP_FILE,
        SUMMARIZE_ONLY
    }

    public static EmbedDecision shouldEmbed(String fileName, int contentLength) {
        String lowerName = fileName.toLowerCase();

        for (String ext : SKIPPED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) return EmbedDecision.SKIP_FILE;
        }

        // if (!TEXTUAL_EXTENSIONS.stream().anyMatch(lowerName::endsWith)) {
        //     return EmbedDecision.SUMMARIZE_ONLY;
        // }

        if (contentLength > 10000000) {
            // return EmbedDecision.SKIP_FILE; // too large to handle safely
            return EmbedDecision.SUMMARIZE_ONLY;
        } else if (contentLength > 10000) {
            return EmbedDecision.EMBED_CHUNKED;
        } else {
            return EmbedDecision.EMBED_DIRECTLY;
        }
    }
}
