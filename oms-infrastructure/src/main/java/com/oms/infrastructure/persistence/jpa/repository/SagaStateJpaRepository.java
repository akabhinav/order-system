package com.oms.infrastructure.persistence.jpa.repository;

import com.oms.infrastructure.persistence.jpa.entity.SagaStateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SagaStateJpaRepository extends JpaRepository<SagaStateJpaEntity, UUID> {
}
