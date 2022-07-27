package io.pleo.antaeus.core.config

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

class LockProvider {

    fun lockProvider(dataSource: DataSource): JdbcTemplateLockProvider {
        return JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .build()
        )
    }
}