package com.gridpulse.common.ticket;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// the A4 e-ticket: title, the booking facts, a 200 px qr code of the ticket code (the gate scans it)
public final class TicketPdf {

    private static final Color ORANGE = new Color(0xff, 0x5b, 0x14);
    private static final Color GREY = new Color(0x69, 0x6c, 0x74);
    private static final int QR_SIZE = 200;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private TicketPdf() {
    }

    public static byte[] render(TicketData ticket) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 56, 56, 56, 56);
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("GridPulse", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, ORANGE)));
        document.add(new Paragraph("E-ticket", FontFactory.getFont(FontFactory.HELVETICA, 14, GREY)));
        document.add(facts(ticket));
        document.add(qrCode(ticket.code()));

        Paragraph code = new Paragraph(ticket.code(), FontFactory.getFont(FontFactory.COURIER_BOLD, 13));
        code.setAlignment(Element.ALIGN_CENTER);
        document.add(code);
        Paragraph gate = new Paragraph("Show this QR at the gate", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        gate.setAlignment(Element.ALIGN_CENTER);
        document.add(gate);
        document.close();
        return out.toByteArray();
    }

    private static PdfPTable facts(TicketData ticket) {
        PdfPTable table = new PdfPTable(new float[] { 1, 2.4f });
        table.setWidthPercentage(100);
        table.setSpacingBefore(24);
        table.setSpacingAfter(24);
        row(table, "Driver", ticket.driverName());
        row(table, "Track", ticket.trackName() + (ticket.trackLocation() == null ? "" : ", " + ticket.trackLocation()));
        row(table, "Date", ticket.sessionDate().format(DAY));
        row(table, "Time", ticket.startTime().format(TIME) + " \u2013 " + ticket.endTime().format(TIME) + " (IST)");
        row(table, "Car", "#" + ticket.carNumber() + " \u00b7 " + ticket.carCategory());
        row(table, "Booking ID", String.valueOf(ticket.bookingId()));
        return table;
    }

    private static void row(PdfPTable table, String name, String value) {
        table.addCell(cell(name, FontFactory.getFont(FontFactory.HELVETICA, 11, GREY)));
        table.addCell(cell(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(0xe3, 0xe4, 0xe7));
        cell.setPaddingTop(8);
        cell.setPaddingBottom(8);
        return cell;
    }

    private static Image qrCode(String code) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage picture = MatrixToImageWriter.toBufferedImage(matrix);
            Image image = Image.getInstance(picture, null);
            image.scaleAbsolute(QR_SIZE, QR_SIZE);
            image.setAlignment(Element.ALIGN_CENTER);
            return image;
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("QR code could not be made", e);
        }
    }
}
