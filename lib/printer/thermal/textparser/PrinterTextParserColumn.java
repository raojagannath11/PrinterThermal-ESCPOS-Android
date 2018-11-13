package lib.printer.thermal.textparser;

import java.util.Hashtable;
import lib.printer.thermal.PrinterCommands;

public class PrinterTextParserColumn {
    
    private static String generateSpace(int nbrSpace) {
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < nbrSpace; i++) {
            str.append(" ");
        }
        return str.toString();
    }
    
    
    private PrinterTextParserLine textParserLine;
    private PrinterTextParserElement[] elements = new PrinterTextParserElement[0];
    
    /**
     * Create a new instance of PrinterTextParserColumn.
     *
     * @param textParserLine Parent PrinterTextParserLine instance
     * @param textColumn Text that the column contain
     */
    public PrinterTextParserColumn(PrinterTextParserLine textParserLine, String textColumn) {
        this.textParserLine = textParserLine;
        PrinterTextParser textParser = this.textParserLine.getTextParser();
        String textAlign = PrinterTextParser.TAGS_ALIGN_LEFT;
        byte[] textUnderlineStartColumn = textParser.getLastTextUnderline();
    
    
        // =================================================================
        // Check the column alignment
        
        switch (textColumn.substring(0, 3).toUpperCase()) {
            case "[" + PrinterTextParser.TAGS_ALIGN_LEFT + "]":
            case "[" + PrinterTextParser.TAGS_ALIGN_CENTER + "]":
            case "[" + PrinterTextParser.TAGS_ALIGN_RIGHT + "]":
                textAlign = textColumn.substring(1, 2).toUpperCase();
                textColumn = textColumn.substring(3);
                break;
        }
        
        String trimmedTextColumn = textColumn.trim();
        boolean isImgOrBarCodeLine = false;
        
        if (this.textParserLine.getNbrColumns() == 1 && trimmedTextColumn.indexOf("<") == 0) {
            // =================================================================
            // Image or Bar Code Lines
            int openTagIndex = trimmedTextColumn.indexOf("<"),
                openTagEndIndex = trimmedTextColumn.indexOf(">", openTagIndex + 1) + 1;
            
            if(openTagIndex < openTagEndIndex) {
                PrinterTextParserTag textParserTag = new PrinterTextParserTag(trimmedTextColumn.substring(openTagIndex, openTagEndIndex));
    
                switch (textParserTag.getTagName()) {
                    case PrinterTextParser.TAGS_IMAGE:
                    case PrinterTextParser.TAGS_BARCODE:
                        String closeTag = "</" + textParserTag.getTagName() + ">";
                        int closeTagPosition = trimmedTextColumn.length() - closeTag.length();
            
                        if (trimmedTextColumn.substring(closeTagPosition).equals(closeTag)) {
                            switch (textParserTag.getTagName()) {
                                case PrinterTextParser.TAGS_IMAGE:
                                    this.appendImage(textAlign, trimmedTextColumn.substring(openTagEndIndex, closeTagPosition));
                                    break;
                                case PrinterTextParser.TAGS_BARCODE:
                                    this.appendBarCode(textAlign, textParserTag.getAttributes(), trimmedTextColumn.substring(openTagEndIndex, closeTagPosition));
                                    break;
                            }
                            isImgOrBarCodeLine = true;
                        }
                        break;
                }
            }
        }
        
        if(!isImgOrBarCodeLine) {
            // =================================================================
            // If the tag is for format text
            
            int offset = 0;
            while (true) {
                int openTagIndex = textColumn.indexOf("<", offset), closeTagIndex = -1;
        
                if (openTagIndex != -1) {
                    closeTagIndex = textColumn.indexOf(">", openTagIndex);
                } else {
                    openTagIndex = textColumn.length();
                }
        
                this.appendString(textColumn.substring(offset, openTagIndex));
        
                if (closeTagIndex == -1) {
                    break;
                }
                
                closeTagIndex++;
                PrinterTextParserTag textParserTag = new PrinterTextParserTag(textColumn.substring(openTagIndex, closeTagIndex));
                
                if (PrinterTextParser.isTagTextFormat(textParserTag.getTagName())) {
                    if(textParserTag.isCloseTag()) {
                        switch (textParserTag.getTagName()) {
                            case PrinterTextParser.TAGS_FORMAT_TEXT_BOLD:
                                textParser.dropTextBold(PrinterCommands.TEXT_WEIGHT_BOLD);
                                break;
                            case PrinterTextParser.TAGS_FORMAT_TEXT_UNDERLINE:
                                textParser.dropLastTextUnderline(PrinterCommands.TEXT_UNDERLINE_LARGE);
                                break;
                            case PrinterTextParser.TAGS_FORMAT_TEXT_FONT:
                                textParser.dropLastTextSize();
                                break;
                        }
                    } else {
                        switch (textParserTag.getTagName()) {
                            case PrinterTextParser.TAGS_FORMAT_TEXT_BOLD:
                                textParser.addTextBold(PrinterCommands.TEXT_WEIGHT_BOLD);
                                break;
                            case PrinterTextParser.TAGS_FORMAT_TEXT_UNDERLINE:
                                textParser.addTextUnderline(PrinterCommands.TEXT_UNDERLINE_LARGE);
                                break;
                            case PrinterTextParser.TAGS_FORMAT_TEXT_FONT:
                                if (textParserTag.hasAttribute("size")) {
                                    switch (textParserTag.getAttribute("size")) {
                                        case PrinterTextParser.ATTR_FORMAT_TEXT_SIZE_SMALL:
                                            textParser.addTextSize(PrinterCommands.TEXT_SIZE_NORMAL);
                                            break;
                                        case PrinterTextParser.ATTR_FORMAT_TEXT_SIZE_MEDIUM:
                                            textParser.addTextSize(PrinterCommands.TEXT_SIZE_MEDIUM);
                                            break;
                                        case PrinterTextParser.ATTR_FORMAT_TEXT_SIZE_TALL:
                                            textParser.addTextSize(PrinterCommands.TEXT_SIZE_DOUBLE_HEIGHT);
                                            break;
                                        case PrinterTextParser.ATTR_FORMAT_TEXT_SIZE_WIDE:
                                            textParser.addTextSize(PrinterCommands.TEXT_SIZE_DOUBLE_WIDTH);
                                            break;
                                        case PrinterTextParser.ATTR_FORMAT_TEXT_SIZE_BIG:
                                            textParser.addTextSize(PrinterCommands.TEXT_SIZE_BIG);
                                            break;
                                    }
                                }
                                break;
                        }
                    }
                    offset = closeTagIndex;
                } else {
                    this.appendString("<");
                    offset = openTagIndex + 1;
                }
            }
            
            // =================================================================
            // Define the number of spaces required for the different alignments
            
            int nbrCharColumn = this.textParserLine.getNbrCharColumn(),
                nbrCharForgetted = this.textParserLine.getNbrCharForgetted(),
                nbrCharColumnExceeded = this.textParserLine.getNbrCharColumnExceeded(),
                nbrCharTextWithoutTag = 0,
                leftSpace = 0,
                rightSpace = 0;
    
            for (PrinterTextParserElement textParserElement : this.elements) {
                nbrCharTextWithoutTag += textParserElement.length();
            }
    
            switch (textAlign) {
                case PrinterTextParser.TAGS_ALIGN_LEFT:
                    rightSpace = nbrCharColumn - nbrCharTextWithoutTag;
                    break;
                case PrinterTextParser.TAGS_ALIGN_CENTER:
                    leftSpace = (int) Math.floor((((float) nbrCharColumn) - ((float) nbrCharTextWithoutTag)) / 2f);
                    rightSpace = nbrCharColumn - nbrCharTextWithoutTag - leftSpace;
                    break;
                case PrinterTextParser.TAGS_ALIGN_RIGHT:
                    leftSpace = nbrCharColumn - nbrCharTextWithoutTag;
                    break;
            }
    
            if (nbrCharForgetted > 0) {
                nbrCharForgetted -= 1;
                rightSpace++;
            }
    
            if (nbrCharColumnExceeded < 0) {
                leftSpace += nbrCharColumnExceeded;
                nbrCharColumnExceeded = 0;
                if (leftSpace < 1) {
                    rightSpace += leftSpace - 1;
                    leftSpace = 1;
                }
            }
    
            if (leftSpace < 0) {
                nbrCharColumnExceeded += leftSpace;
                leftSpace = 0;
            }
            if (rightSpace < 0) {
                nbrCharColumnExceeded += rightSpace;
                rightSpace = 0;
            }
    
            if (leftSpace > 0) {
                this.prependString(PrinterTextParserColumn.generateSpace(leftSpace), PrinterCommands.TEXT_SIZE_MEDIUM, PrinterCommands.TEXT_WEIGHT_NORMAL, textUnderlineStartColumn);
            }
            if (rightSpace > 0) {
                this.appendString(PrinterTextParserColumn.generateSpace(rightSpace), PrinterCommands.TEXT_SIZE_MEDIUM, PrinterCommands.TEXT_WEIGHT_NORMAL, textParser.getLastTextUnderline());
            }
    
            // =================================================================================================
            // nbrCharForgetted and nbrCharColumnExceeded is use to define number of spaces for the next columns
            
            this.textParserLine
                .setNbrCharForgetted(nbrCharForgetted)
                .setNbrCharColumnExceeded(nbrCharColumnExceeded);
        }
    }
    
    /**
     *
     * @param text
     * @return Fluent method
     */
    private PrinterTextParserColumn prependString(String text) {
        PrinterTextParser textParser = this.textParserLine.getTextParser();
        return this.prependString(text, textParser.getLastTextSize(), textParser.getLastTextBold(), textParser.getLastTextUnderline());
    }
    
    private PrinterTextParserColumn prependString(String text, byte[] textSize, byte[] textBold, byte[] textUnderline) {
        return this.prependElement(new PrinterTextParserString(text, textSize, textBold, textUnderline));
    }
    
    private PrinterTextParserColumn appendString(String text) {
        PrinterTextParser textParser = this.textParserLine.getTextParser();
        return this.appendString(text, textParser.getLastTextSize(), textParser.getLastTextBold(), textParser.getLastTextUnderline());
    }
    
    private PrinterTextParserColumn appendString(String text, byte[] textSize, byte[] textBold, byte[] textUnderline) {
        return this.appendElement(new PrinterTextParserString(text, textSize, textBold, textUnderline));
    }
    
    private PrinterTextParserColumn prependImage(String textAlign, String hexString) {
        return this.prependElement(new PrinterTextParserImg(this, textAlign, hexString));
    }
    
    private PrinterTextParserColumn appendImage(String textAlign, String hexString) {
        return this.appendElement(new PrinterTextParserImg(this, textAlign, hexString));
    }
    
    private PrinterTextParserColumn prependBarCode(String textAlign, Hashtable<String,String> barCodeAttributes, String code) {
        return this.prependElement(new PrinterTextParserBarCode(this, textAlign, barCodeAttributes, code));
    }
    
    private PrinterTextParserColumn appendBarCode(String textAlign, Hashtable<String,String> barCodeAttributes, String code) {
        return this.appendElement(new PrinterTextParserBarCode(this, textAlign, barCodeAttributes, code));
    }
    
    private PrinterTextParserColumn prependElement(PrinterTextParserElement element) {
        PrinterTextParserElement[] elementsTmp = new PrinterTextParserElement[this.elements.length + 1];
        elementsTmp[0] = element;
        for (int i = 1; i <= this.elements.length; i++) {
            elementsTmp[i] = this.elements[i - 1];
        }
        this.elements = elementsTmp;
        return this;
    }
    
    private PrinterTextParserColumn appendElement(PrinterTextParserElement element) {
        PrinterTextParserElement[] elementsTmp = new PrinterTextParserElement[this.elements.length + 1];
        for (int i = 0; i < this.elements.length; i++) {
            elementsTmp[i] = this.elements[i];
        }
        elementsTmp[this.elements.length] = element;
        this.elements = elementsTmp;
        return this;
    }
    
    public PrinterTextParserLine getLine() {
        return this.textParserLine;
    }
    
    public PrinterTextParserElement[] getElements() {
        return this.elements;
    }
}
