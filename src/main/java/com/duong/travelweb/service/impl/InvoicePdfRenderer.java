package com.duong.travelweb.service.impl;

import com.duong.travelweb.model.dto.InvoiceDTO;
import com.duong.travelweb.model.dto.InvoiceItemDTO;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Xuất hoá đơn ra PDF (OpenPDF). Nhúng font DejaVu Sans (resources/fonts) để hiển thị được tiếng Việt —
 * font chuẩn Helvetica của PDF không có dấu tiếng Việt.
 */
@Component
public class InvoicePdfRenderer {
    private static final Color PRIMARY = new Color(0, 51, 102);
    private static final Color MUTED = new Color(100, 110, 120);
    private static final Color HEADER_BG = new Color(235, 240, 246);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BaseFont regular;
    private final BaseFont bold;

    public InvoicePdfRenderer() {
        this.regular = loadFont("fonts/DejaVuSans.ttf");
        this.bold = loadFont("fonts/DejaVuSans-Bold.ttf");
    }

    public byte[] render(InvoiceDTO invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.addTitle("Invoice " + invoice.getInvoiceNumber());
            document.open();
            writeHeader(document, invoice);
            writeParties(document, invoice);
            writeItems(document, invoice);
            writeTotals(document, invoice);
            writeFooter(document, invoice);
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được PDF hoá đơn " + invoice.getInvoiceNumber(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private void writeHeader(Document document, InvoiceDTO invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1, 1});
        table.setWidthPercentage(100);
        PdfPCell brand = cell(new Phrase("TripNova", font(bold, 22, PRIMARY)), Element.ALIGN_LEFT);
        table.addCell(brand);
        Paragraph right = new Paragraph();
        right.add(new Phrase("INVOICE\n", font(bold, 16, PRIMARY)));
        right.add(new Phrase(invoice.getInvoiceNumber() + "\n", font(regular, 10, Color.BLACK)));
        right.add(new Phrase("Issued: " + formatDateTime(invoice.getIssuedAt()), font(regular, 9, MUTED)));
        PdfPCell meta = new PdfPCell();
        meta.addElement(right);
        meta.setBorder(Rectangle.NO_BORDER);
        right.setAlignment(Element.ALIGN_RIGHT);
        table.addCell(meta);
        document.add(table);
        document.add(spacer(12));
    }

    private void writeParties(Document document, InvoiceDTO invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1, 1});
        table.setWidthPercentage(100);

        Paragraph billTo = new Paragraph();
        billTo.add(new Phrase("BILL TO\n", font(bold, 9, MUTED)));
        billTo.add(new Phrase(nvl(invoice.getBillingName(), invoice.getCustomerName()) + "\n", font(bold, 11, Color.BLACK)));
        billTo.add(new Phrase(nvl(invoice.getCustomerEmail(), "") + "\n", font(regular, 9, Color.BLACK)));
        if (invoice.getBillingAddress() != null) {
            billTo.add(new Phrase(invoice.getBillingAddress() + "\n", font(regular, 9, Color.BLACK)));
        }
        if (invoice.getBillingTaxCode() != null) {
            billTo.add(new Phrase("Tax code: " + invoice.getBillingTaxCode(), font(regular, 9, Color.BLACK)));
        }
        table.addCell(block(billTo));

        Paragraph order = new Paragraph();
        order.add(new Phrase("ORDER\n", font(bold, 9, MUTED)));
        order.add(new Phrase(invoice.getOrderCode() + "  (" + invoice.getOrderStatus() + ")\n", font(bold, 11, Color.BLACK)));
        if (invoice.getPaymentMethod() != null) {
            order.add(new Phrase("Payment: " + invoice.getPaymentMethod() + "\n", font(regular, 9, Color.BLACK)));
        }
        if (invoice.getTransactionId() != null) {
            order.add(new Phrase("Transaction: " + invoice.getTransactionId() + "\n", font(regular, 9, Color.BLACK)));
        }
        if (invoice.getPaidAt() != null) {
            order.add(new Phrase("Paid at: " + formatDateTime(invoice.getPaidAt()), font(regular, 9, Color.BLACK)));
        }
        table.addCell(block(order));
        document.add(table);
        document.add(spacer(16));
    }

    private void writeItems(Document document, InvoiceDTO invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{4.2f, 2.6f, 1f, 1.2f, 2f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String title : new String[]{"Item", "Period", "Nights / days", "Guests", "Amount"}) {
            PdfPCell header = cell(new Phrase(title, font(bold, 9, PRIMARY)),
                    "Amount".equals(title) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            header.setBackgroundColor(HEADER_BG);
            header.setPadding(6);
            table.addCell(header);
        }
        String currency = invoice.getCurrencyCode();
        for (InvoiceItemDTO item : invoice.getItems()) {
            Paragraph name = new Paragraph();
            name.add(new Phrase(nvl(item.getTitle(), item.getHotelName()) + "\n", font(bold, 9, Color.BLACK)));
            name.add(new Phrase(nvl(item.getSubtitle(), item.getRoomTypeName()) + " · " + item.getStatus(), font(regular, 8, MUTED)));
            PdfPCell nameCell = new PdfPCell();
            nameCell.addElement(name);
            table.addCell(row(nameCell));
            table.addCell(row(cell(new Phrase(formatDate(item), font(regular, 9, Color.BLACK)), Element.ALIGN_LEFT)));
            table.addCell(row(cell(new Phrase(String.valueOf(nvl(item.getNights(), 0)), font(regular, 9, Color.BLACK)), Element.ALIGN_LEFT)));
            table.addCell(row(cell(new Phrase(item.getGuests() == null ? "—" : String.valueOf(item.getGuests()), font(regular, 9, Color.BLACK)), Element.ALIGN_LEFT)));
            Paragraph amount = new Paragraph(new Phrase(money(item.getAmount(), currency), font(regular, 9, Color.BLACK)));
            if (item.getRefundAmount() != null && item.getRefundAmount().signum() > 0) {
                amount.add(new Phrase("\nRefunded " + money(item.getRefundAmount(), currency), font(regular, 8, new Color(180, 30, 30))));
            }
            amount.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell amountCell = new PdfPCell();
            amountCell.addElement(amount);
            table.addCell(row(amountCell));
        }
        document.add(table);
        document.add(spacer(10));
    }

    private void writeTotals(Document document, InvoiceDTO invoice) throws DocumentException {
        String currency = invoice.getCurrencyCode();
        PdfPTable table = new PdfPTable(new float[]{3, 1.4f});
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        addTotalRow(table, "Subtotal", money(invoice.getSubtotal(), currency), false);
        if (invoice.getDiscountTotal() != null && invoice.getDiscountTotal().signum() > 0) {
            addTotalRow(table, "Discount", "-" + money(invoice.getDiscountTotal(), currency), false);
        }
        addTotalRow(table, "Tax", money(invoice.getTaxAmount(), currency), false);
        addTotalRow(table, "Total paid", money(invoice.getTotalAmount(), currency), true);
        if (invoice.getRefundedAmount() != null && invoice.getRefundedAmount().signum() > 0) {
            addTotalRow(table, "Refunded", "-" + money(invoice.getRefundedAmount(), currency), false);
            addTotalRow(table, "Net amount", money(invoice.getTotalAmount().subtract(invoice.getRefundedAmount()), currency), true);
        }
        document.add(table);
    }

    private void writeFooter(Document document, InvoiceDTO invoice) throws DocumentException {
        document.add(spacer(30));
        Paragraph note = new Paragraph("Thank you for travelling with TripNova. This invoice was generated electronically "
                + "and is valid without a signature.", font(regular, 8, MUTED));
        note.setAlignment(Element.ALIGN_CENTER);
        document.add(note);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean strong) {
        BaseFont base = strong ? bold : regular;
        PdfPCell labelCell = cell(new Phrase(label, font(base, 10, strong ? PRIMARY : Color.BLACK)), Element.ALIGN_LEFT);
        PdfPCell valueCell = cell(new Phrase(value, font(base, 10, strong ? PRIMARY : Color.BLACK)), Element.ALIGN_RIGHT);
        if (strong) {
            labelCell.setBorder(Rectangle.TOP);
            valueCell.setBorder(Rectangle.TOP);
        }
        labelCell.setPaddingTop(4);
        valueCell.setPaddingTop(4);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private PdfPCell cell(Phrase phrase, int align) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private PdfPCell block(Paragraph content) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(content);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell row(PdfPCell cell) {
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(220, 224, 230));
        cell.setPadding(6);
        return cell;
    }

    private Paragraph spacer(float height) {
        Paragraph spacer = new Paragraph(" ", font(regular, 1, Color.WHITE));
        spacer.setSpacingAfter(height);
        return spacer;
    }

    private Font font(BaseFont base, float size, Color color) {
        return new Font(base, size, Font.NORMAL, color);
    }

    private static String formatDate(InvoiceItemDTO item) {
        return item.getCheckInDate().format(DATE) + " → " + item.getCheckOutDate().format(DATE);
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }

    private static String money(BigDecimal amount, String currency) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount == null ? BigDecimal.ZERO : amount) + (currency == null ? "" : " " + currency);
    }

    private static <T> T nvl(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static BaseFont loadFont(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được font " + path, e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Font không hợp lệ: " + path, e);
        }
    }
}
