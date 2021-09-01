package io.github.vlsergey.springdatarestutils.withenum;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntityWithEnumRepo extends JpaRepository<EntityWithEnum, UUID> {

}
