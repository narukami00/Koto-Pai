package com.kotopai.outing.engine.validation;

import static com.kotopai.outing.engine.EngineFixtures.NABILA;
import static com.kotopai.outing.engine.EngineFixtures.RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.RAHIM_GUEST_OF_RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.TANVIR;
import static com.kotopai.outing.engine.model.ShareLine.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kotopai.outing.engine.EngineFixtures;
import com.kotopai.outing.engine.model.OutingSnapshot;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnapshotValidatorTest {

    private final SnapshotValidator validator = SnapshotValidator.withDefaultRules();

    @Test
    @DisplayName("The burger-night outing is valid")
    void burgerNightIsValid() {
        ValidationResult result = validator.validate(EngineFixtures.burgerNight());

        assertThat(result.isValid()).isTrue();
        assertThat(result.issues()).isEmpty();
        validator.validateOrThrow(EngineFixtures.burgerNight());
    }

    @Test
    @DisplayName("One broken outing: ALL problems are reported together, not just the first")
    void reportsEveryProblemAtOnce() {
        OutingSnapshot broken = OutingSnapshot.builder()
            .participants(RAFSAN, RAHIM_GUEST_OF_RAFSAN)
            .personalItem("Burger", "200", TANVIR)                                  // Tanvir is not on the outing
            .percentageItem("Pizza", "1000", of(RAFSAN, "50"), of(NABILA, "30"))    // Nabila not on it; 80% total
            .paid(RAFSAN, "1000")                                                   // bill is 1200
            .build();

        ValidationResult result = validator.validate(broken);

        assertThat(result.isValid()).isFalse();
        assertThat(result.codes()).containsExactly(
            "UNKNOWN_CONSUMER",       // Tanvir on Burger
            "UNKNOWN_CONSUMER",       // Nabila on Pizza
            "PERCENTAGES_NOT_100",    // 80, not 100
            "PAID_NOT_EQUAL_BILL");   // 1000 paid vs 1200 bill
        assertThat(result.messages()).hasSize(4);
    }

    @Test
    @DisplayName("validateOrThrow throws one exception carrying every issue")
    void validateOrThrowCarriesAllIssues() {
        OutingSnapshot empty = OutingSnapshot.builder().build();

        assertThatThrownBy(() -> validator.validateOrThrow(empty))
            .isInstanceOfSatisfying(EngineValidationException.class, e -> {
                assertThat(e.getResult().codes()).containsExactly("NO_PARTICIPANTS", "NO_ITEMS");
                assertThat(e.getMessage()).startsWith("Invalid outing:")
                    .contains("Add at least one participant.").contains("Add at least one item.");
            });
    }

    @Test
    @DisplayName("Custom rule sets work: rules run in the given order")
    void customRules() {
        SnapshotValidator custom = new SnapshotValidator(List.of(
            snapshot -> List.of(new ValidationIssue("FIRST", "first")),
            snapshot -> List.of(new ValidationIssue("SECOND", "second"))));

        assertThat(custom.validate(EngineFixtures.burgerNight()).codes()).containsExactly("FIRST", "SECOND");
    }

    @Test
    @DisplayName("Results and issues are immutable and null-safe")
    void immutableAndNullSafe() {
        ValidationResult result = new ValidationResult(List.of(new ValidationIssue("A", "a")));

        assertThatThrownBy(() -> result.issues().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new ValidationIssue(null, "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ValidationIssue("c", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SnapshotValidator(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EngineValidationException(null)).isInstanceOf(NullPointerException.class);
    }
}
