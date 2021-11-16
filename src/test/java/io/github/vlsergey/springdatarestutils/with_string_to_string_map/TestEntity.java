package io.github.vlsergey.springdatarestutils.with_string_to_string_map;

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

    @ElementCollection(targetClass = String.class)
    @SuppressWarnings("rawtypes")
    private Map testExplicitElementCollection;

    @ElementCollection
    private @NonNull Map<String, String> testNonNullElementCollection;

    @ElementCollection
    private @Nullable Map<String, String> testNullableElementCollection;

}
