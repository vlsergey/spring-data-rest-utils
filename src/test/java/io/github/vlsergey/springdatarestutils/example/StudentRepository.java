package io.github.vlsergey.springdatarestutils.example;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Long countByGroupGroupId(UUID groupId);

}
