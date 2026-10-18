package com.oms.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditFlagLabelTest {

    @Test
    void labelsAndTonesFollowFlagCode() {
        assertEquals("Bình thường", CreditFlag.label("OK"));
        assertEquals("Vượt hạn mức", CreditFlag.label("OVER_LIMIT"));
        assertEquals("Quá hạn", CreditFlag.label("OVERDUE"));
        assertEquals("danger", CreditFlag.tone("OVERDUE"));
        assertEquals("warning", CreditFlag.tone("OVER_LIMIT"));
    }

    @Test
    void unknownOrMissingCodeIsTreatedAsNormal() {
        assertEquals("Bình thường", CreditFlag.label(null));
        assertEquals("Bình thường", CreditFlag.label("XYZ"));
        assertEquals("success", CreditFlag.tone(null));
    }
}
