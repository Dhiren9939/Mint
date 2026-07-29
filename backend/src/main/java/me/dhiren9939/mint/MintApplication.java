package me.dhiren9939.mint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MintApplication {
	// Dummy backend change for branch and PR workflow validation.
	public static void main(String[] args) {
		SpringApplication.run(MintApplication.class, args);
	}
}
