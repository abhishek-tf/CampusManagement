package com.campus.enums;

/**
 * How a group expense is divided among its participants.
 * Mirrors the {@code group_expense.split_type} CHECK constraint in schema.sql.
 */
public enum SplitType {
    /** Divide the total equally among all group members. */
    EQUAL,
    /** Each participant owes an explicitly supplied amount. */
    EXACT,
    /** Each participant owes a supplied percentage of the total. */
    PERCENT
}
