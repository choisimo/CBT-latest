"""
OpenAI API 서비스 클래스
- OpenAI API 통합 관리
- 채팅 기능 (일반/스트리밍)
- 대화 기록 관리
- 파일 저장/불러오기
"""

import os
import json
from typing import List, Dict, Optional
from datetime import datetime
from openai import OpenAI
import logging

# .env 파일 자동 로드
try:
    from dotenv import load_dotenv
    load_dotenv()  # .env 파일에서 환경변수 로드
except ImportError:
    pass  # python-dotenv가 설치되지 않은 경우 무시

# 로깅 설정
logger = logging.getLogger(__name__)

class OpenAIService:
    """OpenAI API를 활용한 채팅 서비스 클래스"""
    
    def __init__(self, api_key: Optional[str] = None):
        """
        OpenAI 서비스 초기화
        
        Args:
            api_key: OpenAI API 키 (없으면 환경변수에서 가져옴)
        """
        self.api_key = api_key or os.getenv('OPENAI_API_KEY')
        if not self.api_key:
            raise ValueError("OpenAI API 키가 필요합니다. 환경변수 OPENAI_API_KEY를 설정하거나 직접 전달하세요.")
        print(self.api_key)
        self.client = OpenAI(api_key=self.api_key)
        self.conversation_history: List[Dict[str, str, str]] = [] # 예시, self.conversation_history = [   {"role": "user", "content": "안녕하세요?", timestamp = "2023-01-01T12:00:00"} ]
        
        # 기본 설정 (환경변수에서 가져오거나 기본값 사용)
        self.default_model = os.getenv('DEFAULT_MODEL', "gpt-3.5-turbo")
        self.default_max_tokens = int(os.getenv('DEFAULT_MAX_TOKENS', "2000")) # **토큰(Token)**은 자연어 처리에서 텍스트를 구성하는 최소 단위입니다. 단어, 구두점, 공백 등이 토큰으로 간주될 수 있습니다. ex) "Hello, world!"는 약 3개의 토큰으로 간주.
        self.default_temperature = float(os.getenv('DEFAULT_TEMPERATURE', "0.7")) # 모델의 창의성 정도, 값이 낮을수록 모델이 더 결정적이고 예측 가능한 응답 생성
        
    def add_message(self, role: str, content: str):
        """대화 기록에 메시지 추가"""
        message = {
            "role": role,
            "content": content,
            "timestamp": datetime.now().isoformat()
        }
        self.conversation_history.append(message)
        logger.info(f"메시지 추가: {role} - {content[:50]}...")
    
    def get_conversation_history(self) -> List[Dict[str, str, str]]:
        """대화 기록 반환"""
        return self.conversation_history
    
    def clear_conversation(self):
        """대화 기록 초기화"""
        self.conversation_history = []
        logger.info("대화 기록 초기화됨")
    
    def chat(self, 
             message: str, 
             model: str = None,
             max_tokens: int = None,
             temperature: float = None,
             system_prompt: str = None) -> str:
        """
        일반 채팅 (한 번에 전체 응답 받기)
        
        Args:
            message: 사용자 메시지
            model: 사용할 AI 모델
            max_tokens: 최대 토큰 수
            temperature: 창의성 정도 (0.0~1.0)
            system_prompt: 시스템 프롬프트
            
        Returns:
            AI 응답 메시지
        """
        try:
            # 기본값 설정
            model = model or self.default_model
            max_tokens = max_tokens or self.default_max_tokens
            temperature = temperature or self.default_temperature
            
            # 메시지 목록 구성
            messages = []
            
            # 시스템 프롬프트 추가 (있는 경우)
            if system_prompt:
                messages.append({"role": "system", "content": system_prompt})
            
            # 대화 기록 추가 (API 형식으로 변환)
            for msg in self.conversation_history:
                if msg["role"] in ["user", "assistant"]:
                    messages.append({"role": msg["role"], "content": msg["content"]})
            
            # 현재 사용자 메시지 추가
            messages.append({"role": "user", "content": message})
            
            # OpenAI API 호출, stream=False (전체 응답을 한 번에 받음)
            response = self.client.chat.completions.create(
                model=model,
                messages=messages,
                max_tokens=max_tokens,
                temperature=temperature
            )
            
            ai_response = response.choices[0].message.content # AI 응답 메시지
            
            # 대화 기록에 추가
            self.add_message("user", message) # 사용자 메시지 추가
            self.add_message("assistant", ai_response) # AI 응답 추가
            
            return ai_response
            
        except Exception as e:
            logger.error(f"채팅 요청 오류: {str(e)}")
            raise Exception(f"AI 응답 생성 중 오류가 발생했습니다: {str(e)}")
    
    def chat_stream(self, 
                   message: str, 
                   model: str = None,
                   max_tokens: int = None,
                   temperature: float = None,
                   system_prompt: str = None):
        """
        스트리밍 채팅 (실시간으로 응답 받기)
        
        Args:
            message: 사용자 메시지
            model: 사용할 AI 모델
            max_tokens: 최대 토큰 수
            temperature: 창의성 정도
            system_prompt: 시스템 프롬프트
            
        Yields:
            AI 응답 조각들
        """
        try:
            # 기본값 설정
            model = model or self.default_model
            max_tokens = max_tokens or self.default_max_tokens
            temperature = temperature or self.default_temperature
            
            # 메시지 목록 구성
            messages = []
            
            if system_prompt:
                messages.append({"role": "system", "content": system_prompt})
            
            for msg in self.conversation_history:
                if msg["role"] in ["user", "assistant"]:
                    messages.append({"role": msg["role"], "content": msg["content"]})
            
            messages.append({"role": "user", "content": message})
            
            # OpenAI API 호출, stream=True (실시간 응답 받기)
            stream = self.client.chat.completions.create(
                model=model,
                messages=messages,
                max_tokens=max_tokens,
                temperature=temperature,
                stream=True
            )
            
            full_response = ""
            
            for chunk in stream:
                if chunk.choices[0].delta.content is not None: 
                    content = chunk.choices[0].delta.content # AI 응답 조각
                    full_response += content # 전체 응답에 추가
                    yield content # 참고: yield 값을 반환하지만 실행을 멈추고 상태를 유지. Content를 순차적으로 반환
            
            # 대화 기록에 추가
            self.add_message("user", message)
            self.add_message("assistant", full_response)
            
        except Exception as e:
            logger.error(f"스트리밍 채팅 오류: {str(e)}")
            raise Exception(f"스트리밍 응답 생성 중 오류가 발생했습니다: {str(e)}")
    
    def generate_title(self, conversation_preview: str) -> str:
        """대화 내용을 바탕으로 제목 생성"""
        try:
            response = self.client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[
                    {"role": "system", "content": "다음 대화 내용을 바탕으로 간단하고 명확한 제목을 생성해주세요. 10글자 이내로 작성하세요."},
                    {"role": "user", "content": conversation_preview}
                ],
                max_tokens=30, 
                temperature=0.2
            )
            
            return response.choices[0].message.content.strip()
            
        except Exception as e:
            logger.error(f"제목 생성 오류: {str(e)}")
            return "새 대화"
    
    def chat_with_history(self, 
                         messages: List[Dict[str, str]], 
                         model: str = None,
                         max_tokens: int = None,
                         temperature: float = None) -> Dict[str, str]:
        """
        외부 메시지 히스토리를 사용한 채팅 (FastAPI용)
        
        Args:
            messages: 메시지 히스토리 리스트 [{"role": "user", "content": "..."}, ...]
            model: 사용할 AI 모델
            max_tokens: 최대 토큰 수
            temperature: 창의성 정도
            
        Returns:
            AI 응답과 메타데이터를 포함한 딕셔너리
        """
        try:
            # 기본값 설정
            model = model or self.default_model
            max_tokens = max_tokens or self.default_max_tokens
            temperature = temperature or self.default_temperature
            
            # OpenAI API 호출
            response = self.client.chat.completions.create(
                model=model,
                messages=messages,
                max_tokens=max_tokens,
                temperature=temperature
            )
            
            ai_response = response.choices[0].message.content
            
            return {
                "content": ai_response,
                "model": model,
                "usage": {
                    "prompt_tokens": response.usage.prompt_tokens if response.usage else 0, # 프롬프트 토큰 수
                    "completion_tokens": response.usage.completion_tokens if response.usage else 0, # 응답 토큰 수
                    "total_tokens": response.usage.total_tokens if response.usage else 0 # 총 토큰 수
                }
            }
            
        except Exception as e:
            logger.error(f"채팅 요청 오류: {str(e)}")
            raise Exception(f"AI 응답 생성 중 오류가 발생했습니다: {str(e)}")
    
    def chat_stream_with_history(self, 
                                messages: List[Dict[str, str]], 
                                model: str = None,
                                max_tokens: int = None,
                                temperature: float = None):
        """
        외부 메시지 히스토리를 사용한 스트리밍 채팅 (FastAPI용)
        
        Args:
            messages: 메시지 히스토리 리스트
            model: 사용할 AI 모델
            max_tokens: 최대 토큰 수
            temperature: 창의성 정도
            
        Yields:
            AI 응답 조각들의 딕셔너리
        """
        try:
            # 기본값 설정
            model = model or self.default_model
            max_tokens = max_tokens or self.default_max_tokens
            temperature = temperature or self.default_temperature
            
            # 스트리밍 API 호출
            stream = self.client.chat.completions.create(
                model=model,
                messages=messages,
                max_tokens=max_tokens,
                temperature=temperature,
                stream=True
            )
            
            for chunk in stream:
                if chunk.choices[0].delta.content is not None:
                    content = chunk.choices[0].delta.content
                    yield {
                        "content": content,
                        "model": model,
                        "finished": False
                    }
            
            # 스트리밍 완료 표시
            yield {
                "content": "",
                "model": model,
                "finished": True
            }
            
        except Exception as e:
            logger.error(f"스트리밍 채팅 오류: {str(e)}")
            raise Exception(f"스트리밍 응답 생성 중 오류가 발생했습니다: {str(e)}")
    
    def save_conversation(self, filename: str):
        """대화 기록을 파일로 저장"""
        try:
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(self.conversation_history, f, ensure_ascii=False, indent=2)
            logger.info(f"대화 기록 저장됨: {filename}")
        except Exception as e:
            logger.error(f"대화 저장 오류: {str(e)}")
    
    def load_conversation(self, filename: str):
        """파일에서 대화 기록 불러오기"""
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                self.conversation_history = json.load(f)
            logger.info(f"대화 기록 불러옴: {filename}")
        except Exception as e:
            logger.error(f"대화 불러오기 오류: {str(e)}")
    
    def get_available_models(self) -> List[str]:
        """사용 가능한 모델 목록 반환"""
        return [
            "gpt-4",
            "gpt-4-turbo-preview",
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k"
        ]
    
    def get_conversation_summary(self) -> Dict[str, int]:
        """대화 통계 정보 반환"""
        total_messages = len(self.conversation_history)
        user_messages = len([msg for msg in self.conversation_history if msg["role"] == "user"])
        ai_messages = len([msg for msg in self.conversation_history if msg["role"] == "assistant"])
        
        return {
            "total_messages": total_messages,
            "user_messages": user_messages,
            "ai_messages": ai_messages
        } 