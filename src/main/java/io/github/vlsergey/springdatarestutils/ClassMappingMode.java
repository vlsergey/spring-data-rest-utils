package io.github.vlsergey.springdatarestutils;

import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ClassMappingMode {

    /**
     * Class is not part of exported entities set, considered as POJO. Enums are
     * also goes here.
     */
    DATA_ITEM(true, x -> "", TaskProperties::getDefaultTypeSuffix),

    /**
     * Class is part of exported entities set, but this mode is for patch
     */
    EXPOSED(false, x -> "", TaskProperties::getDefaultTypeSuffix),

    /**
     * Container for properties that present in hierarchy base class.
     */
    INHERITANCE_BASE(false, TaskProperties::getBaseTypePrefix, TaskProperties::getDefaultTypeSuffix),

    /**
     * Extends {@link ClassMappingMode#INHERITANCE_BASE} and includes own
     * properties.
     */
    INHERITANCE_CHILD(false, x -> "", TaskProperties::getDefaultTypeSuffix),

    /**
     * The part of entity where _links property is described
     */
    LINKS(false, x -> "", TaskProperties::getLinksTypeSuffix),

    WITH_LINKS(false, x -> "", TaskProperties::getWithLinksTypeSuffix),

    /**
     * Class is projection, top level
     */
    PROJECTION(true, x -> "", TaskProperties::getDefaultTypeSuffix),

    ;

    private final boolean mappedEntitiesExpoded;

    @Getter
    private final Function<TaskProperties, String> prefix;

    @Getter
    private final Function<TaskProperties, String> suffix;

}