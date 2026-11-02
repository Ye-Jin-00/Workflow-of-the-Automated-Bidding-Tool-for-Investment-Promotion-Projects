package com.sincit.bid.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sincit.bid.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * JSON 文件持久化：内存读写 + 原子落盘。
 * 单机演示足够用，且便于直接查看/备份数据；多实例部署时应替换为数据库。
 */
@Component
public class JsonDataStore {

    private static final Logger log = LoggerFactory.getLogger(JsonDataStore.class);

    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private Database database = new Database();
    private Path file;

    public JsonDataStore(ObjectMapper objectMapper, AppProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    public synchronized void init() {
        try {
            Path dir = Paths.get(properties.getDataDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            this.file = dir.resolve("db.json");
            if (Files.exists(file) && Files.size(file) > 0) {
                this.database = objectMapper.readValue(file.toFile(), Database.class);
                log.info("data loaded from {}, projects={}", file, database.getProjects().size());
            } else {
                log.info("no data file found, waiting for demo data initializer: {}", file);
            }
        } catch (IOException e) {
            throw new IllegalStateException("初始化数据存储失败: " + e.getMessage(), e);
        }
    }

    public synchronized Database database() {
        return database;
    }

    public synchronized void save() {
        if (file == null) {
            return;
        }
        try {
            Path tmp = file.resolveSibling("db.json.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), database);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicError) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("保存数据失败: " + e.getMessage(), e);
        }
    }

    public Path getFile() {
        return file;
    }
}
