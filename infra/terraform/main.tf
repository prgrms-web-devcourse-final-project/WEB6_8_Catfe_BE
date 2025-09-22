terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source  = "hashicorp/aws"
    }
  }
}

# 디폴드 리전 설정
provider "aws" {
  region = "ap-northeast-2"
}

resource "aws_vpc" "example" {
  cidr_block = "10.0.0.0/16"

  tags = {
    Name = "example"
  }
}

