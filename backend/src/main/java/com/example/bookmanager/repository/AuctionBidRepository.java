package com.example.bookmanager.repository;

import com.example.bookmanager.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    @Query("SELECT b FROM AuctionBid b JOIN FETCH b.user WHERE b.auction.id = :auctionId ORDER BY b.bidAmount DESC")
    List<AuctionBid> findByAuctionIdOrderByBidAmountDesc(@Param("auctionId") Long auctionId);
    
    Optional<AuctionBid> findTopByAuctionIdOrderByBidAmountDesc(Long auctionId);
    
    long countByAuctionId(Long auctionId);
    List<AuctionBid> findByUserIdOrderByBidTimeDesc(Long userId);
}