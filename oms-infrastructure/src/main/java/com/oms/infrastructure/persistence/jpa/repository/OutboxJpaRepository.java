package com.oms.infrastructure.persistence.jpa.repository;

import com.oms.infrastructure.persistence.jpa.entity.OutboxJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    List<OutboxJpaEntity> findTop100ByProcessedFalseOrderByCreatedAtAsc();
}
