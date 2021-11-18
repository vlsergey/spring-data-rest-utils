package io.github.vlsergey.springdatarestutils.with_nonexposed_one_to_many;

import java.util.List;
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

    private String parentValue;

    @OneToMany(mappedBy = "id.parent")
    @OrderColumn(name = "child_index")
    private List<TestChild> testOneToMany;

}
