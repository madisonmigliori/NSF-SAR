
package com.nsf.langchain.utils;

import java.util.ArrayList;
import java.util.List;

public class TextUtils {

    public static List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        for (int start = 0; start < len; start += (chunkSize - overlap)) {
            int end = Math.min(len, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }
}
