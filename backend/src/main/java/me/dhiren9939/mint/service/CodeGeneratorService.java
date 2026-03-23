package me.dhiren9939.mint.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.dhiren9939.mint.exception.FileCodeGenerationFailure;
import me.dhiren9939.mint.model.entity.FileMetaData;
import me.dhiren9939.mint.repository.FileMetaDataRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class CodeGeneratorService {

    private final FileMetaDataRepository fileMetaDataRepository;

    private static final String alpha = ".0123456789abcdefghijklmnopqrstuvwxyz";
    private static final Integer codeSize = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    public String getRandomCode() {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < codeSize; i++) {
            int randomIdx = secureRandom.nextInt(alpha.length());
            stringBuilder.append(alpha.charAt(randomIdx));
        }

        return stringBuilder.toString();
    }

    public String getUniqueFileCode() throws FileCodeGenerationFailure {
        for (int i = 0; i < 2; i++) {
            String fileCode = this.getRandomCode();
            Optional<FileMetaData> metaDataOptional = fileMetaDataRepository.findByFileCode(fileCode);
            if (metaDataOptional.isEmpty())
                return fileCode;
        }

        throw new FileCodeGenerationFailure();
    }
}
