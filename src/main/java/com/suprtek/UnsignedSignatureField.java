package com.suprtek;

import java.io.FileOutputStream;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfAcroForm;
import com.lowagie.text.pdf.PdfWriter;

public class UnsignedSignatureField {

    public static void main(String[] args) throws Exception {
        Document doc = new Document();
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream("unsigned_signature_field.pdf"));
        doc.open();
        doc.add(new Paragraph("Hello world with digital signature block."));
        PdfAcroForm acroForm = writer.getAcroForm();
        acroForm.addSignature("sig", 73, 705, 149, 759);
        doc.close();
    }
}