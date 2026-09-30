terraform {
  backend "s3" {
    bucket       = "dhiren9939-state-bucket"
    region       = "ap-south-1"
    use_lockfile = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.39.0"
    }
  }
}

provider "aws" {
  region = var.region
}

# The state key is not set here: it's injected at `terraform init` time via
# `-backend-config="key=projects/mint/$ENVIRONMENT.tfstate"`, exactly like the main
# `infra/` root. Reusing that same composite action (.github/actions/setup-aws-terraform)
# with `environment: loadgen` lands on `projects/mint/loadgen.tfstate`, the fixed key this
# standalone root is meant to use, without duplicating the key-building logic.
