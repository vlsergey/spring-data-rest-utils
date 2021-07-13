package io.github.vlsergey.springdatarestutils.example;

import java.util.UUID;

import javax.persistence.*;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
public class Student {

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID studentId;

    @ManyToOne
    @ToString.Exclude
    @Column(nullable = false)
    private @NonNull Group group;

}
