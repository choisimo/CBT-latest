"""
OpenAI GPT API 패키지
- 분리된 모듈들을 편리하게 import할 수 있도록 제공
"""

# 핵심 클래스들을 패키지 레벨에서 직접 import 가능하게 설정
from .ai_api.openai_service import OpenAIService
from .ai_cli.chatbot import ChatBot, AdvancedChatBot
from .ai_cli.examples import *

# 패키지 정보
__version__ = "1.0.0"
__author__ = "OpenAI GPT API 예제"
__description__ = "OpenAI GPT API를 활용한 파이썬 채팅 서비스"

# 공개 API 정의
__all__ = [
    'OpenAIService',
    'ChatBot', 
    'AdvancedChatBot',
    'example_basic_chat',
    'example_streaming_chat',
    'example_custom_prompt',
    'example_different_models',
    'example_temperature_variations',
    'example_conversation_management',
    'example_title_generation',
    'example_code_assistant',
    'run_all_examples'
] 