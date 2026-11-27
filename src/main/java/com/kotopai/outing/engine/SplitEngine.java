package com.kotopai.outing.engine;

import com.kotopai.outing.engine.balance.BalanceCalculator;
import com.kotopai.outing.engine.balance.GuestFolder;
import com.kotopai.outing.engine.balance.ParticipantBalance;
import com.kotopai.outing.engine.model.OutingSnapshot;
import com.kotopai.outing.engine.validation.EngineValidationException;
import com.kotopai.outing.engine.validation.SnapshotValidator;
import java.util.List;
import java.util.Objects;

/**
 * Facade: the single entry point to the calculation engine. Callers (the preview API, the
 * activation service, the ledger adapter) give it a snapshot and get a result; they do not need to know that
 * validation, split strategies, balance calculation and guest folding exist or in which order they run.
 * Without it, every caller would repeat that sequence and one of them would eventually skip validation.
 */
public final class SplitEngine {

    private final SnapshotValidator validator;
    private final BalanceCalculator calculator;
    private final GuestFolder guestFolder;

    public SplitEngine(SnapshotValidator validator, BalanceCalculator calculator, GuestFolder guestFolder) {
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.guestFolder = Objects.requireNonNull(guestFolder, "guestFolder must not be null");
    }

    /** Engine wired with the standard rules, strategies and guest folding. */
    public static SplitEngine withDefaults() {
        return new SplitEngine(SnapshotValidator.withDefaultRules(),
            BalanceCalculator.withDefaultStrategies(), new GuestFolder());
    }

    /**
     * Validates, calculates and folds guests.
     *
     * @throws EngineValidationException carrying every problem if the snapshot is not calculable
     */
    public EngineResult calculate(OutingSnapshot snapshot) {
        validator.validateOrThrow(snapshot);
        List<ParticipantBalance> balances = calculator.calculate(snapshot);
        return new EngineResult(snapshot.billTotal(), balances, guestFolder.fold(balances));
    }
}
