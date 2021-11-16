package io.github.vlsergey.springdatarestutils.with_list_of_embeddables;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.*;

import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@Setter
@ToString
public class TestEntity {

    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private UUID id;

    @ElementCollection
    @SuppressWarnings("rawtypes")
    private Map testAnyElementCollection;

    @ElementCollection(targetClass = TestEmbeddable.class)
    @SuppressWarnings("rawtypes")
    private Map testExplicitElementCollection;

    @ElementCollection
    private @NonNull List<TestEmbeddable> testNonNullElementCollection;

    @ElementCollection
    private @Nullable List<TestEmbeddable> testNullableElementCollection;

}
