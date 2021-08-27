package io.github.vlsergey.springdatarestutils;

import java.util.function.Function;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum RequestType {

    CREATE(TaskProperties::getCreateTypePrefix, TaskProperties::getCreateTypeSuffix),

    PATCH(x -> "", TaskProperties::getPatchTypeSuffix),

    /**
     * This is part of 'parameter' argument in URL path
     */
    PARAMETER(x -> "", x -> ""),

    RESPONSE(x -> "", x -> "") {
	@Override
	public boolean generatedValuesWillPresent() {
	    return true;
	}
    },

    UPDATE(TaskProperties::getUpdateTypePrefix, TaskProperties::getUpdateTypeSuffix),

    ;

    @Getter
    private final @NonNull Function<TaskProperties, String> prefix;

    @Getter
    private final @NonNull Function<TaskProperties, String> suffix;

    public boolean generatedValuesWillPresent() {
	return false;
    }

}