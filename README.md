 ### 1. Docker Desktop 실행

  먼저 Windows에서 Docker Desktop을 켜고 Docker 엔진이 실행될 때까지 기다리세요.

  확인:

  docker version
  docker compose version

  ### 2. 프로젝트 실행

  PowerShell에서 아래 명령을 그대로 실행하세요.

  cd C:\Users\clfrj\saas_create

  $env:FORGEFLOW_DB_PASSWORD="forgeflow_test_local_only"
  $env:FORGEFLOW_GATEWAY_TOKEN="forgeflow_gateway_test_only"

  docker compose up --build

  이 명령 하나로 다음 3개가 모두 실행됩니다.

  - PostgreSQL DB
  - Spring Boot 백엔드
  - React 프론트엔드

  처음 실행할 때는 이미지를 내려받고 빌드하므로 시간이 조금 걸립니다.

  ### 3. 브라우저 접속

  컨테이너 실행 후 접속하세요.

  - 화면: http://localhost:8080
  - 백엔드 상태: http://localhost:8080/actuator/health

  정상이면 상태 주소에서 대략 다음 결과가 나옵니다.

  {"status":"UP"}

  ### 4. 백그라운드로 실행하려면

  터미널을 계속 차지하지 않게 하려면:

  docker compose up -d --build

  상태 확인:

  docker compose ps

  로그 확인:

  docker compose logs -f

  특정 서비스 로그만 보려면:

  docker compose logs -f api
  docker compose logs -f web
  docker compose logs -f db

  로그 화면에서 빠져나올 때는 Ctrl+C를 누르면 됩니다. 백그라운드 컨테이너는 계속 실행됩니다.

  ### 5. 전체 자동 테스트

  서비스가 실행 중인 상태에서 새로운 PowerShell 창을 열고:

  cd C:\Users\clfrj\saas_create
  powershell -ExecutionPolicy Bypass -File scripts\verify-all.ps1

  이 테스트는 다음을 한 번에 검사합니다.

  - 정적 검증
  - 화면 및 이벤트 연결
  - E2E 동작
  - DB 데이터 저장
  - 백엔드 상태
  - 보안 헤더

  마지막에 "status":"PASS"가 나오면 전체 테스트가 통과한 것입니다.

  ### 6. 종료

  docker compose down