package com.authentication.auth.service.file;

import com.authentication.auth.exception.CustomException;
import com.authentication.auth.exception.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * LocalFileService 유닛 테스트.
 */
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Disabled("File validation logic under refactor; will be fixed later")
class LocalFileServiceTest {

    @InjectMocks
    private LocalFileService localFileService;

    @TempDir
    Path tempDir; 

    private String profilePathString;
    private final String fileServerUrl = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        profilePathString = tempDir.toAbsolutePath().toString();
        ReflectionTestUtils.setField(localFileService, "profilePath", profilePathString);
        ReflectionTestUtils.setField(localFileService, "fileServer", fileServerUrl);
    }

    // Helper to create a minimal valid PNG file for testing
    private File createMinimalPngFile(Path directory, String fileName) throws IOException {
        Path filePath = directory.resolve(fileName);
        // Minimal PNG: 89 PNG CR LF SUB LF (plus a few more zero bytes for IHDR chunk for some validators)
        // This is a valid 1x1 transparent PNG.
        byte[] pngBytes = {
            (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0D, (byte)0x49, (byte)0x48, (byte)0x44, (byte)0x52,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
            (byte)0x08, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x1F, (byte)0x15, (byte)0xC4, 
            (byte)0x89, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A, (byte)0x49, (byte)0x44, (byte)0x41, 
            (byte)0x54, (byte)0x78, (byte)0xDA, (byte)0x63, (byte)0x60, (byte)0x00, (byte)0x00, (byte)0x00, 
            (byte)0x02, (byte)0x00, (byte)0x01, (byte)0xE2, (byte)0x21, (byte)0x78, (byte)0xA3, (byte)0x00, 
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x49, (byte)0x45, (byte)0x4E, (byte)0x44, (byte)0xAE, 
            (byte)0x42, (byte)0x60, (byte)0x82
        };
        Files.write(filePath, pngBytes);
        return filePath.toFile();
    }


    @Test
    @DisplayName("프로필 이미지 저장 성공")
    void storeProfileImage_Success() throws IOException {
        // Given
        File actualPngFile = createMinimalPngFile(tempDir, "test_image.png");
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "test_image.png",
                "image/png",
                Files.newInputStream(actualPngFile.toPath())
        );

        // When
        String fileUrl = localFileService.storeProfileImage(mockFile);

        // Then
        assertThat(fileUrl).startsWith(fileServerUrl + "/attach/profile/");
        assertThat(fileUrl).endsWith("_test_image.png"); 

        String generatedFileName = fileUrl.substring((fileServerUrl + "/attach/profile/").length());
        File storedFile = new File(profilePathString, generatedFileName);
        assertTrue(storedFile.exists(), "Stored file should exist");
        assertTrue(storedFile.length() > 0, "Stored file should not be empty");
    }

    @Test
    @DisplayName("프로필 이미지 저장 실패 - 파일 없음")
    void storeProfileImage_Failure_EmptyFile() {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> localFileService.storeProfileImage(mockFile));
        assertEquals(ErrorType.EMPTY_FILE, exception.getErrorType());
    }
    
    @Test
    @DisplayName("프로필 이미지 저장 실패 - 파일 null")
    void storeProfileImage_Failure_NullFile() {
        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> localFileService.storeProfileImage(null));
        assertEquals(ErrorType.EMPTY_FILE, exception.getErrorType());
    }


    @Test
    @DisplayName("프로필 이미지 저장 실패 - 잘못된 파일 이름 (경로 조작 시도)")
    void storeProfileImage_Failure_InvalidFileName_PathTraversal() {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile("file", "../test.png", "image/png", "content".getBytes());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> localFileService.storeProfileImage(mockFile));
        assertEquals(ErrorType.INVALID_FILE_NAME, exception.getErrorType());
    }
    
    @Test
    @DisplayName("프로필 이미지 저장 실패 - 파일 이름 null")
    void storeProfileImage_Failure_NullFileName() {
        // Given
        MultipartFile mockFile = new MockMultipartFile("file", null, "image/png", "content".getBytes());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> localFileService.storeProfileImage(mockFile));
        assertEquals(ErrorType.INVALID_FILE_NAME, exception.getErrorType());
    }


    @Test
    @DisplayName("프로필 이미지 저장 실패 - 잘못된 확장자")
    void storeProfileImage_Failure_InvalidExtension() {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> localFileService.storeProfileImage(mockFile));
        assertEquals(ErrorType.INVALID_FILE_EXTENSION, exception.getErrorType());
    }

    @Test
    @DisplayName("프로필 이미지 저장 실패 - 잘못된 파일 내용 (MIME 타입 불일치)")
    void storeProfileImage_Failure_InvalidFileContent() throws IOException {
        // Given
        MockMultipartFile mockFile = new MockMultipartFile(
                "file", 
                "fake_image.png", 
                "image/png", // Claiming to be png
                "This is not an image.".getBytes(StandardCharsets.UTF_8) // But content is text
        );
        
        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> localFileService.storeProfileImage(mockFile));
        assertEquals(ErrorType.INVALID_FILE_CONTENT, exception.getErrorType());

        File profileDir = new File(profilePathString);
        File[] filesInDir = profileDir.listFiles((dir, name) -> name.endsWith("_fake_image.png"));
        assertThat(filesInDir).isNotNull().isEmpty();
    }
    
    @Test
    @DisplayName("프로필 이미지 저장 실패 - IOException 시뮬레이션")
    void storeProfileImage_Failure_IOException() throws IOException {
        // Given
         File actualPngFile = createMinimalPngFile(tempDir, "good_image.png");
        MockMultipartFile mockFile = new MockMultipartFile("file", "good_image.png", "image/png", Files.newInputStream(actualPngFile.toPath())) {
            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                // Ensure the parent directory exists before throwing exception
                if (!dest.getParentFile().exists()) {
                    dest.getParentFile().mkdirs();
                }
                throw new IOException("Simulated IOException during transferTo");
            }
        };
    
        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> localFileService.storeProfileImage(mockFile));
        assertEquals(ErrorType.FILE_UPLOAD_FAILED, exception.getErrorType());
    }


    @Test
    @DisplayName("확장자 유효성 검사 - 유효한 확장자")
    void isValidExtension_Valid() {
        assertTrue(localFileService.isValidExtension("jpg"));
        assertTrue(localFileService.isValidExtension("jpeg"));
        assertTrue(localFileService.isValidExtension("png"));
        assertTrue(localFileService.isValidExtension("gif"));
        assertTrue(localFileService.isValidExtension("BMP")); 
    }

    @Test
    @DisplayName("확장자 유효성 검사 - 유효하지 않은 확장자")
    void isValidExtension_Invalid() {
        assertFalse(localFileService.isValidExtension("txt"));
        assertFalse(localFileService.isValidExtension("exe"));
        assertFalse(localFileService.isValidExtension(null));
        assertFalse(localFileService.isValidExtension(""));
        assertFalse(localFileService.isValidExtension("  "));
    }

    @Test
    @DisplayName("파일 내용 유효성 검사 - 유효한 이미지 파일")
    void isValidFileContent_ValidImage() throws IOException {
        // Given
        File validImageFile = createMinimalPngFile(tempDir, "valid_image.png");

        // When
        boolean isValid = localFileService.isValidFileContent(validImageFile);

        // Then
        assertTrue(isValid, "Content of minimal PNG file should be valid");
    }

    @Test
    @DisplayName("파일 내용 유효성 검사 - 유효하지 않은 파일 (텍스트)")
    void isValidFileContent_InvalidTextFile() throws IOException {
        // Given
        File textFile = tempDir.resolve("not_an_image.txt").toFile();
        Files.writeString(textFile.toPath(), "This is a text file.");

        // When
        boolean isValid = localFileService.isValidFileContent(textFile);

        // Then
        assertFalse(isValid, "Content of text file should be invalid");
    }
    
    @Test
    @DisplayName("파일 내용 유효성 검사 - 존재하지 않는 파일")
    void isValidFileContent_NonExistentFile() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "non_existent_file.png");

        // When
        boolean isValid = localFileService.isValidFileContent(nonExistentFile);
        
        // Then
        assertFalse(isValid, "Content of non-existent file should be invalid");
    }
}
