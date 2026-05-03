# ─────────────────────────────────────────
# GitHub OIDC Provider
# AWS가 GitHub의 토큰을 검증할 수 있도록 등록
# ─────────────────────────────────────────
resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = [
    "sts.amazonaws.com",
  ]

  # GitHub OIDC의 인증서 지문 (AWS 공식 문서 값)
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]

  tags = {
    Name = "${var.project}-github-oidc"
  }
}

# ─────────────────────────────────────────
# GitHub Actions가 가정(assume)할 IAM Role
# ─────────────────────────────────────────
variable "github_repo" {
  description = "GitHub 저장소 (형식: 'org/repo')"
  type        = string
  # 예: "ureca-RDB-with-Spring-study/devjohnpark-rdb-with-spring"
}

variable "github_branch" {
  description = "허용할 브랜치 (와일드카드 가능)"
  type        = string
  default     = "main"
}

resource "aws_iam_role" "github_actions" {
  name = "${var.project}-github-actions"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        # 핵심: 이 저장소의 이 브랜치만 Role을 가정 가능
        StringLike = {
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repo}:ref:refs/heads/${var.github_branch}"
        }
      }
    }]
  })

  tags = {
    Name = "${var.project}-github-actions-role"
  }
}

# ─────────────────────────────────────────
# Role에 부착할 정책
# 학습 단계: PowerUserAccess (IAM 작업 외 모든 것)
# ─────────────────────────────────────────
resource "aws_iam_role_policy_attachment" "github_actions_power" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/PowerUserAccess"
}

# ─────────────────────────────────────────
# Output: GitHub Secrets에 등록할 Role ARN
# ─────────────────────────────────────────
output "github_actions_role_arn" {
  description = "GitHub Secrets의 AWS_ROLE_ARN에 등록할 값"
  value       = aws_iam_role.github_actions.arn
}
