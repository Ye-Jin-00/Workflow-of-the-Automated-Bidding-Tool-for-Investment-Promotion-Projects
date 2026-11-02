package com.sincit.bid.service;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.common.DateUtils;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.ExtractionResult;
import com.sincit.bid.web.dto.ImportDtos;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 招标文件导入：支持 TXT / MD / CSV / DOCX 文本解析，并用规则引擎识别项目名称、
 * 招标人、预算、保证金、截止时间与要素清单。识别结果只做预填，最终以人工核对为准。
 */
@Service
public class ImportService {

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?:项目名称|项目名|采购项目名称)\\s*[:：]\\s*([^\\n\\r]{2,100})");
    private static final Pattern CLIENT_PATTERN = Pattern.compile(
            "(?:招标人|采购人|招标单位|采购单位|招标方)\\s*[:：]\\s*([^\\n\\r]{2,100})");
    private static final long MAX_SIZE = 20 * 1024 * 1024L;

    private final ExtractionService extractionService;
    private final TableFileReader tableFileReader;

    public ImportService(ExtractionService extractionService, TableFileReader tableFileReader) {
        this.extractionService = extractionService;
        this.tableFileReader = tableFileReader;
    }

    public ImportDtos.ImportResult parseText(String text, String filename) {
        String content = text == null ? "" : text.trim();
        if (content.length() < 20) {
            throw BusinessException.invalid("招标正文太短，请至少提供 20 个字");
        }
        if (content.length() > 100000) {
            content = content.substring(0, 100000);
        }
        BidProject temp = new BidProject();
        temp.setName("导入识别");
        ExtractionResult extraction = extractionService.extract(temp, content);
        return buildResult(content, extraction, filename);
    }

    public ImportDtos.ImportResult parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.invalid("请选择要导入的招标文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BusinessException.invalid("文件不能超过 20MB");
        }
        String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        String text;
        try {
            byte[] bytes = file.getBytes();
            if ("xlsx".equals(ext)) {
                return parseProjectTemplate(bytes, original);
            } else if ("docx".equals(ext)) {
                text = extractDocx(bytes);
            } else if ("txt".equals(ext) || "md".equals(ext) || "markdown".equals(ext) || "csv".equals(ext)) {
                text = new String(bytes, StandardCharsets.UTF_8);
            } else {
                throw BusinessException.invalid("支持 TXT、MD、CSV、DOCX、XLSX 文件；PDF 请复制正文后粘贴识别");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("读取文件失败", e);
        }
        if (text.startsWith("\ufeff")) {
            text = text.substring(1);
        }
        return parseText(text, original);
    }

    /**
     * 识别批量导入模板中的第一行有效项目，用于新建页“智能导入”直接回填。
     */
    private ImportDtos.ImportResult parseProjectTemplate(byte[] bytes, String filename) {
        List<List<String>> rows = tableFileReader.read(bytes, filename);
        if (rows.size() < 2) {
            throw BusinessException.invalid("Excel 模板中没有可识别的项目数据");
        }
        Map<String, Integer> columns = mapTemplateHeader(rows.get(0));
        if (!columns.containsKey("name")) {
            throw BusinessException.invalid("Excel 表头未识别，请使用“项目批量导入模板”");
        }
        int rowNumber = -1;
        List<String> row = null;
        for (int i = 1; i < rows.size(); i++) {
            String name = cell(rows.get(i), columns.get("name"));
            if (!name.isEmpty() && !name.startsWith("示例")) {
                row = rows.get(i);
                rowNumber = i + 1;
                break;
            }
        }
        if (row == null) {
            throw BusinessException.invalid("Excel 中没有有效数据行（示例行不会被识别）");
        }
        String projectName = cell(row, columns.get("name"));
        String clientName = cell(row, columns.get("client"));
        String industry = cell(row, columns.get("industry"));
        String source = cell(row, columns.get("source"));
        String tenderText = cell(row, columns.get("tender"));
        BigDecimal budget = parseAmount(cell(row, columns.get("budget")));
        BigDecimal deposit = parseAmount(cell(row, columns.get("deposit")));
        LocalDateTime deadline = DateUtils.parseDateTime(cell(row, columns.get("deadline")));

        ExtractionResult extraction = new ExtractionResult();
        if (tenderText.length() >= 20) {
            BidProject temp = new BidProject();
            temp.setName(projectName);
            extraction = extractionService.extract(temp, tenderText);
        }
        if (budget == null) {
            budget = extraction.getBudgetAmount();
        }
        if (deposit == null) {
            deposit = extraction.getDepositAmount();
        }
        if (deadline == null) {
            deadline = extraction.getDeadline();
        }
        if (source.isEmpty()) {
            source = "招标文件导入";
        }
        List<String> warnings = new ArrayList<String>(extraction.getWarnings());
        warnings.add("已从 Excel 模板第 " + rowNumber + " 行读取，请核对项目名称、预算、截止时间与资格条件");
        String summary = "从 Excel 模板第 " + rowNumber + " 行识别，预算 "
                + (budget == null ? "未填写" : budget.stripTrailingZeros().toPlainString() + " 万元")
                + "，截止 " + (deadline == null ? "未填写" : deadline.toString().replace('T', ' '));

        return ImportDtos.ImportResult.builder()
                .projectName(projectName)
                .clientName(clientName)
                .industry(industry)
                .source(source)
                .budgetAmount(budget)
                .depositAmount(deposit)
                .deadline(deadline)
                .tenderText(tenderText)
                .summary(summary)
                .charCount(tenderText.length())
                .requirements(extraction.getItems())
                .warnings(warnings)
                .filename(filename)
                .build();
    }

    private Map<String, Integer> mapTemplateHeader(List<String> header) {
        Map<String, Integer> columns = new HashMap<String, Integer>();
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i) == null ? "" : header.get(i)
                    .replaceAll("[\\s\\*＊]", "")
                    .replace("（必填）", "").replace("(必填)", "")
                    .replace("（管理员可选）", "").replace("(管理员可选)", "");
            if (h.contains("项目名称")) {
                columns.put("name", i);
            } else if (h.contains("招标人") || h.contains("委托方")) {
                columns.put("client", i);
            } else if (h.contains("所属产业")) {
                columns.put("industry", i);
            } else if (h.contains("线索来源")) {
                columns.put("source", i);
            } else if (h.contains("预算")) {
                columns.put("budget", i);
            } else if (h.contains("保证金")) {
                columns.put("deposit", i);
            } else if (h.contains("截止")) {
                columns.put("deadline", i);
            } else if (h.contains("招标正文")) {
                columns.put("tender", i);
            }
        }
        return columns;
    }

    private String cell(List<String> row, Integer index) {
        if (index == null || index < 0 || index >= row.size() || row.get(index) == null) {
            return "";
        }
        return row.get(index).trim();
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim().replaceAll("[,\\s￥¥]", "").replace("人民币", "")
                .replace("亿元", "").replace("亿", "")
                .replace("万元", "").replace("万", "").replace("元", "");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ImportDtos.ImportResult buildResult(String content, ExtractionResult extraction, String filename) {
        String projectName = match(NAME_PATTERN, content);
        String clientName = match(CLIENT_PATTERN, content);
        if (projectName == null) {
            projectName = guessName(content);
        }
        if (clientName == null) {
            clientName = "以招标文件为准";
        }
        List<String> warnings = new ArrayList<String>(extraction.getWarnings());
        warnings.add("自动识别结果仅供参考，保存前请核对项目名称、预算、截止时间与资格条件");

        String industry = guessIndustry(content);
        String source = content.contains("公共资源交易") || content.contains("政府采购")
                ? "公共资源交易平台" : "招标文件导入";
        String summary = String.format("识别 %d 项要素，预算 %s，截止 %s",
                extraction.getItems().size(),
                extraction.getBudgetAmount() == null ? "未识别"
                        : extraction.getBudgetAmount().stripTrailingZeros().toPlainString() + " 万元",
                extraction.getDeadline() == null ? "未识别"
                        : extraction.getDeadline().toString().replace('T', ' '));

        return ImportDtos.ImportResult.builder()
                .projectName(projectName)
                .clientName(clientName)
                .industry(industry)
                .source(source)
                .budgetAmount(extraction.getBudgetAmount())
                .depositAmount(extraction.getDepositAmount())
                .deadline(extraction.getDeadline())
                .tenderText(content)
                .summary(summary)
                .charCount(content.length())
                .requirements(extraction.getItems())
                .warnings(warnings)
                .filename(filename)
                .build();
    }

    private String match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return clean(matcher.group(1));
        }
        return null;
    }

    private String guessName(String text) {
        for (String line : text.split("\\n")) {
            String value = line.trim();
            if (value.length() >= 4 && value.length() <= 100
                    && (value.contains("项目") || value.contains("服务") || value.contains("采购"))) {
                return clean(value);
            }
        }
        return "导入项目（待完善）";
    }

    private String guessIndustry(String text) {
        String[][] mapping = {
                {"数字经济", "数字经济"}, {"大数据", "数字经济"}, {"人工智能", "数字经济"},
                {"文旅", "文旅商业"}, {"旅游", "文旅商业"}, {"商业综合体", "文旅商业"},
                {"总部经济", "商务服务/总部经济"}, {"商务服务", "商务服务/总部经济"},
                {"科技", "科技服务"}, {"金融", "现代金融"}, {"文化创意", "文化创意"},
                {"制造", "先进制造"}, {"生物医药", "生物医药"}
        };
        for (String[] item : mapping) {
            if (text.contains(item[0])) {
                return item[1];
            }
        }
        return "";
    }

    private String clean(String value) {
        String result = value.trim()
                .replaceAll("[。；;，,]+$", "")
                .replaceAll("\\s{2,}", " ");
        return result.length() > 100 ? result.substring(0, 100) : result;
    }

    /**
     * 轻量 DOCX 文本提取：读取 word/document.xml，将段落、换行与制表符还原为文本。
     * 不依赖 Apache POI，适合演示与常见公文；复杂表格/文本框可能丢失格式。
     */
    private String extractDocx(byte[] bytes) throws IOException {
        byte[] documentXml = null;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    documentXml = readAll(zip);
                    break;
                }
            }
        }
        if (documentXml == null) {
            throw BusinessException.invalid("DOCX 文件结构异常，未找到正文内容");
        }
        String xml = new String(documentXml, StandardCharsets.UTF_8);
        xml = xml.replaceAll("</w:p>", "\n")
                .replaceAll("<w:tab[^>]*/>", "\t")
                .replaceAll("<w:br[^>]*/>", "\n")
                .replaceAll("<[^>]+>", "");
        xml = xml.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'");
        return xml.replaceAll("\\n{3,}", "\n\n").trim();
    }

    private byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
