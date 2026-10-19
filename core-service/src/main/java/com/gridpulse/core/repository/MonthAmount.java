package com.gridpulse.core.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// one row of a per-month count or sum, the query names its columns "month" and "amount"
public interface MonthAmount {

    String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    Integer getMonth();

    Number getAmount();

    // fills all 12 months, 0 for the months the query did not return (index 0 = January)
    static double[] twelveMonths(List<MonthAmount> rows) {
        // month number -> amount, a month that comes twice keeps its last row
        Map<Integer, Double> byMonth = rows.stream()
                .collect(Collectors.toMap(MonthAmount::getMonth, row -> row.getAmount().doubleValue(),
                        (first, later) -> later));
        // jan to dec, a month without a row is 0
        return IntStream.rangeClosed(1, 12)
                .mapToDouble(month -> byMonth.getOrDefault(month, 0.0))
                .toArray();
    }
}
