package me.dhiren9939.mint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class CodeGeneratorService {
    private static final String alpha = ".0123456789abcdefghijklmnopqrstuvwxyz";
    private static final Integer codeSize = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    public String getRandomCode(){
        StringBuilder stringBuilder = new StringBuilder();

        for(int i = 0;i<codeSize;i++){
            int randomIdx = secureRandom.nextInt(alpha.length());
            stringBuilder.append(alpha.charAt(randomIdx));
        }

        return stringBuilder.toString();
    }
}
