package com.authentication.auth.service.file;

import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 로컬 파일 시스템을 사용한 파일 서비스 구현체
 */
@Slf4j
@Service
public class LocalFileService implements FileService {

    @Value("${file.profile-path}")
    private String profilePath;
    
    @Value("${file.server}")
    private String fileServer;
    
    /**
     * 허용되는 이미지 파일 확장자 목록
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff"
    );
    
    /**
     * 허용되는 MIME 타입 목록
     */
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/bmp", 
        "image/webp", "image/svg+xml", "image/tiff"
    );

    @Override
    public String storeProfileImage(MultipartFile file) throws CustomException {
        // 1. 파일 null 체크
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorType.EMPTY_FILE);
        }
        
        // 2. 파일 이름 유효성 검사
        String originName = file.getOriginalFilename();
        if (originName == null || originName.trim().isEmpty() || originName.contains("..")) {
            log.error("Invalid file name: {}", originName);
            throw new CustomException(ErrorType.INVALID_FILE_NAME, "Invalid file name: " + originName);
        }

        // 3. 확장자 유효성 검사
        String extension = FilenameUtils.getExtension(originName).toLowerCase();
        if (!isValidExtension(extension)) {
            log.error("Invalid file extension: {}", extension);
            throw new CustomException(ErrorType.INVALID_FILE_EXTENSION, "Unsupported file extension: " + extension);
        }

        try {
            // 4. 파일 저장
            String fileName = generateUniqueFileName(originName, extension);
            File directory = new File(profilePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            File targetFile = new File(profilePath, fileName);
            file.transferTo(targetFile);
            
            log.info("File saved to: {}", targetFile.getAbsolutePath());

            // 5. 파일 내용(MIME 타입) 유효성 검사
            if (!isValidFileContent(targetFile)) {
                // 유효하지 않은 파일은 삭제
                targetFile.delete();
                log.error("Invalid file content for file: {}", originName);
                throw new CustomException(ErrorType.INVALID_FILE_CONTENT, "File content is not valid");
            }

            // 6. 접근 가능한 URL 반환
            return fileServer + "/attach/profile/" + fileName;

        } catch (IOException e) {
            log.error("File upload failed for file: {}", originName, e);
            throw new CustomException(ErrorType.FILE_UPLOAD_FAILED, "Failed to upload profile image", e);
        }
    }

    @Override
    public boolean isValidExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public boolean isValidFileContent(File file) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType);
        } catch (IOException e) {
            log.error("Error checking file content for file: {}", file.getName(), e);
            return false;
        }
    }
    
    /**
     * 고유한 파일 이름을 생성합니다.
     */
    private String generateUniqueFileName(String originalName, String extension) {
        String baseName = FilenameUtils.getBaseName(originalName);
        return UUID.randomUUID().toString() + "_" + baseName + "." + extension;
    }
}
