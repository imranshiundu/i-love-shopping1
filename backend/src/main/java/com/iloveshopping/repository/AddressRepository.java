package com.iloveshopping.repository;

import com.iloveshopping.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    List<Address> findByUserId(String userId);

    List<Address> findByUserIdAndTypeId(String userId, Address.AddressType type);

    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.type = :type AND a.isDefault = true")
    int unsetDefaultAddress(@Param("userId") String userId, @Param("type") Address.AddressType type);

    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.isDefault = true WHERE a.id = :addressId")
    int setDefaultAddress(@Param("addressId") String addressId);
}