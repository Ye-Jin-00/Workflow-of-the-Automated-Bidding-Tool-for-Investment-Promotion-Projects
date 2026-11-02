package com.sincit.bid.persistence;

import com.sincit.bid.domain.AuditLog;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.Notification;
import com.sincit.bid.domain.QualificationRule;
import com.sincit.bid.domain.User;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 单机演示库。生产环境可平滑替换为 MySQL + JPA/MyBatis，仓储层接口保持不变。
 */
@Data
public class Database {

    private List<User> users = new ArrayList<User>();
    private List<BidProject> projects = new ArrayList<BidProject>();
    private List<QualificationRule> rules = new ArrayList<QualificationRule>();
    private List<AuditLog> audits = new ArrayList<AuditLog>();
    private List<Notification> notifications = new ArrayList<Notification>();
    private Map<String, Integer> sequences = new LinkedHashMap<String, Integer>();
}
