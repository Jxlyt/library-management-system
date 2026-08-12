package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Address;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.AddressRepository;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ApiResponse<List<Address>> getMyAddresses(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return ApiResponse.success(addressRepository.findByUserOrderByIsDefaultDesc(user));
    }

    @Override
    @Transactional
    public ApiResponse<Address> addAddress(String username, Address address) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        address.setUser(user);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultByUser(user);
        }
        return ApiResponse.success("添加成功", addressRepository.save(address));
    }

    @Override
    @Transactional
    public ApiResponse<Address> updateAddress(Long addressId, String username, Address updated) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!address.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        address.setName(updated.getName());
        address.setPhone(updated.getPhone());
        address.setProvince(updated.getProvince());
        address.setCity(updated.getCity());
        address.setDistrict(updated.getDistrict());
        address.setDetail(updated.getDetail());
        if (Boolean.TRUE.equals(updated.getIsDefault())) {
            addressRepository.clearDefaultByUser(address.getUser());
        }
        address.setIsDefault(updated.getIsDefault());
        return ApiResponse.success("更新成功", addressRepository.save(address));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteAddress(Long addressId, String username) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!address.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        addressRepository.delete(address);
        return ApiResponse.success("删除成功", null);
    }

    @Override
    @Transactional
    public ApiResponse<Address> setDefaultAddress(Long addressId, String username) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));
        if (!address.getUser().getUsername().equals(username)) {
            return ApiResponse.error(403, "无权操作");
        }
        addressRepository.clearDefaultByUser(address.getUser());
        address.setIsDefault(true);
        return ApiResponse.success("设置成功", addressRepository.save(address));
    }
}