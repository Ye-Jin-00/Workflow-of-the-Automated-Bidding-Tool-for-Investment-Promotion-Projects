package com.sincit.bid.service;

import com.sincit.bid.common.BusinessException;
import org.springframework.stereotype.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 轻量表格读取：XLSX 通过 ZIP + XML 解析，CSV 使用标准逗号分隔并支持引号。
 * 不额外引入 POI，适合批量导入模板这类结构固定的表格。
 */
@Service
public class TableFileReader {

    private static final int MAX_ROWS = 500;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<List<String>> read(byte[] bytes, String filename) {
        String ext = extension(filename);
        if ("csv".equals(ext)) {
            return readCsv(new String(bytes, StandardCharsets.UTF_8));
        }
        if ("xlsx".equals(ext)) {
            return readXlsx(bytes);
        }
        throw BusinessException.invalid("批量导入仅支持 .xlsx 或 .csv 文件");
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private List<List<String>> readCsv(String text) {
        if (text.startsWith("\ufeff")) {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                row.add(cell.toString().trim());
                cell.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString().trim());
                cell.setLength(0);
                addRow(rows, row);
                row = new ArrayList<String>();
                if (rows.size() >= MAX_ROWS) {
                    break;
                }
            } else {
                cell.append(ch);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString().trim());
            addRow(rows, row);
        }
        return rows;
    }

    private void addRow(List<List<String>> rows, List<String> row) {
        boolean blank = true;
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                blank = false;
                break;
            }
        }
        if (!blank) {
            rows.add(new ArrayList<String>(row));
        }
    }

    private List<List<String>> readXlsx(byte[] bytes) {
        Map<String, byte[]> entries = unzip(bytes);
        List<String> shared = readSharedStrings(entries.get("xl/sharedStrings.xml"));
        boolean[] dateStyles = readDateStyles(entries.get("xl/styles.xml"));
        String sheetPath = entries.keySet().stream()
                .filter(name -> name.startsWith("xl/worksheets/") && name.endsWith(".xml"))
                .sorted()
                .findFirst()
                .orElse(null);
        if (sheetPath == null) {
            throw BusinessException.invalid("Excel 文件中未找到工作表");
        }
        try {
            Document doc = parse(entries.get(sheetPath));
            NodeList rowNodes = doc.getElementsByTagNameNS("*", "row");
            List<List<String>> rows = new ArrayList<List<String>>();
            for (int i = 0; i < rowNodes.getLength() && rows.size() < MAX_ROWS; i++) {
                Element rowEl = (Element) rowNodes.item(i);
                NodeList cellNodes = rowEl.getElementsByTagNameNS("*", "c");
                Map<Integer, String> values = new TreeMap<Integer, String>();
                int maxCol = -1;
                for (int j = 0; j < cellNodes.getLength(); j++) {
                    Element cellEl = (Element) cellNodes.item(j);
                    int col = columnIndex(cellEl.getAttribute("r"));
                    if (col < 0) {
                        col = maxCol + 1;
                    }
                    maxCol = Math.max(maxCol, col);
                    String value = cellValue(cellEl, shared, dateStyles);
                    if (value != null && !value.isEmpty()) {
                        values.put(col, value);
                    }
                }
                if (!values.isEmpty()) {
                    List<String> row = new ArrayList<String>();
                    for (int c = 0; c <= maxCol; c++) {
                        row.add(values.getOrDefault(c, ""));
                    }
                    rows.add(row);
                }
            }
            return rows;
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw BusinessException.invalid("Excel 文件解析失败，请确认文件未损坏且为 .xlsx 格式");
        }
    }

    private Map<String, byte[]> unzip(byte[] bytes) {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (!entry.getName().startsWith("xl/")) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                entries.put(entry.getName(), out.toByteArray());
            }
        } catch (IOException e) {
            throw BusinessException.invalid("Excel 文件无法解压，请重新下载模板后填写");
        }
        return entries;
    }

    private List<String> readSharedStrings(byte[] xmlBytes) {
        List<String> result = new ArrayList<String>();
        if (xmlBytes == null) {
            return result;
        }
        try {
            Document doc = parse(xmlBytes);
            NodeList items = doc.getElementsByTagNameNS("*", "si");
            for (int i = 0; i < items.getLength(); i++) {
                NodeList texts = ((Element) items.item(i)).getElementsByTagNameNS("*", "t");
                StringBuilder value = new StringBuilder();
                for (int j = 0; j < texts.getLength(); j++) {
                    value.append(texts.item(j).getTextContent());
                }
                result.add(value.toString());
            }
        } catch (Exception e) {
            throw BusinessException.invalid("Excel 共享字符串解析失败");
        }
        return result;
    }

    private boolean[] readDateStyles(byte[] xmlBytes) {
        if (xmlBytes == null) {
            return new boolean[0];
        }
        try {
            Document doc = parse(xmlBytes);
            Map<Integer, String> custom = new HashMap<Integer, String>();
            NodeList formats = doc.getElementsByTagNameNS("*", "numFmt");
            for (int i = 0; i < formats.getLength(); i++) {
                Element fmt = (Element) formats.item(i);
                custom.put(Integer.parseInt(fmt.getAttribute("numFmtId")), fmt.getAttribute("formatCode"));
            }
            NodeList xfs = doc.getElementsByTagNameNS("*", "xf");
            List<Boolean> dateStyles = new ArrayList<Boolean>();
            for (int i = 0; i < xfs.getLength(); i++) {
                Element xf = (Element) xfs.item(i);
                if (!"cellXfs".equals(xf.getParentNode().getLocalName())) {
                    continue;
                }
                int id;
                try {
                    id = Integer.parseInt(xf.getAttribute("numFmtId"));
                } catch (NumberFormatException e) {
                    id = 0;
                }
                dateStyles.add(isDateStyle(id, custom.get(id)));
            }
            boolean[] result = new boolean[dateStyles.size()];
            for (int i = 0; i < dateStyles.size(); i++) {
                result[i] = dateStyles.get(i);
            }
            return result;
        } catch (Exception e) {
            return new boolean[0];
        }
    }

    private boolean isDateStyle(int id, String format) {
        if ((id >= 14 && id <= 22) || (id >= 45 && id <= 47)) {
            return true;
        }
        if (format == null) {
            return false;
        }
        String value = format.toLowerCase(Locale.ROOT)
                .replaceAll("\"[^\"]*\"", "")
                .replaceAll("\\[[^\\]]*\\]", "");
        return value.contains("y") || value.contains("d") || value.contains("h")
                || value.contains("s") || value.contains("m");
    }

    private String cellValue(Element cell, List<String> shared, boolean[] dateStyles) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList texts = cell.getElementsByTagNameNS("*", "t");
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < texts.getLength(); i++) {
                value.append(texts.item(i).getTextContent());
            }
            return value.toString();
        }
        String raw = textOf(cell, "v");
        if (raw == null) {
            return "";
        }
        if ("s".equals(type)) {
            try {
                int index = Integer.parseInt(raw.trim());
                return index >= 0 && index < shared.size() ? shared.get(index) : "";
            } catch (NumberFormatException e) {
                return "";
            }
        }
        if ("str".equals(type) || "b".equals(type) || "e".equals(type)) {
            return raw;
        }
        if ("d".equals(type)) {
            return raw.replace('T', ' ');
        }
        int style = 0;
        try {
            style = Integer.parseInt(cell.getAttribute("s"));
        } catch (NumberFormatException ignored) {
            // default style
        }
        if (style >= 0 && style < dateStyles.length && dateStyles[style]) {
            try {
                LocalDateTime date = fromExcelSerial(new BigDecimal(raw.trim()));
                return date.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                        ? date.format(DATE) : date.format(DATE_TIME);
            } catch (Exception ignored) {
                return raw;
            }
        }
        try {
            return new BigDecimal(raw.trim()).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private LocalDateTime fromExcelSerial(BigDecimal value) {
        long days = value.longValue();
        BigDecimal fraction = value.subtract(new BigDecimal(days));
        long seconds = fraction.multiply(new BigDecimal(86400)).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
        LocalDateTime base = LocalDateTime.of(1899, 12, 30, 0, 0);
        return base.plusDays(days).plusSeconds(seconds);
    }

    private String textOf(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagNameNS("*", tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private int columnIndex(String ref) {
        if (ref == null || ref.isEmpty()) {
            return -1;
        }
        int value = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = Character.toUpperCase(ref.charAt(i));
            if (ch < 'A' || ch > 'Z') {
                break;
            }
            value = value * 26 + (ch - 'A' + 1);
        }
        return value - 1;
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }
}
