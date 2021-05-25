package io.github.vlsergey.springdatarestutils.test;

import java.time.Instant;
import java.util.UUID;

import javax.persistence.*;

import lombok.Data;
import lombok.NonNull;

@Data
public class TestEntity {

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "created", nullable = false, updatable = false)
    private @NonNull Instant created;

    @Column(name = "updated", nullable = false)
    private @NonNull Instant updated;

}
