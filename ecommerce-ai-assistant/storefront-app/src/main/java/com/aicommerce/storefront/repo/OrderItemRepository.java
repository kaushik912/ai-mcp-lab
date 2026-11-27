package com.aicommerce.storefront.repo;

import com.aicommerce.storefront.domain.OrderItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Items are always reached through their owning order, joined on the owner's user id. */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select i from OrderItem i, CustomerOrder o
            where i.orderId = o.orderId and o.userId = :userId and i.orderId = :orderId
            order by i.id
            """)
    List<OrderItem> findByOrderForUser(@Param("orderId") String orderId, @Param("userId") Long userId);

    @Query("""
            select i from OrderItem i, CustomerOrder o
            where i.orderId = o.orderId and o.userId = :userId
              and i.orderId = :orderId and i.sku = :sku
            """)
    Optional<OrderItem> findByOrderAndSkuForUser(@Param("orderId") String orderId,
                                                 @Param("sku") String sku,
                                                 @Param("userId") Long userId);

    /** Same as findByOrderAndSkuForUser but row-locked: serialises concurrent return/voucher writes on one item. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from OrderItem i, CustomerOrder o
            where i.orderId = o.orderId and o.userId = :userId
              and i.orderId = :orderId and i.sku = :sku
            """)
    Optional<OrderItem> lockByOrderAndSkuForUser(@Param("orderId") String orderId,
                                                 @Param("sku") String sku,
                                                 @Param("userId") Long userId);

    @Query("""
            select i from OrderItem i, CustomerOrder o
            where i.orderId = o.orderId and o.userId = :userId
            order by o.orderDate desc, i.id
            """)
    List<OrderItem> findAllForUser(@Param("userId") Long userId);
}
