package com.edumanage.results.service;

import com.edumanage.results.entity.GradeSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the grade_ranges JSONB mapping on GradeSnapshot.
 * The column is PostgreSQL JSONB and must be mapped with a Hibernate JSON
 * jdbc type bound to a structured Java type (JsonNode), never as a plain
 * String, otherwise PostgreSQL rejects the bind with:
 * "column \"grade_ranges\" is of type jsonb but expression is of type
 * character varying".
 */
class GradeSnapshotJsonbMappingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void createGradeSnapshotProducesStructuredJson() throws Exception {
        List<GradeSnapshot.RangeEntry> entries = List.of(
                new GradeSnapshot.RangeEntry("A", new BigDecimal("80"), new BigDecimal("100"), new BigDecimal("4.0"), "Excellent"),
                new GradeSnapshot.RangeEntry("B", new BigDecimal("60"), new BigDecimal("79"), new BigDecimal("3.0"), "Good"),
                new GradeSnapshot.RangeEntry("F", new BigDecimal("0"), new BigDecimal("39"), null, "Fail")
        );

        JsonNode json = MAPPER.valueToTree(entries);

        assertNotNull(json);
        assertTrue(json.isArray());
        assertEquals(3, json.size());
        assertEquals("A", json.get(0).get("grade").asText());
        assertEquals(0, new BigDecimal(json.get(0).get("minMark").asText()).compareTo(new BigDecimal("80")));
        assertEquals(0, new BigDecimal(json.get(0).get("maxMark").asText()).compareTo(new BigDecimal("100")));
        assertEquals(0, new BigDecimal(json.get(0).get("gradePoint").asText()).compareTo(new BigDecimal("4.0")));
        assertEquals("Excellent", json.get(0).get("remark").asText());
        assertEquals("F", json.get(2).get("grade").asText());
        assertTrue(json.get(2).get("gradePoint").isNull());
    }

    @Test
    void gradeRangesFieldIsMappedAsJsonB() throws Exception {
        Field field = GradeSnapshot.class.getDeclaredField("gradeRanges");
        assertEquals(JsonNode.class, field.getType(),
                "gradeRanges must be a structured JsonNode, not String");

        var jdbcType = field.getAnnotation(org.hibernate.annotations.JdbcType.class);
        assertNotNull(jdbcType, "gradeRanges must carry @JdbcType");
        assertEquals(org.hibernate.dialect.PostgreSQLJsonPGObjectJsonbType.class, jdbcType.value(),
                "gradeRanges must use the PostgreSQL jsonb jdbc type");

        var column = field.getAnnotation(jakarta.persistence.Column.class);
        assertNotNull(column);
        assertEquals("JSONB", column.columnDefinition());
    }
}
