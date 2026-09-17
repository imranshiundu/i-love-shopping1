package com.iloveshopping.repository;

import com.iloveshopping.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, String> {

    List<ShippingMethod> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<ShippingMethod> findAllByOrderByDisplayOrderAsc();
}
