package com.yk.back.repository;

import com.yk.back.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findAllByProductIdOrderByCreatedAtDesc(UUID productId);

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN sm.type = 'IN'         THEN  sm.quantity
                WHEN sm.type = 'OUT'        THEN -sm.quantity
                WHEN sm.type = 'ADJUSTMENT' THEN  sm.quantity
                ELSE 0
            END
        ), 0)
        FROM StockMovement sm
        WHERE sm.product.id = :productId
    """)
    int currentStock(UUID productId);
}
