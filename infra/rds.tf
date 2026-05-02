# DB 서브넷 그룹: AWS 정책상 RDS는 반드시 2개 이상의 AZ에 걸친 서브넷을 등록해야 함.
# 현재는 Single-AZ로 RDS 1대만 띄우지만, 향후 multi_az = true 활성화 시
# 두 번째 서브넷이 자동으로 Standby 인스턴스 자리로 사용됨.
resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-db-subnets"
  subnet_ids = aws_subnet.private[*].id # private-0 (AZ-2a) + private-1 (AZ-2c)
  tags       = { Name = "${var.project}-db-subnets" }
}

resource "aws_db_instance" "mysql" {
  identifier             = "${var.project}-mysql"
  engine                 = "mysql"
  engine_version         = "8.4.8"
  instance_class         = "db.t4g.micro"
  allocated_storage      = 20
  storage_type           = "gp3"
  db_name                = var.db_name
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Single-AZ 구성 (학습/개발용).
  # AWS가 등록된 두 서브넷 중 하나에 RDS Primary를 자동 배치하고,
  # 다른 서브넷은 Multi-AZ 활성화 시까지 빈 슬롯으로 예약됨.
  multi_az = false # 운영 환경에서는 true 권장 (자동 failover, 비용 약 2배)

  skip_final_snapshot     = true
  publicly_accessible     = false
  backup_retention_period = 0     # 7 → 0 으로 변경 (프리티어 호환)
  deletion_protection     = false # 운영은 true 권장
}
