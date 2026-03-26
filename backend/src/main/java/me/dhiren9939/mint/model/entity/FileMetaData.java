package me.dhiren9939.mint.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.dhiren9939.mint.service.ExpiryDuration;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileMetaData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false)
    private String fileCode;

    @Column(unique = true, nullable = false)
    private String fileKey;

    private LocalDateTime cleanAt;

    private int downloadCount;
    private int maxDownloadCount = 100;

    @Enumerated(EnumType.STRING)
    private FileState fileState = FileState.PENDING;

    @Enumerated(EnumType.STRING)
    private ExpiryDuration fileExpiryDuration;
}
