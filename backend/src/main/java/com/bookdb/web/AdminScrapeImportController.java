package com.bookdb.web;

import com.bookdb.dto.ScrapeImportDtos.ScrapeImportSummary;
import com.bookdb.service.ScrapeImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/scrape-imports")
@RequiredArgsConstructor
public class AdminScrapeImportController {

    private final ScrapeImportService scrapeImportService;

    @PostMapping
    public ScrapeImportSummary importFile(@RequestParam MultipartFile file) throws IOException {
        return scrapeImportService.importJson(file);
    }
}
