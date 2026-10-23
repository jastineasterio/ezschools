package com.edumanage.certificates.service;

import com.edumanage.certificates.dto.CertificateDto;
import com.edumanage.branding.entity.SchoolBranding;
import com.edumanage.schools.entity.School;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CertificatePdfService {
    public byte[] render(School school, SchoolBranding branding, CertificateDto certificate) {
        String schoolName = school == null ? "School" : school.getName();
        String contact = school == null ? "" : List.of(
                nonBlank(school.getEmail()), nonBlank(school.getPhone()), nonBlank(school.getAddress()))
                .stream().filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.joining(" | "));
        List<String> lines = List.of(
                schoolName,
                contact,
                "CERTIFICATE",
                certificate.title(),
                "This certificate is proudly presented to",
                certificate.studentName(),
                "Admission No: " + certificate.admissionNumber(),
                "Class: " + certificate.className() + (certificate.streamName() == null ? "" : " - " + certificate.streamName()),
                certificate.subjectName() == null ? "" : "Subject: " + certificate.subjectName(),
                "Academic Year: " + certificate.academicYear(),
                certificate.percentage() == null ? "" : "Published result: " + certificate.percentage().stripTrailingZeros().toPlainString() + "%",
                certificate.description() == null ? "" : certificate.description(),
                "Issued: " + (certificate.issuedAt() == null ? "Not released" : certificate.issuedAt().toLocalDate()),
                "Certificate No: " + certificate.certificateNumber(),
                "Verification: " + certificate.verificationCode()
        );

        String primaryColor = colorRgb(branding == null ? null : branding.getPrimaryColor());
        StringBuilder content = new StringBuilder("q\n").append(primaryColor).append(" RG\n3 w\n");
        content.append("32 32 778 538 re S\n");
        int y = 510;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) continue;
            int size = i == 2 ? 16 : i == 3 || i == 5 ? 22 : 10;
            int maxCharacters = size >= 20 ? 62 : 108;
            for (String wrappedLine : wrap(line, maxCharacters)) {
                content.append(primaryColor).append(" rg\nBT /F1 ").append(size).append(" Tf 1 0 0 1 ")
                        .append(Math.max(45, (842 - wrappedLine.length() * size / 2) / 2)).append(' ').append(y)
                        .append(" Tm (").append(escape(wrappedLine)).append(") Tj ET\n");
                y -= size >= 20 ? 27 : 17;
            }
            y -= i == 2 || i == 3 || i == 5 ? 5 : 2;
        }
        content.append("Q\n");
        return pdf(content.toString());
    }

    private String nonBlank(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String escape(String value) {
        String ascii = value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String colorRgb(String value) {
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}")) return "0.231 0.510 0.965";
        int red = Integer.parseInt(value.substring(1, 3), 16);
        int green = Integer.parseInt(value.substring(3, 5), 16);
        int blue = Integer.parseInt(value.substring(5, 7), 16);
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", red / 255d, green / 255d, blue / 255d);
    }

    private List<String> wrap(String value, int maxCharacters) {
        List<String> lines = new ArrayList<>();
        String remaining = value.trim();
        while (remaining.length() > maxCharacters) {
            int splitAt = remaining.lastIndexOf(' ', maxCharacters);
            if (splitAt <= 0) splitAt = maxCharacters;
            lines.add(remaining.substring(0, splitAt).trim());
            remaining = remaining.substring(splitAt).trim();
        }
        if (!remaining.isEmpty()) lines.add(remaining);
        return lines;
    }

    private byte[] pdf(String content) {
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        byte[] stream = content.getBytes(StandardCharsets.ISO_8859_1);
        objects.add("<< /Length " + stream.length + " >>\nstream\n" + content + "endstream");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(output.size());
            write(output, (i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n");
        }
        int xrefOffset = output.size();
        write(output, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) {
            write(output, String.format("%010d 00000 n \n", offsets.get(i)));
        }
        write(output, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF");
        return output.toByteArray();
    }

    private void write(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
    }
}
