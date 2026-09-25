package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.Producer;
import com.vollailelink.backend.model.enums.ProducerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProducerRepository extends JpaRepository<Producer, Long> {
    List<Producer> findByStatus(ProducerStatus status);

    long countByStatus(ProducerStatus status);
}
