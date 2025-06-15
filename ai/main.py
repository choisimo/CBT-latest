"""
OpenAI GPT API 메인 실행 파일
- 통합 메뉴 제공
- 다양한 모드 선택
- 환경 설정 확인
"""

import os
import sys
import logging
from ai.ai_api import OpenAIService
from ai.ai_cli import (
    ChatBot, AdvancedChatBot,
    example_basic_chat, example_streaming_chat, example_custom_prompt,
    example_different_models, example_temperature_variations,
    example_conversation_management, example_title_generation,
    example_code_assistant, run_all_examples
)

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(levelname)s: %(message)s')
logger = logging.getLogger(__name__)

def check_environment():
    """환경 설정 확인"""
    api_key = os.getenv('OPENAI_API_KEY')
    
    if not api_key:
        print("⚠️  경고: OPENAI_API_KEY 환경변수가 설정되지 않았습니다.")
        print()
        print("📋 API 키 설정 방법:")
        print("  1. OpenAI 플랫폼에서 API 키 발급: https://platform.openai.com/api-keys")
        print("  2. 환경변수 설정:")
        print("     • Windows: set OPENAI_API_KEY=your-api-key-here")
        print("     • Linux/Mac: export OPENAI_API_KEY='your-api-key-here'")
        print("  3. 또는 .env 파일에 OPENAI_API_KEY=your-api-key-here 추가")
        print()
        
        choice = input("❓ API 키 없이 계속하시겠습니까? (y/N): ").lower()
        if choice != 'y':
            print("👋 프로그램을 종료합니다.")
            sys.exit(0)
        return False
    
    print("✅ OpenAI API 키가 설정되었습니다.")
    return True

def show_main_menu():
    """메인 메뉴 표시"""
    print("🚀 OpenAI GPT API 통합 프로그램")
    print("=" * 50)
    print()
    print("📋 사용 가능한 모드:")
    print("  1. 🤖 기본 챗봇 - 간단한 대화형 인터페이스")
    print("  2. 🚀 고급 챗봇 - 설정 변경 가능한 인터페이스")
    print("  3. 📚 예제 모음 - 다양한 사용 예제 실행")
    print("  4. ⚙️  환경 설정 확인")
    print("  5. ❓ 도움말")
    print("  6. 👋 종료")
    print()

def run_basic_chatbot():
    """기본 챗봇 실행"""
    print("🤖 기본 챗봇 모드를 시작합니다...")
    try:
        chatbot = ChatBot()
        chatbot.start_chat()
    except Exception as e:
        print(f"❌ 챗봇 실행 오류: {str(e)}")

def run_advanced_chatbot():
    """고급 챗봇 실행"""
    print("🚀 고급 챗봇 모드를 시작합니다...")
    try:
        chatbot = AdvancedChatBot()
        chatbot.start_chat()
    except Exception as e:
        print(f"❌ 고급 챗봇 실행 오류: {str(e)}")

def run_examples_menu():
    """예제 메뉴 실행"""
    while True:
        print("\n📚 예제 모음")
        print("=" * 30)
        
        examples_menu = {
            "1": ("기본 채팅", example_basic_chat),
            "2": ("스트리밍 채팅", example_streaming_chat),
            "3": ("커스텀 프롬프트", example_custom_prompt),
            "4": ("다양한 모델", example_different_models),
            "5": ("창의성 수준 변화", example_temperature_variations),
            "6": ("대화 기록 관리", example_conversation_management),
            "7": ("제목 생성", example_title_generation),
            "8": ("코드 어시스턴트", example_code_assistant),
            "9": ("모든 예제 실행", run_all_examples),
            "0": ("메인 메뉴로 돌아가기", None)
        }
        
        print("실행할 예제를 선택하세요:")
        for key, (name, _) in examples_menu.items():
            print(f"  {key}. {name}")
        
        try:
            choice = input("\n선택 (0-9): ").strip()
            
            if choice == "0":
                break
            elif choice in examples_menu and examples_menu[choice][1]:
                name, func = examples_menu[choice]
                print(f"\n▶️ {name} 예제 실행 중...")
                print("=" * 50)
                func()
                input("\n⏸️ 메인 메뉴로 돌아가려면 Enter를 누르세요...")
            else:
                print("❌ 올바른 선택지를 입력해주세요.")
                
        except Exception as e:
            print(f"❌ 오류: {str(e)}")

def show_environment_info():
    """환경 정보 표시"""
    print("\n⚙️  환경 설정 정보")
    print("=" * 30)
    
    # API 키 확인
    api_key = os.getenv('OPENAI_API_KEY')
    if api_key:
        masked_key = api_key[:8] + "..." + api_key[-4:] if len(api_key) > 12 else "설정됨"
        print(f"🔑 OpenAI API 키: {masked_key}")
    else:
        print("🔑 OpenAI API 키: ❌ 설정되지 않음")
    
    # 패키지 정보
    try:
        import openai
        print(f"📦 OpenAI 패키지 버전: {openai.__version__}")
    except Exception as e:
        print(f"📦 OpenAI 패키지: ❌ 설치되지 않음 ({str(e)})")
    
    # Python 버전
    print(f"🐍 Python 버전: {sys.version.split()[0]}")
    
    # 작업 디렉토리
    print(f"📁 현재 디렉토리: {os.getcwd()}")
    
    # 테스트 연결
    if api_key:
        print("\n🔍 OpenAI API 연결 테스트 중...")
        try:
            service = OpenAIService()
            test_response = service.chat("안녕하세요!", max_tokens=10)
            print("✅ API 연결 성공!")
        except Exception as e:
            print(f"❌ API 연결 실패: {str(e)}")

def show_help():
    """도움말 표시"""
    print("\n❓ 도움말")
    print("=" * 20)
    print()
    print("🎯 이 프로그램의 기능:")
    print("  • OpenAI GPT API를 활용한 다양한 AI 채팅 기능")
    print("  • 기본/고급 챗봇 인터페이스")
    print("  • 다양한 사용 예제와 튜토리얼")
    print()
    print("📋 시작하기 전에:")
    print("  1. OpenAI API 키 필요 (https://platform.openai.com)")
    print("  2. 환경변수 OPENAI_API_KEY 설정")
    print("  3. 필요한 패키지 설치: pip install -r requirements.txt")
    print()
    print("🚀 추천 사용 순서:")
    print("  1. 환경 설정 확인 (메뉴 4)")
    print("  2. 예제 모음에서 기본 채팅 체험 (메뉴 3 → 1)")
    print("  3. 기본 챗봇으로 대화 시작 (메뉴 1)")
    print("  4. 고급 기능이 필요하면 고급 챗봇 사용 (메뉴 2)")
    print()
    print("💡 팁:")
    print("  • 스트리밍 기능으로 실시간 응답 확인 가능")
    print("  • 시스템 프롬프트로 AI 성격 조정 가능")
    print("  • 대화 기록 저장/불러오기 기능 제공")
    print("  • 다양한 AI 모델 선택 가능 (GPT-3.5, GPT-4 등)")

def main():
    """메인 함수"""
    # 환경 확인
    check_environment()
    
    while True:
        try:
            show_main_menu()
            choice = input("선택 (1-6): ").strip()
            
            if choice == "1":
                run_basic_chatbot()
                
            elif choice == "2":
                run_advanced_chatbot()
                
            elif choice == "3":
                run_examples_menu()
                
            elif choice == "4":
                show_environment_info()
                input("\n📄 메인 메뉴로 돌아가려면 Enter를 누르세요...")
                
            elif choice == "5":
                show_help()
                input("\n📄 메인 메뉴로 돌아가려면 Enter를 누르세요...")
                
            elif choice == "6":
                print("👋 프로그램을 종료합니다. 감사합니다!")
                break
                
            else:
                print("❌ 올바른 메뉴 번호를 입력해주세요.")
                input("⏸️ 계속하려면 Enter를 누르세요...")
                
        except KeyboardInterrupt:
            print("\n\n👋 사용자가 프로그램을 종료했습니다.")
            break
        except Exception as e:
            print(f"❌ 예상치 못한 오류: {str(e)}")
            logger.error(f"메인 프로그램 오류: {str(e)}", exc_info=True)
            input("⏸️ 계속하려면 Enter를 누르세요...")

if __name__ == "__main__":
    main() 