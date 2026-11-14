package com.kotopai.outing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kotopai.outing.api.dto.OutingDtos.ItemDto;
import com.kotopai.outing.api.dto.OutingDtos.OutingContentsRequest;
import com.kotopai.outing.api.dto.OutingDtos.OutingResponse;
import com.kotopai.outing.api.dto.OutingDtos.ParticipantDto;
import com.kotopai.outing.api.dto.OutingDtos.ShareDto;
import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.Item;
import com.kotopai.outing.domain.Outing;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.domain.OutingParticipant;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class OutingApiFacadeTest {

    private final OutingService service = mock(OutingService.class);
    private final OutingApiFacade facade = new OutingApiFacade(service);

    private static Outing sampleOuting() {
        Outing o = new Outing("Burger night", OutingCategory.EATING, LocalDate.of(2026, 11, 14), 1L);
        ReflectionTestUtils.setField(o, "id", 7L);
        Guest rahim = new Guest(1L, "Rahim");
        ReflectionTestUtils.setField(rahim, "id", 10L);
        OutingParticipant rafsan = o.addUserParticipant(1L);
        OutingParticipant guest = o.addGuestParticipant(rahim);
        Item pizza = o.addItem("Pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE);
        pizza.addShare(rafsan, new BigDecimal("60"));
        pizza.addShare(guest, new BigDecimal("40"));
        o.addContribution(rafsan, new BigDecimal("1000.00"));
        return o;
    }

    @Test
    @DisplayName("An outing becomes a response with totals, participants, shares and payments")
    void mapsEntityToResponse() {
        OutingResponse response = OutingApiFacade.toResponse(sampleOuting());

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.billTotal()).isEqualByComparingTo("1000.00");
        assertThat(response.paidTotal()).isEqualByComparingTo("1000.00");
        assertThat(response.participants()).hasSize(2);
        assertThat(response.participants().get(0).userId()).isEqualTo(1L);
        assertThat(response.participants().get(0).guestId()).isNull();
        assertThat(response.participants().get(1).guestId()).isEqualTo(10L);
        assertThat(response.participants().get(1).guestName()).isEqualTo("Rahim");
        assertThat(response.items().get(0).shares()).extracting(s -> s.percentage().intValue()).containsExactly(60, 40);
        assertThat(response.contributions()).hasSize(1);
    }

    @Test
    @DisplayName("The summary carries only the headline fields")
    void mapsSummary() {
        var summary = OutingApiFacade.toSummary(sampleOuting());

        assertThat(summary.title()).isEqualTo("Burger night");
        assertThat(summary.creatorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Use cases delegate to the service with the acting user and convert the result")
    void delegates() {
        Outing outing = sampleOuting();
        when(service.view(1L, 7L)).thenReturn(outing);
        when(service.submit(1L, 7L)).thenReturn(outing);
        when(service.cancel(1L, 7L)).thenReturn(outing);

        assertThat(facade.get(1L, 7L).id()).isEqualTo(7L);
        assertThat(facade.submit(1L, 7L).id()).isEqualTo(7L);
        assertThat(facade.cancel(1L, 7L).id()).isEqualTo(7L);
        facade.delete(1L, 7L);
        verify(service).delete(1L, 7L);
    }

    @Test
    @DisplayName("The page size is clamped between 1 and 100")
    void clampsLimit() {
        when(service.listVisibleTo(any(), any())).thenReturn(List.of());
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);

        facade.list(1L, 1000);
        facade.list(1L, 0);
        facade.list(1L, 20);

        verify(service, times(3)).listVisibleTo(any(), page.capture());
        assertThat(page.getAllValues()).extracting(Pageable::getPageSize).containsExactly(100, 1, 20);
    }

    @Test
    @DisplayName("A request converts to a command; a participant with both or neither id is refused")
    void requestToCommand() {
        OutingContentsRequest request = new OutingContentsRequest(
            List.of(new ParticipantDto(1L, null), new ParticipantDto(null, 10L)),
            List.of(new ItemDto("Pizza", new BigDecimal("1000.00"), SplitType.PERCENTAGE,
                List.of(new ShareDto(new ParticipantDto(1L, null), new BigDecimal("60"))))),
            List.of());

        OutingContents command = request.toCommand();

        assertThat(command.participants()).hasSize(2);
        assertThat(command.items().get(0).shares().get(0).percentage()).isEqualByComparingTo("60");
        assertThatThrownBy(() -> new ParticipantDto(1L, 2L).toKey()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ParticipantDto(null, null).toKey()).isInstanceOf(IllegalArgumentException.class);
    }
}
