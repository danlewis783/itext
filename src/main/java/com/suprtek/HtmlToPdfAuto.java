package com.suprtek;

import com.lowagie.text.Document;
import com.lowagie.text.html.HtmlParser;
import com.lowagie.text.pdf.PdfWriter;
import org.xml.sax.InputSource;

import java.io.FileOutputStream;

// based on Bruno Lowagie examples iText in Action
public class HtmlToPdfAuto {
    public static void main(String[] args) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream("htmlToPdfAuto.pdf"));
        doc.open();
        HtmlParser.parse(doc, new InputSource(HtmlToPdfAuto.class.getResourceAsStream("/htmlToPdfSaxAuto.html")));
        doc.close();
    }
}
