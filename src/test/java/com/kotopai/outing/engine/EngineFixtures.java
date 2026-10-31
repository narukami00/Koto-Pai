package com.kotopai.outing.engine;

import static com.kotopai.outing.engine.model.ShareLine.of;

import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.model.ParticipantRef;

/** Shared test data: the "burger night" example from ARCHITECTURE.md §3. */
public final class EngineFixtures {

    public static final ParticipantRef RAFSAN = ParticipantRef.user(1);
    public static final ParticipantRef TANVIR = ParticipantRef.user(2);
    public static final ParticipantRef NABILA = ParticipantRef.user(3);
    public static final ParticipantRef RAHIM_GUEST_OF_RAFSAN = ParticipantRef.guest(10, 1);

    private EngineFixtures() {
    }

    /**
     * Burger 200 (Rafsan), pizza 500 (Tanvir), cold coffee 200 (Nabila),
     * large pizza 1000 shared 50/30/20; Rafsan paid 1400, Tanvir 500. Bill 1900.
     * Expected nets: Rafsan +700, Tanvir -300, Nabila -400.
     */
    public static OutingSnapshot burgerNight() {
        return OutingSnapshot.builder()
            .participants(RAFSAN, TANVIR, NABILA)
            .personalItem("Burger", "200", RAFSAN)
            .personalItem("Pizza", "500", TANVIR)
            .personalItem("Cold coffee", "200", NABILA)
            .percentageItem("Large pizza", "1000", of(RAFSAN, "50"), of(TANVIR, "30"), of(NABILA, "20"))
            .paid(RAFSAN, "1400")
            .paid(TANVIR, "500")
            .build();
    }
}
