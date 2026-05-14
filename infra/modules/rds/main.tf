resource "aws_db_instance" "db" {
  allocated_storage      = 20
  db_name                = "mintdb"
  engine                 = "postgres"
  engine_version         = "17"
  instance_class         = "db.t3.micro"
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = var.db_subnet_group_name
  skip_final_snapshot    = true
  multi_az               = false
  vpc_security_group_ids = [var.rds_sg_id]
}
