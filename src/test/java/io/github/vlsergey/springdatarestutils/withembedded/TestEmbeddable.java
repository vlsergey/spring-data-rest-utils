package io.github.vlsergey.springdatarestutils.withembedded;

import javax.persistence.Embeddable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@NoArgsConstructor
@Setter
@ToString
public class TestEmbeddable {

    private int embValue;

}
