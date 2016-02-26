package com.suprtek;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.lowagie.text.*;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.lowagie.text.html.HtmlTags;
import com.lowagie.text.pdf.CMYKColor;
import com.lowagie.text.pdf.PdfWriter;

// based on Bruno Lowagie examples iText in Action
public class HtmlToPdfSaxManual extends DefaultHandler {
    private static final CMYKColor DARK_BLUE = new CMYKColor(0.9f, 0.7f, 0.4f, 0.1f); // RGB=23,69,139
    private static final Font HELVETICA_DARK_BLUE_DEFAULT_SIZE =
            new Font(Font.HELVETICA, Font.UNDEFINED, Font.UNDEFINED, DARK_BLUE);
    private static final int[] HEADING_FONT_SIZES = {24, 18, 16, 14, 12, 10};
    private static final Map<String, Font> HEADING_FONTS = loadHeadingFonts();
    private static final String BLOCKQUOTE = "blockquote";

    private Document document;
    private Stack<Object> stack;
    private Chunk currentChunk;
    private String html;
    private String pdf;

    public HtmlToPdfSaxManual(String html, String pdf) {
        this.html = html;
        this.pdf = pdf;
        document = new Document();
        stack = new Stack<>();
    }

    public void start() throws IOException, DocumentException, ParserConfigurationException, SAXException {
        PdfWriter.getInstance(document, new FileOutputStream(pdf));
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(new InputSource(this.getClass().getResourceAsStream(html)), this);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String content = new String(ch, start, length);
        if (content.trim().length() == 0) {
            return;
        }
        if (currentChunk == null) {
            currentChunk = new Chunk(content.trim());
        } else {
            currentChunk.append(" ");
            currentChunk.append(content.trim());
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (document.isOpen()) {
            updateStack();
            for (String heading : HtmlTags.H) {
                if (heading.equals(qName)) {
                    flushStack();
                    Font font = HEADING_FONTS.get(heading);
                    stack.push(new Paragraph(Float.NaN, "", font));
                    return;
                }
            }
            if (BLOCKQUOTE.equals(qName)) {
                flushStack();
                Paragraph p = new Paragraph();
                p.setIndentationLeft(50);
                p.setIndentationRight(20);
                stack.push(p);
            } else if (HtmlTags.ANCHOR.equals(qName)) {
                Anchor anchor = new Anchor("", HELVETICA_DARK_BLUE_DEFAULT_SIZE);
                anchor.setReference(attributes.getValue(HtmlTags.REFERENCE));
                stack.push(anchor);
            } else if (HtmlTags.ORDEREDLIST.equals(qName)) {
                stack.push(new List(List.ORDERED, 10));
            } else if (HtmlTags.UNORDEREDLIST.equals(qName)) {
                stack.push(new List(List.UNORDERED, 10));
            } else if (HtmlTags.LISTITEM.equals(qName)) {
                stack.push(new ListItem());
            } else if (HtmlTags.IMAGE.equals(qName)) {
                handleImage(attributes);
            }
        } else if (HtmlTags.BODY.equals(qName)) {
            document.open();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (document.isOpen()) {
            updateStack();
            for (String heading : HtmlTags.H) {
                if (heading.equals(qName)) {
                    flushStack();
                    return;
                }
            }
            if (BLOCKQUOTE.equals(qName) || HtmlTags.ORDEREDLIST.equals(qName) || HtmlTags.UNORDEREDLIST.equals(qName)) {
                flushStack();
            } else if (HtmlTags.NEWLINE.equals(qName)) {
                currentChunk = Chunk.NEWLINE;
                updateStack();
            } else if (HtmlTags.LISTITEM.equals(qName)) {
                ListItem listItem = (ListItem) stack.pop();
                List list = (List) stack.pop();
                list.add(listItem);
                stack.push(list);
            } else if (HtmlTags.ANCHOR.equals(qName)) {
                Anchor anchor = (Anchor) stack.pop();
                addIfEmptyElseAddToArray(anchor);
            } else if (HtmlTags.HTML.equals(qName)) {
                flushStack();
                document.close();
            }
        } else {
            if (HtmlTags.TITLE.equals(qName)) {
                document.addTitle(currentChunk.getContent().trim());
            }
            currentChunk = null;
        }
    }

    private void updateStack() {
        if (currentChunk != null) {
            TextElementArray current;
            if (stack.isEmpty()) {
                current = new Paragraph();
            } else {
                current = (TextElementArray) stack.pop();
                if (!(current instanceof Paragraph) || !((Paragraph) current).isEmpty()) {
                    current.add(new Chunk(" "));
                }
            }
            current.add(currentChunk);
            stack.push(current);
            currentChunk = null;
        }
    }

    private void flushStack() {
        while (!stack.isEmpty()) {
            Element element = (Element) stack.pop();
            addIfEmptyElseAddToArray(element);
        }
    }

    private void addIfEmptyElseAddToArray(Element element) {
        if (stack.isEmpty()) {
            try {
                document.add(element);
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
        } else {
            TextElementArray prev = (TextElementArray) stack.pop();
            prev.add(element);
            stack.push(prev);
        }
    }

    private void handleImage(Attributes attributes) {
        String url = attributes.getValue(HtmlTags.URL);
        String alt = attributes.getValue(HtmlTags.ALT);
        if (url == null) {
            return;
        }
        Image img;
        try {
            img = Image.getInstance(getClass().getResource(url));
            if (alt != null) {
                img.setAlt(alt);
            }
        } catch (IOException | BadElementException e) {
            if (alt == null) {
                try {
                    document.add(new Paragraph(e.getMessage()));
                } catch (DocumentException e1) {
                    throw new RuntimeException(e1);
                }
            } else {
                try {
                    document.add(new Paragraph(alt));
                } catch (DocumentException e1) {
                    throw new RuntimeException(e1);
                }
            }
            return;
        }
        String property;
        property = attributes.getValue(HtmlTags.BORDERWIDTH);
        if (property != null) {
            int border = Integer.parseInt(property);
            if (border == 0) {
                img.setBorder(Image.NO_BORDER);
            } else {
                img.setBorder(Image.BOX);
                img.setBorderWidth(border);
            }
        }
        property = attributes.getValue(HtmlTags.ALIGN);
        if (property != null) {
            int align = Image.DEFAULT;
            if (ElementTags.ALIGN_LEFT.equalsIgnoreCase(property)) {
                align = Image.LEFT;
            } else if (ElementTags.ALIGN_RIGHT.equalsIgnoreCase(property)) {
                align = Image.RIGHT;
            } else if (ElementTags.ALIGN_MIDDLE.equalsIgnoreCase(property)) {
                align = Image.MIDDLE;
            }
            img.setAlignment(align | Image.TEXTWRAP);
        }
        property = attributes.getValue(HtmlTags.PLAINWIDTH);
        if (property != null) {
            int w = Integer.parseInt(property);
            property = attributes.getValue(HtmlTags.PLAINHEIGHT);
            if (property != null) {
                int h = Integer.parseInt(property);
                img.scaleAbsolute(w, h);
            }
        }
        try {
            document.add(img);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Font> loadHeadingFonts() {
        Map<String, Font> ret = new HashMap<>();
        for (int i = 0; i < HtmlTags.H.length; i++) {
            String tag = HtmlTags.H[i];
            Font font = new Font(HELVETICA_DARK_BLUE_DEFAULT_SIZE);
            font.setSize(HEADING_FONT_SIZES[i]);
            ret.put(tag, font);
        }
        return ret;
    }

    public static void main(final String[] args) throws Exception {
        HtmlToPdfSaxManual flyer = new HtmlToPdfSaxManual("/htmlToPdfSaxManual.html", "htmlToPdfSaxManual.pdf");
        flyer.start();
    }
}