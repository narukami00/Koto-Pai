package com.kotopai.outing.domain;

import static com.kotopai.outing.domain.OutingStatus.ACTIVE;
import static com.kotopai.outing.domain.OutingStatus.CANCELLED;
import static com.kotopai.outing.domain.OutingStatus.DRAFT;
import static com.kotopai.outing.domain.OutingStatus.SETTLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.outing.engine.split.SplitType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OutingStatusTest {

    private static Function<OutingStatus, OutingStatus> action(String name) {
        return switch (name) {
            case "submit" -> OutingStatus::submit;
            case "cancel" -> OutingStatus::cancel;
            case "settle" -> OutingStatus::markSettled;
            default -> throw new IllegalArgumentException(name);
        };
    }

    /** The full transition table: every action from every status. */
    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @CsvSource({
        "DRAFT,     submit, ACTIVE",
        "DRAFT,     cancel, CANCELLED",
        "ACTIVE,    cancel, CANCELLED",
        "ACTIVE,    settle, SETTLED",
    })
    @DisplayName("Allowed transitions")
    void allowedTransitions(OutingStatus from, String action, OutingStatus expected) {
        assertThat(action(action).apply(from)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} cannot {1}")
    @CsvSource({
        "DRAFT,     settle",
        "ACTIVE,    submit",
        "SETTLED,   submit",
        "SETTLED,   cancel",
        "SETTLED,   settle",
        "CANCELLED, submit",
        "CANCELLED, cancel",
        "CANCELLED, settle",
    })
    @DisplayName("Forbidden transitions throw")
    void forbiddenTransitions(OutingStatus from, String action) {
        assertThatThrownBy(() -> action(action).apply(from))
            .isInstanceOfSatisfying(IllegalOutingTransitionException.class, e -> {
                assertThat(e.getFrom()).isEqualTo(from);
                assertThat(e.getAction()).isEqualTo(action);
                assertThat(e.getMessage()).contains(from.name());
            });
    }

    @ParameterizedTest(name = "{0}, payment recorded={1} -> editable={2}")
    @CsvSource({
        "DRAFT,     false, true",
        "DRAFT,     true,  true",
        "ACTIVE,    false, true",
        "ACTIVE,    true,  false",
        "SETTLED,   false, false",
        "CANCELLED, false, false",
    })
    @DisplayName("Editing rules")
    void editingRules(OutingStatus status, boolean paymentRecorded, boolean editable) {
        assertThat(status.canEdit(paymentRecorded)).isEqualTo(editable);
    }

    // --- the Outing aggregate delegates to its status ---

    private static Outing outing() {
        return new Outing("Trip", OutingCategory.TRAVEL, LocalDate.of(2026, 11, 6), 1L);
    }

    @Test
    @DisplayName("A new outing is a draft; submit, then settle walks the happy path")
    void happyPath() {
        Outing o = outing();
        assertThat(o.getStatus()).isEqualTo(DRAFT);

        o.submit();
        assertThat(o.getStatus()).isEqualTo(ACTIVE);

        o.markSettled();
        assertThat(o.getStatus()).isEqualTo(SETTLED);
    }

    @Test
    @DisplayName("Cancelling works from draft and active; a cancelled outing stays cancelled")
    void cancelling() {
        Outing draft = outing();
        draft.cancel();
        assertThat(draft.getStatus()).isEqualTo(CANCELLED);

        Outing active = outing();
        active.submit();
        active.cancel();
        assertThat(active.getStatus()).isEqualTo(CANCELLED);
        assertThatThrownBy(active::submit).isInstanceOf(IllegalOutingTransitionException.class);
    }

    @Test
    @DisplayName("Items can be added while draft or active, but not once settled or cancelled")
    void editingGuardsOnAggregate() {
        Outing o = outing();
        o.addItem("Tea", new BigDecimal("30.00"), SplitType.EQUAL);
        o.submit();
        o.addItem("Coffee", new BigDecimal("40.00"), SplitType.EQUAL);
        o.cancel();

        assertThatThrownBy(() -> o.addItem("Late", new BigDecimal("1.00"), SplitType.EQUAL))
            .isInstanceOf(IllegalOutingTransitionException.class).hasMessageContaining("edit");
        assertThatThrownBy(() -> o.addUserParticipant(5L)).isInstanceOf(IllegalOutingTransitionException.class);
        assertThatThrownBy(() -> o.addContribution(null, BigDecimal.ONE)).isInstanceOf(IllegalOutingTransitionException.class);
        assertThatThrownBy(() -> o.addGuestParticipant(null)).isInstanceOf(IllegalOutingTransitionException.class);
        assertThatThrownBy(() -> o.removeItem(null)).isInstanceOf(IllegalOutingTransitionException.class);
    }

    @Test
    @DisplayName("assertEditable blocks an active outing once a payment exists")
    void assertEditableUsesPaymentFlag() {
        Outing o = outing();
        o.submit();

        o.assertEditable(false);
        assertThatThrownBy(() -> o.assertEditable(true))
            .isInstanceOf(IllegalOutingTransitionException.class).hasMessageContaining("edit");
    }
}
