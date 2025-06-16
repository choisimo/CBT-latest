"""
FastAPI 기반 OpenAI GPT API 서버
백엔드 서비스와 통신하여 GPT 응답을 제공
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
import asyncio
import json
import uuid
import logging
from datetime import datetime

from .openai_service import OpenAIService
from .prompts import DIARY_EMOTION_ANALYSIS_PROMPT

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="ChatGPT Clone AI Service",
    description="OpenAI GPT API를 활용한 AI 채팅 서비스",
    version="1.0.0"
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 개발용, 운영에서는 구체적인 도메인 설정 필요
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# OpenAI 서비스 인스턴스
openai_service = OpenAIService()

# 스트리밍 세션 관리
streaming_sessions: Dict[str, Dict[str, Any]] = {}

class ChatRequest(BaseModel):
    message: str
    conversation_history: Optional[List[Dict[str, str]]] = []
    system_prompt: Optional[str] = DIARY_EMOTION_ANALYSIS_PROMPT
    model: Optional[str] = "gpt-3.5-turbo"
    temperature: Optional[float] = 0.7
    max_tokens: Optional[int] = 4000
    stream: Optional[bool] = False

class ChatResponse(BaseModel):
    response: str
    usage: Optional[Dict[str, Any]] = None
    model: str
    timestamp: str

class StreamingChatRequest(BaseModel):
    message: str
    conversation_history: Optional[List[Dict[str, str]]] = []
    system_prompt: Optional[str] = DIARY_EMOTION_ANALYSIS_PROMPT
    model: Optional[str] = "gpt-3.5-turbo"
    temperature: Optional[float] = 0.7
    max_tokens: Optional[int] = 4000

class StreamingStartResponse(BaseModel):
    stream_id: str
    status: str

@app.get("/")
async def root():
    """루트 엔드포인트 - 서비스 상태 확인"""
    return {
        "service": "ChatGPT Clone AI Service",
        "status": "running",
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat()
    }

@app.get("/health")
async def health_check():
    """헬스 체크 엔드포인트"""
    try:
        # OpenAI API 연결 테스트
        test_response = openai_service.chat("테스트", max_tokens=4000)
        return {
            "status": "healthy",
            "openai_api": "connected",
            "timestamp": datetime.now().isoformat()
        }
    except Exception as e:
        logger.error(f"헬스 체크 실패: {str(e)}")
        return {
            "status": "unhealthy",
            "openai_api": "disconnected",
            "error": str(e),
            "timestamp": datetime.now().isoformat()
        }

@app.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """일반 채팅 API - 전체 응답을 한 번에 반환"""
    try:
        logger.info(f"채팅 요청 - 메시지: {request.message[:50]}...")
        
        # 대화 기록 준비
        messages = []
        
        # 시스템 프롬프트 추가
        if request.system_prompt:
            messages.append({"role": "system", "content": request.system_prompt})
        
        # 이전 대화 기록 추가
        for msg in request.conversation_history:
            messages.append(msg)
        
        # 현재 사용자 메시지 추가
        messages.append({"role": "user", "content": request.message})
        
        # OpenAI API 호출
        response = openai_service.chat_with_history(
            messages=messages,
            model=request.model,
            temperature=request.temperature,
            max_tokens=request.max_tokens
        )
        
        return ChatResponse(
            response=response["content"],
            usage=response.get("usage"),
            model=request.model,
            timestamp=datetime.now().isoformat()
        )
        
    except Exception as e:
        logger.error(f"채팅 처리 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"채팅 처리 중 오류가 발생했습니다: {str(e)}")

@app.post("/chat/stream", response_model=StreamingStartResponse)
async def start_streaming_chat(request: StreamingChatRequest):
    """스트리밍 채팅 시작 - 스트리밍 ID 반환"""
    try:
        stream_id = str(uuid.uuid4())
        logger.info(f"스트리밍 채팅 시작 - Stream ID: {stream_id}")
        
        # 스트리밍 세션 정보 저장
        streaming_sessions[stream_id] = {
            "request": request,
            "status": "started",
            "created_at": datetime.now().isoformat()
        }
        
        return StreamingStartResponse(
            stream_id=stream_id,
            status="started"
        )
        
    except Exception as e:
        logger.error(f"스트리밍 시작 오류: {str(e)}")
        raise HTTPException(status_code=500, detail=f"스트리밍 시작 중 오류가 발생했습니다: {str(e)}")

@app.get("/chat/stream/{stream_id}")
async def stream_chat(stream_id: str):
    """스트리밍 채팅 응답 - SSE 형태로 실시간 응답"""
    if stream_id not in streaming_sessions:
        raise HTTPException(status_code=404, detail="스트리밍 세션을 찾을 수 없습니다")
    
    session = streaming_sessions[stream_id]
    request = session["request"]
    
    async def generate_stream():
        try:
            logger.info(f"스트리밍 응답 시작 - Stream ID: {stream_id}")
            
            # 시작 이벤트
            yield f"event: chunk\ndata: {json.dumps({'type': 'START', 'stream_id': stream_id}, ensure_ascii=False)}\n\n"
            
            # 대화 기록 준비
            messages = []
            
            # 시스템 프롬프트 추가
            if request.system_prompt:
                messages.append({"role": "system", "content": request.system_prompt})
            
            # 이전 대화 기록 추가
            for msg in request.conversation_history:
                messages.append(msg)
            
            # 현재 사용자 메시지 추가
            messages.append({"role": "user", "content": request.message})
            
            # 스트리밍 응답 생성
            full_content = ""
            
            for chunk in openai_service.chat_stream_with_history(
                messages=messages,
                model=request.model,
                temperature=request.temperature,
                max_tokens=request.max_tokens
            ):
                if stream_id not in streaming_sessions:
                    # 세션이 중단된 경우
                    break
                
                if chunk.get("content"):
                    full_content += chunk["content"]
                    
                    # 콘텐츠 청크 전송
                    yield f"event: chunk\ndata: {json.dumps({'type': 'CONTENT', 'content': full_content}, ensure_ascii=False)}\n\n"
                    
                    # 잠시 대기 (브라우저가 처리할 시간)
                    await asyncio.sleep(0.01)
            
            # 완료 이벤트
            yield f"event: chunk\ndata: {json.dumps({'type': 'END', 'content': full_content}, ensure_ascii=False)}\n\n"
            
            # 세션 정리
            if stream_id in streaming_sessions:
                del streaming_sessions[stream_id]
            
            logger.info(f"스트리밍 응답 완료 - Stream ID: {stream_id}")
            
        except Exception as e:
            logger.error(f"스트리밍 응답 오류 - Stream ID: {stream_id}, 오류: {str(e)}")
            
            # 오류 이벤트
            yield f"event: chunk\ndata: {json.dumps({'type': 'ERROR', 'error': str(e)}, ensure_ascii=False)}\n\n"
            
            # 세션 정리
            if stream_id in streaming_sessions:
                del streaming_sessions[stream_id]
    
    return StreamingResponse(
        generate_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"  # nginx에서 버퍼링 비활성화
        }
    )

@app.delete("/chat/stream/{stream_id}")
async def stop_streaming_chat(stream_id: str):
    """스트리밍 채팅 중단"""
    if stream_id in streaming_sessions:
        del streaming_sessions[stream_id]
        logger.info(f"스트리밍 중단 - Stream ID: {stream_id}")
        return {"message": "스트리밍이 중단되었습니다", "stream_id": stream_id}
    else:
        raise HTTPException(status_code=404, detail="스트리밍 세션을 찾을 수 없습니다")

@app.get("/models")
async def get_available_models():
    """사용 가능한 모델 목록 반환"""
    return {
        "models": [
            {"id": "gpt-3.5-turbo", "name": "GPT-3.5 Turbo", "description": "빠르고 효율적인 모델"},
            {"id": "gpt-4", "name": "GPT-4", "description": "가장 강력한 모델"},
            {"id": "gpt-4-turbo-preview", "name": "GPT-4 Turbo", "description": "GPT-4의 빠른 버전"}
        ]
    }

if __name__ == "__main__":
    import uvicorn
    
    logger.info("AI 서비스 시작 중...")
    uvicorn.run(
        "api_server:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    ) 