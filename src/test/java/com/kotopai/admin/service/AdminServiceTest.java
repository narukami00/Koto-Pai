package com.kotopai.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutingRepository outingRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DebtRepository debtRepository;

    @InjectMocks
    private DefaultAdminService adminService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User("admin", "Admin User", "admin@kotopai.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(adminUser, "id", 1L);

        regularUser = new User("regular", "Regular User", "regular@kotopai.com", "hash", Role.USER);
        ReflectionTestUtils.setField(regularUser, "id", 2L);
    }

    @Test
    @DisplayName("getSystemStats aggregates metrics across modules correctly")
    void getSystemStats_success() {
        when(userRepository.count()).thenReturn(10L);
        when(outingRepository.count()).thenReturn(5L);
        when(friendshipRepository.count()).thenReturn(8L);
        when(notificationRepository.count()).thenReturn(20L);

        Debt paidDebt1 = Debt.forOuting(10L, 1L, 2L, BigDecimal.valueOf(1500));
        ReflectionTestUtils.setField(paidDebt1, "status", DebtStatus.PAID);

        Debt paidDebt2 = Debt.forOuting(10L, 2L, 3L, BigDecimal.valueOf(500));
        ReflectionTestUtils.setField(paidDebt2, "status", DebtStatus.PAID);

        Debt pendingDebt = Debt.forOuting(10L, 1L, 3L, BigDecimal.valueOf(700));

        when(debtRepository.findAll()).thenReturn(List.of(paidDebt1, paidDebt2, pendingDebt));

        AdminSystemStats stats = adminService.getSystemStats();

        assertThat(stats.totalUsers()).isEqualTo(10L);
        assertThat(stats.totalOutings()).isEqualTo(5L);
        assertThat(stats.totalFriendships()).isEqualTo(8L);
        assertThat(stats.totalNotifications()).isEqualTo(20L);
        assertThat(stats.totalSettledVolume()).isEqualByComparingTo(BigDecimal.valueOf(2000.00));
        assertThat(stats.getFormattedSettledVolume()).isEqualTo("৳2,000.00");
    }

    @Test
    @DisplayName("listUsers returns all users mapped to AdminUserView")
    void listUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser, regularUser));

        List<AdminUserView> views = adminService.listUsers();

        assertThat(views).hasSize(2);
        assertThat(views.get(0).userCode()).isEqualTo("admin");
        assertThat(views.get(0).role()).isEqualTo(Role.ADMIN);
        assertThat(views.get(1).userCode()).isEqualTo("regular");
        assertThat(views.get(1).role()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("changeUserRole successfully promotes regular user to ADMIN")
    void changeUserRole_promote_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        adminService.changeUserRole(1L, 2L, Role.ADMIN);

        assertThat(regularUser.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("changeUserRole prevents self-demotion when sole admin")
    void changeUserRole_selfDemote_failsWhenSoleAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminService.changeUserRole(1L, 1L, Role.USER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sole remaining administrator");
    }

    @Test
    @DisplayName("changeUserRole allows self-demotion when multiple admins exist")
    void changeUserRole_selfDemote_succeedsWhenMultipleAdmins() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);

        adminService.changeUserRole(1L, 1L, Role.USER);

        assertThat(adminUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("changeUserRole throws ResourceNotFoundException if user does not exist")
    void changeUserRole_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.changeUserRole(1L, 99L, Role.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

