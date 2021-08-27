package io.github.vlsergey.springdatarestutils.hibernate;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.*;

@Getter
@MappedSuperclass
@NoArgsConstructor
@Setter
@ToString
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(name = "created", nullable = false, updatable = false)
    private @NonNull Instant created;

    @LastModifiedDate
    @CreationTimestamp
    @UpdateTimestamp
    @Column(name = "updated", nullable = false)
    private @NonNull Instant updated;

}
