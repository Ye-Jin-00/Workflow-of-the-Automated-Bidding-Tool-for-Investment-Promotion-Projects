package com.sincit.bid.web;

import com.sincit.bid.auth.AuthService;
import com.sincit.bid.auth.CurrentUser;
import com.sincit.bid.auth.UserContext;
import com.sincit.bid.common.ApiResponse;
import com.sincit.bid.config.AppProperties;
import com.sincit.bid.domain.CompanyCapabilities;
import com.sincit.bid.domain.QualificationProfile;
import com.sincit.bid.domain.User;
import com.sincit.bid.domain.UserRole;
import com.sincit.bid.persistence.UserRepository;
import com.sincit.bid.web.dto.AuthDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表单字典与用户列表。避免前端硬编码部门、产业等选项。
 */
@RestController
@RequestMapping("/meta")
public class MetaController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AppProperties properties;

    public MetaController(UserRepository userRepository, AuthService authService, AppProperties properties) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.properties = properties;
    }

    @GetMapping("/options")
    public ApiResponse<Map<String, Object>> options() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("departments", Arrays.asList("招商一部", "招商二部", "产业研究中心", "品牌运营部", "风控合规部", "总经办"));
        data.put("industries", Arrays.asList("商务服务/总部经济", "数字经济", "文旅商业", "科技服务", "现代金融",
                "文化创意", "先进制造", "生物医药"));
        data.put("sources", Arrays.asList("政府推荐", "公共资源交易平台", "客户委托", "渠道推荐", "自主开发", "老客户复购"));
        data.put("methods", Arrays.asList("电子招投标平台", "纸质标书现场递交", "邮箱/邮寄递交", "其他"));
        data.put("attachmentCategories", Arrays.asList("招标文件", "资格材料", "标书文件", "回执凭证", "其他"));
        data.put("roles", Arrays.asList(
                roleOption("ADMIN", "系统管理员", "全部数据、成员与规则管理"),
                roleOption("BID_MANAGER", "投标经理", "创建并推进本人负责的项目"),
                roleOption("REVIEWER", "复核/风控", "复核项目，回避本人负责项目"),
                roleOption("VIEWER", "只读观察员", "仅查看已通过/已提交/已归档项目")));
        data.put("company", companyInfo());
        data.put("aiProvider", properties.getAi().getProvider());
        data.put("aiModel", properties.getAi().getModel());
        return ApiResponse.ok(data);
    }

    @GetMapping("/profile-template")
    public ApiResponse<QualificationProfile> profileTemplate() {
        return ApiResponse.ok(QualificationProfile.demoDefault());
    }

    @GetMapping("/system")
    public ApiResponse<Map<String, Object>> system() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("name", "招商智投");
        data.put("version", "1.3.0");
        data.put("releaseDate", "2026-10");
        data.put("aiProvider", properties.getAi().getProvider());
        data.put("aiModel", properties.getAi().getModel());
        data.put("aiEnabled", "openai-compatible".equalsIgnoreCase(properties.getAi().getProvider())
                && properties.getAi().getApiKey() != null && !properties.getAi().getApiKey().trim().isEmpty());
        data.put("storage", "JSON 文件（演示版）");
        data.put("auth", "HMAC-SHA256 令牌 + 令牌版本");
        data.put("company", companyInfo());
        data.put("features", Arrays.asList(
                "六环节自动投标工作流", "可配置资格规则", "AI 标书初稿与降级",
                "人工复核与职责分离", "全动作审计与追踪号", "个人中心与头像",
                "站内消息与截止提醒", "附件、评论与关注", "成员与权限管理",
                "草稿箱与回收站", "招标文件智能导入", "批量导入与批量操作"));
        return ApiResponse.ok(data);
    }

    @GetMapping("/users")
    public ApiResponse<List<AuthDtos.UserView>> users() {
        CurrentUser actor = UserContext.require();
        List<AuthDtos.UserView> views = new ArrayList<AuthDtos.UserView>();
        for (User user : userRepository.findAll()) {
            if (!user.isEnabled()) {
                continue;
            }
            if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.BID_MANAGER) {
                continue;
            }
            if (actor.isAdmin()
                    || (actor.getRole() == UserRole.BID_MANAGER
                    && java.util.Objects.equals(actor.getDepartment(), user.getDepartment()))) {
                views.add(authService.toView(user));
            }
        }
        return ApiResponse.ok(views);
    }

    private Map<String, Object> roleOption(String code, String label, String description) {
        Map<String, Object> role = new LinkedHashMap<String, Object>();
        role.put("code", code);
        role.put("label", label);
        role.put("description", description);
        return role;
    }

    private Map<String, Object> companyInfo() {
        Map<String, Object> company = new LinkedHashMap<String, Object>();
        company.put("name", CompanyCapabilities.COMPANY_NAME);
        company.put("brand", CompanyCapabilities.BRAND);
        company.put("backdrop", CompanyCapabilities.BACKDROP);
        company.put("credit", CompanyCapabilities.CREDIT);
        company.put("certifications", CompanyCapabilities.CERTIFICATIONS);
        company.put("agencies", CompanyCapabilities.AGENCIES);
        company.put("brokers", CompanyCapabilities.BROKERS);
        company.put("universities", CompanyCapabilities.UNIVERSITIES);
        company.put("associations", CompanyCapabilities.ASSOCIATIONS);
        company.put("address", CompanyCapabilities.ADDRESS);
        company.put("phone", CompanyCapabilities.PHONE);
        company.put("website", CompanyCapabilities.WEBSITE);
        return company;
    }
}
