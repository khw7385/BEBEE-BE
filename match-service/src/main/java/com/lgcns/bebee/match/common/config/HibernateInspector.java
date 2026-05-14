package com.lgcns.bebee.match.common.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.regex.Pattern;

@Slf4j
public class HibernateInspector implements StatementInspector {
    // 만약 조건이 늘어난다면 로직 변경
    private static final String STRAIGHT_JOIN_MARKER = "STRAIGHT_JOIN_HINT";

    @Override
    public String inspect(String sql) {

        if(sql.contains(STRAIGHT_JOIN_MARKER)) {
            return sql
                    .replace("/* " + STRAIGHT_JOIN_MARKER + " */", "")
                    .replaceFirst("(?i)select", "SELECT STRAIGHT_JOIN");
        }

        return sql.replaceAll("(?s)/\\*.*?\\*/", "").trim();
    }
}
