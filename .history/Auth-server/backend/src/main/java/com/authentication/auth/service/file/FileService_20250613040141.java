package com.authentication.auth.service.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 처리 서비스 인터페이스
 * 파일 업로드, 저장, 유효성 검사 등의 책임을 담당합니다.
 */
public interface FileService {
    
    /**
     * 프로필 이미지를 저장하고 접근 가능한 URL을 반환합니다.
     * 
     * @param file 업로드할 이미지 파일
     * @return 저장된 파일의 URL
     * @throws CustomException 파일이 유효하지 않거나 저장에 실패한 경우
     */
    String storeProfileImage(MultipartFile file);
    
    /**
     * 파일 확장자가 유효한지 검사합니다.
     * 
     * @param extension 파일 확장자
     * @return 유효한 확장자인 경우 true
     */
    boolean isValidExtension(String extension);
    
    /**
     * 파일 내용(MIME 타입)이 유효한지 검사합니다.
     * 
     * @param file 검사할 파일
     * @return 유효한 파일 내용인 경우 true
     */
    boolean isValidFileContent(java.io.File file);
}
