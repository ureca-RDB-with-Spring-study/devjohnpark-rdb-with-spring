# AWS 인프라 제어 스크립트

smartclearance 프로젝트의 EC2/RDS 인스턴스를 시작/정지/조회하는 스크립트.

## 사용법

### 개별 인스턴스 제어

```bash
# Spring Boot EC2
./scripts/app/start.sh    # 시작
./scripts/app/stop.sh     # 정지
./scripts/app/status.sh   # 상태

# K6 EC2
./scripts/k6/start.sh
./scripts/k6/stop.sh
./scripts/k6/status.sh

# RDS
./scripts/rds/start.sh
./scripts/rds/stop.sh
./scripts/rds/status.sh
```

### 통합 제어 (모두 한 번에)

```bash
./scripts/all/start.sh    # 모두 시작
./scripts/all/stop.sh     # 모두 정지
./scripts/all/status.sh   # 모두 상태
```

## 사전 요건

- AWS CLI 설치 + `aws configure` 완료
- 적절한 IAM 권한 (EC2/RDS 시작/정지)
- bash 4.0+ (macOS 기본 zsh 환경에서도 동작)

## 주의사항

- EC2 정지 후 시작 시 **퍼블릭 IP가 변경됨** (Elastic IP 미사용 시)
- RDS는 정지 후 **7일 경과 시 자동 재시작**
- 정지 중에도 EBS/스토리지 비용은 발생 (월 $5 이내)

## 트러블슈팅

| 증상 | 해결 |
|---|---|
| `command not found: aws` | AWS CLI 설치 |
| `Unable to locate credentials` | `aws configure` 실행 |
| `IncorrectInstanceState` | 이미 정지/시작 중. 1~2분 대기 후 재시도 |
