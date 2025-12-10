package com.metaverse.msme.extractor;

import com.metaverse.msme.model.MsmeUnitDetails;
import com.metaverse.msme.repository.MsmeUnitDetailsRepository;
import com.metaverse.msme.service.AddressParseResult;
import com.metaverse.msme.service.AddressParseService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MsmeExcelService {

    private static final Logger log = LoggerFactory.getLogger(MsmeExcelService.class);

    // tuned chunk size — feels good for DB & memory; adjust to your environment
    private static final int CHUNK_SIZE = 2000;

    private final MsmeUnitDetailsRepository repository;
    private final AddressParseService addressParseService;

    @Autowired
    public MsmeExcelService(MsmeUnitDetailsRepository repository,
                            AddressParseService addressParseService) {
        this.repository = repository;
        this.addressParseService = addressParseService;
    }

    /**
     * IMPORTANT: This implementation uses ID-based paging (no OFFSET).
     * Ensure you add the following repository method (example) to MsmeUnitDetailsRepository:
     *
     * List<MsmeUnitDetails> findNextChunk(Integer afterSlno, Pageable pageable);
     *
     * Example JPQL:
     * @Query("SELECT u FROM MsmeUnitDetails u WHERE u.slno > :after ORDER BY u.slno ASC")
     * List<MsmeUnitDetails> findNextChunk(@Param("after") Integer after, Pageable pageable);
     *
     * @param startAfterSlno  start processing after this slno (0 to start from beginning)
     * @param totalRecords    stop after processing this many records (use Integer.MAX_VALUE for no limit)
     */
    public byte[] generateExcel(Integer startAfterSlno, int totalRecords) {

        ExecutorService pool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

        try (
                SXSSFWorkbook workbook = new SXSSFWorkbook(1000); // keep a larger window to reduce flush I/O
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("MSME ADDRESS PARSE");
            createHeader(sheet);

            int rowIndex = 1;
            int processed = 0;
            Integer lastId = Objects.requireNonNullElse(startAfterSlno, 0);

            while (processed < totalRecords) {

                // fetch next chunk without OFFSET
                List<MsmeUnitDetails> chunk = repository.findNextChunk(lastId, PageRequest.of(0, CHUNK_SIZE));

                if (chunk == null || chunk.isEmpty()) {
                    break;
                }

                // parse in parallel but preserve order
                List<CompletableFuture<AddressParseResult>> futures = new ArrayList<>(chunk.size());

                for (MsmeUnitDetails u : chunk) {
                    CompletableFuture<AddressParseResult> f = CompletableFuture.supplyAsync(() -> parseSafely(u), pool);
                    futures.add(f);
                }

                // wait for all to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // collect results in same order as chunk
                List<AddressParseResult> results = futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());

                for (int i = 0; i < chunk.size(); i++) {
                    System.out.println("Processed row number: " + rowIndex);
                    MsmeUnitDetails u = chunk.get(i);
                    AddressParseResult result = results.get(i);

                    Row row = sheet.createRow(rowIndex++);

                    row.createCell(0).setCellValue(value(u.getSlno()));
                    row.createCell(1).setCellValue(value(u.getUnitName()));
                    row.createCell(2).setCellValue(value(u.getUnitAddress()));
                    row.createCell(3).setCellValue(value(u.getVillage()));
                    row.createCell(4).setCellValue(value(result.getVillage()));
                    row.createCell(5).setCellValue(value(result.getMandal()));
                    row.createCell(6).setCellValue(value(u.getDistrict()));
                    row.createCell(7).setCellValue(
                            result.getVillageStatus() != null
                                    ? result.getVillageStatus().name()
                                    : "NOT_FOUND"
                    );
                    row.createCell(8).setCellValue(buildDetails(result));

                    lastId = u.getSlno();
                }

                processed += chunk.size();

                log.debug("Processed {} records, lastId={}", processed, lastId);
            }

            setColumnWidths(sheet);

            workbook.write(out);
            workbook.dispose();

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Excel generation failed", e);
            throw new RuntimeException("Excel generation failed", e);
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ------------------ HELPERS ------------------

    private AddressParseResult parseSafely(MsmeUnitDetails u) {
        try {
            if (u.getUnitAddress() != null && !"null".equalsIgnoreCase(u.getUnitAddress())) {
                // if district is static, consider caching a parse context in AddressParseService
                return addressParseService.parse("Adilabad", u.getUnitAddress());
            }
        } catch (Exception ex) {
            log.debug("parse failed for slno={}: {}", u.getSlno(), ex.getMessage());
        }
        return AddressParseResult.fromMandalResult(MandalDetectionResult.notFound());
    }

    private void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("SL NO");
        header.createCell(1).setCellValue("Unit Name");
        header.createCell(2).setCellValue("RAW ADDRESS");
        header.createCell(3).setCellValue("Village Name");
        header.createCell(4).setCellValue("DETECTED VILLAGE");
        header.createCell(5).setCellValue("DETECTED MANDAL");
        header.createCell(6).setCellValue("DETECTED DISTRICT");
        header.createCell(7).setCellValue("ADDRESS STATUS");
        header.createCell(8).setCellValue("DETAILS");
    }

    private String buildDetails(AddressParseResult r) {
        StringBuilder sb = new StringBuilder(200);

        sb.append("Mandal: ").append(value(r.getMandal())).append('\n');
        sb.append("Mandal Status: ").append(value(r.getMandalStatus())).append('\n');
        sb.append("Multiple Mandals: ")
                .append(r.getMultipleMandals() != null
                        ? String.join(", ", r.getMultipleMandals())
                        : "NOT_FOUND")
                .append('\n');
        sb.append("Village: ").append(value(r.getVillage())).append('\n');
        sb.append("Village Status: ").append(value(r.getVillageStatus())).append('\n');
        sb.append("Multiple Villages: ")
                .append(r.getMultipleVillages() != null
                        ? String.join(", ", r.getMultipleVillages())
                        : "NOT_FOUND");

        return sb.toString();
    }

    private void setColumnWidths(Sheet sheet) {
        for (int i = 0; i <= 8; i++) {
            sheet.setColumnWidth(i, 6000);
        }
    }

    private String value(Object o) {
        return o == null ? "" : o.toString();
    }
}
