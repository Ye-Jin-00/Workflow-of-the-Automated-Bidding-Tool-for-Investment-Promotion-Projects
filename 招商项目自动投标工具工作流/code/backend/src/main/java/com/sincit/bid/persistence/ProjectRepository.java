package com.sincit.bid.persistence;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.BidProject;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 投标项目仓储。接口语义与数据库仓储一致，便于后续切换实现。
 */
@Repository
public class ProjectRepository {

    private final JsonDataStore store;

    public ProjectRepository(JsonDataStore store) {
        this.store = store;
    }

    public synchronized List<BidProject> findAll() {
        return new ArrayList<BidProject>(store.database().getProjects());
    }

    public synchronized Optional<BidProject> findById(String id) {
        return store.database().getProjects().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    public synchronized Optional<BidProject> findByCode(String code) {
        return store.database().getProjects().stream()
                .filter(p -> p.getCode().equals(code))
                .findFirst();
    }

    public synchronized BidProject get(String id) {
        BidProject project = findById(id)
                .orElseThrow(() -> BusinessException.notFound("投标项目不存在: " + id));
        if (project.isDeleted()) {
            throw BusinessException.notFound("项目已删除，请在回收站中恢复");
        }
        return project;
    }

    public synchronized BidProject save(BidProject project) {
        List<BidProject> projects = store.database().getProjects();
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId().equals(project.getId())) {
                projects.set(i, project);
                store.save();
                return project;
            }
        }
        projects.add(project);
        store.save();
        return project;
    }

    /**
     * 生成形如 BP20261024-001 的业务编号，按天递增。
     */
    public synchronized String nextCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "project-" + date;
        int seq = store.database().getSequences().getOrDefault(key, 0) + 1;
        store.database().getSequences().put(key, seq);
        return String.format("BP%s-%03d", date, seq);
    }

    public synchronized boolean deleteById(String id) {
        boolean removed = store.database().getProjects().removeIf(p -> p.getId().equals(id));
        if (removed) {
            store.save();
        }
        return removed;
    }

    public synchronized long count() {
        return store.database().getProjects().size();
    }
}
