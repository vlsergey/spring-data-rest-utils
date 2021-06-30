package io.github.vlsergey.springdatarestutils.inheritance;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@DiscriminatorValue("b")
@Getter
@Setter
@ToString
public class ChildB extends BaseEntity {

    private String fromB;

}
