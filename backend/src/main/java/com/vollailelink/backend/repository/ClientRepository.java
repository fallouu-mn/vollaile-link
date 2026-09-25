package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByTelephone(String telephone);
    boolean existsByTelephone(String telephone);

    @Query("SELECT c FROM Client c WHERE LOWER(c.nom) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :query, '%')) OR c.telephone LIKE CONCAT('%', :query, '%')")
    List<Client> searchClients(@Param("query") String query);

    long countByCreatedAtAfter(OffsetDateTime date);
}
