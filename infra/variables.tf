variable "region" {
  description = "The AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "db_username" {
  type        = string
  description = "The username for the RDS database"
  sensitive   = true
}

variable "db_password" {
  type        = string
  description = "The password for the RDS database"
  sensitive   = true
}

variable "db_name" {
  type = string
  description = "The name of the databse"
  default = "mintdb"
}

variable "mint_user_files" {
  type    = string
  default = "mint-user-files-bucket"
}

variable "ssh_public_key" {
  type        = string
  description = "The public key for Mint Key Pair"
  default     = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0uYGtqbp73M9prIVb1nGl5aCXDqGiQ6cr4E3NIUifo1Mii0Tu/8EhOIqeShfLIc9RIflFru25/0h6P5z01pqjBFEKgtp1UbWkqT/xRXjf93b/M/P7SWjvMbQB+PcLW0i8JqBJO2er+mR5XOGMZa1V3yzbV/dUaE8nYES97RQFI+V10CehvoPHgBhte/zidUUqdrppd+lgSppzst3Wq7OQK1DXXYVD5myrzY2txNaj/dzAKaPIww4HV6xaWPnYfleXwHHqN0XjS56mmw5T4TWzlV+vS2ZQyIWLMaK1OhdDZTBioKQRhSxD87MpuYAxPhnNiWZqkhVbm2NsVL9V38pT MintKey"
}

variable "domain_name" {
  description = "The target domain name"
  type        = string
  default     = "dhiren.xyz"
}

variable "zone_id" {
  type        = string
  description = "The id of the route53 hosted zone"
  default     = "Z08728257AAJ6Q96KGZY"
}
