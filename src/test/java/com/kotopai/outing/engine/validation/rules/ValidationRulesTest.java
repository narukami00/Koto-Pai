package com.kotopai.outing.engine.validation.rules;

import static com.kotopai.outing.engine.EngineFixtures.NABILA;
import static com.kotopai.outing.engine.EngineFixtures.RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.RAHIM_GUEST_OF_RAFSAN;
import static com.kotopai.outing.engine.EngineFixtures.TANVIR;
import static com.kotopai.outing.engine.model.ShareLine.of;
import static org.assertj.core.api.Assertions.assertThat;

import com.kotopai.common.money.Money;
import com.kotopai.outing.engine.EngineFixtures;
import com.kotopai.outing.engine.model.ItemLine;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;
import com.kotopai.outing.engine.model.ShareLine;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.engine.validation.ValidationIssue;
import com.kotopai.outing.engine.validation.ValidationRule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** One nested class per rule: each checks "passes on valid input" and "reports the right issue". */
class ValidationRulesTest {

    private static List<String> codes(ValidationRule rule, OutingSnapshot snapshot) {
        return rule.check(snapshot).stream().map(ValidationIssue::code).toList();
    }

    @Nested
    @DisplayName("AtLeastOneParticipantRule")
    class AtLeastOneParticipant {
        private final ValidationRule rule = new AtLeastOneParticipantRule();

        @Test
        void passesWithParticipants() {
            assertThat(rule.check(EngineFixtures.burgerNight())).isEmpty();
        }

        @Test
        void reportsEmptyOuting() {
            assertThat(codes(rule, OutingSnapshot.builder().build())).containsExactly("NO_PARTICIPANTS");
        }
    }

    @Nested
    @DisplayName("BillNotEmptyRule")
    class BillNotEmpty {
        private final ValidationRule rule = new BillNotEmptyRule();

        @Test
        void passesWithItems() {
            assertThat(rule.check(EngineFixtures.burgerNight())).isEmpty();
        }

        @Test
        void reportsNoItems() {
            assertThat(codes(rule, OutingSnapshot.builder().participants(RAFSAN).build()))
                .containsExactly("NO_ITEMS");
        }

        @Test
        void reportsZeroBill() {
            OutingSnapshot free = OutingSnapshot.builder().participants(RAFSAN)
                .personalItem("Free sample", "0", RAFSAN).build();

            assertThat(codes(rule, free)).containsExactly("BILL_NOT_POSITIVE");
        }
    }

    @Nested
    @DisplayName("KnownParticipantsRule")
    class KnownParticipants {
        private final ValidationRule rule = new KnownParticipantsRule();

        @Test
        void passesWhenEveryoneIsOnTheOuting() {
            assertThat(rule.check(EngineFixtures.burgerNight())).isEmpty();
        }

        @Test
        void reportsUnknownConsumerAndPayer() {
            OutingSnapshot snapshot = OutingSnapshot.builder()
                .participants(RAFSAN)
                .personalItem("Burger", "200", TANVIR)
                .paid(NABILA, "200")
                .build();

            List<ValidationIssue> issues = rule.check(snapshot);

            assertThat(issues).extracting(ValidationIssue::code).containsExactly("UNKNOWN_CONSUMER", "UNKNOWN_PAYER");
            assertThat(issues.get(0).message()).contains("user #2").contains("Burger");
            assertThat(issues.get(1).message()).contains("user #3").contains("200.00");
        }
    }

    @Nested
    @DisplayName("GuestOwnerPresentRule")
    class GuestOwnerPresent {
        private final ValidationRule rule = new GuestOwnerPresentRule();

        @Test
        void passesWhenOwnerIsOnTheOuting() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN, RAHIM_GUEST_OF_RAFSAN).build();

            assertThat(rule.check(snapshot)).isEmpty();
        }

        @Test
        void reportsGuestWhoseOwnerIsMissing() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(TANVIR, RAHIM_GUEST_OF_RAFSAN).build();

            List<ValidationIssue> issues = rule.check(snapshot);

            assertThat(issues).extracting(ValidationIssue::code).containsExactly("GUEST_OWNER_MISSING");
            assertThat(issues.get(0).message()).contains("guest #10").contains("user #1");
        }
    }

    @Nested
    @DisplayName("ItemHasConsumerRule")
    class ItemHasConsumer {
        private final ValidationRule rule = new ItemHasConsumerRule();

        @Test
        void passesOnValidItems() {
            assertThat(rule.check(EngineFixtures.burgerNight())).isEmpty();
        }

        @Test
        void reportsItemWithNobody() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
                .item(new ItemLine("Tea", Money.of(30), SplitType.EQUAL, List.of())).build();

            assertThat(codes(rule, snapshot)).containsExactly("ITEM_NO_CONSUMER");
        }

        @Test
        void reportsPersonalItemWithTwoPeople() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN, TANVIR)
                .item(new ItemLine("Burger", Money.of(200), SplitType.PERSONAL, List.of(of(RAFSAN), of(TANVIR)))).build();

            assertThat(codes(rule, snapshot)).containsExactly("PERSONAL_ITEM_MULTIPLE_CONSUMERS");
        }

        @Test
        void reportsDuplicateConsumer() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
                .equalItem("Tea", "30", RAFSAN, RAFSAN).build();

            assertThat(codes(rule, snapshot)).containsExactly("DUPLICATE_CONSUMER");
        }
    }

    @Nested
    @DisplayName("SharesSumTo100Rule")
    class SharesSumTo100 {
        private final ValidationRule rule = new SharesSumTo100Rule();

        private OutingSnapshot pizza(ShareLine... shares) {
            return OutingSnapshot.builder().participants(RAFSAN, TANVIR, NABILA)
                .percentageItem("Pizza", "1000", shares).build();
        }

        @Test
        void passesAt100() {
            assertThat(rule.check(EngineFixtures.burgerNight())).isEmpty();
        }

        @Test
        void decimalHundredCountsAs100() {
            assertThat(rule.check(pizza(of(RAFSAN, "50.00"), of(TANVIR, "30.00"), of(NABILA, "20.00")))).isEmpty();
            assertThat(rule.check(pizza(of(RAFSAN, "33.33"), of(TANVIR, "33.33"), of(NABILA, "33.34")))).isEmpty();
        }

        @Test
        void reportsTotalNot100() {
            List<ValidationIssue> issues = rule.check(pizza(of(RAFSAN, "50"), of(TANVIR, "30"), of(NABILA, "10")));

            assertThat(issues).extracting(ValidationIssue::code).containsExactly("PERCENTAGES_NOT_100");
            assertThat(issues.get(0).message()).contains("90").contains("Pizza");
        }

        @Test
        void reportsMissingAndNegativePercentages() {
            assertThat(codes(rule, pizza(of(RAFSAN, "100"), of(TANVIR)))).containsExactly("PERCENTAGE_MISSING");
            assertThat(codes(rule, pizza(of(RAFSAN, "120"), of(TANVIR, "-20")))).containsExactly("PERCENTAGE_NEGATIVE");
        }

        @Test
        void skipsPercentageItemWithNoConsumers() {
            // reported by ItemHasConsumerRule instead; this rule must not double-report or crash
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
                .item(new ItemLine("Pizza", Money.of(100), SplitType.PERCENTAGE, List.of())).build();

            assertThat(rule.check(snapshot)).isEmpty();
        }

        @Test
        void ignoresNonPercentageItems() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN, TANVIR)
                .equalItem("Tea", "30", RAFSAN, TANVIR).build();

            assertThat(rule.check(snapshot)).isEmpty();
        }
    }

    @Nested
    @DisplayName("PaidEqualsBillRule")
    class PaidEqualsBill {
        private final ValidationRule rule = new PaidEqualsBillRule();

        @Test
        void passesWhenPaidEqualsBill() {
            assertThat(rule.check(EngineFixtures.burgerNight())).isEmpty();
        }

        @Test
        void reportsShortfall() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
                .personalItem("Burger", "200", RAFSAN).paid(RAFSAN, "150").build();

            List<ValidationIssue> issues = rule.check(snapshot);

            assertThat(issues).extracting(ValidationIssue::code).containsExactly("PAID_NOT_EQUAL_BILL");
            assertThat(issues.get(0).message()).contains("150.00").contains("200.00").contains("short by 50.00");
        }

        @Test
        void reportsOverpayment() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
                .personalItem("Burger", "200", RAFSAN).paid(RAFSAN, "250").build();

            assertThat(rule.check(snapshot).get(0).message()).contains("over by 50.00");
        }

        @Test
        void nobodyPaidAnythingIsAShortfall() {
            OutingSnapshot snapshot = OutingSnapshot.builder().participants(RAFSAN)
                .personalItem("Burger", "200", RAFSAN).build();

            assertThat(codes(rule, snapshot)).containsExactly("PAID_NOT_EQUAL_BILL");
        }
    }

    @Test
    @DisplayName("ParticipantRef labels are readable in messages")
    void labels() {
        assertThat(ParticipantRef.user(7).label()).isEqualTo("user #7");
        assertThat(ParticipantRef.guest(10, 1).label()).isEqualTo("guest #10");
    }
}
