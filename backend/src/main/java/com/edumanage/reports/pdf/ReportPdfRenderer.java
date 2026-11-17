package com.edumanage.reports.pdf;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

@Service
public class ReportPdfRenderer {

    private static final float MARGIN = 54f;
    private static final float TOP_BAND_HEIGHT = 92f;
    private static final float TITLE_BLOCK_GAP = 16f;
    private static final float FOOTER_Y = 46f;
    private static final float LOGO_SIZE = 46f;
    private static final float TABLE_FONT_SIZE = 7.6f;
    private static final float TABLE_LINE_HEIGHT = 10.6f;
    private static final float CELL_PADDING = 5f;
    private static final float HEADER_FONT_SIZE = 15f;
    private static final float META_FONT_SIZE = 8.6f;
    private static final float META_LABEL_SIZE = 7.4f;
    private static final float STAT_FONT_SIZE = 9.2f;
    private static final float STAT_LABEL_SIZE = 7.2f;
    private static final float FOOTER_FONT_SIZE = 7.6f;

    public byte[] render(ReportRenderData data) throws IOException {
        boolean landscape = data.isLandscape();
        PDRectangle pageSize = landscape
                ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                : PDRectangle.A4;

        try (PDDocument document = new PDDocument()) {
            PDType0Font regular = loadFont(document, "/fonts/DejaVuSans.ttf");
            PDType0Font bold = loadFont(document, "/fonts/DejaVuSans-Bold.ttf");
            PDImageXObject logo = loadLogo(document, data.getLogoUrl());

            float y = startPage(document, pageSize, data, logo, regular, bold, true);

            y = drawTitleBlock(document, data, regular, bold, y);
            y = drawSummary(document, data, regular, bold, y);

            if (data.getColumns() != null && !data.getColumns().isEmpty()) {
                drawTable(document, pageSize, data, regular, bold, y);
            } else {
                drawNoData(document, data, regular, bold, y);
            }

            // Pass 2: add "Page X of Y" to every page footer
            int totalPages = document.getNumberOfPages();
            for (int i = 0; i < totalPages; i++) {
                PDPage page = document.getPage(i);
                try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true)) {
                    String pageText = String.format(Locale.ROOT, "Page %d of %d", i + 1, totalPages);
                    float width = stringWidth(bold, pageText, FOOTER_FONT_SIZE);
                    float x = pageSize.getWidth() - MARGIN - width;
                    cs.beginText();
                    cs.setFont(bold, FOOTER_FONT_SIZE);
                    setColor(cs, 0.2f, 0.2f, 0.25f, false);
                    cs.newLineAtOffset(x, FOOTER_Y - 8f);
                    cs.showText(pageText);
                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private float startPage(PDDocument document, PDRectangle pageSize, ReportRenderData data, PDImageXObject logo,
            PDType0Font regular, PDType0Font bold, boolean firstPage) throws IOException {
        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.OVERWRITE, true)) {
            drawRunningHeader(cs, pageSize, data, logo, regular, bold, firstPage);
        }
        return pageSize.getHeight() - MARGIN - (firstPage ? TOP_BAND_HEIGHT : 44f);
    }

    private void drawRunningHeader(PDPageContentStream cs, PDRectangle pageSize, ReportRenderData data,
            PDImageXObject logo, PDType0Font regular, PDType0Font bold, boolean firstPage) throws IOException {
        float top = pageSize.getHeight();
        float bandBottom = top - (firstPage ? TOP_BAND_HEIGHT : 44f);

        float cursorX = MARGIN;
        if (logo != null && firstPage) {
            float scale = LOGO_SIZE / logo.getWidth();
            float drawnHeight = logo.getHeight() * scale;
            cs.drawImage(logo, MARGIN, top - MARGIN - drawnHeight, LOGO_SIZE, drawnHeight);
            cursorX = MARGIN + LOGO_SIZE + 14f;
        }

        String schoolName = data.getSchoolName() == null ? "School" : data.getSchoolName();
        setColor(cs, data.getPrimaryColor(), false);
        cs.beginText();
        cs.setFont(bold, firstPage ? 17f : 12.5f);
        cs.newLineAtOffset(cursorX, top - MARGIN - 18f);
        cs.showText(schoolName);
        cs.endText();

        if (firstPage) {
            List<String> contactLines = contactLines(data);
            float cy = top - MARGIN - 18f - 11f;
            setColor(cs, data.getSecondaryColor(), false);
            for (String line : contactLines) {
                cs.beginText();
                cs.setFont(regular, 8.6f);
                cs.newLineAtOffset(cursorX, cy);
                cs.showText(line);
                cs.endText();
                cy -= 10.5f;
            }
        } else {
            cs.beginText();
            cs.setFont(bold, 9.5f);
            setColor(cs, data.getPrimaryColor(), false);
            cs.newLineAtOffset(cursorX, top - MARGIN - 13f);
            cs.showText(reportTitle(data));
            cs.endText();
            cs.beginText();
            cs.setFont(regular, 7.6f);
            setColor(cs, 0.45f, 0.45f, 0.5f, false);
            cs.newLineAtOffset(cursorX, top - MARGIN - 24f);
            cs.showText("Generated " + data.getGeneratedAt());
            cs.endText();
        }

        setColor(cs, data.getSecondaryColor(), true);
        cs.moveTo(MARGIN, bandBottom - 6f);
        cs.lineTo(pageSize.getWidth() - MARGIN, bandBottom - 6f);
        cs.stroke();
    }

    private String reportTitle(ReportRenderData data) {
        String title = data.getReportName();
        if (title == null || title.isBlank()) {
            title = data.getReportTypeLabel();
        }
        String extra = "";
        if (data.getClassName() != null && !data.getClassName().isBlank()) {
            extra = " - " + data.getClassName();
        } else if (data.getStreamName() != null && !data.getStreamName().isBlank()) {
            extra = " - " + data.getStreamName();
        }
        return title + extra;
    }

    private List<String> contactLines(ReportRenderData data) {
        List<String> lines = new ArrayList<>();
        String contact = joinNonBlank(data.getSchoolAddress(), data.getSchoolPhone(), data.getSchoolEmail(),
                data.getSchoolWebsite());
        if (!contact.isBlank()) {
            lines.add(contact);
        }
        String reg = data.getSchoolRegistrationNumber();
        if (reg != null && !reg.isBlank()) {
            lines.add("Registration No: " + reg);
        }
        return lines;
    }

    private float drawTitleBlock(PDDocument document, ReportRenderData data, PDType0Font regular, PDType0Font bold,
            float y) throws IOException {
        PDPage page = document.getPage(0);
        try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true)) {
            cs.beginText();
            cs.setFont(bold, HEADER_FONT_SIZE);
            setColor(cs, 0.1f, 0.1f, 0.12f, false);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(data.getReportName() == null || data.getReportName().isBlank()
                    ? data.getReportTypeLabel() : data.getReportName());
            cs.endText();
            y -= HEADER_FONT_SIZE + 7f;

            List<String[]> meta = metaRows(data);
            float cy = y;
            for (String[] row : meta) {
                cs.beginText();
                cs.setFont(bold, META_LABEL_SIZE);
                setColor(cs, 0.4f, 0.4f, 0.45f, false);
                cs.newLineAtOffset(MARGIN, cy);
                cs.showText(row[0]);
                cs.endText();

                cs.beginText();
                cs.setFont(regular, META_FONT_SIZE);
                setColor(cs, 0.15f, 0.15f, 0.18f, false);
                cs.newLineAtOffset(MARGIN + 150f, cy);
                cs.showText(row[1]);
                cs.endText();
                cy -= 13.5f;
            }
            y = cy - 6f;
        }
        return y - TITLE_BLOCK_GAP;
    }

    private List<String[]> metaRows(ReportRenderData data) {
        List<String[]> rows = new ArrayList<>();
        addRow(rows, "Report Type", data.getReportTypeLabel());
        addRow(rows, "Academic Year", data.getAcademicYear());
        addRow(rows, "Class", data.getClassName());
        addRow(rows, "Stream", data.getStreamName());
        addRow(rows, "Subject", data.getSubjectName());
        addRow(rows, "Examination", data.getExaminationName());
        addRow(rows, "Reporting Period", data.getReportingPeriod());
        addRow(rows, "Generated At", data.getGeneratedAt());
        return rows;
    }

    private void addRow(List<String[]> rows, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        rows.add(new String[] { label, value });
    }

    private float drawSummary(PDDocument document, ReportRenderData data, PDType0Font regular, PDType0Font bold,
            float y) throws IOException {
        List<String[]> stats = data.getSummaryStats();
        if (stats == null || stats.isEmpty()) {
            return y;
        }
        PDPage page = document.getPage(0);
        float contentWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
        int perRow = Math.min(4, stats.size());
        float boxWidth = contentWidth / perRow;
        float boxHeight = 56f;

        try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true)) {
            int rowCount = (int) Math.ceil(stats.size() / (double) perRow);
            for (int r = 0; r < rowCount; r++) {
                float by = y;
                for (int c = 0; c < perRow; c++) {
                    int idx = r * perRow + c;
                    if (idx >= stats.size()) {
                        break;
                    }
                    String[] stat = stats.get(idx);
                    float bx = MARGIN + c * boxWidth;
                    setColor(cs, 0.96f, 0.97f, 0.99f, false);
                    cs.addRect(bx, by - boxHeight, boxWidth - 10f, boxHeight);
                    cs.fill();
                    setColor(cs, data.getPrimaryColor(), true);
                    cs.addRect(bx, by - boxHeight, boxWidth - 10f, boxHeight);
                    cs.stroke();

                    cs.beginText();
                    cs.setFont(bold, STAT_FONT_SIZE);
                    setColor(cs, 0.1f, 0.1f, 0.12f, false);
                    cs.newLineAtOffset(bx + 10f, by - 22f);
                    cs.showText(stat[1]);
                    cs.endText();

                    cs.beginText();
                    cs.setFont(regular, STAT_LABEL_SIZE);
                    setColor(cs, 0.45f, 0.45f, 0.5f, false);
                    cs.newLineAtOffset(bx + 10f, by - 36f);
                    cs.showText(stat[0]);
                    cs.endText();
                }
                y = by - boxHeight - 14f;
            }
        }
        return y - 10f;
    }

    private void drawTable(PDDocument document, PDRectangle pageSize, ReportRenderData data, PDType0Font regular,
            PDType0Font bold, float startY) throws IOException {
        List<String> columns = data.getColumns();
        List<List<String>> rows = data.getRows() == null ? new ArrayList<>() : data.getRows();
        float contentWidth = pageSize.getWidth() - 2 * MARGIN;
        int colCount = columns.size();
        float colWidth = contentWidth / colCount;

        float y = startY;
        y = drawTableHeader(document, pageSize, data, columns, colWidth, bold, y);

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            List<List<String>> wrapped = new ArrayList<>();
            int maxLines = 1;
            for (int c = 0; c < colCount; c++) {
                String cell = c < row.size() ? row.get(c) : "";
                List<String> lines = wrapText(regular, TABLE_FONT_SIZE, cell, colWidth - 2 * CELL_PADDING);
                wrapped.add(lines);
                maxLines = Math.max(maxLines, lines.size());
            }
            float rowHeight = maxLines * TABLE_LINE_HEIGHT + 2 * CELL_PADDING;

            PDPage current = document.getPage(document.getNumberOfPages() - 1);
            if (y - rowHeight < MARGIN + 20f) {
                drawFooter(document, pageSize, data, regular, current);
                y = startPage(document, pageSize, data, null, regular, bold, false);
                pagesAdd(document);
                current = document.getPage(document.getNumberOfPages() - 1);
                y = drawTableHeader(document, pageSize, data, columns, colWidth, bold, y);
                current = document.getPage(document.getNumberOfPages() - 1);
            }

            boolean alternate = i % 2 == 1;
            try (PDPageContentStream cs = new PDPageContentStream(document, current, AppendMode.APPEND, true)) {
                if (alternate) {
                    setColor(cs, 0.97f, 0.975f, 0.985f, false);
                    cs.addRect(MARGIN, y - rowHeight, contentWidth, rowHeight);
                    cs.fill();
                }
                float ty = y - CELL_PADDING - 1.5f;
                for (int c = 0; c < colCount; c++) {
                    List<String> lines = wrapped.get(c);
                    float cx = MARGIN + c * colWidth + CELL_PADDING;
                    float lineY = ty;
                    for (String line : lines) {
                        cs.beginText();
                        cs.setFont(regular, TABLE_FONT_SIZE);
                        setColor(cs, 0.15f, 0.15f, 0.18f, false);
                        cs.newLineAtOffset(cx, lineY);
                        cs.showText(line);
                        cs.endText();
                        lineY -= TABLE_LINE_HEIGHT;
                    }
                }
                setColor(cs, 0.55f, 0.6f, 0.66f, true);
                cs.moveTo(MARGIN, y - rowHeight);
                cs.lineTo(pageSize.getWidth() - MARGIN, y - rowHeight);
                cs.stroke();
                y -= rowHeight;
            }
        }
    }

    private void pagesAdd(PDDocument document) {
        // no-op; startPage already added the page
    }

    private float drawTableHeader(PDDocument document, PDRectangle pageSize, ReportRenderData data,
            List<String> columns, float colWidth, PDType0Font bold, float y) throws IOException {
        PDPage page = document.getPage(document.getNumberOfPages() - 1);
        try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true)) {
            float headerHeight = TABLE_LINE_HEIGHT + 2 * CELL_PADDING;
            setColor(cs, data.getPrimaryColor(), false);
            cs.addRect(MARGIN, y - headerHeight, pageSize.getWidth() - 2 * MARGIN, headerHeight);
            cs.fill();

            float ty = y - CELL_PADDING - 1.5f;
            for (int c = 0; c < columns.size(); c++) {
                cs.beginText();
                cs.setFont(bold, TABLE_FONT_SIZE);
                setColor(cs, 1f, 1f, 1f, false);
                cs.newLineAtOffset(MARGIN + c * colWidth + CELL_PADDING, ty);
                cs.showText(columns.get(c));
                cs.endText();
            }
            setColor(cs, 0.55f, 0.6f, 0.66f, true);
            cs.moveTo(MARGIN, y - headerHeight);
            cs.lineTo(pageSize.getWidth() - MARGIN, y - headerHeight);
            cs.stroke();
            return y - headerHeight - 4f;
        }
    }

    private void drawFooter(PDDocument document, PDRectangle pageSize, ReportRenderData data, PDType0Font regular,
            PDPage page) throws IOException {
        try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true)) {
            float fy = FOOTER_Y;
            setColor(cs, 0.75f, 0.78f, 0.82f, true);
            cs.moveTo(MARGIN, fy + 10f);
            cs.lineTo(pageSize.getWidth() - MARGIN, fy + 10f);
            cs.stroke();

            cs.beginText();
            cs.setFont(regular, FOOTER_FONT_SIZE);
            setColor(cs, 0.4f, 0.4f, 0.45f, false);
            cs.newLineAtOffset(MARGIN, fy);
            cs.showText(data.getSchoolName() == null ? "School" : data.getSchoolName());
            cs.endText();

            cs.beginText();
            cs.setFont(regular, 6.8f);
            setColor(cs, 0.5f, 0.5f, 0.55f, false);
            cs.newLineAtOffset(MARGIN, fy - 13f);
            cs.showText("Confidential - For internal school use only.");
            cs.endText();

            float sigX = pageSize.getWidth() - MARGIN - 170f;
            cs.beginText();
            cs.setFont(regular, 6.8f);
            cs.newLineAtOffset(sigX, fy - 13f);
            cs.showText("Prepared by: ____________________");
            cs.endText();
            cs.beginText();
            cs.newLineAtOffset(sigX, fy - 21f);
            cs.showText("Approved by: ____________________");
            cs.endText();
        }
    }

    private void drawNoData(PDDocument document, ReportRenderData data, PDType0Font regular, PDType0Font bold,
            float y) throws IOException {
        PDPage page = document.getPage(document.getNumberOfPages() - 1);
        try (PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true)) {
            cs.beginText();
            cs.setFont(regular, 11f);
            setColor(cs, 0.5f, 0.5f, 0.55f, false);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText("No records found matching the selected filters.");
            cs.endText();
        }
    }

    private List<String> wrapText(PDType0Font font, float size, String text, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String trial = current.length() == 0 ? word : current + " " + word;
            if (stringWidth(font, trial, size) <= maxWidth || current.length() == 0) {
                current = new StringBuilder(trial);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private float stringWidth(PDType0Font font, String text, float size) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        return font.getStringWidth(text) / 1000f * size;
    }

    private PDType0Font loadFont(PDDocument document, String path) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Font resource not found: " + path);
            }
            return PDType0Font.load(document, is);
        }
    }

    private PDImageXObject loadLogo(PDDocument document, String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return null;
        }
        try {
            URL url = new URL(logoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "EduManage-ReportGenerator/1.0");
            try (InputStream is = conn.getInputStream()) {
                BufferedImage image = ImageIO.read(is);
                if (image == null) {
                    return null;
                }
                return LosslessFactory.createFromImage(document, image);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void setColor(PDPageContentStream cs, String hex, boolean stroke) throws IOException {
        float[] rgb = decodeHex(hex);
        if (stroke) {
            cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        } else {
            cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        }
    }

    private void setColor(PDPageContentStream cs, float r, float g, float b, boolean stroke) throws IOException {
        if (stroke) {
            cs.setStrokingColor(r, g, b);
        } else {
            cs.setNonStrokingColor(r, g, b);
        }
    }

    private float[] decodeHex(String hex) {
        if (hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) {
            return new float[] { 0.231f, 0.51f, 0.965f };
        }
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return new float[] { r / 255f, g / 255f, b / 255f };
    }

    private String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!first) {
                    sb.append("  |  ");
                }
                sb.append(p);
                first = false;
            }
        }
        return sb.toString();
    }
}
