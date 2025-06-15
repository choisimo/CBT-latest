"""
OpenAI GPT API 사용 예제들
- 기본 채팅 예제
- 스트리밍 채팅 예제
- 커스텀 프롬프트 예제
- 다양한 사용 사례
"""

from ai.ai_api.openai_service import OpenAIService
import time
import asyncio
from typing import List

def example_basic_chat():
    """기본 채팅 예제"""
    print("=== 기본 채팅 예제 ===")
    print("✨ 일반적인 질문-답변 방식의 채팅")
    print()
    
    try:
        # OpenAI 서비스 초기화
        ai_service = OpenAIService()
        
        # 간단한 대화
        print("👤 사용자: 안녕하세요! 파이썬에 대해 간단히 설명해주세요.")
        response = ai_service.chat("안녕하세요! 파이썬에 대해 간단히 설명해주세요.")
        print(f"🤖 AI: {response}")
        print()
        
        # 후속 대화 (이전 대화 기록 포함)
        print("👤 사용자: 파이썬의 주요 특징 3가지만 알려주세요.")
        response = ai_service.chat("파이썬의 주요 특징 3가지만 알려주세요.")
        print(f"🤖 AI: {response}")
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def example_streaming_chat():
    """스트리밍 채팅 예제"""
    print("=== 스트리밍 채팅 예제 ===")
    print("🔄 실시간으로 AI 응답을 받아보는 예제")
    print()
    
    try:
        ai_service = OpenAIService()
        
        print("👤 사용자: 머신러닝과 딥러닝의 차이점을 자세히 설명해주세요.")
        print("🤖 AI: ", end="", flush=True)
        
        # 스트리밍으로 응답 받기
        for chunk in ai_service.chat_stream("머신러닝과 딥러닝의 차이점을 자세히 설명해주세요."):
            print(chunk, end="", flush=True)
            time.sleep(0.05)  # 타이핑 효과
        print()
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def example_custom_prompt():
    """커스텀 시스템 프롬프트 예제"""
    print("=== 커스텀 시스템 프롬프트 예제 ===")
    print("🎯 시스템 프롬프트로 AI 성격과 답변 스타일 조정")
    print()
    
    try:
        ai_service = OpenAIService()
        
        # 1. 전문가 모드
        print("📚 전문가 모드:")
        expert_prompt = """
        당신은 컴퓨터 과학 전문가입니다.
        기술적인 질문에 대해 정확하고 상세한 답변을 제공하며,
        필요시 코드 예제와 함께 설명합니다.
        """
        
        print("👤 사용자: REST API란 무엇인가요?")
        response = ai_service.chat(
            "REST API란 무엇인가요?",
            system_prompt=expert_prompt
        )
        print(f"🤖 AI: {response}")
        print()
        
        # 대화 기록 초기화
        ai_service.clear_conversation()
        
        # 2. 친근한 튜터 모드
        print("👨‍🏫 친근한 튜터 모드:")
        tutor_prompt = """
        당신은 친근하고 인내심 있는 프로그래밍 튜터입니다.
        초보자도 이해하기 쉽게 단계별로 설명하고,
        격려하는 말투로 대화합니다.
        """
        
        print("👤 사용자: 프로그래밍을 처음 시작하는데 어떻게 공부하면 좋을까요?")
        response = ai_service.chat(
            "프로그래밍을 처음 시작하는데 어떻게 공부하면 좋을까요?",
            system_prompt=tutor_prompt
        )
        print(f"🤖 AI: {response}")
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def example_different_models():
    """다른 AI 모델 사용 예제"""
    print("=== 다양한 AI 모델 사용 예제 ===")
    print("🔧 GPT-3.5와 GPT-4 모델 비교")
    print()
    
    try:
        ai_service = OpenAIService()
        question = "창의적인 단편소설 아이디어를 하나 제안해주세요."
        
        print("👤 사용자:", question)
        print()
        
        # GPT-3.5 사용
        print("🤖 GPT-3.5 응답:")
        response_35 = ai_service.chat(question, model="gpt-3.5-turbo")
        print(response_35)
        print()
        
        # 대화 기록 초기화
        ai_service.clear_conversation()
        
        # GPT-4 사용 (API 키에 GPT-4 권한이 있을 때만)
        print("🤖 GPT-4 응답:")
        try:
            response_4 = ai_service.chat(question, model="gpt-4")
            print(response_4)
        except Exception as e:
            print(f"GPT-4 사용 불가: {str(e)}")
            print("(GPT-4 API 접근 권한이 필요합니다)")
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def example_temperature_variations():
    """창의성 수준(temperature) 변화 예제"""
    print("=== 창의성 수준 변화 예제 ===")
    print("🌡️ Temperature 값에 따른 응답 변화 확인")
    print()
    
    try:
        ai_service = OpenAIService()
        question = "행복에 대한 짧은 시를 써주세요."
        
        print("👤 사용자:", question)
        print()
        
        temperatures = [0.2, 0.7, 1.0]
        labels = ["보수적 (0.2)", "균형적 (0.7)", "창의적 (1.0)"]
        
        for temp, label in zip(temperatures, labels):
            print(f"🎨 {label}:")
            response = ai_service.chat(question, temperature=temp)
            print(response)
            print()
            
            # 대화 기록 초기화
            ai_service.clear_conversation()
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def example_conversation_management():
    """대화 기록 관리 예제"""
    print("=== 대화 기록 관리 예제 ===")
    print("💾 대화 저장, 불러오기, 통계 확인")
    print()
    
    try:
        ai_service = OpenAIService()
        
        # 여러 메시지 주고받기
        messages = [
            "안녕하세요!",
            "파이썬에 대해 알려주세요.",
            "파이썬으로 할 수 있는 프로젝트는 뭐가 있나요?",
            "웹 개발도 가능한가요?"
        ]
        
        print("📝 대화 시뮬레이션:")
        for msg in messages:
            print(f"👤 사용자: {msg}")
            response = ai_service.chat(msg)
            print(f"🤖 AI: {response[:100]}..." if len(response) > 100 else f"🤖 AI: {response}")
            print()
        
        # 대화 통계
        stats = ai_service.get_conversation_summary()
        print("📊 대화 통계:")
        print(f"  • 전체 메시지: {stats['total_messages']}개")
        print(f"  • 사용자 메시지: {stats['user_messages']}개")
        print(f"  • AI 메시지: {stats['ai_messages']}개")
        print()
        
        # 대화 저장
        filename = "example_conversation.json"
        ai_service.save_conversation(filename)
        print(f"💾 대화가 '{filename}' 파일에 저장되었습니다.")
        print()
        
        # 대화 기록 초기화 후 불러오기
        ai_service.clear_conversation()
        print("🔄 대화 기록 초기화됨")
        
        ai_service.load_conversation(filename)
        print(f"📂 '{filename}' 파일에서 대화 기록을 불러왔습니다.")
        
        # 불러온 후 통계 확인
        stats = ai_service.get_conversation_summary()
        print("📊 불러온 대화 통계:")
        print(f"  • 전체 메시지: {stats['total_messages']}개")
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def example_title_generation():
    """제목 생성 예제"""
    print("=== 제목 생성 예제 ===")
    print("📝 대화 내용을 바탕으로 자동 제목 생성")
    print()
    
    try:
        ai_service = OpenAIService()
        
        # 대화 시작
        ai_service.chat("안녕하세요! 파이썬 데이터 분석에 대해 궁금합니다.")
        ai_service.chat("pandas 라이브러리의 주요 기능을 알려주세요.")
        ai_service.chat("데이터 시각화는 어떻게 하나요?")
        
        # 첫 번째 메시지를 미리보기로 사용
        preview = ai_service.get_conversation_history()[0]["content"]
        title = ai_service.generate_title(preview)
        
        print(f"💡 생성된 제목: '{title}'")
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def example_code_assistant():
    """코드 어시스턴트 예제"""
    print("=== 코드 어시스턴트 예제 ===")
    print("💻 프로그래밍 도움을 받는 예제")
    print()
    
    try:
        ai_service = OpenAIService()
        
        # 코딩 전문가 프롬프트
        coding_prompt = """
        당신은 숙련된 파이썬 개발자입니다.
        코드 예제를 제공할 때는 주석을 포함하여 설명하고,
        best practice를 따르는 깔끔한 코드를 작성합니다.
        """
        
        questions = [
            "파이썬으로 간단한 계산기 클래스를 만들어주세요.",
            "이 코드에 예외 처리를 추가해주세요.",
            "단위 테스트도 작성해주세요."
        ]
        
        for question in questions:
            print(f"👤 사용자: {question}")
            response = ai_service.chat(question, system_prompt=coding_prompt)
            print(f"🤖 AI: {response}")
            print("-" * 50)
        
    except Exception as e:
        print(f"❌ 오류: {str(e)}")

def run_all_examples():
    """모든 예제 실행"""
    examples = [
        example_basic_chat,
        example_streaming_chat,
        example_custom_prompt,
        example_different_models,
        example_temperature_variations,
        example_conversation_management,
        example_title_generation,
        example_code_assistant
    ]
    
    print("🚀 OpenAI GPT API 모든 예제 실행")
    print("=" * 60)
    
    for i, example_func in enumerate(examples, 1):
        print(f"\n[{i}/{len(examples)}] {example_func.__doc__}")
        print("=" * 60)
        
        try:
            example_func()
        except Exception as e:
            print(f"❌ 예제 실행 중 오류: {str(e)}")
        
        if i < len(examples):
            input("\n⏸️ 다음 예제로 진행하려면 Enter를 누르세요...")
    
    print("\n✅ 모든 예제 실행 완료!")

if __name__ == "__main__":
    """직접 실행 시 예제 선택 메뉴 표시"""
    print("📚 OpenAI GPT API 예제 모음")
    print("=" * 40)
    
    examples_menu = {
        "1": ("기본 채팅", example_basic_chat),
        "2": ("스트리밍 채팅", example_streaming_chat),
        "3": ("커스텀 프롬프트", example_custom_prompt),
        "4": ("다양한 모델", example_different_models),
        "5": ("창의성 수준 변화", example_temperature_variations),
        "6": ("대화 기록 관리", example_conversation_management),
        "7": ("제목 생성", example_title_generation),
        "8": ("코드 어시스턴트", example_code_assistant),
        "9": ("모든 예제 실행", run_all_examples)
    }
    
    print("실행할 예제를 선택하세요:")
    for key, (name, _) in examples_menu.items():
        print(f"  {key}. {name}")
    
    try:
        choice = input("\n선택 (1-9): ").strip()
        
        if choice in examples_menu:
            name, func = examples_menu[choice]
            print(f"\n▶️ {name} 예제 실행 중...")
            print("=" * 50)
            func()
        else:
            print("❌ 올바른 선택지를 입력해주세요.")
            
    except Exception as e:
        print(f"❌ 오류: {str(e)}")
        print("OpenAI API 키가 올바르게 설정되었는지 확인해주세요.") 