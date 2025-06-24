"""
AI CLI 모듈
- 터미널 기반 대화형 인터페이스
- 다양한 사용 예제
"""

from .chatbot import ChatBot, AdvancedChatBot
from .examples import (
    example_basic_chat,
    example_streaming_chat,
    example_custom_prompt,
    example_different_models,
    example_temperature_variations,
    example_conversation_management,
    example_title_generation,
    example_code_assistant,
    run_all_examples
)

__version__ = "1.0.0"
__description__ = "OpenAI GPT API CLI 도구 모듈"

# 공개 API 정의
__all__ = [
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