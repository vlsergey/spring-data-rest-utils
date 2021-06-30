package io.github.vlsergey.springdatarestutils.inheritance;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BaseEntityRepo extends JpaRepository<BaseEntity, UUID> {

}
