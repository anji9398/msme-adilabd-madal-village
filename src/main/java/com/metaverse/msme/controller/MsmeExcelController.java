package com.metaverse.msme.controller;

import com.metaverse.msme.extractor.MsmeExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/msme")
public class MsmeExcelController {

    private final MsmeExcelService excelService;

    public MsmeExcelController(MsmeExcelService excelService) {
        this.excelService = excelService;
    }

    @GetMapping("/download-excel")
    public ResponseEntity<byte[]> downloadExcel(@RequestParam(defaultValue = "0") int start, @RequestParam(defaultValue = "10") int end) {

        byte[] excel = excelService.generateExcel(start,end);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=msme_details.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }
}
