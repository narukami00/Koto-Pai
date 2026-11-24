package com.kotopai.admin.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.admin.dto.AdminSystemStats;
import com.kotopai.admin.dto.AdminUserView;
import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.friend.repository.FriendshipRepository;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.notification.repository.NotificationRepository;
import com.kotopai.outing.repository.OutingRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.repository.UserRepository;

@Service
@Transactional
public class DefaultAdminService implements AdminService {

    private final UserRepository userRepository;
    private final OutingRepository outingRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationRepository notificationRepository;
    private final DebtRepository debtRepository;

    public DefaultAdminService(
            UserRepository userRepository,
            OutingRepository outingRepository,
            FriendshipRepository friendshipRepository,
            NotificationRepository notificationRepository,
            DebtRepository debtRepository) {
        this.userRepository = userRepository;
        this.outingRepository = outingRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationRepository = notificationRepository;
        this.debtRepository = debtRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSystemStats getSystemStats() {
        long totalUsers = userRepository.count();
        long totalOutings = 0L;
        try {
            totalOutings = outingRepository.count();
        } catch (Exception ignored) {
        }

        long totalFriendships = 0L;
        try {
            totalFriendships = friendshipRepository.count();
        } catch (Exception ignored) {
        }

        long totalNotifications = 0L;
        try {
            totalNotifications = notificationRepository.count();
        } catch (Exception ignored) {
        }

        BigDecimal totalSettledVolume = BigDecimal.ZERO.setScale(2);
        try {
            totalSettledVolume = debtRepository.findAll().stream()
                    .filter(d -> d.getStatus() == DebtStatus.PAID)
                    .map(Debt::getAmount)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        } catch (Exception ignored) {
        }

        return new AdminSystemStats(
                totalUsers,
                totalOutings,
                totalFriendships,
                totalNotifications,
                totalSettledVolume
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserView> listUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserView::from)
                .toList();
    }

    @Override
    public void changeUserRole(Long currentAdminId, Long targetUserId, Role newRole) {
        Objects.requireNonNull(targetUserId, "targetUserId must not be null");
        Objects.requireNonNull(newRole, "newRole must not be null");

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        if (Objects.equals(currentAdminId, targetUserId) && newRole != Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot demote the sole remaining administrator");
            }
        }

        targetUser.changeRole(newRole);
    }
}

