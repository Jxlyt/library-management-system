package com.example.bookmanager.service;

import com.example.bookmanager.entity.BorrowRecord;
import com.example.bookmanager.entity.ReadingPartner;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.BorrowRecordRepository;
import com.example.bookmanager.repository.ReadingPartnerRepository;
import com.example.bookmanager.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReadingPartnerService {

    private final ReadingPartnerRepository partnerRepository;
    private final UserRepository userRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    public ReadingPartnerService(ReadingPartnerRepository partnerRepository,
                                  UserRepository userRepository,
                                  BorrowRecordRepository borrowRecordRepository) {
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
        this.borrowRecordRepository = borrowRecordRepository;
    }

    /**
     * 每天0点执行匹配（实际用cron）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void dailyMatch() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        // 清除旧匹配
        partnerRepository.deleteByMatchDate(today);

        List<User> users = userRepository.findAll();
        if (users.size() < 2) return;

        // 为每个用户计算借阅图书的集合
        Map<Long, Set<Long>> userBooks = new HashMap<>();
        for (User u : users) {
            Set<Long> bookIds = borrowRecordRepository.findByUserUsernameOrderByBorrowDateDesc(u.getUsername())
                    .stream()
                    .map(r -> r.getBook().getId())
                    .collect(Collectors.toSet());
            userBooks.put(u.getId(), bookIds);
        }

        // 为每个用户找最佳匹配
        Set<Long> matched = new HashSet<>();
        for (User u : users) {
            if (matched.contains(u.getId())) continue;
            User bestPartner = null;
            int bestScore = -1;
            Set<Long> bestCommon = null;

            for (User p : users) {
                if (u.getId().equals(p.getId()) || matched.contains(p.getId())) continue;
                Set<Long> common = new HashSet<>(userBooks.getOrDefault(u.getId(), Collections.emptySet()));
                common.retainAll(userBooks.getOrDefault(p.getId(), Collections.emptySet()));
                int score = common.size();
                if (score > bestScore) {
                    bestScore = score;
                    bestPartner = p;
                    bestCommon = common;
                }
            }

            if (bestPartner != null) {
                ReadingPartner rp = new ReadingPartner();
                rp.setUser(u);
                rp.setPartner(bestPartner);
                rp.setMatchDate(today);
                rp.setCommonBooks(bestCommon.stream().map(String::valueOf).collect(Collectors.joining(",")));
                partnerRepository.save(rp);
                matched.add(u.getId());
                matched.add(bestPartner.getId());
            }
        }
    }

    public ReadingPartner getTodayMatch(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return partnerRepository.findByUserIdAndMatchDate(user.getId(), today).orElse(null);
    }
}