package io.github.vlsergey.springdatarestutils.customfinders;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TestEntityRepo extends JpaRepository<TestEntity, UUID> {

    @Override
    Page<TestEntity> findAll(Pageable pageable);

    List<TestEntity> findByValue(int value);

    @Query("SELECT t FROM TestEntity t WHERE value=:value")
    List<TestEntity> findListByValue(int value);

    @Query("SELECT t FROM TestEntity t WHERE value IN (:value)")
    List<TestEntity> findListByValuesSet(Set<Integer> value);

    @Query("SELECT t FROM TestEntity t WHERE value=?1")
    TestEntity findOneSimple(int value);

    @Query("SELECT t FROM TestEntity t WHERE value=?1")
    Optional<TestEntity> findOneWrapped(int value);

}
