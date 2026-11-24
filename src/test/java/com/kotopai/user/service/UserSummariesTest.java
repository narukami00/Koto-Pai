package com.kotopai.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kotopai.friend.service.FriendService;
import com.kotopai.ledger.repository.DebtRepository;
import com.kotopai.outing.repository.OutingRepository;
import com.kotopai.user.domain.Role;
import com.kotopai.user.domain.User;
import com.kotopai.user.dto.UserSummary;
import com.kotopai.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserSummariesTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final DefaultUserService service = new DefaultUserService(
        repository, mock(FriendService.class), mock(OutingRepository.class), mock(DebtRepository.class));

    private static User user(long id, String code, String name) {
        User u = new User(code, name, code + "@example.com", "hash", Role.USER);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    @DisplayName("Several users are looked up at once and keyed by id; unknown ids are absent")
    void summariesByIds() {
        when(repository.findAllById(Set.of(1L, 2L, 99L)))
            .thenReturn(List.of(user(1, "rafsan10", "Rafsan"), user(2, "tanvir_h", "Tanvir")));

        Map<Long, UserSummary> result = service.summariesOf(Set.of(1L, 2L, 99L));

        assertThat(result).containsOnlyKeys(1L, 2L);
        assertThat(result.get(1L).displayName()).isEqualTo("Rafsan");
        assertThat(result.get(2L).userCode()).isEqualTo("tanvir_h");
    }

    @Test
    @DisplayName("A null id collection is refused")
    void nullRefused() {
        assertThatThrownBy(() -> service.summariesOf(null)).isInstanceOf(NullPointerException.class);
    }
}
