package com.kotopai.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.kotopai.common.exception.ResourceNotFoundException;
import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.domain.Debt;
import com.kotopai.ledger.domain.DebtOrigin;
import com.kotopai.ledger.domain.DebtStatus;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.repository.OutingRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.ProfileStatsView;
import com.kotopai.user.dto.UpdateProfileRequest;
import com.kotopai.user.dto.UserProfileResponse;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendService friendService;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private DebtRepository debtRepository;

    @InjectMocks
    private DefaultUserService userService;

    private User createUser(Long id, String userCode, String displayName) {
        User user = new User(userCode, displayName, userCode + "@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("getProfile returns UserProfileResponse when user exists")
    void getProfile_success() {
        User user = createUser(1L, "rafsan10", "Rafsan");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userCode()).isEqualTo("rafsan10");
        assertThat(response.displayName()).isEqualTo("Rafsan");
        assertThat(response.getInitial()).isEqualTo("R");
    }

    @Test
    @DisplayName("getProfile throws ResourceNotFoundException when user does not exist")
    void getProfile_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }

    @Test
    @DisplayName("updateProfile updates displayName, bio, avatarColor successfully")
    void updateProfile_success() {
        User user = createUser(1L, "rafsan10", "Old Name");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "New Name", "New bio here", "#2B8C67");

        UserProfileResponse response = userService.updateProfile(1L, request);

        assertThat(response.displayName()).isEqualTo("New Name");
        assertThat(response.bio()).isEqualTo("New bio here");
        assertThat(response.avatarColor()).isEqualTo("#2B8C67");
    }

    @Test
    @DisplayName("findByCode normalizes handle with leading @ and case-insensitivity")
    void findByCode_success() {
        User user = createUser(2L, "tanvir_h", "Tanvir");
        when(userRepository.findByUserCode("tanvir_h")).thenReturn(Optional.of(user));

        UserSummary summary = userService.findByCode("  @Tanvir_H  ");

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isEqualTo(2L);
        assertThat(summary.userCode()).isEqualTo("tanvir_h");
    }

    @Test
    @DisplayName("findByCode throws ResourceNotFoundException when code does not exist")
    void findByCode_notFound() {
        when(userRepository.findByUserCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByCode("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getStats calculates outings, friends, and settled debts")
    void getStats_success() {
        when(outingRepository.findVisibleTo(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(org.mockito.Mockito.mock(Outing.class), org.mockito.Mockito.mock(Outing.class)));
        when(friendService.friendsOf(1L))
                .thenReturn(List.of(new UserSummary(2L, "tanvir", "Tanvir", "#18604B")));

        Debt paidDebt = Debt.forOuting(10L, 1L, 2L, BigDecimal.valueOf(1500));
        ReflectionTestUtils.setField(paidDebt, "status", DebtStatus.PAID);

        Debt openDebt = Debt.forOuting(20L, 1L, 3L, BigDecimal.valueOf(500));
        ReflectionTestUtils.setField(openDebt, "status", DebtStatus.OPEN);

        when(debtRepository.findAllByDebtorIdOrCreditorIdOrderByCreatedAtDesc(1L, 1L))
                .thenReturn(List.of(paidDebt, openDebt));

        ProfileStatsView stats = userService.getStats(1L);

        assertThat(stats.outingsCount()).isEqualTo(2);
        assertThat(stats.friendsCount()).isEqualTo(1);
        assertThat(stats.settledAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(stats.getFormattedSettledAmount()).contains("1,500");
    }
}
