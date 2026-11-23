package com.kotopai.user.service;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.outing.repository.OutingRepository;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.ProfileStatsView;
import com.kotopai.user.dto.UpdateProfileRequest;
import com.kotopai.user.dto.UserProfileResponse;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;

@Service
@Transactional
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;
    private final FriendService friendService;
    private final OutingRepository outingRepository;
    private final DebtRepository debtRepository;

    public DefaultUserService(
            UserRepository userRepository,
            FriendService friendService,
            OutingRepository outingRepository,
            DebtRepository debtRepository) {
        this.userRepository = userRepository;
        this.friendService = friendService;
        this.outingRepository = outingRepository;
        this.debtRepository = debtRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserProfileResponse.from(user);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        user.updateProfile(request.displayName(), request.bio(), request.avatarColor());
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary findByCode(String userCode) {
        if (userCode == null || userCode.isBlank()) {
            throw new IllegalArgumentException("userCode must not be blank");
        }

        String normalizedCode = userCode.trim();
        if (normalizedCode.startsWith("@")) {
            normalizedCode = normalizedCode.substring(1).trim();
        }
        normalizedCode = normalizedCode.toLowerCase();

        User user = userRepository.findByUserCode(normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userCode", userCode));
        return UserSummary.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileStatsView getStats(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        int outingsCount = 0;
        try {
            outingsCount = outingRepository.findVisibleTo(userId, Pageable.unpaged()).size();
        } catch (Exception ignored) {
            // Defensive: resilient against uninitialized outing state
        }

        int friendsCount = 0;
        try {
            friendsCount = friendService.friendsOf(userId).size();
        } catch (Exception ignored) {
            // Defensive: resilient against uninitialized friend state
        }

        BigDecimal settledAmount = BigDecimal.ZERO.setScale(2);
        try {
            settledAmount = debtRepository.findAllByDebtorIdOrCreditorIdOrderByCreatedAtDesc(userId, userId).stream()
                    .filter(d -> d.getStatus() == DebtStatus.PAID)
                    .map(Debt::getAmount)
                    .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        } catch (Exception ignored) {
            // Defensive: resilient against uninitialized debt state
        }

        return new ProfileStatsView(outingsCount, friendsCount, settledAmount);
    }
}

