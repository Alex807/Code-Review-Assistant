package com.haufegroup.hackthon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CodeIssue {
    private String severity;
    private Integer line;
    private String type;
    private String description;
    private String suggestion;
}