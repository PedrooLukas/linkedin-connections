package com.pedro.linkedin.connections.api;

import com.pedro.linkedin.connections.api.dto.ConnectionResponse;
import com.pedro.linkedin.connections.api.dto.LinkedinProfileAnalysisResponse;
import com.pedro.linkedin.connections.api.dto.ConnectionStatsResponse;
import com.pedro.linkedin.connections.api.dto.ImportResultResponse;
import com.pedro.linkedin.connections.service.ConnectionImportService;
import com.pedro.linkedin.connections.service.ConnectionQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/connections")
public class ConnectionController {

    private final ConnectionImportService connectionImportService;
    private final ConnectionQueryService connectionQueryService;

    public ConnectionController(
            ConnectionImportService connectionImportService,
            ConnectionQueryService connectionQueryService
    ) {
        this.connectionImportService = connectionImportService;
        this.connectionQueryService = connectionQueryService;
    }

    @PostMapping("/import")
    public ImportResultResponse importConnections(@RequestPart("file") MultipartFile file) {
        return connectionImportService.importCsv(file);
    }

    @PostMapping("/import/batch")
    public ImportResultResponse importConnectionsBatch(@RequestPart("files") List<MultipartFile> files) {
        return connectionImportService.importFiles(files);
    }

    @GetMapping
    public List<ConnectionResponse> listConnections(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String position
    ) {
        return connectionQueryService.findByFilters(company, position);
    }

    @GetMapping("/stats")
    public ConnectionStatsResponse getStats(
            @RequestParam(defaultValue = "10")
            @Min(1)
            @Max(100) int top
    ) {
        return connectionQueryService.buildStats(top);
    }

    @GetMapping("/analysis")
    public LinkedinProfileAnalysisResponse getProfileAnalysis() {
        return connectionQueryService.buildProfileAnalysis();
    }

    @GetMapping("/strategic")
    public List<ConnectionResponse> getStrategicContacts(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String positionKeyword,
            @RequestParam(defaultValue = "false") boolean recruitersOnly,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(200) int limit
    ) {
        return connectionQueryService.findStrategicContacts(company, positionKeyword, recruitersOnly, limit);
    }
}
