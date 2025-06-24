# OpenAI GPT CLI 도구

OpenAI GPT API를 활용한 명령줄 인터페이스(CLI) 도구입니다. 터미널에서 직접 AI와 대화하고 다양한 예제를 실행할 수 있습니다.

## 📁 프로젝트 구조

```
ai_cli/
├── chatbot.py           # 대화형 챗봇 인터페이스
├── examples.py          # 다양한 사용 예제들
└── README.md           # 이 파일
```

## 🚀 주요 기능

- 🤖 **대화형 챗봇**: 터미널에서 직접 채팅 가능
- 🔧 **고급 설정**: 모델 변경, 창의성 조정 등
- 📚 **다양한 예제**: 8가지 실용적인 사용 예제
- 💾 **대화 기록 관리**: 대화 내용 저장/불러오기
- 🎯 **커스텀 프롬프트**: 시스템 프롬프트로 AI 성격 조정

## 🎮 사용법

### 기본 챗봇 사용
```python
from ai.ai_cli.chatbot import ChatBot

# 기본 챗봇
basic_bot = ChatBot()
basic_bot.start_chat()
```

### 고급 챗봇 사용
```python
from ai.ai_cli.chatbot import AdvancedChatBot

# 고급 챗봇 (모델/온도 설정 가능)
advanced_bot = AdvancedChatBot()
advanced_bot.start_chat()
```

### 예제 실행
```python
from ai.ai_cli.examples import example_basic_chat, example_streaming_chat

# 기본 채팅 예제
example_basic_chat()

# 스트리밍 채팅 예제
example_streaming_chat()
```

## 🤖 ChatBot 클래스

### 기본 챗봇 명령어
- `quit`, `exit`, `종료`: 채팅 종료
- `clear`, `초기화`: 대화 기록 초기화
- `history`: 대화 기록 확인
- `save <파일명>`: 대화 저장
- `load <파일명>`: 대화 불러오기
- `models`: 사용 가능한 모델 목록
- `stats`: 대화 통계
- `help`: 도움말

### 고급 챗봇 추가 명령어
- `set-prompt <프롬프트>`: 시스템 프롬프트 설정
- `set-model <모델명>`: AI 모델 변경
- `set-temp <0.0-1.0>`: 창의성 수준 조정
- `show-settings`: 현재 설정 확인

## 📚 Examples 모듈

### 사용 가능한 예제들

1. **기본 채팅**: 질문-답변 형태
2. **스트리밍 채팅**: 실시간 응답 확인
3. **커스텀 프롬프트**: AI 성격 조정
4. **다양한 모델**: GPT-3.5 vs GPT-4 비교
5. **창의성 수준 변화**: Temperature 값 변화 효과
6. **대화 기록 관리**: 저장/불러오기/통계
7. **제목 생성**: 자동 제목 생성
8. **코드 어시스턴트**: 프로그래밍 도움

### 예제 실행 방법
```python
from ai.ai_cli.examples import *

# 개별 예제 실행
example_basic_chat()
example_streaming_chat()
example_custom_prompt()

# 모든 예제 실행
run_all_examples()
```

## 🔧 고급 설정

### 시스템 프롬프트 사용
```python
from ai.ai_api.openai_service import OpenAIService

service = OpenAIService()

system_prompt = """
You are an AI Diary Assistant specialized in emotion analysis. 
When the user sends a diary entry, you must:
1. Analyze the text and detect primary and secondary emotions.  
2. 구체적인 감정 강도(0~100)와 감정 카테고리(예: 기쁨, 슬픔, 분노, 불안, 평온 등)를 JSON으로 반환.  
3. 요약(summary): 2–3문장으로 오늘의 정서 요약.  
4. 코칭(coaching): 내일을 위한 제안 또는 격려의 말을 최소 100글자 이상 토큰을 전부 소진.  
5. 모든 출력은 valid JSON 객체 하나로만 응답.
6. 불안지수가 50이상이면 isNegative를 true로 반환

Example JSON schema:
```json
{
  "aiResponse":{
    "emotions": [
        {"category": "기쁨",    "intensity": 75},
        {"category": "불안",    "intensity": 40},
        {"category": "분노",    "intensity": 10}
    ],
    "summary": "오늘은 새로운 프로젝트에 도전하며 뿌듯함을 느꼈으나, 마감이 다가와 약간의 불안도 있었습니다.",
    "coaching": "작은 성취부터 차근차근 기록하며 불안을 줄여보세요! 때로는 모든 것을 버리고 새로 시작해보는 것도 좋지만 그렇게 하기 전에 먼저 오늘 하루를 돌아보고 내일을 위한 준비를 해보세요!"
  }
  "isNegative": true
  "timestamp": "2025-06-16T10:00:00Z"
}
```
"""

response = service.chat("파이썬이란?", system_prompt=system_prompt)
```

### 모델 선택
```python
# GPT-4 사용 (API 권한 필요)
response = service.chat("질문", model="gpt-4")

# GPT-3.5 사용 (기본값)
response = service.chat("질문", model="gpt-3.5-turbo")
```

### 창의성 조정
```python
# 보수적 응답 (정확성 중심)
response = service.chat("질문", temperature=0.2)

# 창의적 응답 (다양성 중심)
response = service.chat("질문", temperature=0.9)
```

## 💡 사용 팁

### 대화 기록 관리
```python
# 대화 저장
chatbot.ai_service.save_conversation("my_chat.json")

# 대화 불러오기
chatbot.ai_service.load_conversation("my_chat.json")

# 대화 통계 확인
stats = chatbot.ai_service.get_conversation_summary()
print(f"총 메시지: {stats['total_messages']}개")
```

### 스트리밍 출력
```python
# 실시간으로 응답 받기
for chunk in service.chat_stream("긴 질문"):
    print(chunk, end="", flush=True)
```

## 🚨 문제 해결

### 자주 발생하는 오류

#### 1. Import 오류
```
ModuleNotFoundError: No module named 'ai.ai_api.openai_service'
```
**해결**: 
- 프로젝트 루트에서 실행하는지 확인
- PYTHONPATH 설정 확인

#### 2. API 키 오류
```
ValueError: OpenAI API 키가 필요합니다.
```
**해결**: 
- `.env` 파일에서 API 키 확인
- 환경변수 `OPENAI_API_KEY` 설정 확인

## 📄 라이센스

이 CLI 도구는 교육 목적으로 자유롭게 사용할 수 있습니다.

---

**🎯 빠른 시작:**
1. 프로젝트 루트에서 `python main.py` 실행
2. 메뉴 1번으로 기본 챗봇 시작
3. 메뉴 2번으로 고급 챗봇 체험
4. 메뉴 3번에서 다양한 예제 실행 