package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Address;

import java.util.List;

public interface AddressService {
    ApiResponse<List<Address>> getMyAddresses(String username);
    ApiResponse<Address> addAddress(String username, Address address);
    ApiResponse<Address> updateAddress(Long addressId, String username, Address address);
    ApiResponse<Void> deleteAddress(Long addressId, String username);
    ApiResponse<Address> setDefaultAddress(Long addressId, String username);
}