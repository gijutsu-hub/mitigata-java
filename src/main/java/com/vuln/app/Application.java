package com.vuln.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

// ============================================================
//  VulnBank (Java / Spring Boot) - DELIBERATELY VULNERABLE.
//  OWASP Top 10 training target. Do NOT deploy.
// ============================================================
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // Seed an in-memory H2 database on startup.
    @Bean
    CommandLineRunner seed(JdbcTemplate jdbc) {
        return args -> {
            jdbc.execute("CREATE TABLE users(id INT PRIMARY KEY, username VARCHAR, password VARCHAR, role VARCHAR, balance INT)");
            jdbc.execute("INSERT INTO users VALUES " +
                "(1,'alice','password123','user',500)," +
                "(2,'bob','hunter2','user',250)," +
                "(3,'admin','S3cr3t_Adm1n_!','admin',999999)");
            jdbc.execute("CREATE TABLE accounts(id INT PRIMARY KEY, owner_id INT, iban VARCHAR, note VARCHAR)");
            jdbc.execute("INSERT INTO accounts VALUES " +
                "(1001,1,'DE-ALICE','alice savings')," +
                "(1002,2,'DE-BOB','bob savings')," +
                "(1003,3,'DE-ADMIN','FLAG{java_idor_account}')");
        };
    }
}
