package com.sincit.bid.service;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.ExtractionResult;
import com.sincit.bid.domain.RequirementItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 招标文件要素抽取（规则版）。
 * 优先用正则识别预算、保证金、截止时间等结构化字段，再按关键词抽取资格、业绩、评分、商务条款；
 * 生产环境可在同一接口后挂接 PDF 解析 / OCR / 大模型抽取，返回结构保持不变。
 */
@Service
public class ExtractionService {

    private static final Pattern BUDGET_PATTERN = Pattern.compile(
            "(?:预算金额|采购预算|项目预算|最高限价|招标控制价|预算)[^0-9]{0,20}([0-9]+(?:\\.[0-9]+)?)\\s*(万元|万|元)");
    private static final Pattern DEPOSIT_PATTERN = Pattern.compile(
            "(?:投标保证金|保证金)[^0-9]{0,20}([0-9]+(?:\\.[0-9]+)?)\\s*(万元|万|元)");
    private static final Pattern DEADLINE_PATTERN = Pattern.compile(
            "(?:投标截止时间|递交投标文件截止时间|投标文件递交截止时间|响应文件提交截止时间|开标时间|截止时间)"
                    + "[^0-9]{0,16}(\\d{4})[-年/.](\\d{1,2})[-月/.](\\d{1,2})日?"
                    + "(?:\\s*(\\d{1,2})[:：时](\\d{1,2})?分?)?");

    private static final String[] QUALIFICATION_KEYS = {"资质", "资格", "备案", "许可证", "认证", "信用"};
    private static final String[] PERFORMANCE_KEYS = {"业绩", "案例", "类似项目", "同类项目"};
    private static final String[] SCORING_KEYS = {"评分", "评标", "分值", "得分", "评审"};
    private static final String[] COMMERCIAL_KEYS = {"保证金", "付款", "报价", "合同", "商务", "发票", "履约"};
    private static final String[] TIME_KEYS = {"服务期", "工期", "交付", "进度", "截止", "时间"};
    private static final String[] TECH_KEYS = {"服务内容", "招商", "运营", "方案", "团队", "人员", "要求", "载体", "产业"};
    private static final String[] MANDATORY_KEYS = {"必须", "须", "应当", "不得", "禁止", "一票否决", "否决"};

    public ExtractionResult extract(BidProject project, String tenderText) {
        if (tenderText == null || tenderText.trim().length() < 20) {
            throw BusinessException.invalid("招标文件正文太短，请粘贴至少 20 个字的招标要求");
        }
        String text = tenderText.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<RequirementItem> items = new ArrayList<RequirementItem>();
        Set<String> fingerprints = new LinkedHashSet<String>();
        List<String> warnings = new ArrayList<String>();
        int seq = 1;

        BigDecimal budget = parseAmount(BUDGET_PATTERN.matcher(text));
        if (budget != null) {
            seq = add(items, fingerprints, seq, "报价要求", "项目预算/最高限价",
                    "本项目预算/最高限价为 " + budget.stripTrailingZeros().toPlainString() + " 万元，报价不得超过该上限。",
                    true, 0.95, matchSource(text, BUDGET_PATTERN));
        } else {
            warnings.add("未识别到预算/最高限价，请人工补充");
        }

        BigDecimal deposit = parseAmount(DEPOSIT_PATTERN.matcher(text));
        if (deposit != null) {
            seq = add(items, fingerprints, seq, "商务条款", "投标保证金",
                    "投标保证金金额为 " + deposit.stripTrailingZeros().toPlainString() + " 万元，需在截止时间前足额缴纳。",
                    true, 0.92, matchSource(text, DEPOSIT_PATTERN));
        }

        LocalDateTime deadline = parseDeadline(text);
        if (deadline != null) {
            seq = add(items, fingerprints, seq, "时间节点", "投标截止时间",
                    "投标文件须于 " + deadline.toString().replace('T', ' ') + " 前递交，逾期不予受理。",
                    true, 0.93, matchSource(text, DEADLINE_PATTERN));
        } else {
            warnings.add("未识别到投标截止时间，请人工确认");
        }

        seq = extractByKeywords(text, items, fingerprints, seq, "资格条件", QUALIFICATION_KEYS, 6);
        seq = extractByKeywords(text, items, fingerprints, seq, "资格条件", PERFORMANCE_KEYS, 3);
        seq = extractByKeywords(text, items, fingerprints, seq, "评标办法", SCORING_KEYS, 3);
        seq = extractByKeywords(text, items, fingerprints, seq, "商务条款", COMMERCIAL_KEYS, 4);
        seq = extractByKeywords(text, items, fingerprints, seq, "技术需求", TECH_KEYS, 5);
        seq = extractByKeywords(text, items, fingerprints, seq, "时间节点", TIME_KEYS, 2);
        extractByKeywords(text, items, fingerprints, seq, "其他", new String[]{"联合体", "分包", "保密", "知识产权"}, 2);

        if (items.size() < 6) {
            warnings.add("结构化要素偏少，已启用句子级补充抽取，建议人工复核");
            seq = sentenceFallback(text, items, fingerprints, seq);
        }
        if (items.stream().noneMatch(i -> "资格条件".equals(i.getCategory()))) {
            warnings.add("未识别到资格条件，请务必人工核对招标公告");
        }

        long mandatoryCount = items.stream().filter(RequirementItem::isMandatory).count();
        String summary = String.format("共抽取 %d 项要素，其中硬性要求 %d 项；%s%s",
                items.size(), mandatoryCount,
                budget == null ? "预算未识别；" : "预算已识别；",
                deadline == null ? "截止时间未识别。" : "截止时间已识别。");

        return ExtractionResult.builder()
                .items(items)
                .extractedAt(LocalDateTime.now())
                .source("RULE")
                .charCount(text.length())
                .summary(summary)
                .warnings(warnings)
                .budgetAmount(budget)
                .depositAmount(deposit)
                .deadline(deadline)
                .build();
    }

    private int extractByKeywords(String text, List<RequirementItem> items, Set<String> fingerprints,
                                  int seq, String category, String[] keywords, int limit) {
        int added = 0;
        String[] lines = text.split("\\n");
        for (String rawLine : lines) {
            if (added >= limit) {
                break;
            }
            String line = rawLine.trim().replaceAll("\\s+", " ");
            if (line.length() < 8 || line.length() > 260) {
                continue;
            }
            if (!containsAny(line, keywords)) {
                continue;
            }
            if (line.matches("^[一二三四五六七八九十0-9、.．（）()\\s]+$")) {
                continue;
            }
            boolean mandatory = containsAny(line, MANDATORY_KEYS);
            String title = category + "要点 " + (added + 1);
            int before = items.size();
            seq = add(items, fingerprints, seq, category, title, line, mandatory, 0.72, truncate(line, 90));
            if (items.size() > before) {
                added++;
            }
        }
        return seq;
    }

    private int sentenceFallback(String text, List<RequirementItem> items, Set<String> fingerprints, int seq) {
        String[] sentences = text.split("[。；;]");
        int added = 0;
        for (String sentence : sentences) {
            if (added >= 6 || items.size() >= 18) {
                break;
            }
            String value = sentence.trim().replaceAll("\\s+", " ");
            if (value.length() < 12 || value.length() > 260) {
                continue;
            }
            if (!containsAny(value, TECH_KEYS) && !containsAny(value, QUALIFICATION_KEYS)) {
                continue;
            }
            String category = containsAny(value, QUALIFICATION_KEYS) ? "资格条件" : "技术需求";
            int before = items.size();
            seq = add(items, fingerprints, seq, category, "补充要求 " + (items.size() + 1),
                    value, containsAny(value, MANDATORY_KEYS), 0.6, truncate(value, 90));
            if (items.size() > before) {
                added++;
            }
        }
        return seq;
    }

    private int add(List<RequirementItem> items, Set<String> fingerprints, int seq, String category, String title,
                    String content, boolean mandatory, double confidence, String source) {
        String fingerprint = category + "|" + content.replaceAll("\\s+", "");
        if (!fingerprints.add(fingerprint)) {
            return seq;
        }
        items.add(RequirementItem.builder()
                .id("RQ" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .category(category)
                .title(title)
                .content(content)
                .mandatory(mandatory)
                .source(source)
                .confidence(confidence)
                .confirmed(false)
                .build());
        return seq + 1;
    }

    private BigDecimal parseAmount(Matcher matcher) {
        if (matcher.find()) {
            BigDecimal value = new BigDecimal(matcher.group(1));
            String unit = matcher.group(2);
            if ("元".equals(unit)) {
                value = value.divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP);
            }
            return value;
        }
        return null;
    }

    private LocalDateTime parseDeadline(String text) {
        Matcher matcher = DEADLINE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));
        int hour = matcher.group(4) == null ? 18 : Integer.parseInt(matcher.group(4));
        int minute = matcher.group(5) == null ? 0 : Integer.parseInt(matcher.group(5));
        try {
            return LocalDateTime.of(year, month, day, hour, minute);
        } catch (Exception e) {
            return null;
        }
    }

    private String matchSource(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int start = Math.max(0, matcher.start() - 10);
            int end = Math.min(text.length(), matcher.end() + 10);
            return truncate(text.substring(start, end).replaceAll("\\s+", " "), 100);
        }
        return "招标文件";
    }

    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
