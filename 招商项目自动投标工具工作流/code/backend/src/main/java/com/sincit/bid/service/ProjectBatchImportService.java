package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.common.DateUtils;
import com.sincit.bid.domain.User;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.web.dto.BatchDtos;
import com.sincit.bid.web.dto.ProjectDtos;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 项目批量导入：读取模板 XLSX/CSV，逐行校验后创建草稿，返回成功与失败明细。
 */
@Service
public class ProjectBatchImportService {

    private static final int MAX_ROWS = 200;
    private static final int MAX_TEXT = 50000;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TableFileReader tableFileReader;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    public ProjectBatchImportService(TableFileReader tableFileReader, ProjectService projectService,
                                     UserRepository userRepository) {
        this.tableFileReader = tableFileReader;
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    public BatchDtos.BatchImportResult importFile(MultipartFile file, CurrentUser actor) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.invalid("请选择要导入的 Excel 或 CSV 文件");
        }
        if (file.getSize() > 20 * 1024 * 1024L) {
            throw BusinessException.invalid("文件不能超过 20MB");
        }
        String filename = file.getOriginalFilename() == null ? "导入文件" : file.getOriginalFilename();
        List<List<String>> rows;
        try {
            rows = tableFileReader.read(file.getBytes(), filename);
        } catch (IOException e) {
            throw new IllegalStateException("读取导入文件失败", e);
        }
        if (rows.size() < 2) {
            throw BusinessException.invalid("模板中没有可导入的数据，请从第 2 行开始填写");
        }
        Map<String, Integer> columns = mapHeader(rows.get(0));
        requireColumn(columns, "name", "项目名称");
        requireColumn(columns, "client", "招标人/委托方");
        requireColumn(columns, "budget", "预算");
        requireColumn(columns, "deadline", "投标截止时间");

        List<RowData> dataRows = new ArrayList<RowData>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            String name = cell(row, columns.get("name"));
            if (name.isEmpty() || name.startsWith("示例")) {
                continue;
            }
            dataRows.add(new RowData(i + 1, row));
        }
        if (dataRows.isEmpty()) {
            throw BusinessException.invalid("没有找到有效数据行（示例行不会被导入）");
        }
        if (dataRows.size() > MAX_ROWS) {
            throw BusinessException.invalid("单次最多导入 " + MAX_ROWS + " 行，当前 " + dataRows.size() + " 行");
        }

        List<BatchDtos.ImportSuccess> successes = new ArrayList<BatchDtos.ImportSuccess>();
        List<BatchDtos.ImportFailure> failures = new ArrayList<BatchDtos.ImportFailure>();
        Set<String> names = new HashSet<String>();
        for (RowData data : dataRows) {
            try {
                ProjectDtos.SaveRequest request = toRequest(data, columns, actor, names);
                ProjectDtos.Detail detail = projectService.create(request, actor);
                successes.add(BatchDtos.ImportSuccess.builder()
                        .row(data.rowNumber)
                        .id(detail.getId())
                        .code(detail.getCode())
                        .name(detail.getName())
                        .build());
            } catch (BusinessException e) {
                failures.add(BatchDtos.ImportFailure.builder()
                        .row(data.rowNumber)
                        .message(e.getMessage())
                        .build());
            } catch (Exception e) {
                failures.add(BatchDtos.ImportFailure.builder()
                        .row(data.rowNumber)
                        .message("导入失败：" + (e.getMessage() == null ? "系统异常" : e.getMessage()))
                        .build());
            }
        }
        return BatchDtos.BatchImportResult.builder()
                .total(dataRows.size())
                .success(successes.size())
                .failed(failures.size())
                .items(successes)
                .errors(failures)
                .build();
    }

    private ProjectDtos.SaveRequest toRequest(RowData data, Map<String, Integer> columns,
                                              CurrentUser actor, Set<String> names) {
        String name = cell(data.values, columns.get("name"));
        String client = cell(data.values, columns.get("client"));
        String budgetText = cell(data.values, columns.get("budget"));
        String deadlineText = cell(data.values, columns.get("deadline"));

        if (name.isEmpty()) {
            throw BusinessException.invalid("项目名称不能为空");
        }
        if (name.length() > 120) {
            throw BusinessException.invalid("项目名称不能超过 120 字");
        }
        if (!names.add(name)) {
            throw BusinessException.invalid("项目名称在文件内重复：" + name);
        }
        if (client.isEmpty()) {
            throw BusinessException.invalid("招标人/委托方不能为空");
        }
        BigDecimal budget = parseAmount(budgetText);
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.invalid("预算必须是大于 0 的数字（单位：万元）");
        }
        LocalDateTime deadline = DateUtils.parseDateTime(deadlineText);
        if (deadline == null) {
            throw BusinessException.invalid("投标截止时间格式不正确，请使用 2026-12-20 17:00");
        }
        if (deadline.isBefore(LocalDateTime.now())) {
            throw BusinessException.invalid("投标截止时间不能早于当前时间");
        }
        BigDecimal deposit = parseAmount(cell(data.values, columns.get("deposit")));
        String ownerId = null;
        String department = cell(data.values, columns.get("department"));

        String ownerAccount = cell(data.values, columns.get("owner"));
        if ((actor.isAdmin() || actor.getRole() == UserRole.BID_MANAGER) && !ownerAccount.isEmpty()) {
            User owner = userRepository.findByUsername(ownerAccount)
                    .orElseThrow(() -> BusinessException.invalid("项目负责人账号不存在：" + ownerAccount));
            projectService.assertOwnerAssignable(owner, actor);
            ownerId = owner.getId();
            if (department.isEmpty()) {
                department = owner.getDepartment();
            }
        }
        if (department.isEmpty()) {
            department = actor.getDepartment();
        }
        String tenderText = cell(data.values, columns.get("tender"));
        if (tenderText.length() > MAX_TEXT) {
            throw BusinessException.invalid("招标正文不能超过 " + MAX_TEXT + " 字");
        }

        ProjectDtos.SaveRequest request = new ProjectDtos.SaveRequest();
        request.setName(name);
        request.setClientName(client);
        request.setCarrierName(cell(data.values, columns.get("carrier")));
        request.setIndustry(cell(data.values, columns.get("industry")));
        request.setSource(cell(data.values, columns.get("source")));
        request.setDepartment(department);
        request.setOwnerId(ownerId);
        request.setDeadline(deadline.format(DATE_TIME));
        request.setBudgetAmount(budget);
        request.setDepositAmount(deposit);
        request.setProjectBrief(cell(data.values, columns.get("brief")));
        request.setTenderText(tenderText);
        return request;
    }

    private Map<String, Integer> mapHeader(List<String> header) {
        Map<String, Integer> columns = new HashMap<String, Integer>();
        for (int i = 0; i < header.size(); i++) {
            String h = normalize(header.get(i));
            if (h.contains("项目名称") || h.equals("项目名")) {
                columns.put("name", i);
            } else if (h.contains("招标人") || h.contains("委托方") || h.contains("采购人")) {
                columns.put("client", i);
            } else if (h.contains("项目载体") || h.contains("载体")) {
                columns.put("carrier", i);
            } else if (h.contains("所属产业") || h.contains("产业")) {
                columns.put("industry", i);
            } else if (h.contains("线索来源") || h.contains("来源")) {
                columns.put("source", i);
            } else if (h.contains("归属部门") || h.contains("部门")) {
                columns.put("department", i);
            } else if (h.contains("预算") || h.contains("最高限价")) {
                columns.put("budget", i);
            } else if (h.contains("保证金")) {
                columns.put("deposit", i);
            } else if (h.contains("截止")) {
                columns.put("deadline", i);
            } else if (h.contains("项目简介") || h.contains("简介")) {
                columns.put("brief", i);
            } else if (h.contains("招标正文") || h.contains("正文")) {
                columns.put("tender", i);
            } else if (h.contains("负责人")) {
                columns.put("owner", i);
            }
        }
        return columns;
    }

    private void requireColumn(Map<String, Integer> columns, String key, String label) {
        if (!columns.containsKey(key)) {
            throw BusinessException.invalid("模板缺少“" + label + "”列，请下载最新模板");
        }
    }

    private String normalize(String header) {
        if (header == null) {
            return "";
        }
        return header.replaceAll("[\\s\\*＊]", "")
                .replace("（必填）", "")
                .replace("(必填)", "")
                .replace("（可选）", "")
                .replace("(可选)", "")
                .trim();
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
        String value = raw.trim().replaceAll("[,\\s￥¥]", "").replace("人民币", "");
        boolean billion = value.contains("亿");
        boolean tenThousand = value.contains("万");
        value = value.replace("亿元", "").replace("亿", "")
                .replace("万元", "").replace("万", "").replace("元", "");
        try {
            BigDecimal amount = new BigDecimal(value);
            if (billion) {
                amount = amount.multiply(new BigDecimal("10000"));
            }
            return amount;
        } catch (NumberFormatException e) {
            throw BusinessException.invalid("预算/保证金格式不正确：" + raw);
        }
    }

    private static class RowData {
        private final int rowNumber;
        private final List<String> values;

        private RowData(int rowNumber, List<String> values) {
            this.rowNumber = rowNumber;
            this.values = values;
        }
    }
}
