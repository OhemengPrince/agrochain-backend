package com.agrochain.backend.repository;

import com.agrochain.backend.model.BatchStage;
import com.agrochain.backend.model.ProduceBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchStageRepository extends JpaRepository<BatchStage, Long> {

    List<BatchStage> findByBatchOrderByCreatedAtAsc(ProduceBatch batch);
}
