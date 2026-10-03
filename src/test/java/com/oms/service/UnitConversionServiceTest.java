package com.oms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UnitConversionServiceTest {

    // S2-07: nhập 2 thùng (1 thùng = 24 lon) thì sổ ghi 48 lon, kèm hệ số chụp lại lúc ghi
    @Test
    void convertsQuantityToBaseUnitAndKeepsFactorSnapshot() {
        UnitConversionService.Converted converted = UnitConversionService.convert(new BigDecimal("2"), new BigDecimal("24"));
        assertEquals(new BigDecimal("48.000"), converted.getQtyBase());
        assertEquals(new BigDecimal("24.0000"), converted.getUnitFactor());
    }

    @Test
    void roundsBaseQuantityToThreeDecimals() {
        assertEquals(new BigDecimal("1.667"),
                UnitConversionService.convert(new BigDecimal("3.3333"), new BigDecimal("0.5")).getQtyBase());
    }

    @Test
    void factorAcceptsCommaOrDotUpToFourDecimals() {
        assertEquals(new BigDecimal("24"), UnitConversionService.parseFactor("24"));
        assertEquals(new BigDecimal("0.5"), UnitConversionService.parseFactor("0,5"));
        assertEquals(new BigDecimal("2.5"), UnitConversionService.parseFactor("2.5"));
    }

    @Test
    void factorRejectsOneZeroNegativeAndTooManyDecimals() {
        assertNull(UnitConversionService.parseFactor("1"));
        assertNull(UnitConversionService.parseFactor("1.0"));
        assertNull(UnitConversionService.parseFactor("0"));
        assertNull(UnitConversionService.parseFactor("-6"));
        assertNull(UnitConversionService.parseFactor("0.12345"));
        assertNull(UnitConversionService.parseFactor("mười"));
        assertNull(UnitConversionService.parseFactor(null));
    }
}
