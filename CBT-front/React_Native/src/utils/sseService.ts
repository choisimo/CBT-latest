// src/utils/sseService.ts

interface SSEEventData {
  id?: string;
  diaryId?: number;
  status?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  emotions?: { category: string; intensity?: number }[];
  coaching?: string;
  errorMessage?: string;
  updatedAt?: string;
}

interface SSECallbacks {
  onConnect?: () => void;
  onStatusUpdate?: (data: SSEEventData) => void;
  onComplete?: (data: SSEEventData) => void;
  onFailed?: (data: SSEEventData) => void;
  onError?: (error: string) => void;
  onDisconnect?: () => void;
}

export class SSEService {
  private isPolling = false;
  private pollInterval: any = null;
  private callbacks: SSECallbacks = {};
  private lastCheckTime = 0;

  constructor(private baseUrl: string, private getToken: () => string | null) { }

  connect(callbacks: SSECallbacks) {
    this.callbacks = callbacks;
    this.isPolling = true;
    this.lastCheckTime = Date.now();

    console.log('실시간 분석 모니터링 시작');
    this.callbacks.onConnect?.();

    // 폴링 방식으로 상태 확인 (SSE 대신 사용)
    this.startPolling();
  }

  private startPolling() {
    if (!this.isPolling) return;

    this.pollInterval = setInterval(async () => {
      if (!this.isPolling) return;

      try {
        // 특정 다이어리의 분석 상태를 확인하는 API 호출
        // 실제로는 다이어리 ID가 필요하지만, 여기서는 전체 최신 분석 상태를 확인
        await this.checkAnalysisStatus();
      } catch (error) {
        console.error('폴링 오류:', error);
      }
    }, 2000); // 2초마다 확인
  }

  private async checkAnalysisStatus() {
    const token = this.getToken();
    if (!token) return;

    try {
      // 최근 분석 상태를 확인하는 API 엔드포인트
      // 이는 백엔드에서 새로 만들어야 할 수도 있습니다
      const response = await fetch(`${this.baseUrl}/api/diaries/recent-analysis`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const data = await response.json();

        // 새로운 업데이트가 있는지 확인 (updatedAt 기준)
        if (data.updatedAt && new Date(data.updatedAt).getTime() > this.lastCheckTime) {
          this.lastCheckTime = new Date(data.updatedAt).getTime();

          switch (data.status) {
            case 'PENDING':
            case 'PROCESSING':
              this.callbacks.onStatusUpdate?.(data);
              break;
            case 'COMPLETED':
              this.callbacks.onComplete?.(data);
              this.stopPolling(); // 완료되면 폴링 중지
              break;
            case 'FAILED':
              this.callbacks.onFailed?.(data);
              this.stopPolling(); // 실패해도 폴링 중지
              break;
          }
        }
      }
    } catch (error) {
      console.error('분석 상태 확인 오류:', error);
    }
  }

  private stopPolling() {
    this.isPolling = false;
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
      this.pollInterval = null;
    }
  }

  disconnect() {
    this.stopPolling();
    console.log('실시간 모니터링 중지');
    this.callbacks.onDisconnect?.();
  }

  isConnected(): boolean {
    return this.isPolling;
  }

  // 특정 다이어리 ID에 대한 분석 상태 모니터링
  monitorDiary(diaryId: number) {
    this.lastCheckTime = Date.now();

    const pollForSpecificDiary = async () => {
      if (!this.isPolling) return;

      try {
        const token = this.getToken();
        if (!token) return;

        const response = await fetch(`${this.baseUrl}/api/diaries/${diaryId}/analysis`, {
          method: 'GET',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        });

        if (response.ok) {
          const data = await response.json();

          // 빈 객체가 아닌 경우 (분석 완료)
          if (data && Object.keys(data).length > 0 && data.status) {
            switch (data.status) {
              case 'PENDING':
              case 'PROCESSING':
                this.callbacks.onStatusUpdate?.(data);
                setTimeout(pollForSpecificDiary, 2000);
                break;
              case 'COMPLETED':
                this.callbacks.onComplete?.(data);
                this.stopPolling();
                break;
              case 'FAILED':
                this.callbacks.onFailed?.(data);
                this.stopPolling();
                break;
            }
          } else {
            // 아직 분석이 시작되지 않았거나 진행 중
            setTimeout(pollForSpecificDiary, 2000);
          }
        }
      } catch (error) {
        console.error('다이어리 분석 상태 확인 오류:', error);
        setTimeout(pollForSpecificDiary, 3000); // 오류 시 3초 후 재시도
      }
    };

    pollForSpecificDiary();
  }
}

// 싱글톤 인스턴스
let sseServiceInstance: SSEService | null = null;

export const createSSEService = (baseUrl: string, getToken: () => string | null): SSEService => {
  if (!sseServiceInstance) {
    sseServiceInstance = new SSEService(baseUrl, getToken);
  }
  return sseServiceInstance;
};

export const getSSEService = (): SSEService | null => {
  return sseServiceInstance;
};
