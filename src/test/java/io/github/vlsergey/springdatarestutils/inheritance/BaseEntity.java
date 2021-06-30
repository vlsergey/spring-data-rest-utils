package io.github.vlsergey.springdatarestutils.inheritance;

import java.util.UUID;

import javax.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "kind")
@DiscriminatorValue("stub")
@NoArgsConstructor
@Setter
@ToString
public class BaseEntity {

    private String fromBase;

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Basic(optional = false)
    @Column(name = "kind", updatable = false, nullable = false, insertable = false)
    private String kind;

}
