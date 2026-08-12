package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Address;
import com.example.bookmanager.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @GetMapping
    public ApiResponse<List<Address>> getMyAddresses(HttpServletRequest request) {
        return addressService.getMyAddresses(getCurrentUsername(request));
    }

    @PostMapping
    public ApiResponse<Address> addAddress(@RequestBody Address address, HttpServletRequest request) {
        return addressService.addAddress(getCurrentUsername(request), address);
    }

    @PutMapping("/{addressId}")
    public ApiResponse<Address> updateAddress(@PathVariable Long addressId, @RequestBody Address address, HttpServletRequest request) {
        return addressService.updateAddress(addressId, getCurrentUsername(request), address);
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long addressId, HttpServletRequest request) {
        return addressService.deleteAddress(addressId, getCurrentUsername(request));
    }

    @PutMapping("/{addressId}/default")
    public ApiResponse<Address> setDefault(@PathVariable Long addressId, HttpServletRequest request) {
        return addressService.setDefaultAddress(addressId, getCurrentUsername(request));
    }
}