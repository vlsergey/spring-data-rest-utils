package io.github.vlsergey.springdatarestutils.withtransient;

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

    private int value;

    @Transient
    public int getValueTransient() {
	return this.getValue();
    }

}
