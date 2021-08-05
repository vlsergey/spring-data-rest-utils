package io.github.vlsergey.springdatarestutils.example;

import java.util.UUID;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
public class Student {

    @ManyToOne
    @ToString.Exclude
    @Column(nullable = false)
    private Group group;

    @Basic(optional = false)
    @Column(nullable = false)
    @SingleLine
    private String name;

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID studentId;

}
