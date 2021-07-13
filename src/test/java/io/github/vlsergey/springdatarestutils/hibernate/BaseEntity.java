package io.github.vlsergey.springdatarestutils.hibernate;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

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
    @UpdateTimestamp
    @Column(name = "updated", nullable = false)
    private @NonNull Instant updated;

}
