"""
가상환경 자동 설정 스크립트
- Python venv 생성
- 필요한 패키지 설치
- 환경변수 파일 생성 도움말
"""

import os
import sys
import subprocess
import platform

def run_command(command, description=""):
    """명령어 실행 및 결과 확인"""
    print(f"🔄 {description}")
    print(f"💻 실행: {command}")
    
    try:
        result = subprocess.run(command, shell=True, check=True, capture_output=True, text=True)
        if result.stdout:
            print(result.stdout)
        print("✅ 성공!")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ 오류: {e}")
        if e.stderr:
            print(f"상세 오류: {e.stderr}")
        return False

def check_python_version():
    """Python 버전 확인"""
    version = sys.version_info
    print(f"🐍 Python 버전: {version.major}.{version.minor}.{version.micro}")
    
    if version.major < 3 or (version.major == 3 and version.minor < 8):
        print("⚠️  경고: Python 3.8 이상을 권장합니다.")
        return False
    
    print("✅ Python 버전이 적합합니다.")
    return True

def create_venv():
    """가상환경 생성"""
    venv_path = "venv"
    
    if os.path.exists(venv_path):
        print(f"⚠️  '{venv_path}' 폴더가 이미 존재합니다.")
        choice = input("기존 가상환경을 삭제하고 새로 만드시겠습니까? (y/N): ").lower()
        
        if choice == 'y':
            print("🗑️  기존 가상환경 삭제 중...")
            if platform.system() == "Windows":
                run_command(f"rmdir /s /q {venv_path}", "기존 venv 폴더 삭제")
            else:
                run_command(f"rm -rf {venv_path}", "기존 venv 폴더 삭제")
        else:
            print("⏭️  기존 가상환경을 유지합니다.")
            return True
    
    return run_command(f"python -m venv {venv_path}", "가상환경 생성")

def get_activation_command():
    """OS별 가상환경 활성화 명령어 반환"""
    if platform.system() == "Windows":
        return "venv\\Scripts\\activate"
    else:
        return "source venv/bin/activate"

def install_packages():
    """패키지 설치"""
    system = platform.system()
    
    if system == "Windows":
        pip_command = "venv\\Scripts\\pip"
    else:
        pip_command = "venv/bin/pip"
    
    # pip 업그레이드
    if not run_command(f"{pip_command} install --upgrade pip", "pip 업그레이드"):
        return False
    
    # 패키지 설치
    return run_command(f"{pip_command} install -r requirements.txt", "필요한 패키지 설치")

def create_env_file():
    """환경변수 파일 생성"""
    env_file = ".env"
    
    if os.path.exists(env_file):
        print(f"⚠️  '{env_file}' 파일이 이미 존재합니다.")
        return True
    
    print("📝 환경변수 파일(.env) 생성 중...")
    
    try:
        with open(env_file, 'w', encoding='utf-8') as f:
            f.write("# OpenAI API 설정\n")
            f.write("# OpenAI 플랫폼에서 발급받은 API 키를 입력하세요\n")
            f.write("# https://platform.openai.com/api-keys\n")
            f.write("OPENAI_API_KEY=your-api-key-here\n")
            f.write("\n")
            f.write("# 기타 설정 (선택사항)\n")
            f.write("# DEFAULT_MODEL=gpt-3.5-turbo\n")
            f.write("# DEFAULT_TEMPERATURE=0.7\n")
            f.write("# DEFAULT_MAX_TOKENS=2000\n")
        
        print("✅ .env 파일이 생성되었습니다!")
        print("📋 다음 단계:")
        print("   1. .env 파일을 열어서 your-api-key-here 부분을")
        print("      실제 OpenAI API 키로 변경하세요.")
        print("   2. API 키는 https://platform.openai.com/api-keys 에서 발급받을 수 있습니다.")
        
        return True
        
    except Exception as e:
        print(f"❌ .env 파일 생성 실패: {e}")
        return False

def show_usage_instructions():
    """사용법 안내"""
    activation_cmd = get_activation_command()
    
    print("\n" + "="*60)
    print("🎉 가상환경 설정 완료!")
    print("="*60)
    print()
    print("📋 사용 방법:")
    print()
    print("1️⃣ 가상환경 활성화:")
    if platform.system() == "Windows":
        print(f"   {activation_cmd}")
    else:
        print(f"   {activation_cmd}")
    
    print()
    print("2️⃣ OpenAI API 키 설정:")
    print("   .env 파일을 열어서 API 키를 입력하세요")
    print()
    print("3️⃣ 프로그램 실행:")
    print("   python main.py")
    print()
    print("4️⃣ 가상환경 비활성화 (작업 완료 후):")
    print("   deactivate")
    print()
    print("💡 팁:")
    print("   • 앞으로 작업할 때마다 1단계(가상환경 활성화)부터 시작하세요")
    print("   • 터미널 프롬프트 앞에 (venv)가 표시되면 가상환경이 활성화된 상태입니다")
    print("   • requirements.txt가 업데이트되면 다시 'pip install -r requirements.txt' 실행")

def main():
    """메인 실행 함수"""
    print("🚀 OpenAI GPT API 프로젝트 가상환경 설정")
    print("="*50)
    print()
    
    # Python 버전 확인
    if not check_python_version():
        print("❌ Python 버전을 확인하고 다시 시도해주세요.")
        return False
    
    print()
    
    # 가상환경 생성
    if not create_venv():
        print("❌ 가상환경 생성에 실패했습니다.")
        return False
    
    print()
    
    # 패키지 설치
    if not install_packages():
        print("❌ 패키지 설치에 실패했습니다.")
        return False
    
    print()
    
    # 환경변수 파일 생성
    create_env_file()
    
    print()
    
    # 사용법 안내
    show_usage_instructions()
    
    return True

if __name__ == "__main__":
    try:
        success = main()
        if not success:
            print("\n❌ 설정 과정에서 오류가 발생했습니다.")
            print("수동으로 설정을 진행해주세요.")
        
    except KeyboardInterrupt:
        print("\n\n⏹️  사용자가 설정을 중단했습니다.")
    except Exception as e:
        print(f"\n❌ 예상치 못한 오류: {e}")
        print("수동으로 가상환경을 설정해주세요.") 