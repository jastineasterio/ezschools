package com.edumanage.fees.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single eligible student returned in a bulk assignment preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAssignmentStudentPreview {

    private Long studentId;
    private String admissionNumber;
    private String firstName;
    private String lastName;
    private String name;
    private String className;
    private String streamName;
}
