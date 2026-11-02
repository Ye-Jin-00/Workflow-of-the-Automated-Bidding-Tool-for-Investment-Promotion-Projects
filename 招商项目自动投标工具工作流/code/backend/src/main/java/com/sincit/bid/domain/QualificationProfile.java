package com.sincit.bid.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 投标主体资格画像。既可由表单录入，也可在后续接入工商、信用、资质库后自动补全。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationProfile {

    private String companyName;
    /** 注册资本，单位：万元 */
    private BigDecimal registeredCapital;
    private Integer establishedYear;
    /** 资质/备案名称列表，如房地产经纪机构备案、ISO9001 */
    @Builder.Default
    private List<String> qualifications = new ArrayList<String>();
    /** AAA / AA / A / B / C */
    private String creditLevel;
    private Integer similarProjectCount;
    /** 近三年同类项目合同额，单位：万元 */
    private BigDecimal similarProjectAmount;
    private Integer teamSize;
    private boolean hasGovernmentClient;
    private boolean bidBondAvailable;
    private boolean blacklisted;
    /** 与招标人是否存在关联关系或利益冲突 */
    private boolean conflictsOfInterest;
    /** 盈利 / 持平 / 亏损 */
    private String financialStatus;
    private String notes;

    public static QualificationProfile demoDefault() {
        QualificationProfile profile = new QualificationProfile();
        profile.setCompanyName("南京市行至集商业服务有限公司");
        profile.setRegisteredCapital(new BigDecimal("1000"));
        profile.setEstablishedYear(2019);
        profile.setQualifications(new ArrayList<String>());
        profile.getQualifications().add("房地产经纪机构备案");
        profile.getQualifications().add("招商运营服务能力");
        profile.getQualifications().add("ISO9001质量管理体系认证");
        profile.getQualifications().add("ISO14001环境管理体系认证");
        profile.getQualifications().add("ISO45001职业健康安全管理体系认证");
        profile.setCreditLevel("AAA");
        profile.setSimilarProjectCount(6);
        profile.setSimilarProjectAmount(new BigDecimal("2600"));
        profile.setTeamSize(28);
        profile.setHasGovernmentClient(true);
        profile.setBidBondAvailable(true);
        profile.setBlacklisted(false);
        profile.setConflictsOfInterest(false);
        profile.setFinancialStatus("盈利");
        profile.setNotes("数据为演示口径，正式投标请以营业执照、资质证书及审计报告为准。");
        return profile;
    }
}
