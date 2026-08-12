package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.*;
import com.example.bookmanager.repository.*;
import com.example.bookmanager.service.AuctionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public AuctionServiceImpl(AuctionRepository auctionRepository, AuctionBidRepository auctionBidRepository,
                              BookRepository bookRepository, UserRepository userRepository,
                              OrderRepository orderRepository) {
        this.auctionRepository = auctionRepository;
        this.auctionBidRepository = auctionBidRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public ApiResponse<Auction> createAuction(Long bookId, Double startPrice, Double minIncrement, String endTime) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("图书不存在"));

        Auction auction = new Auction();
        auction.setBook(book);
        auction.setStartPrice(startPrice);
        auction.setCurrentPrice(startPrice);
        auction.setMinIncrement(minIncrement != null ? minIncrement : 1.0);
        auction.setStartTime(LocalDateTime.now());
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            auction.setEndTime(LocalDateTime.parse(endTime, formatter));
        } catch (Exception e) {
            auction.setEndTime(LocalDateTime.now().plusHours(24));
        }
        auction.setStatus("ACTIVE");
        auctionRepository.save(auction);
        return ApiResponse.success("拍卖创建成功", auction);
    }

    @Override
    @Transactional
    public ApiResponse<AuctionBid> placeBid(Long auctionId, Double bidAmount, String username) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("拍卖不存在"));

        if (!"ACTIVE".equals(auction.getStatus())) {
            return ApiResponse.error(400, "拍卖已结束");
        }

        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            auction.setStatus("ENDED");
            auctionRepository.save(auction);
            return ApiResponse.error(400, "拍卖已结束");
        }

        if (bidAmount <= auction.getCurrentPrice()) {
            return ApiResponse.error(400, "出价必须高于当前价格 ¥" + String.format("%.2f", auction.getCurrentPrice()));
        }

        if (bidAmount - auction.getCurrentPrice() < auction.getMinIncrement()) {
            return ApiResponse.error(400, "每次加价不能低于 ¥" + String.format("%.2f", auction.getMinIncrement()));
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        AuctionBid bid = new AuctionBid();
        bid.setAuction(auction);
        bid.setUser(user);
        bid.setBidAmount(bidAmount);
        bid.setBidTime(LocalDateTime.now());
        auctionBidRepository.save(bid);

        auction.setCurrentPrice(bidAmount);
        auctionRepository.save(auction);

        return ApiResponse.success("出价成功", bid);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Page<Auction>> getAuctions(String status, Pageable pageable) {
        Page<Auction> page;
        if (status != null && !status.isEmpty()) {
            if ("ENDED".equals(status)) {
                // "已结束" 标签展示所有非进行中的拍卖（已成交、流拍、已取消）
                page = auctionRepository.findByStatusInOrderByEndTimeDesc(List.of("ENDED", "FAILED", "CANCELLED"), pageable);
            } else {
                page = auctionRepository.findByStatusOrderByEndTimeAsc(status, pageable);
            }
        } else {
            page = auctionRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        page.forEach(a -> a.setBidCount((int)auctionBidRepository.countByAuctionId(a.getId())));
        return ApiResponse.success(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Auction> getAuctionDetail(Long auctionId) {
        Auction auction = auctionRepository.findByIdWithDetails(auctionId)
                .orElseThrow(() -> new RuntimeException("拍卖不存在"));
        auction.setBidCount((int)auctionBidRepository.countByAuctionId(auctionId));
        return ApiResponse.success(auction);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<AuctionBid>> getBids(Long auctionId) {
        return ApiResponse.success(auctionBidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId));
    }

    @Override
    @Transactional
    public ApiResponse<Auction> cancelAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("拍卖不存在"));
        if (!"ACTIVE".equals(auction.getStatus())) {
            return ApiResponse.error(400, "只能取消进行中的拍卖");
        }
        auction.setStatus("CANCELLED");
        auctionRepository.save(auction);
        return ApiResponse.success("拍卖已取消", auction);
    }

    @Override
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void checkAndEndAuctions() {
        List<Auction> activeAuctions = auctionRepository.findByStatus("ACTIVE");
        for (Auction auction : activeAuctions) {
            if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                // 查找最高出价
                AuctionBid topBid = auctionBidRepository.findTopByAuctionIdOrderByBidAmountDesc(auction.getId()).orElse(null);
                if (topBid != null) {
                    // 有出价：结束拍卖，生成订单
                    auction.setStatus("ENDED");
                    auction.setWinner(topBid.getUser());

                    // 生成拍卖订单
                    User winner = topBid.getUser();
                    Book book = auction.getBook();
                    String orderNumber = "AU" + System.currentTimeMillis() + (int)(Math.random() * 1000);

                    Order order = new Order();
                    order.setUser(winner);
                    order.setOrderNumber(orderNumber);
                    order.setTotalAmount(auction.getCurrentPrice());
                    order.setPayAmount(auction.getCurrentPrice());
                    order.setStatus("PAID"); // 拍卖订单直接已支付
                    order.setPayTime(LocalDateTime.now());

                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setBookId(book.getId());
                    item.setBookTitle(book.getTitle());
                    item.setPrice(auction.getCurrentPrice());
                    item.setQuantity(1);
                    item.setSubtotal(auction.getCurrentPrice());
                    order.getItems().add(item);

                    orderRepository.save(order);
                    auction.setOrderId(order.getId());

                    // 扣减库存
                    if (book.getSaleableCopies() != null && book.getSaleableCopies() > 0) {
                        book.setSaleableCopies(book.getSaleableCopies() - 1);
                        bookRepository.save(book);
                    }
                } else {
                    // 无人出价：流拍
                    auction.setStatus("FAILED");
                }
                auctionRepository.save(auction);
            }
        }
    }
}