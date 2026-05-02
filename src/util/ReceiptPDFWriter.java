package util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates styled PDF receipts using raw PDF 1.4 spec — zero external dependencies.
 * Uses built-in Type1 fonts (Helvetica / Helvetica-Bold).
 */
public class ReceiptPDFWriter {

    private static final float W      = 420f;   // narrow receipt width (pts)
    private static final float H      = 640f;   // receipt height
    private static final float MARGIN = 32f;
    private static final float COL2   = MARGIN + 155f; // value column x

    // ── Public API ──────────────────────────────────────────────────────────

    public static String writeEntrySlip(String plate, String owner, String contact,
                                         String vehicleType, String slotNumber, String confCode) {
        String dt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy  HH:mm:ss"));
        String row = (slotNumber != null && slotNumber.length() > 0) ? slotNumber.substring(0, 1) : "-";

        List<Row> rows = new ArrayList<>();
        rows.add(Row.header("SMART ePARK"));
        rows.add(Row.subtitle("VEHICLE ENTRY SLIP"));
        rows.add(Row.conf(confCode));
        rows.add(Row.div());
        rows.add(Row.field("Vehicle Plate",  plate));
        rows.add(Row.field("Owner Name",     owner));
        rows.add(Row.field("Contact",        contact));
        rows.add(Row.field("Vehicle Type",   vehicleType.toUpperCase()));
        rows.add(Row.div());
        rows.add(Row.field("Row",            row));
        rows.add(Row.field("Slot Number",    slotNumber));
        rows.add(Row.field("Entry Time",     dt));
        rows.add(Row.div());
        rows.add(Row.footer("Thank you for using Smart ePark!"));

        new File(util.FileManager.RECEIPT_DIR).mkdirs();
        String path = util.FileManager.RECEIPT_DIR + "Entry_" + confCode + ".pdf";
        try { buildPDF(path, rows); return path; }
        catch (Exception e) { System.err.println("PDF error: " + e.getMessage()); return null; }
    }

    public static String writeExitReceipt(String confCode, String plate, String slotNumber,
                                           LocalDateTime checkIn, double hours,
                                           double baseFee, double tax, double total,
                                           String payMethod) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy  HH:mm");
        String now = LocalDateTime.now().format(fmt);
        String cin = (checkIn != null) ? checkIn.format(fmt) : "-";

        List<Row> rows = new ArrayList<>();
        rows.add(Row.header("SMART ePARK"));
        rows.add(Row.subtitle("EXIT RECEIPT"));
        rows.add(Row.conf(confCode));
        rows.add(Row.div());
        rows.add(Row.field("Vehicle Plate",  plate));
        rows.add(Row.field("Slot",           slotNumber));
        rows.add(Row.field("Check-In",       cin));
        rows.add(Row.field("Check-Out",      now));
        rows.add(Row.field("Duration",       String.format("%.2f hrs", hours)));
        rows.add(Row.div());
        rows.add(Row.field("Base Fee",       "Rs. " + String.format("%.2f", baseFee)));
        rows.add(Row.field("Tax (13%)",      "Rs. " + String.format("%.2f", tax)));
        rows.add(Row.total("TOTAL",          "Rs. " + String.format("%.2f", total)));
        rows.add(Row.field("Payment",        payMethod));
        rows.add(Row.div());
        rows.add(Row.footer("Thank you for using Smart ePark!"));

        new File(util.FileManager.RECEIPT_DIR).mkdirs();
        String path = util.FileManager.RECEIPT_DIR + "Receipt_" + confCode + "_" + System.currentTimeMillis() + ".pdf";
        try { buildPDF(path, rows); return path; }
        catch (Exception e) { System.err.println("PDF error: " + e.getMessage()); return null; }
    }

    // ── Row model ───────────────────────────────────────────────────────────

    private record Row(String type, String label, String value) {
        static Row header(String t)              { return new Row("HEADER",   t,     null);  }
        static Row subtitle(String t)            { return new Row("SUBTITLE", t,     null);  }
        static Row conf(String c)                { return new Row("CONF",     c,     null);  }
        static Row div()                         { return new Row("DIV",      null,  null);  }
        static Row field(String l, String v)     { return new Row("FIELD",    l,     v);     }
        static Row total(String l, String v)     { return new Row("TOTAL",    l,     v);     }
        static Row footer(String t)              { return new Row("FOOTER",   t,     null);  }
    }

    // ── PDF builder ─────────────────────────────────────────────────────────

    private static void buildPDF(String path, List<Row> rows) throws IOException {
        StringBuilder cs = new StringBuilder();
        float y = H - 24f;

        for (Row r : rows) {
            switch (r.type()) {
                case "HEADER" -> {
                    // Dark navy header bar
                    rect(cs, MARGIN - 8, y - 10, W - 2 * MARGIN + 16, 36, "0.08 0.18 0.33");
                    fill(cs);
                    textAt(cs, "F2", 16, centerX(r.label(), 16), y + 10, "1 1 1", r.label());
                    y -= 44;
                }
                case "SUBTITLE" -> {
                    textAt(cs, "F2", 11, centerX(r.label(), 11), y, "0.35 0.55 0.80", r.label());
                    y -= 20;
                }
                case "CONF" -> {
                    String t = "Conf #: " + r.label();
                    textAt(cs, "F1", 10, centerX(t, 10), y, "0.40 0.40 0.40", t);
                    y -= 18;
                }
                case "DIV" -> {
                    line(cs, MARGIN, y + 4, W - MARGIN, y + 4, "0.80 0.80 0.80");
                    y -= 10;
                }
                case "FIELD" -> {
                    textAt(cs, "F2", 10, MARGIN, y, "0.35 0.35 0.35", r.label());
                    textAt(cs, "F1", 10, COL2,   y, "0.10 0.10 0.10", r.value() != null ? r.value() : "");
                    y -= 18;
                }
                case "TOTAL" -> {
                    // Highlight band
                    rect(cs, MARGIN - 4, y - 4, W - 2 * MARGIN + 8, 22, "0.88 0.97 0.88");
                    fill(cs);
                    textAt(cs, "F2", 12, MARGIN, y, "0.05 0.45 0.05", r.label());
                    textAt(cs, "F2", 12, COL2,   y, "0.05 0.45 0.05", r.value() != null ? r.value() : "");
                    y -= 24;
                }
                case "FOOTER" -> {
                    y -= 4;
                    textAt(cs, "F1", 9, centerX(r.label(), 9), y, "0.60 0.60 0.60", r.label());
                    y -= 14;
                }
            }
        }

        String contentStr = "BT\n" + cs + "ET\n";
        byte[] contentBytes = contentStr.getBytes(StandardCharsets.ISO_8859_1);

        // Build PDF objects
        List<byte[]> objs = new ArrayList<>();
        objs.add(pdfObj(1, "<< /Type /Catalog /Pages 2 0 R >>"));
        objs.add(pdfObj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
        objs.add(pdfObj(3, String.format(
            "<< /Type /Page /Parent 2 0 R\n" +
            "   /MediaBox [0 0 %.1f %.1f]\n" +
            "   /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >>\n" +
            "   /Contents 4 0 R >>", W, H)));
        objs.add(streamObj(4, contentBytes));
        objs.add(pdfObj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objs.add(pdfObj(6, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));

        // Write file
        try (FileOutputStream fos = new FileOutputStream(path)) {
            byte[] hdr = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
            fos.write(hdr);
            int[] offsets = new int[objs.size() + 1];
            int off = hdr.length;
            for (int i = 0; i < objs.size(); i++) {
                offsets[i + 1] = off;
                fos.write(objs.get(i));
                off += objs.get(i).length;
            }
            int xrefOff = off;
            StringBuilder xr = new StringBuilder("xref\n0 " + (objs.size() + 1) + "\n");
            xr.append("0000000000 65535 f \n");
            for (int i = 1; i <= objs.size(); i++)
                xr.append(String.format("%010d 00000 n \n", offsets[i]));
            fos.write(xr.toString().getBytes(StandardCharsets.ISO_8859_1));
            fos.write(String.format(
                "trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n",
                objs.size() + 1, xrefOff).getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    // ── PDF drawing helpers ─────────────────────────────────────────────────

    private static void textAt(StringBuilder cs, String font, float size,
                                float x, float y, String color, String text) {
        cs.append(color).append(" rg\n");
        cs.append("/").append(font).append(" ").append(size).append(" Tf\n");
        cs.append(String.format("1 0 0 1 %.2f %.2f Tm\n", x, y));
        cs.append(ps(text)).append(" Tj\n");
    }

    private static void rect(StringBuilder cs, float x, float y, float w, float h, String rgb) {
        cs.append(rgb).append(" rg\n");
        cs.append(String.format("%.2f %.2f %.2f %.2f re\n", x, y, w, h));
    }

    private static void fill(StringBuilder cs) { cs.append("f\n"); }

    private static void line(StringBuilder cs, float x1, float y1, float x2, float y2, String rgb) {
        cs.append(rgb).append(" RG\n0.5 w\n");
        cs.append(String.format("%.2f %.2f m %.2f %.2f l S\n", x1, y1, x2, y2));
    }

    private static float centerX(String text, float size) {
        float tw = text.length() * size * 0.50f;
        return Math.max(MARGIN, (W - tw) / 2f);
    }

    /** Escape a string as a PDF literal string */
    private static String ps(String s) {
        if (s == null) s = "";
        // Only keep printable ASCII (Helvetica Type1 supports ISO-8859-1)
        StringBuilder sb = new StringBuilder("(");
        for (char c : s.toCharArray()) {
            if (c == '\\') sb.append("\\\\");
            else if (c == '(')  sb.append("\\(");
            else if (c == ')')  sb.append("\\)");
            else if (c >= 32 && c <= 126) sb.append(c);
            else sb.append('?');   // replace non-ASCII with ?
        }
        return sb.append(")").toString();
    }

    private static byte[] pdfObj(int id, String dict) {
        return (id + " 0 obj\n" + dict + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] streamObj(int id, byte[] content) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        b.write((id + " 0 obj\n<< /Length " + content.length + " >>\nstream\n")
            .getBytes(StandardCharsets.ISO_8859_1));
        b.write(content);
        b.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        return b.toByteArray();
    }
}
