resource "aws_dynamodb_table" "file_meta_data_table" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "fileCode"

  attribute {
    name = "fileCode"
    type = "S"
  }

  ttl {
    attribute_name = "cleanAt"
    enabled        = true
  }
}