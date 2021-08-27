package io.github.vlsergey.springdatarestutils.projections;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.Data;
import lombok.NonNull;

@Data
public class TestEntity {

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private @NonNull Instant created;

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "parent", nullable = true)
    private @Nullable TestEntity parent;

    @UpdateTimestamp
    @Column(name = "updated", nullable = false, insertable = false, updatable = false)
    private @NonNull Instant updated;

}
