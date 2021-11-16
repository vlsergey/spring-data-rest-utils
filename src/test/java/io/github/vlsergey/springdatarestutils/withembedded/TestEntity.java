package io.github.vlsergey.springdatarestutils.withembedded;

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
public class TestEntity {

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private UUID id;

    @Embedded
    private TestEmbeddable embedded;

}
