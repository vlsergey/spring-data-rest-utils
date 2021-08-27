package io.github.vlsergey.springdatarestutils.userprojectroles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@Setter
@ToString
public class Project {

    @Id
    @Column(name = "project_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID projectId;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @ToString.Exclude
    private List<UserProjectRole> userProjectRoles = new ArrayList<>(0);

    @Column(name = "label", nullable = false, unique = false)
    private @NonNull String label;

}
