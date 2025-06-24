"""
FastAPI 기반의 AI 분석 서버
- /analyze 엔드포인트를 통해 텍스트 감정 및 해결책 분석
"""

import os
import logging
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv

from ai_api import OpenAIService

# .env 파일에서 환경 변수 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# FastAPI 앱 인스턴스 생성
app = FastAPI(
    title="CBT-Diary AI Analysis Server",
    description="일기 내용을 분석하여 감정과 해결책을 제공하는 API 서버",
    version="1.0.0"
)

# OpenAI 서비스 인스턴스 생성
# API 키는 환경 변수 'OPENAI_API_KEY'에서 자동으로 로드됩니다.
try:
    openai_service = OpenAIService()
    logger.info("OpenAI 서비스가 성공적으로 초기화되었습니다.")
except Exception as e:
    logger.error(f"OpenAI 서비스 초기화 실패: {e}")
    openai_service = None

# 요청 본문을 위한 Pydantic 모델 정의
class AnalysisRequest(BaseModel):
    text: str

# AI 분석 결과를 위한 응답 모델
class AIAnalysisResponse(BaseModel):
    emotion: str
    solution: str

@app.on_event("startup")
async def startup_event():
    """서버 시작 시 실행되는 이벤트"""
    if not openai_service:
        logger.error("OpenAI 서비스가 초기화되지 않아 서버를 시작할 수 없습니다.")
        # 실제 운영 환경에서는 여기서 서버를 강제 종료할 수도 있습니다.
        # import sys; sys.exit(1)
    api_key = os.getenv('OPENAI_API_KEY')
    if not api_key:
        logger.warning("OPENAI_API_KEY 환경변수가 설정되지 않았습니다.")
    else:
        logger.info("OPENAI_API_KEY가 성공적으로 로드되었습니다.")

@app.post("/diary/analyze", response_model=AIAnalysisResponse)
async def analyze_text(request: AnalysisRequest):
    """
    입력된 텍스트를 분석하여 감정과 해결책을 반환합니다.

    - **request**: 'text' 필드를 포함하는 JSON 객체
    - **return**: 'emotion'과 'solution' 필드를 포함하는 JSON 객체
    """
    if not openai_service:
        raise HTTPException(status_code=503, detail="AI 서비스를 사용할 수 없습니다. 서버 설정을 확인하세요.")

    if not request.text or not request.text.strip():
        raise HTTPException(status_code=400, detail="분석할 텍스트를 입력해주세요.")

    logger.info(f"분석 요청 수신: {request.text[:50]}...")

    try:
        # 1. 감정 분석
        emotion_prompt = f"다음 문장에서 드러나는 주요 감정 한 가지를 '기쁨', '슬픔', '분노', '불안', '놀람', '평온' 중 하나로만 답해줘. 문장: '{request.text}'"
        emotion = openai_service.chat(message=emotion_prompt, max_tokens=20).strip()
        logger.info(f"감정 분석 결과: {emotion}")

        # 2. 해결책 제시
        solution_prompt = f"'{request.text}'라는 상황을 겪는 사람에게 인지행동치료(CBT) 관점에서 조언 한 문장을 작성해줘."
        solution = openai_service.chat(message=solution_prompt, max_tokens=200).strip()
        logger.info(f"해결책 제시 결과: {solution[:50]}...")

                # emotion과 solution을 직접 포함하는 응답을 반환합니다.
        return AIAnalysisResponse(emotion=emotion, solution=solution)

    except Exception as e:
        logger.error(f"API 처리 중 오류 발생: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"AI 서버 처리 중 오류 발생: {e}")

@app.get("/health", summary="서버 상태 확인")
async def health_check():
    """서버의 현재 상태를 반환합니다."""
    if openai_service:
        return {"status": "ok", "message": "AI 서비스가 정상적으로 실행 중입니다."}
    else:
        return {"status": "error", "message": "AI 서비스 초기화 실패. 설정을 확인하세요."}

# uvicorn으로 서버를 실행하려면 터미널에서 다음 명령어를 사용하세요:
# uvicorn main:app --host 0.0.0.0 --port 8000 --reload