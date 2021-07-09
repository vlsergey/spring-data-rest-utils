package io.github.vlsergey.springdatarestutils.projections;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(types = TestEntity.class)
public interface TestEntityDefaultProjection extends TestEntityProjectionParent {

    @Value("#{target.parent.parent}")
    TestEntity getGrandParent();

    TestEntity getParent();

    @Value("#{target.parentId}")
    UUID getParentId();

}
