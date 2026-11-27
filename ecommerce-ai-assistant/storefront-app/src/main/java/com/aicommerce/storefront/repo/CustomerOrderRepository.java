package com.aicommerce.storefront.repo;

import com.aicommerce.storefront.domain.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Identity guardrail: there is intentionally NO unscoped lookup by orderId.
 * Every read is bound to the authenticated user's id (WHERE user_id = :userId).
 */
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String> {

    @Query("select o from CustomerOrder o where o.userId = :userId order by o.orderDate desc")
    List<CustomerOrder> findAllForUser(@Param("userId") Long userId);

    @Query("select o from CustomerOrder o where o.orderId = :orderId and o.userId = :userId")
    Optional<CustomerOrder> findByIdForUser(@Param("orderId") String orderId, @Param("userId") Long userId);
}
