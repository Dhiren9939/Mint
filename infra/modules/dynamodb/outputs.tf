output "file_metadata_table_arn" {
  value = aws_dynamodb_table.file_meta_data_table.arn
}

output "table_name" {
  value = aws_dynamodb_table.file_meta_data_table.name
}
