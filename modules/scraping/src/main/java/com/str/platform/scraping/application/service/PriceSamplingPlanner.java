package com.str.platform.scraping.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates date ranges for price sampling jobs.
 */
@Slf4j
@Service
public class PriceSamplingPlanner {

    /**
     * Generate date ranges for price sampling.
     * 
     * Strategy: 12 monthly samples
     * - Start: 30 days from now
     * - Duration: 7 nights each
     * - Interval: 30 days between samples
     */
    public List<DateRange> generatePriceSamplePeriods() {
        List<DateRange> periods = new ArrayList<>();
        LocalDate baseDate = LocalDate.now().plusDays(30);

        for (int i = 0; i < 12; i++) {
            LocalDate start = baseDate.plusDays(i * 30L);
            LocalDate end = start.plusDays(7);
            periods.add(new DateRange(start, end));
        }

        log.debug("Generated {} price sample periods from {} to {}",
            periods.size(),
            periods.get(0).start(),
            periods.get(periods.size() - 1).end());

        return periods;
    }

    /**
     * Default search range for FULL_PROFILE jobs.
     */
    public DateRange defaultSearchRange() {
        return new DateRange(
            LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(37)
        );
    }

    public record DateRange(LocalDate start, LocalDate end) {}
}
