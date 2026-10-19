package com.gridpulse.common.paging;

// one bar or slice of a chart: its label (a month, a status ...) and its value
public record ChartPoint(String label, double value) {
}
