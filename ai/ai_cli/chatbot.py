"""
대화형 챗봇 인터페이스
- 터미널 기반 채팅 인터페이스
- 명령어 처리
- 실시간 스트리밍 출력
"""

from typing import Optional
from ai.ai_api.openai_service import OpenAIService
import logging

logger = logging.getLogger(__name__)

class ChatBot:
    """간단한 채팅봇 인터페이스"""
    
    def __init__(self, api_key: Optional[str] = None):
        """
        챗봇 초기화
        
        Args:
            api_key: OpenAI API 키
        """
        self.ai_service = OpenAIService(api_key)
        
    def start_chat(self):
        """대화형 채팅 시작"""
        self._show_welcome_message()
        
        while True:
            try:
                user_input = input("\n👤 You: ")
                
                if self._handle_command(user_input):
                    continue
                
                # AI 응답 생성
                self._generate_ai_response(user_input)
                
            except KeyboardInterrupt:
                print("\n👋 채팅을 종료합니다!")
                break
            except Exception as e:
                print(f"❌ 오류 발생: {str(e)}")
    
    def _show_welcome_message(self):
        """환영 메시지 출력"""
        print("🤖 OpenAI 챗봇에 오신 것을 환영합니다!")
        print("💬 메시지를 입력하세요")
        print()
        print("📋 사용 가능한 명령어:")
        print("  • quit, exit, 종료: 채팅 종료")
        print("  • clear, 초기화: 대화 기록 초기화")
        print("  • history: 대화 기록 확인")
        print("  • save <파일명>: 대화 저장")
        print("  • load <파일명>: 대화 불러오기")
        print("  • models: 사용 가능한 모델 목록")
        print("  • stats: 대화 통계")
        print("  • help: 도움말")
        print("-" * 50)
    
    def _handle_command(self, user_input: str) -> bool:
        """
        사용자 명령어 처리
        
        Args:
            user_input: 사용자 입력
            
        Returns:
            True if command was handled, False otherwise
        """
        command = user_input.lower().strip()
        
        # 종료 명령어
        if command in ['quit', 'exit', '종료']:
            print("👋 채팅을 종료합니다!")
            exit(0)
        
        # 대화 기록 초기화
        elif command in ['clear', '초기화']:
            self.ai_service.clear_conversation()
            print("🔄 대화 기록이 초기화되었습니다.")
            return True
        
        # 대화 기록 확인
        elif command == 'history':
            self._show_history()
            return True
        
        # 대화 저장
        elif command.startswith('save '):
            filename = command[5:].strip()
            if filename:
                self._save_conversation(filename)
            else:
                print("❌ 파일명을 입력해주세요. 예: save conversation.json")
            return True
        
        # 대화 불러오기
        elif command.startswith('load '):
            filename = command[5:].strip()
            if filename:
                self._load_conversation(filename)
            else:
                print("❌ 파일명을 입력해주세요. 예: load conversation.json")
            return True
        
        # 모델 목록
        elif command == 'models':
            self._show_models()
            return True
        
        # 통계
        elif command == 'stats':
            self._show_stats()
            return True
        
        # 도움말
        elif command == 'help':
            self._show_help()
            return True
        
        return False
    
    def _generate_ai_response(self, user_input: str):
        """AI 응답 생성 및 출력"""
        print("🤖 AI: ", end="", flush=True)
        
        try:
            # 스트리밍 방식으로 응답 출력
            for chunk in self.ai_service.chat_stream(user_input):
                print(chunk, end="", flush=True)
            print()  # 줄바꿈
        except Exception as e:
            print(f"\n❌ AI 응답 생성 중 오류: {str(e)}")
    
    def _show_history(self):
        """대화 기록 표시"""
        history = self.ai_service.get_conversation_history()
        
        if not history:
            print("📝 대화 기록이 없습니다.")
            return
        
        print(f"📊 대화 기록 ({len(history)}개 메시지):")
        print("-" * 30)
        
        # 최근 10개 메시지만 표시
        recent_messages = history[-10:]
        
        for i, msg in enumerate(recent_messages, 1):
            role_emoji = "👤" if msg["role"] == "user" else "🤖"
            content = msg["content"][:100] + "..." if len(msg["content"]) > 100 else msg["content"]
            timestamp = msg.get("timestamp", "시간 정보 없음")
            
            print(f"{i}. {role_emoji} [{timestamp[:19]}] {content}")
        
        if len(history) > 10:
            print(f"... 그리고 {len(history) - 10}개 더")
    
    def _save_conversation(self, filename: str):
        """대화 저장"""
        try:
            if not filename.endswith('.json'):
                filename += '.json'
            
            self.ai_service.save_conversation(filename)
            print(f"💾 대화 기록이 '{filename}'에 저장되었습니다.")
        except Exception as e:
            print(f"❌ 저장 실패: {str(e)}")
    
    def _load_conversation(self, filename: str):
        """대화 불러오기"""
        try:
            if not filename.endswith('.json'):
                filename += '.json'
            
            self.ai_service.load_conversation(filename)
            print(f"📂 '{filename}'에서 대화 기록을 불러왔습니다.")
        except Exception as e:
            print(f"❌ 불러오기 실패: {str(e)}")
    
    def _show_models(self):
        """사용 가능한 모델 목록 표시"""
        models = self.ai_service.get_available_models()
        print("🔧 사용 가능한 AI 모델:")
        for i, model in enumerate(models, 1):
            current = " (현재 사용 중)" if model == self.ai_service.default_model else ""
            print(f"  {i}. {model}{current}")
    
    def _show_stats(self):
        """대화 통계 표시"""
        stats = self.ai_service.get_conversation_summary()
        print("📈 대화 통계:")
        print(f"  • 전체 메시지: {stats['total_messages']}개")
        print(f"  • 사용자 메시지: {stats['user_messages']}개")
        print(f"  • AI 메시지: {stats['ai_messages']}개")
    
    def _show_help(self):
        """도움말 표시"""
        print("🆘 챗봇 사용법:")
        print()
        print("기본 사용:")
        print("  • 메시지 입력 후 Enter: AI와 대화")
        print("  • 스트리밍으로 실시간 응답 확인")
        print()
        print("명령어:")
        print("  • quit, exit, 종료: 프로그램 종료")
        print("  • clear, 초기화: 대화 기록 삭제")
        print("  • history: 최근 대화 기록 확인")
        print("  • save <파일명>: 대화를 JSON 파일로 저장")
        print("  • load <파일명>: JSON 파일에서 대화 불러오기")
        print("  • models: 사용 가능한 AI 모델 목록")
        print("  • stats: 현재 대화 통계 정보")
        print("  • help: 이 도움말 표시")
        print()
        print("팁:")
        print("  • Ctrl+C: 언제든지 프로그램 종료")
        print("  • 긴 응답은 실시간으로 출력됩니다")
        print("  • 대화 기록은 자동으로 유지됩니다")


class AdvancedChatBot(ChatBot):
    """고급 기능이 포함된 챗봇"""
    
    def __init__(self, api_key: Optional[str] = None):
        super().__init__(api_key)
        self.system_prompt = None
        self.current_model = None
        self.current_temperature = None
    
    def start_chat(self):
        """고급 설정이 포함된 채팅 시작"""
        self._show_advanced_welcome()
        super().start_chat()
    
    def _show_advanced_welcome(self):
        """고급 환영 메시지"""
        print("🚀 OpenAI 고급 챗봇에 오신 것을 환영합니다!")
        print("💡 추가 기능:")
        print("  • set-prompt <프롬프트>: 시스템 프롬프트 설정")
        print("  • set-model <모델명>: AI 모델 변경")
        print("  • set-temp <0.0-1.0>: 창의성 수준 조정")
        print("  • show-settings: 현재 설정 확인")
        print()
    
    def _handle_command(self, user_input: str) -> bool:
        """고급 명령어 처리"""
        command = user_input.lower().strip()
        
        # 시스템 프롬프트 설정
        if command.startswith('set-prompt '):
            prompt = user_input[11:].strip()
            self.system_prompt = prompt if prompt else None
            print(f"🎯 시스템 프롬프트 설정: {prompt[:50]}...")
            return True
        
        # 모델 변경
        elif command.startswith('set-model '):
            model = command[10:].strip()
            available_models = self.ai_service.get_available_models()
            if model in available_models:
                self.current_model = model
                print(f"🔧 모델 변경: {model}")
            else:
                print(f"❌ 사용할 수 없는 모델: {model}")
                print(f"사용 가능한 모델: {', '.join(available_models)}")
            return True
        
        # 온도 설정
        elif command.startswith('set-temp '):
            try:
                temp = float(command[9:].strip())
                if 0.0 <= temp <= 1.0:
                    self.current_temperature = temp
                    print(f"🌡️ 창의성 수준 설정: {temp}")
                else:
                    print("❌ 온도는 0.0~1.0 사이의 값이어야 합니다.")
            except ValueError:
                print("❌ 올바른 숫자를 입력해주세요.")
            return True
        
        # 현재 설정 확인
        elif command == 'show-settings':
            self._show_current_settings()
            return True
        
        # 부모 클래스의 명령어 처리
        return super()._handle_command(user_input)
    
    def _generate_ai_response(self, user_input: str):
        """고급 설정을 적용한 AI 응답 생성"""
        print("🤖 AI: ", end="", flush=True)
        
        try:
            # 고급 설정 적용
            for chunk in self.ai_service.chat_stream(
                user_input,
                model=self.current_model,
                temperature=self.current_temperature,
                system_prompt=self.system_prompt
            ):
                print(chunk, end="", flush=True)
            print()  # 줄바꿈
        except Exception as e:
            print(f"\n❌ AI 응답 생성 중 오류: {str(e)}")
    
    def _show_current_settings(self):
        """현재 설정 표시"""
        print("⚙️ 현재 설정:")
        print(f"  • 모델: {self.current_model or self.ai_service.default_model}")
        print(f"  • 창의성: {self.current_temperature or self.ai_service.default_temperature}")
        print(f"  • 시스템 프롬프트: {self.system_prompt[:50] + '...' if self.system_prompt and len(self.system_prompt) > 50 else self.system_prompt or '설정되지 않음'}") 