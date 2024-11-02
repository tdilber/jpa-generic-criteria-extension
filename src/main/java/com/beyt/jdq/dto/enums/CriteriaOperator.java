package com.beyt.jdq.dto.enums;


import java.io.Serializable;

/**
 * Created by tdilber at 15-Sep-19
 */
public enum CriteriaOperator implements Serializable {
    CONTAIN(1, "İçeriyor (String)"),
    DOES_NOT_CONTAIN(2, "İçermiyor (String)"),
    END_WITH(3, "İle bitiyor (String)"),
    START_WITH(4, "İle başlıyor (String)"),
    SPECIFIED(5, "Not Null ise 'true', Null ise 'false'"),
    EQUAL(6, "Eşittir"),
    NOT_EQUAL(7, "Eşit Değildir"),
    GREATER_THAN(8, "Büyüktür"),
    GREATER_THAN_OR_EQUAL(9, "Büyüktür veya eşittir"),
    LESS_THAN(10, "Küçüktür"),
    LESS_THAN_OR_EQUAL(11, "Küçüktür veya eşittir"),
    OR(12, "Veya (Konulduğu yeri 2 ye Böler) (key ve values önemsizdir)"),
    PARENTHES(13, "Sub Criteria"),
    IN(14, "Birden fazla OR değeri"),
    NOT_IN(15, "Birden fazla NOT OR değeri");

    private int value = -1;
    private String meaning;

    CriteriaOperator(int value, String meaning) {
        this.value = value;
        this.meaning = meaning;
    }

    public int getValue() {
        return value;
    }

    public String getMeaning() {
        return meaning;
    }
}
