package com.example.bookmanager.service;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.Auction;
import com.example.bookmanager.entity.AuctionBid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuctionService {
    ApiResponse<Auction> createAuction(Long bookId, Double startPrice, Double minIncrement, String endTime);
    ApiResponse<AuctionBid> placeBid(Long auctionId, Double bidAmount, String username);
    ApiResponse<Page<Auction>> getAuctions(String status, Pageable pageable);
    ApiResponse<Auction> getAuctionDetail(Long auctionId);
    ApiResponse<List<AuctionBid>> getBids(Long auctionId);
    ApiResponse<Auction> cancelAuction(Long auctionId);
    void checkAndEndAuctions();
}