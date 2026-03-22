package com.oms.infrastructure.persistence.jpa.repository;

import com.oms.infrastructure.persistence.jpa.entity.OrderJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    /**
     * Cursor-based pagination: fetch orders for a customer created before the given cursor timestamp,
     * ordered by creation time descending.
     */
    @Query("SELECT o FROM OrderJpaEntity o WHERE o.customerId = :customerId " +
           "AND o.createdAt < :cursor ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findByCustomerIdWithCursor(
            @Param("customerId") UUID customerId,
            @Param("cursor") Instant cursor,
            Pageable pageable);
}
