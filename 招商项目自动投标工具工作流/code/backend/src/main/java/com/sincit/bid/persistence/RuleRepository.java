package com.sincit.bid.persistence;

import com.sincit.bid.domain.QualificationRule;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RuleRepository {

    private final JsonDataStore store;

    public RuleRepository(JsonDataStore store) {
        this.store = store;
    }

    public synchronized List<QualificationRule> findAll() {
        return new ArrayList<QualificationRule>(store.database().getRules());
    }

    public synchronized Optional<QualificationRule> findByCode(String code) {
        return store.database().getRules().stream()
                .filter(r -> r.getCode().equals(code))
                .findFirst();
    }

    public synchronized QualificationRule save(QualificationRule rule) {
        List<QualificationRule> rules = store.database().getRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getCode().equals(rule.getCode())) {
                rules.set(i, rule);
                store.save();
                return rule;
            }
        }
        rules.add(rule);
        store.save();
        return rule;
    }

    public synchronized void saveAll(List<QualificationRule> rules) {
        store.database().getRules().clear();
        store.database().getRules().addAll(rules);
        store.save();
    }

    public synchronized boolean isEmpty() {
        return store.database().getRules().isEmpty();
    }
}
