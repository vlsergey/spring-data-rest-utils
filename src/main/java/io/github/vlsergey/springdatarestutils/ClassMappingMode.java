package io.github.vlsergey.springdatarestutils;

import org.springframework.hateoas.Link;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ClassMappingMode {

    /**
     * Class is not part of exported entities set, considered as POJO. Enums are
     * also goes here.
     */
    DATA_ITEM(true) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    if (cls.isEnum()) {
		return cls.getSimpleName() + props.getEnumTypeSuffix();
	    } else {
		return cls.getSimpleName() + props.getDefaultTypeSuffix();
	    }
	}
    },

    /**
     * Class is part of exported entities set.
     */
    EXPOSED(false),

    /**
     * Class is part of exported entities set, but this mode is for patch
     */
    EXPOSED_PATCH(false) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getPatchTypeSuffix();
	}
    },

    /**
     * The part of entity where _links property is described
     */
    LINKS(false) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getLinksTypeSuffix();
	}
    },

    WITH_LINKS(false) {
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
	return cls.getSimpleName() + props.getDefaultTypeSuffix();
    }
}