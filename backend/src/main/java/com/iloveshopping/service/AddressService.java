package com.iloveshopping.service;

import com.iloveshopping.dto.user.AddressRequest;
import com.iloveshopping.dto.user.UserProfileResponse;
import com.iloveshopping.entity.Address;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.AddressRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<UserProfileResponse.AddressResponse> getUserAddresses() {
        User user = getCurrentUser();
        List<Address> addresses = addressRepository.findByUserId(user.getId());
        return addresses.stream()
                .map(UserProfileResponse.AddressResponse::from)
                .toList();
    }

    @Transactional
    public UserProfileResponse.AddressResponse addAddress(AddressRequest request) {
        User user = getCurrentUser();

        if (request.isDefault()) {
            addressRepository.unsetDefaultAddress(user.getId(), request.getType());
        }

        Address address = Address.builder()
                .user(user)
                .type(request.getType())
                .name(request.getName())
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .isDefault(request.isDefault())
                .build();

        address = addressRepository.save(address);
        return UserProfileResponse.AddressResponse.from(address);
    }

    @Transactional
    public UserProfileResponse.AddressResponse updateAddress(String id, AddressRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));

        User user = getCurrentUser();
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address", "id", id);
        }

        address.setType(request.getType());
        address.setName(request.getName());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPhone(request.getPhone());

        if (request.isDefault() && !address.getIsDefault()) {
            addressRepository.unsetDefaultAddress(user.getId(), request.getType());
            address.setIsDefault(true);
        }

        address = addressRepository.save(address);
        return UserProfileResponse.AddressResponse.from(address);
    }

    @Transactional
    public void deleteAddress(String id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));

        User user = getCurrentUser();
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address", "id", id);
        }

        addressRepository.deleteById(id);
    }

    @Transactional
    public UserProfileResponse.AddressResponse setDefaultAddress(String id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", id));

        User user = getCurrentUser();
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address", "id", id);
        }

        addressRepository.unsetDefaultAddress(user.getId(), address.getType());
        addressRepository.setDefaultAddress(id);
        address.setIsDefault(true);

        return UserProfileResponse.AddressResponse.from(address);
    }

    private User getCurrentUser() {
        // In production, use SecurityContextHolder
        return null;
    }
}