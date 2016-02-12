package com.suprtek;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;

// based on Bruno Lowagie examples iText in Action
public class Table {
    public static void main(String[] args) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream("table.pdf"));
        doc.open();
        PdfPTable table = new PdfPTable(3);
        PdfPCell cell = new PdfPCell(new Paragraph("header with colspan 3"));
        cell.setColspan(3);
        table.addCell(cell);
        table.addCell("1.1");
        table.addCell("2.1");
        table.addCell("3.1");
        table.addCell("1.2");
        table.addCell("2.2");
        table.addCell("3.2");
        doc.add(table);
        doc.close();
    }
}
