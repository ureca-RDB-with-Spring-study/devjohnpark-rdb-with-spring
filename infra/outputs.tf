output "app_public_ip" { value = aws_instance.app.public_ip }
output "k6_public_ip" { value = aws_instance.k6.public_ip }
output "rds_endpoint" { value = aws_db_instance.mysql.address }
output "ecr_repo_url" { value = aws_ecr_repository.app.repository_url }
