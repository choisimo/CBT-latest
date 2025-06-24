API 정리
1. 이메일 API
api 엔드 포인트 내용 구현 상태 의문점
/api/public/emailSend 이메일 인증 코드 발 송
OX
1. 이메일 보내는 건 백엔드에서 알아서 하는거지? 2. 인증방식 사용 안 해도 됨? 나는 로그 인 한 상태에서 이메 일 인증 요청하는 걸 로 이해 했는데
/api/private/customEmailSend 커스텀 이메일 발송 X 이메일 인증 코드를 보낼 때 쓰는 거임?
/api/public/emailCheck 이메일 인증 코드 확 인
O
/api/protected/sendEmailPassword 임시 비밀번호 이메 일 전송
X 로그인 했는데 비밀 번호를 왜 찾아?
/api/public/findPassWithEmail 아이디로 이메일 찾 아 임시 비밀번호 전 송
X 아이디를 쓸거야? 나는 이메일만 써도 충분하다고 생각하는
2. 이메일 API
api 엔드 포인트 내용 구현 상태 의문점
/api/public/join 회원 가입 OX
1. 회원 가입 시키 고 이메일 인증 시 킨다 그러지 않았 어? 2. userId 써야 됨? 3. userName, phone, birthdate, gender 써야 됨? 4. isPrivate, code 뭐임??
/api/public/profileUpload 프로필 이미지 업 로드
X
/api/public/check/userId/IsDuplicate 사용자 ID 중복 체 크 항목
X
/api/public/check/nickname/IsDuplicate 닉네임 중복 체크 X
/api/public/clean/userTokenCookie 사용자 토큰 쿠키 정리 (로그아웃)
OX
앱은
credentials: 'includeʼ이 안되 서
인증 방식을 사용 해야됨
3. 관리자 필터 API
뭔지도 모르고 구현도 안했음 공부해야할 듯
4. 이메일 API
api 엔드 포인트 내용 구현 상태 의문점
/api/auth/login 로그인 OX User 정보 반환 필요
/auth_check 인증 상태 확인 O
/auth/api/protected/refresh JWT 토큰 재발급 O
/api/public/oauth2/ oauth2 관련 api X 한 명 정해서 sns 인증 권한?? 받아야 할 듯
5. 일기 API
api 엔드 포인트 내용 구현 상태 의문점
/api/diaries(POST) 일기 API O
/api/diaries(GET) 일기 목록 조회 OX
제목 내용 검색 날짜별 검색 일기 쓴 날 조회(달력 표 시) 기능이 필요한데 이거 프 론트에서 처리해??? 백에서 나눠서 만들어 주 는 게 맞는 것 같은데
/api/diaries/{diaryId} GET
특정 일기 상세 조회 OX
/api/diaries/{diaryId} PUT
특정 일기 수정 X 한 명 정해서 sns 인증 권한?? 받아야 할 듯
/api/diaries/{diaryId} DELETE
일기 삭제 X
추가 API
6. 세팅 API
얘도 아직 안함 