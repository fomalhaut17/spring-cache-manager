package com.example.demo.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.example.demo.mapper") // Scan for mappers
public class AppConfig {

    // Define a DataSource bean (e.g., H2 in-memory for simplicity)
    // This is needed for SqlSessionFactory to be created.
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            // .addScript("schema.sql") // if you have a schema
            .build();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setConfigLocation(new PathMatchingResourcePatternResolver().getResource("classpath:mybatis-config.xml"));
        // Optional: if mappers are not picked by configLocation or @MapperScan
        // sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mappers/*.xml"));
        return sessionFactory.getObject();
    }
}
