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
	public String getName(PluginProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getEnumSuffix().get();
	}
    },

    /**
     * Class is projection, top level
     */
    PROJECTION(true),

    /**
     * Class is part of exported entities set, but currently is included into
     * first-level projection
     */
    SECOND_LEVEL(false),

    /**
     * Class is part of exported entities set
     */
    TOP_LEVEL_ENTITY(false) {
	@Override
	public String getName(PluginProperties props, Class<?> cls) {
	    return cls.getSimpleName() + props.getWithLinksTypeSuffix().get();
	}
    },

    ;

    @Getter
    private final boolean mappedEntitiesExpoded;

    public String getName(PluginProperties props, Class<?> cls) {
	if (cls.isAssignableFrom(Link.class)) {
	    return props.getLinkTypeName().get();
	}
	return cls.getSimpleName() + props.getTypeSuffix().get();
    }
}