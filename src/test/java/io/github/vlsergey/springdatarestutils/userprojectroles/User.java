package io.github.vlsergey.springdatarestutils.userprojectroles;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@Setter
@ToString
public class User {

    @Column(name = "email", nullable = true)
    private String email;

    @Column(name = "name", nullable = false)
    private @NonNull String name;

    @Id
    @Column(name = "username", nullable = false, unique = true)
    private @NonNull String username;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @ToString.Exclude
    private List<UserProjectRole> userProjectRoles = new ArrayList<>(0);

}
