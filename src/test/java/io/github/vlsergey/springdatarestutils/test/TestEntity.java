package io.github.vlsergey.springdatarestutils.test;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Id;

import lombok.Data;
import lombok.NonNull;

@Data
public class TestEntity {

    @Column(name = "created", nullable = false, updatable = false)
    private @NonNull Instant created;

    @Id
    private UUID id;

    @Column(name = "parent", nullable = true)
    private @Nullable TestEntity parent;

    @Column(name = "updated", nullable = false)
    private @NonNull Instant updated;

}
