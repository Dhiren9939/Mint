package me.dhiren9939.mint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;

@EnableScheduling
@SpringBootApplication
public class MintApplication {
	public static void main(String[] args) {
		// Re-resolve DNS quickly so an ElastiCache failover reaches the new primary
		Security.setProperty("networkaddress.cache.ttl", "5");
		SpringApplication.run(MintApplication.class, args);
	}
}
