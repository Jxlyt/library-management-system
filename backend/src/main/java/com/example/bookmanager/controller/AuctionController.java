package com.example.bookmanager.controller;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Auction;
import com.example.bookmanager.entity.AuctionBid;
import com.example.bookmanager.service.AuctionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    private String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    @PostMapping("/create")
    public ApiResponse<Auction> create(@RequestBody Map<String, Object> body) {
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Double startPrice = Double.valueOf(body.get("startPrice").toString());
        Double minIncrement = body.get("minIncrement") != null ? Double.valueOf(body.get("minIncrement").toString()) : null;
        String endTime = body.get("endTime").toString();
        return auctionService.createAuction(bookId, startPrice, minIncrement, endTime);
    }

    @PostMapping("/{auctionId}/bid")
    public ApiResponse<AuctionBid> bid(@PathVariable Long auctionId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Double bidAmount = Double.valueOf(body.get("bidAmount").toString());
        return auctionService.placeBid(auctionId, bidAmount, getCurrentUsername(request));
    }

    @GetMapping
    public ApiResponse<Page<Auction>> getAuctions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auctionService.getAuctions(status, pageable);
    }

    @GetMapping("/{auctionId}")
    public ApiResponse<Auction> getDetail(@PathVariable Long auctionId) {
        return auctionService.getAuctionDetail(auctionId);
    }

    @GetMapping("/{auctionId}/bids")
    public ApiResponse<List<AuctionBid>> getBids(@PathVariable Long auctionId) {
        return auctionService.getBids(auctionId);
    }

    @PostMapping("/{auctionId}/cancel")
    public ApiResponse<Auction> cancel(@PathVariable Long auctionId) {
        return auctionService.cancelAuction(auctionId);
    }
}