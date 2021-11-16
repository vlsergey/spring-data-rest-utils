package io.github.vlsergey.springdatarestutils.with_list_of_strings;

import java.util.List;
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
    private List testAnyElementCollection;

    @ElementCollection(targetClass = String.class)
    @SuppressWarnings("rawtypes")
    private List testExplicitElementCollection;

    @ElementCollection
    private @NonNull List<String> testNonNullElementCollection;

    @ElementCollection
    private @Nullable List<String> testNullableElementCollection;

}
