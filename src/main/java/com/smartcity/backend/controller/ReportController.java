package com.smartcity.backend.controller;

import com.smartcity.backend.entity.Report;
import com.smartcity.backend.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    @GetMapping
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @PostMapping
    public Report createReport(@RequestBody Report report) {
        return reportRepository.save(report);
    }
}