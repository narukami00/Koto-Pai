package com.kotopai.outing.engine.split;

/** How an item's cost is divided between its consumers. */
public enum SplitType {
    /** One consumer pays for the whole item. */
    PERSONAL,
    /** Divided equally between the consumers. */
    EQUAL,
    /** Divided by estimated percentages that add up to 100. */
    PERCENTAGE
}
