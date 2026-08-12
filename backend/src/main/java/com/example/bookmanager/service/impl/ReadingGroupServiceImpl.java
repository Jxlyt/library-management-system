package com.example.bookmanager.service.impl;

import com.example.bookmanager.dto.ApiResponse;
import com.example.bookmanager.entity.GroupMember;
import com.example.bookmanager.entity.GroupPost;
import com.example.bookmanager.entity.ReadingGroup;
import com.example.bookmanager.entity.User;
import com.example.bookmanager.repository.GroupMemberRepository;
import com.example.bookmanager.repository.GroupPostRepository;
import com.example.bookmanager.repository.ReadingGroupRepository;
import com.example.bookmanager.repository.UserRepository;
import com.example.bookmanager.service.ReadingGroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ReadingGroupServiceImpl implements ReadingGroupService {

    private final ReadingGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupPostRepository postRepository;
    private final UserRepository userRepository;

    public ReadingGroupServiceImpl(ReadingGroupRepository groupRepository,
                                   GroupMemberRepository memberRepository,
                                   GroupPostRepository postRepository,
                                   UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ApiResponse<ReadingGroup> createGroup(String username, String name, String description, String coverColor) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        ReadingGroup group = new ReadingGroup();
        group.setName(name);
        group.setDescription(description);
        group.setCoverColor(coverColor);
        group.setCreator(user);
        ReadingGroup saved = groupRepository.save(group);

        // creator自动加入群
        GroupMember member = new GroupMember();
        member.setGroup(saved);
        member.setUser(user);
        memberRepository.save(member);

        return ApiResponse.success("创建成功", saved);
    }

    @Override
    @Transactional
    public ApiResponse<Void> joinGroup(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ReadingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小组不存在"));

        if (memberRepository.findByGroupAndUser(group, user).isPresent()) {
            return ApiResponse.error(400, "你已加入该小组");
        }

        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        memberRepository.save(member);
        group.setMemberCount(group.getMemberCount() + 1);
        groupRepository.save(group);

        return ApiResponse.success(null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> leaveGroup(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ReadingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小组不存在"));

        if (group.getCreator().getId().equals(user.getId())) {
            return ApiResponse.error(400, "创建者不能退出小组");
        }

        GroupMember member = memberRepository.findByGroupAndUser(group, user)
                .orElseThrow(() -> new RuntimeException("你不在该小组"));
        memberRepository.delete(member);
        group.setMemberCount(group.getMemberCount() - 1);
        groupRepository.save(group);

        return ApiResponse.success(null);
    }

    @Override
    @Transactional
    public ApiResponse<GroupPost> createPost(Long groupId, String username, String title, String content) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ReadingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小组不存在"));

        if (!memberRepository.findByGroupAndUser(group, user).isPresent()) {
            return ApiResponse.error(400, "请先加入小组才能发帖");
        }

        GroupPost post = new GroupPost();
        post.setGroup(group);
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        GroupPost saved = postRepository.save(post);
        group.setPostCount(group.getPostCount() + 1);
        groupRepository.save(group);

        return ApiResponse.success("发布成功", saved);
    }

    @Override
    @Transactional
    public ApiResponse<Void> deletePost(Long postId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        GroupPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        if (!post.getUser().getId().equals(user.getId())) {
            return ApiResponse.error(403, "只能删除自己的帖子");
        }

        ReadingGroup group = post.getGroup();
        postRepository.delete(post);
        group.setPostCount(group.getPostCount() - 1);
        groupRepository.save(group);

        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<Page<ReadingGroup>> getGroups(String keyword, Pageable pageable) {
        Page<ReadingGroup> page;
        if (StringUtils.hasText(keyword)) {
            page = groupRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            page = groupRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return ApiResponse.success(page);
    }

    @Override
    public ApiResponse<Page<ReadingGroup>> getMyGroups(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Page<GroupMember> myMembers = memberRepository.findByUserOrderByJoinedAtDesc(user, pageable);
        return ApiResponse.success(myMembers.map(GroupMember::getGroup));
    }

    @Override
    public ApiResponse<Page<GroupPost>> getGroupPosts(Long groupId, Pageable pageable) {
        return ApiResponse.success(postRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable));
    }

    @Override
    public ApiResponse<ReadingGroup> getGroup(Long groupId) {
        ReadingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小组不存在"));
        return ApiResponse.success(group);
    }

    @Override
    public ApiResponse<Boolean> checkMembership(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        ReadingGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小组不存在"));
        boolean isMember = memberRepository.findByGroupAndUser(group, user).isPresent();
        return ApiResponse.success(isMember);
    }
}