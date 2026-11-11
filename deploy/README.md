# EC2 배포 설정

`.github/workflows/deploy.yml`이 `develop`/`main`에 push될 때마다 빌드·테스트 후 이 EC2로 jar를 올리고 재시작합니다. 아래는 **EC2 인스턴스를 처음 만들 때 한 번만** 하면 되는 설정입니다.

## 1. EC2에서 할 일 (SSH로 접속해서)

```bash
# Java 17 설치 (Amazon Linux 2023 기준)
sudo dnf install -y java-17-amazon-corretto

# 앱 디렉터리 준비
sudo mkdir -p /opt/decisionlab
sudo chown ec2-user:ec2-user /opt/decisionlab
```

`/opt/decisionlab/app.env` 파일을 만들고 DB 비밀번호와 LLM API 키를 넣습니다 (README의 "배포 계획"대로 서버 환경변수로만 관리 — 이 파일은 깃에 올리지 않습니다):

```bash
sudo tee /opt/decisionlab/app.env > /dev/null <<'EOF'
SPRING_DATASOURCE_URL=jdbc:mysql://<RDS 엔드포인트>:3306/decision_lab
SPRING_DATASOURCE_USERNAME=dl_user
SPRING_DATASOURCE_PASSWORD=<RDS 비밀번호>
SPRING_AI_ANTHROPIC_API_KEY=<배포용 Anthropic API 키>
SPRING_AI_ANTHROPIC_CHAT_OPTIONS_MODEL=claude-sonnet-5-5
EOF
sudo chmod 600 /opt/decisionlab/app.env
```

이 저장소의 `deploy/decisionlab.service`를 EC2로 복사하고 등록합니다 (Ubuntu AMI를 쓴다면 파일의 `User=ec2-user`를 `User=ubuntu`로 바꾸세요):

```bash
sudo cp decisionlab.service /etc/systemd/system/decisionlab.service
sudo systemctl daemon-reload
sudo systemctl enable decisionlab
```

GitHub Actions가 배포 시 `sudo systemctl restart decisionlab`을 실행하므로, 배포 계정이 비밀번호 없이 그 명령만 쓸 수 있게 해 둡니다:

```bash
echo "ec2-user ALL=(ALL) NOPASSWD: /bin/systemctl restart decisionlab, /bin/systemctl is-active --quiet decisionlab" \
  | sudo tee /etc/sudoers.d/decisionlab-deploy
```

## 2. GitHub Actions Secrets (리포지토리 Settings → Secrets and variables → Actions)

| Secret | 값 |
|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | `ec2-user` (Amazon Linux) 또는 `ubuntu` |
| `EC2_SSH_KEY` | EC2 접속용 프라이빗 키(.pem) 전체 내용 |

DB 비밀번호와 Anthropic API 키는 GitHub Secrets가 아니라 **EC2의 `/opt/decisionlab/app.env`에만** 둡니다 — README의 배포 계획과 동일합니다.

## 3. 보안 그룹

- EC2: 22(SSH, 내 IP만), 8080(또는 사용하는 포트, 필요한 범위만)
- RDS: 3306을 EC2 보안 그룹에서 오는 트래픽만 허용

## 확인

```bash
sudo systemctl status decisionlab
sudo journalctl -u decisionlab -n 50 --no-pager   # 기동 로그 확인
```

헬스체크 엔드포인트가 필요하면 `spring-boot-starter-actuator`를 추가하고 `/actuator/health`를 쓰는 걸 추천합니다 (지금은 의존성에 없어서 빠졌습니다).
