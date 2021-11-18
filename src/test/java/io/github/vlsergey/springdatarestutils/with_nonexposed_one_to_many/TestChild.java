package io.github.vlsergey.springdatarestutils.with_nonexposed_one_to_many;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@Setter
@ToString
public class TestChild {

    @EmbeddedId
    @JsonIgnore
    private Pk id;

    private String childValue;

    @Data
    @Embeddable
    public static class Pk implements Serializable {

	private static final long serialVersionUID = 1L;

	@ManyToOne
	private TestEntity parent;

	private int childIndex;
    }

}
