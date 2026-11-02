package com.sincit.bid.service;

import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.PermissionService;
import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.AuditAction;
import com.sincit.bid.domain.QualificationRule;
import com.sincit.bid.domain.RuleDefaults;
import com.sincit.bid.persistence.RuleRepository;
import com.sincit.bid.web.dto.RuleDtos;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资格规则配置。规则调整属于高风险操作，仅管理员可执行并强制审计。
 */
@Service
public class RuleService {

    private final RuleRepository ruleRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public RuleService(RuleRepository ruleRepository, PermissionService permissionService, AuditService auditService) {
        this.ruleRepository = ruleRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    public List<RuleDtos.RuleView> list() {
        return ruleRepository.findAll().stream()
                .map(RuleDtos.RuleView::from)
                .collect(Collectors.toList());
    }

    public RuleDtos.RuleView update(String code, RuleDtos.UpdateRequest request, CurrentUser actor) {
        permissionService.assertAdmin(actor);
        QualificationRule rule = ruleRepository.findByCode(code)
                .orElseThrow(() -> BusinessException.notFound("规则不存在: " + code));
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getMandatory() != null) {
            rule.setMandatory(request.getMandatory());
        }
        if (request.getThreshold() != null) {
            if (request.getThreshold().trim().isEmpty()) {
                throw BusinessException.invalid("阈值不能为空");
            }
            rule.setThreshold(request.getThreshold().trim());
        }
        if (request.getWeight() != null) {
            if (request.getWeight() < 0 || request.getWeight() > 100) {
                throw BusinessException.invalid("权重需在 0-100 之间");
            }
            rule.setWeight(request.getWeight());
        }
        if (request.getSuggestion() != null) {
            rule.setSuggestion(request.getSuggestion().trim());
        }
        if (request.getDescription() != null) {
            rule.setDescription(request.getDescription().trim());
        }
        ruleRepository.save(rule);
        auditService.record(actor, null, AuditAction.RULE_UPDATE,
                "更新规则 " + code + "：" + rule.getName() + "，阈值 " + rule.getThreshold()
                        + "，必填 " + rule.isMandatory() + "，启用 " + rule.isEnabled());
        return RuleDtos.RuleView.from(rule);
    }

    public List<RuleDtos.RuleView> reset(CurrentUser actor) {
        permissionService.assertAdmin(actor);
        ruleRepository.saveAll(RuleDefaults.all());
        auditService.record(actor, null, AuditAction.RULE_UPDATE, "恢复默认资格预审规则");
        return list();
    }
}
