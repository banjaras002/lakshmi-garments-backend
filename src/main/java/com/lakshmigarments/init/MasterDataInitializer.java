package com.lakshmigarments.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
public class MasterDataInitializer {

//    private final JdbcTemplate jdbcTemplate;
//    private static final Logger LOGGER = LoggerFactory.getLogger(MasterDataInitializer.class);
//
//    public MasterDataInitializer(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public void run(String... args) throws Exception {
//        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Long.class);
//        if (count != null && count == 0) {
//            LOGGER.info("Inserting master data...");
//            var resource = new ClassPathResource("data/db_script.sql");
//            ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), resource);
//            LOGGER.info("Master data inserted successfully.");
//        }
//    }
}
