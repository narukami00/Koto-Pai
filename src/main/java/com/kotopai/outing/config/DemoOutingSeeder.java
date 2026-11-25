package com.kotopai.outing.config;

import com.kotopai.outing.domain.Guest;
import com.kotopai.outing.domain.OutingCategory;
import com.kotopai.outing.engine.split.SplitType;
import com.kotopai.outing.service.GuestService;
import com.kotopai.outing.service.OutingCommands.ContributionInput;
import com.kotopai.outing.service.OutingCommands.CreateOuting;
import com.kotopai.outing.service.OutingCommands.ItemInput;
import com.kotopai.outing.service.OutingCommands.OutingContents;
import com.kotopai.outing.service.OutingCommands.ParticipantKey;
import com.kotopai.outing.service.OutingCommands.ShareInput;
import com.kotopai.outing.service.OutingService;
import com.kotopai.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Seeds a few demo outings for the local (h2) and demo profiles so a fresh run is not an empty app. It goes through
 * the real services, so submitting also generates the debts. It never runs under the {@code dev} profile (shared
 * database) and does nothing when Rafsan already has outings.
 */
@Component
@Profile({"h2", "demo"})
@Order(2)
public class DemoOutingSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoOutingSeeder.class);

    private final UserRepository users;
    private final OutingService outings;
    private final GuestService guests;

    public DemoOutingSeeder(UserRepository users, OutingService outings, GuestService guests) {
        this.users = users;
        this.outings = outings;
        this.guests = guests;
    }

    @Override
    public void run(String... args) {
        Long rafsan = idOf("rafsan10");
        Long tanvir = idOf("tanvir_h");
        Long nabila = idOf("nabila_r");
        Long samiha = idOf("samiha");
        if (rafsan == null || tanvir == null || nabila == null || samiha == null) {
            log.info("DemoOutingSeeder: demo users are missing; skipping.");
            return;
        }
        if (!outings.listVisibleTo(rafsan, PageRequest.of(0, 1)).isEmpty()) {
            log.info("DemoOutingSeeder: outings already exist; skipping.");
            return;
        }
        burgerNight(rafsan, tanvir, nabila);
        coxsBazarTrip(tanvir, rafsan, samiha);
        movieNightDraft(rafsan, nabila);
        log.info("DemoOutingSeeder: seeded 2 active outings and 1 draft.");
    }

    /** Rafsan's burger night with the guest Rahim: Rafsan paid 1050, Tanvir 500. */
    private void burgerNight(Long rafsan, Long tanvir, Long nabila) {
        Guest rahim = guests.create(rafsan, "Rahim");
        ParticipantKey r = ParticipantKey.user(rafsan);
        ParticipantKey t = ParticipantKey.user(tanvir);
        ParticipantKey n = ParticipantKey.user(nabila);
        ParticipantKey g = ParticipantKey.guest(rahim.getId());
        Long id = outings.create(rafsan, new CreateOuting("Burger night", OutingCategory.EATING,
            LocalDate.now().minusDays(2))).getId();
        outings.updateContents(rafsan, id, new OutingContents(List.of(r, t, n, g), List.of(
            new ItemInput("Large pizza", money("1000.00"), SplitType.PERCENTAGE, List.of(
                new ShareInput(r, money("40")), new ShareInput(t, money("20")),
                new ShareInput(n, money("10")), new ShareInput(g, money("30")))),
            new ItemInput("Fries", money("300.00"), SplitType.EQUAL, List.of(
                new ShareInput(r, null), new ShareInput(t, null), new ShareInput(n, null))),
            new ItemInput("Shake", money("250.00"), SplitType.PERSONAL, List.of(new ShareInput(g, null)))),
            List.of(new ContributionInput(r, money("1050.00")), new ContributionInput(t, money("500.00")))));
        outings.submit(rafsan, id);
    }

    /** Tanvir's trip, split equally, all paid by Tanvir, so Rafsan and Samiha owe him. */
    private void coxsBazarTrip(Long tanvir, Long rafsan, Long samiha) {
        ParticipantKey t = ParticipantKey.user(tanvir);
        ParticipantKey r = ParticipantKey.user(rafsan);
        ParticipantKey s = ParticipantKey.user(samiha);
        Long id = outings.create(tanvir, new CreateOuting("Cox's Bazar trip", OutingCategory.TRAVEL,
            LocalDate.now().minusDays(9))).getId();
        outings.updateContents(tanvir, id, new OutingContents(List.of(t, r, s), List.of(
            new ItemInput("Hotel", money("4500.00"), SplitType.EQUAL, List.of(
                new ShareInput(t, null), new ShareInput(r, null), new ShareInput(s, null))),
            new ItemInput("Bus tickets", money("1500.00"), SplitType.EQUAL, List.of(
                new ShareInput(t, null), new ShareInput(r, null), new ShareInput(s, null)))),
            List.of(new ContributionInput(t, money("6000.00")))));
        outings.submit(tanvir, id);
    }

    /** An unfinished draft only Rafsan can see, so the Drafts section and the Edit flow can be tried. */
    private void movieNightDraft(Long rafsan, Long nabila) {
        ParticipantKey r = ParticipantKey.user(rafsan);
        ParticipantKey n = ParticipantKey.user(nabila);
        Long id = outings.create(rafsan, new CreateOuting("Movie night", OutingCategory.ENTERTAINMENT,
            LocalDate.now().plusDays(3))).getId();
        outings.updateContents(rafsan, id, new OutingContents(List.of(r, n), List.of(
            new ItemInput("Tickets", money("800.00"), SplitType.EQUAL, List.of(
                new ShareInput(r, null), new ShareInput(n, null)))), List.of()));
    }

    private Long idOf(String userCode) {
        return users.findByUserCode(userCode).map(u -> u.getId()).orElse(null);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
