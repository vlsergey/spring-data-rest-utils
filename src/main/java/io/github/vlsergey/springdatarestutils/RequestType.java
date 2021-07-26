package io.github.vlsergey.springdatarestutils;

import java.util.function.Function;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum RequestType {

    PATCH(TaskProperties::getPatchTypeSuffix),

    /**
     * This is part of 'parameter' argument in URL path
     */
    PARAMETER(x -> ""),

    CREATE_OR_UPDATE(TaskProperties::getRequestTypeSuffix),

    RESPONSE(x -> "") {
	@Override
	public boolean generatedValuesWillPresent() {
	    return true;
	}
    };

    @Getter
    private final @NonNull Function<TaskProperties, String> suffix;

    public boolean generatedValuesWillPresent() {
	return false;
    }

}