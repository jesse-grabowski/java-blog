package com.jessegrabowski.blog.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(path = "/csp", consumes = "application/csp-report")
    public void acceptCSPReport(@RequestBody CSPReport report) {
        log.info("test");
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(path = "/reports", consumes = "application/reports+json")
    public void acceptReport(@RequestBody String report) {
        log.info("test2");
    }


}
