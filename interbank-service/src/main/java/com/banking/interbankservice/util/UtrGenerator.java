package com.banking.interbankservice.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public final class UtrGenerator {

    private static final Random RANDOM = new Random();

    private UtrGenerator() {
    }

    /**
     * UTR style reference, e.g. NEFT20260821012345 - rail + date + sequence.
     * Real UTRs come back from the rail switch; here we mint one so the
     * transfer can be traced end to end.
     */
    public static String generate(String rail) {
        String datePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int sequence = 100000 + RANDOM.nextInt(900000);
        return rail + datePart + sequence;
    }
}
