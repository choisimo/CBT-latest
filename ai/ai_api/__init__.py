"""
AI API 모듈
- FastAPI 기반 웹 서비스
- OpenAI API 서비스 클래스
"""

from .openai_service import OpenAIService
from .api_server import app

__version__ = "1.0.0"
__description__ = "OpenAI GPT API 웹 서비스 모듈"

# 공개 API 정의
__all__ = [
    'OpenAIService',
    'app'
] 