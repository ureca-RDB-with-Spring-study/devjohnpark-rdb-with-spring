variable "region" { default = "ap-northeast-2" }
variable "project" { default = "smartclearance" }
variable "environment" { default = "prod" }
variable "key_name" { default = "app-keypair" }
variable "my_ip_cidr" { description = "내 PC IP/32 (SSH 허용용)" }
variable "db_username" { default = "app" }
variable "db_password" { sensitive = true }
variable "db_name" { default = "appdb" }
