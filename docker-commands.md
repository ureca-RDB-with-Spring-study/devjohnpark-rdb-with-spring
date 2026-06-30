# Docker 핵심 명령어

## 이미지

```bash
docker pull <image>          # 이미지 다운로드
docker images                # 이미지 목록
docker rmi <image>           # 이미지 삭제
docker build -t <name> .     # Dockerfile로 이미지 빌드
```

## 컨테이너 실행

```bash
docker run <image>                        # 컨테이너 실행
docker run -d <image>                     # 백그라운드 실행
docker run -p 8080:80 <image>             # 포트 매핑 (호스트:컨테이너)
docker run -v /host:/container <image>    # 볼륨 마운트
docker run -e KEY=VALUE <image>           # 환경변수 설정
docker run --name <name> <image>          # 이름 지정
docker run --rm <image>                   # 종료 시 자동 삭제
```

## 컨테이너 관리

```bash
docker ps                    # 실행 중인 컨테이너
docker ps -a                 # 전체 컨테이너
docker stop <container>      # 정지
docker start <container>     # 시작
docker restart <container>   # 재시작
docker rm <container>        # 삭제
docker rm -f <container>     # 강제 삭제
```

## 컨테이너 내부

```bash
docker exec -it <container> bash    # 컨테이너 내부 진입
docker logs <container>             # 로그 확인
docker logs -f <container>          # 로그 실시간 스트림
docker inspect <container>          # 상세 정보
docker cp <src> <container>:<dest>  # 파일 복사
```

## 정리

```bash
docker system prune          # 미사용 리소스 전체 삭제
docker image prune           # 미사용 이미지 삭제
docker container prune       # 정지된 컨테이너 삭제
```

## Docker Compose

```bash
docker compose up -d         # 서비스 시작
docker compose down          # 서비스 중지 + 컨테이너 삭제
docker compose ps            # 서비스 상태
docker compose logs -f       # 로그 스트림
docker compose build         # 이미지 빌드
```
