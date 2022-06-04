package com.jessegrabowski.blog.telemetry;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonTypeName(value = "csp-report")
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class CSPReport {

    private String documentUri;
    private String referrer;
    private String violatedDirective;
    private String effectiveDirective;
    private String originalPolicy;
    private String disposition;
    private String blockedUri;
    private String lineNumber;
    private String columnNumber;
    private String sourceFile;
    private String statusCode;
    private String scriptSample;

}
