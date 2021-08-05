package io.github.vlsergey.springdatarestutils.example;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.Min;

import org.hibernate.annotations.Formula;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
public class Group {

    @Min(0)
    @Basic(fetch = FetchType.LAZY)
    @Formula("(SELECT COUNT(1) FROM students s WHERE s.group_id={alias}.group_id)")
    private long countOfStudents;

    @Nullable
    @Basic(fetch = FetchType.LAZY)
    @Formula("(SELECT MIN(name) FROM students s WHERE s.group_id={alias}.group_id)")
    @SingleLine
    private String firstStudentName;

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID groupId;

    @ManyToOne
    @ToString.Exclude
    private @NonNull Set<Student> students = new HashSet<>();

    private String[] subjects;

    @Basic(optional = false)
    @SingleLine
    private @NonNull String title;

}
