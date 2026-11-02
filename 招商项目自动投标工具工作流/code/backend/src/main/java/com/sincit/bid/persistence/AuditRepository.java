package com.sincit.bid.persistence;

import com.sincit.bid.domain.AuditLog;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditRepository {

    private final JsonDataStore store;

    public AuditRepository(JsonDataStore store) {
        this.store = store;
    }

    public synchronized void append(AuditLog log) {
        store.database().getAudits().add(log);
        store.save();
    }

    public synchronized List<AuditLog> findAll() {
        return new ArrayList<AuditLog>(store.database().getAudits());
    }

    public synchronized List<AuditLog> findByProject(String projectId) {
        List<AuditLog> result = new ArrayList<AuditLog>();
        for (AuditLog log : store.database().getAudits()) {
            if (projectId.equals(log.getProjectId())) {
                result.add(log);
            }
        }
        return result;
    }
}
