package io.github.vlsergey.springdatarestutils;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

class OptionalUtils {

    private static final Optional<Boolean> OP_FALSE = Optional.of(Boolean.FALSE);
    private static final Optional<Boolean> OP_TRUE = Optional.of(Boolean.TRUE);

    private static final Predicate<Optional<Boolean>> IS_FALSE = op -> op.isPresent() && !op.get().booleanValue();

    @SafeVarargs
    static Optional<Boolean> allTrue(Optional<Boolean>... args) {
	if (Arrays.stream(args).anyMatch(IS_FALSE)) {
	    return OP_FALSE;
	}
	if (args.length == 0 || Arrays.stream(args).anyMatch(o -> !o.isPresent())) {
	    return Optional.empty();
	}
	return OP_TRUE;
    }

    @SafeVarargs
    static <T> Optional<T> coalesce(Optional<T>... args) {
	for (Optional<T> op : args) {
	    if (op.isPresent()) {
		return op;
	    }
	}
	return Optional.empty();
    }

}
