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
     * Class is part of exported entities set, but this mode is for patch
     */
    EXPOSED_PATCH(false) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getPatchTypeSuffix();
	}
    },

    EXPOSED_RETURN(false) {
	@Override
	public boolean generatedValuesMustPresent() {
	    return true;
	}
    },

    EXPOSED_SUBMIT(false) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getRequestTypeSuffix();
	}
    },

    /**
     * Container for properties that present in hierarchy base class.
     */
    INHERITANCE_BASE(false) {
	@Override
	public String getName(TaskProperties props, Class<?> cls) {
	    return props.getBaseTypePrefix() + cls.getSimpleName() + props.getDefaultTypeSuffix();
	}
    },

    /**
     * Extends {@link ClassMappingMode#INHERITANCE_BASE} and includes own
     * properties.
     */
    INHERITANCE_CHILD(false),

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
	public boolean generatedValuesMustPresent() {
	    return true;
	}

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

    public boolean generatedValuesMustPresent() {
	return false;
    }

    public String getName(TaskProperties props, Class<?> cls) {
	if (cls.isAssignableFrom(Link.class)) {
	    return props.getLinkTypeName();
	}
	return cls.getSimpleName() + props.getDefaultTypeSuffix();
    }
}