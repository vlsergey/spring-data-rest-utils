package io.github.vlsergey.springdatarestutils.example;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.*;

import org.hibernate.annotations.Formula;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
public class Group {

    @Basic(fetch = FetchType.LAZY)
    @Formula("(SELECT COUNT(1) FROM students s WHERE s.group_id={alias}.group_id)")
    private long countOfStudents;

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID groupId;

    @ManyToOne
    @ToString.Exclude
    private @NonNull Set<Student> students = new HashSet<>();

    @Basic(optional = false)
    @ToString.Exclude
    private @NonNull String title;

}
