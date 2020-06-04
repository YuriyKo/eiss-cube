package eiss.config;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

public enum Config {

    INSTANCE;

    private final static String configDir = "config";
    private final static String keyFileName = "serverkey.txt";
    private final static String authFileName = "auth.json";

    @Getter
    private String key;
    @Getter
    private Long accessExpiration;
    @Getter
    private Long refreshExpiration;
    @Getter
    private Integer minimumResetDays;
    @Getter
    private Integer expirePasswordDays;
    @Getter
    private Integer inactivityLogoutMinutes;

    Config() {
        Path keyFile = Paths.get(HomeDir.getFolderPath(configDir).toString(), keyFileName);
        try {
            byte[] data = Files.readAllBytes(keyFile);
            key = new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            key = "changeme";
        }

        Path authFile = Paths.get(HomeDir.getFolderPath(configDir).toString(), authFileName);
        try {
            StringBuilder lines = new StringBuilder();
            Files.readAllLines(authFile, UTF_8).forEach(lines::append);

            JsonObject authCfg = new JsonObject(lines.toString());
            accessExpiration = authCfg.getLong("accessExpiration", 600L);
            refreshExpiration = authCfg.getLong("refreshExpiration", 86400L);
            minimumResetDays = authCfg.getInteger("minimumResetDays", 1);
            expirePasswordDays = authCfg.getInteger("expirePasswordDays", 90);
            inactivityLogoutMinutes = authCfg.getInteger("inactivityLogoutMinutes", 20);
        } catch (IOException e) {
            accessExpiration = 600L; // 10 min
            refreshExpiration = 86400L; // 1 day
            minimumResetDays = 1;
            expirePasswordDays = 90;
            inactivityLogoutMinutes = 20;
        }
    }

}
