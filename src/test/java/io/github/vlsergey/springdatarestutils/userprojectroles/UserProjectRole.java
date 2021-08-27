package io.github.vlsergey.springdatarestutils.userprojectroles;

import java.util.UUID;

import javax.persistence.*;

import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@Setter
public class UserProjectRole {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    private @NonNull Project project;

    @Basic(optional = false)
    @Column(name = "project_id", updatable = false, nullable = false, insertable = false)
    private @NonNull UUID projectId;

    @Basic(optional = false)
    @Column(name = "role", nullable = false)
    private @NonNull String role;

    @JoinColumn(name = "username", nullable = false)
    @ManyToOne
    @ToString.Exclude
    private @NonNull User user;

    @Basic(optional = false)
    @Column(name = "username", updatable = false, nullable = false, insertable = false)
    private @NonNull String username;

    @Basic(optional = false)
    @Column(name = "user_project_role_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    private UUID userProjectRoleId;

}
