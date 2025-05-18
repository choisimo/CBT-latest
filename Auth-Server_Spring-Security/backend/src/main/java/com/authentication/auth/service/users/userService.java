package com.career_block.auth.service.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authentication.auth.DTO.users.joinRequest;
import com.authentication.auth.domain.Role;
import com.authentication.auth.domain.users;
import com.authentication.auth.repository.usersRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class userService {

    private final usersRepository usersRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public ResponseEntity<?> join(joinRequest request){
        if (usersRepository.existsByUserIdOrNickname(request.getUserId(), request.getNickname())) {
            log.error("이미 존재하는 아이디 혹은 닉네임 입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        try {
            joinRequest joinRequestBuilder = joinRequest.builder()
                    .userId(request.getUserId())
                    .userPw(passwordEncoder.encode(request.getUserPw()))
                    .userName(request.getUserName())
                    .nickname(request.getNickname())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .role(Role.USER)
                    .birthDate(request.getBirthDate())
                    .gender(request.getGender())
                    .isPrivate(request.isPrivate())
                    .profile(request.getProfile() != null ? request.getProfile() : "대충 이미지")
                    .build();
            users joinUser = joinRequestBuilder.toEntity();
            usersRepository.save(joinUser);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            log.error("회원 가입 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Transactional
    public String getEmailByUserId(String userId){
        try{
            users user = usersRepository.findByUserId(userId);
            return user.getEmail();
        } catch (Exception e){
            log.error("이메일 찾기 실패", e);
            return null;
        }
    }


    @Transactional
    public void UpdateUserPassword(String userId, String temporalPassword) {
        try {
            int updateCount = usersRepository.updatePassword(userId, passwordEncoder.encode(temporalPassword));
            if (updateCount == 0) throw new Exception("비밀번호 변경 실패! 사용자를 찾을 수 없음");
        } catch (Exception e) {
            log.error("비밀번호 변경 실패", e);
        }
    }



    @Transactional
    public boolean checkUserIdIsDuplicate(String userId) {
        return usersRepository.existsByUserId(userId);
    }

    @Transactional
    public boolean checkNickNameIsDuplicate(String nickname) {
        return usersRepository.existsByNickname(nickname);
    }



}
