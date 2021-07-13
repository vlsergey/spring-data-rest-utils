package io.github.vlsergey.springdatarestutils.hibernate;

import java.util.UUID;

import javax.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor
@Setter
@ToString
public class TestEntity extends BaseEntity {

    @Basic(optional = false)
    @Column(name = "entity_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private UUID entityId;

}
