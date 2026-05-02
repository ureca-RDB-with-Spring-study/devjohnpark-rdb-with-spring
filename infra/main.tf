terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.60" }
  }
  # 팀 협업 시 활성화
  # backend "s3" {
  #   bucket         = "my-tf-state-..."
  #   key            = "app/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   dynamodb_table = "terraform-locks"
  # }
}

provider "aws" {
  region = var.region
  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
