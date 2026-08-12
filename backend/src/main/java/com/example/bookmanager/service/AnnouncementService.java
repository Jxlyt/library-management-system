package com.example.bookmanager.service;

import com.example.bookmanager.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AnnouncementService {

    Page<Announcement> findAll(Pageable pageable);

    Announcement create(Announcement announcement);

    Announcement update(Long id, Announcement announcement);

    void delete(Long id);

    List<Announcement> getLatest(int count);
}