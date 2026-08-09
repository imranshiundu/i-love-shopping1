package com.iloveshopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.iloveshopping.repository")
@EntityScan(basePackages = "com.iloveshopping.entity")
@ComponentScan(basePackages = "com.iloveshopping")
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
public class ILoveShoppingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ILoveShoppingApplication.class, args);
    }
}