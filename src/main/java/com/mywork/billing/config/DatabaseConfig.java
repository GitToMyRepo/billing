package com.mywork.billing.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Read/Write datasource splitting configuration.
 *
 * Only activated when billing.datasource.routing.enabled=true
 * (disabled by default to avoid conflicting with Spring Boot auto-configuration).
 *
 * Routes database connections based on transaction type:
 * - @Transactional(readOnly = true)  → read replica (SELECT only, better performance)
 * - @Transactional                   → primary database (INSERT, UPDATE, DELETE)
 *
 * In production, spring.datasource.url and spring.read-datasource.url
 * would point to different database hosts (primary and read replica).
 * Here both point to the same database for demonstration purposes.
 *
 * Real-world benefit:
 * - Reduces load on primary database
 * - Read replicas can scale horizontally
 * - Better performance for read-heavy billing queries (invoice lookups etc.)
 *
 * To enable: billing.datasource.routing.enabled=true in application.properties
 */
@Configuration
@ConditionalOnProperty(name = "billing.datasource.routing.enabled", havingValue = "true")
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.read-datasource.url}")
    private String readUrl;

    @Bean("primaryDataSource")
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(primaryUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("primary-pool");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }

    @Bean("readDataSource")
    public DataSource readDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(readUrl); // in production: read replica URL
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("read-pool");
        config.setMaximumPoolSize(20); // more connections for read-heavy workload
        config.setReadOnly(true);      // hint to the driver
        return new HikariDataSource(config);
    }

    /**
     * Routing datasource - inspects the current transaction's readOnly flag
     * and routes to the appropriate datasource automatically.
     *
     * Works in conjunction with @Transactional(readOnly = true) on service methods.
     * CustomerService uses @Transactional(readOnly = true) as the class default,
     * so all GET operations automatically route to the read replica.
     */
    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primary,
            @Qualifier("readDataSource") DataSource read) {

        Map<Object, Object> dataSources = new HashMap<>();
        dataSources.put(DataSourceType.PRIMARY, primary);
        dataSources.put(DataSourceType.READ_ONLY, read);

        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(dataSources);
        routingDataSource.setDefaultTargetDataSource(primary);
        return routingDataSource;
    }

    /**
     * Custom routing logic - checks if current transaction is read-only.
     * Spring sets this flag based on @Transactional(readOnly = true).
     */
    static class RoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            return isReadOnly ? DataSourceType.READ_ONLY : DataSourceType.PRIMARY;
        }
    }

    enum DataSourceType {
        PRIMARY, READ_ONLY
    }
}
