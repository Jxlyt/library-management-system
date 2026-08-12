package com.example.bookmanager.repository;

import com.example.bookmanager.entity.GroupMember;
import com.example.bookmanager.entity.ReadingGroup;
import com.example.bookmanager.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupAndUser(ReadingGroup group, User user);
    Page<GroupMember> findByUserOrderByJoinedAtDesc(User user, Pageable pageable);
    long countByGroup(ReadingGroup group);
}