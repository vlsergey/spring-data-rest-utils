package io.github.vlsergey.springdatarestutils;

import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public enum ClassMappingMode {

    /**
     * Class is not part of exported entities set, considered as POJO. Enums are
     * also goes here.
     */
    DATA_ITEM(x -> "", TaskProperties::getDefaultTypeSuffix) {
	@Override
	boolean isCompatibleWith(@NonNull Class<?> cls) {
	    return true;
	}
    },

    /**
     * Class is part of exported entities set, but this mode is for patch
     */
    EXPOSED(x -> "", TaskProperties::getDefaultTypeSuffix),

    /**
     * Container for properties that present in hierarchy base class.
     */
    INHERITANCE_BASE(TaskProperties::getBaseTypePrefix, TaskProperties::getDefaultTypeSuffix),

    /**
     * Extends {@link ClassMappingMode#INHERITANCE_BASE} and includes own
     * properties.
     */
    INHERITANCE_CHILD(x -> "", TaskProperties::getDefaultTypeSuffix),

    /**
     * The part of entity where _links property is described
     */
    LINKS(x -> "", TaskProperties::getLinksTypeSuffix),

    /**
     * Class is projection, top level
     */
    PROJECTION(x -> "", TaskProperties::getDefaultTypeSuffix) {
	@Override
	boolean isCompatibleWith(@NonNull Class<?> cls) {
	    return cls.isInterface();
	}
    },

    WITH_LINKS(x -> "", TaskProperties::getWithLinksTypeSuffix),

    /**
     * Entity type that can be returned from finder or from getter (findOne)
     */
    WITH_PROJECTIONS(TaskProperties::getWithProjectionsTypePrefix, TaskProperties::getWithProjectionsTypeSuffix),

    ;

    @Getter
    private final Function<TaskProperties, String> prefix;

    @Getter
    private final Function<TaskProperties, String> suffix;

    boolean isCompatibleWith(final @NonNull Class<?> cls) {
	return !cls.isInterface();
    }

}