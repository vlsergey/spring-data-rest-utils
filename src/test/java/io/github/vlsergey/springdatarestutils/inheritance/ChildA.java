package io.github.vlsergey.springdatarestutils.inheritance;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@DiscriminatorValue("a")
@Getter
@Setter
@ToString
public class ChildA extends BaseEntity {

    private String fromA;

}
