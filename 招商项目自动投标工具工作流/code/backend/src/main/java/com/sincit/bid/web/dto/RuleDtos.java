package com.sincit.bid.web.dto;

import com.sincit.bid.domain.QualificationRule;
import lombok.Data;

/**
 * 规则配置 DTO。
 */
public final class RuleDtos {

    private RuleDtos() {
    }

    @Data
    public static class UpdateRequest {
        private Boolean enabled;
        private Boolean mandatory;
        private String threshold;
        private Integer weight;
        private String suggestion;
        private String description;
    }

    @Data
    public static class RuleView {
        private String code;
        private String name;
        private String category;
        private String field;
        private String fieldLabel;
        private String operator;
        private String operatorSymbol;
        private String threshold;
        private boolean mandatory;
        private boolean enabled;
        private int weight;
        private String suggestion;
        private String description;

        public static RuleView from(QualificationRule rule) {
            RuleView view = new RuleView();
            view.setCode(rule.getCode());
            view.setName(rule.getName());
            view.setCategory(rule.getCategory());
            view.setField(rule.getField().name());
            view.setFieldLabel(rule.getField().getLabel());
            view.setOperator(rule.getOperator().name());
            view.setOperatorSymbol(rule.getOperator().getSymbol());
            view.setThreshold(rule.getThreshold());
            view.setMandatory(rule.isMandatory());
            view.setEnabled(rule.isEnabled());
            view.setWeight(rule.getWeight());
            view.setSuggestion(rule.getSuggestion());
            view.setDescription(rule.getDescription());
            return view;
        }
    }
}
