package com.metaverse.msme.extractor;

import com.metaverse.msme.model.MsmeUnitDetails;
import com.metaverse.msme.repository.MsmeUnitDetailsRepository;
import com.metaverse.msme.service.AddressParseResult;
import com.metaverse.msme.service.AddressParseService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class MsmeExcelService {

    private static final int CHUNK_SIZE = 500;

    private final MsmeUnitDetailsRepository repository;

    @Autowired
    private AddressParseService addressParseService;

    public MsmeExcelService(MsmeUnitDetailsRepository repository) {
        this.repository = repository;
    }

    public byte[] generateExcel(int startPage, int totalRecords) {

        try (
                SXSSFWorkbook workbook = new SXSSFWorkbook(200); // ✅ streaming
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("MSME ADDRESS PARSE");

            createHeader(sheet);

            int rowIndex = 1;
            int processed = 0;
            int page = startPage;

            while (processed < totalRecords) {

                Page<MsmeUnitDetails> records =
                        repository.findAll(PageRequest.of(page, CHUNK_SIZE));

                if (!records.hasContent()) {
                    break;
                }

                for (MsmeUnitDetails u : records.getContent()) {

                    AddressParseResult result = parseSafely(u);

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
                }

                processed += records.getNumberOfElements();
                page++;
            }

            // ✅ fixed column width (much faster than autoSize)
            setColumnWidths(sheet);

            workbook.write(out);
            workbook.dispose(); // ✅ clear temp files
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    // ------------------ HELPERS ------------------

    private AddressParseResult parseSafely(MsmeUnitDetails u) {
        try {
            if (u.getUnitAddress() != null && !"null".equalsIgnoreCase(u.getUnitAddress())) {
                return addressParseService.parse("Adilabad", u.getUnitAddress());
            }
        } catch (Exception ignored) {
        }
        return AddressParseResult.fromMandalResult(
                MandalDetectionResult.notFound()
        );
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
