package com.example.bookmanager.service.impl;

import com.example.bookmanager.entity.Announcement;
import com.example.bookmanager.repository.AnnouncementRepository;
import com.example.bookmanager.service.AnnouncementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementServiceImpl(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Override
    public Page<Announcement> findAll(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional
    public Announcement create(Announcement announcement) {
        return announcementRepository.save(announcement);
    }

    @Override
    @Transactional
    public Announcement update(Long id, Announcement announcement) {
        Announcement existing = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公告不存在"));
        existing.setTitle(announcement.getTitle());
        existing.setContent(announcement.getContent());
        return announcementRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        announcementRepository.deleteById(id);
    }

    @Override
    public List<Announcement> getLatest(int count) {
        return announcementRepository.findTop5ByOrderByCreatedAtDesc();
    }
}