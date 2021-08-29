package io.github.vlsergey.springdatarestutils.baserepo;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepo<T, ID extends Serializable> extends JpaRepository<T, ID> {

    @Override
    List<T> findAll();

    @Override
    long count();

}
