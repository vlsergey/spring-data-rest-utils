package io.github.vlsergey.springdatarestutils.withenum;

import java.util.UUID;

import javax.persistence.*;

import lombok.Data;

@Data
public class EntityWithEnum {

    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private TestEnum testEnum;

}
