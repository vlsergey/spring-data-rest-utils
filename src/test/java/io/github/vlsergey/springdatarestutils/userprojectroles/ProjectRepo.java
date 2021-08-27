package io.github.vlsergey.springdatarestutils.userprojectroles;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepo extends JpaRepository<Project, UUID> {

}
