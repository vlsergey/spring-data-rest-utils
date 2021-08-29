package io.github.vlsergey.springdatarestutils.baserepo;

import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@NoArgsConstructor
@Setter
@ToString
public class TestEntity {

    @Basic(optional = false)
    @Column(name = "entity_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private UUID entityId;

    @Column(nullable = false)
    private String name;

}
