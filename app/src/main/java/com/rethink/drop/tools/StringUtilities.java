package com.rethink.drop.tools;


import java.util.ArrayList;

public class StringUtilities {
    public static ArrayList<String> parseHashTags(String text) {
        ArrayList<String> hashTags = new ArrayList<>();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '#') {
                int j = i;
                while (j < chars.length && chars[j] != ' ') {
                    j++;
                }
                hashTags.add(text.substring(i + 1,
                                            j));
            }
        }
        return hashTags;
    }
}
