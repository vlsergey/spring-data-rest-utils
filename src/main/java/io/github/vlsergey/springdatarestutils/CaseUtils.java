package io.github.vlsergey.springdatarestutils;

import java.util.regex.Pattern;

class CaseUtils {

    private static final Pattern pattern = Pattern.compile("([a-z])([A-Z]+)");
    private static final String KEBAB_REPLACEMENT = "$1-$2";
    private static final String SNAKE_REPLACEMENT = "$1_$2";

    static String camelToKebab(String str) {
	return pattern.matcher(str).replaceAll(KEBAB_REPLACEMENT).toLowerCase();
    }

    static String camelToSnake(String str) {
	return pattern.matcher(str).replaceAll(SNAKE_REPLACEMENT).toLowerCase();
    }

}
