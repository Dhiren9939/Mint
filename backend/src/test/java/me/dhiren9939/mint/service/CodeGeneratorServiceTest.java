package me.dhiren9939.mint.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CodeGeneratorServiceTest {

    CodeGeneratorService codeGeneratorService = new CodeGeneratorService();

    @Test
    @DisplayName("Should return a 6 digit code")
    public void shouldReturnFileCode(){
        String result = codeGeneratorService.getRandomCode();
        assertEquals(6,result.length());
    }
}