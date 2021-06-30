package io.github.vlsergey.springdatarestutils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

class ReflectionUtils {

    static Optional<Method> findMethod(Class<?> cls, String methodName, String... paramArgsClassNames) {
	return Arrays.stream(cls.getMethods()) //
		.filter(method -> Objects.equals(method.getName(), methodName))
		.filter(method -> method.getParameterCount() == paramArgsClassNames.length) //
		.filter(method -> {
		    Class<?>[] parameterTypes = method.getParameterTypes();
		    for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> arg = parameterTypes[i];
			if (!Objects.equals(arg.getName(), paramArgsClassNames[i])) {
			    return false;
			}
		    }
		    return true;
		}) //
		.findAny();

    }

}
