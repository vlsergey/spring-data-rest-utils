package io.github.vlsergey.springdatarestutils;

import org.springframework.hateoas.Link;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ClassMappingMode {
    /**
     * Class is not part of exported entities set, considered as POJO
     */
    DATA_ITEM(true),

    ENUM(false) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getEnumSuffix();
	}
    },

    /**
     * Class is part of exported entities set, but currently is included into
     * first-level projection
     */
    EXPOSED_NO_LINKS(false),

    /**
     * Class is part of exported entities set
     */
    EXPOSED_WITH_LINKS(false) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getWithLinksTypeSuffix();
	}
    },

    /**
     * Class is projection, top level
     */
    PROJECTION(true),

    ;

    @Getter
    private final boolean mappedEntitiesExpoded;

    public String getName(TaskProperties props, Class<?> cls) {
	if (cls.isAssignableFrom(Link.class)) {
	    return props.getLinkTypeName();
	}
	return cls.getSimpleName() + props.getTypeSuffix();
    }
}