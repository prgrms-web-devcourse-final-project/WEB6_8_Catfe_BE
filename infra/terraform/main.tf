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

# VPC_1
resource "aws_vpc" "vpc_1" {
  cidr_block = "10.0.0.0/16"
  enable_dns_support = true
  enable_dns_hostnames = true

  tags = {
    Name = "team5-vpc-1"
  }
}

# 퍼블릭 서브넷 (Subnet_1)
resource "aws_subnet" "subnet_1" {
  vpc_id = aws_vpc.vpc_1.id
  cidr_block = "10.0.1.0/24"
  availability_zone = "ap-northeast-2a"
  map_public_ip_on_launch = true # 퍼블릭 IP 자동 할당

  tags = {
    Name = "team5-subnet-1-public"
  }
}

# 프라이빗 서브넷 (Subnet_2)
resource "aws_subnet" "subnet_2" {
  vpc_id = aws_vpc.vpc_1.id
  cidr_block = "10.0.2.0/24"
  availability_zone = "ap-northeast-2b"

  tags = {
    Name = "team5-subnet-2-private"
  }
}

# 인터넷 게이트 웨이
resource "aws_internet_gateway" "igw_1" {
  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Name = "team5-igw-1"
  }
}

# 라우팅 테이블
resource "aws_route_table" "rt_1" {
  vpc_id = aws_vpc.vpc_1.id

  # 모든 트래픽에 대해 인터넷 게이트웨이로 보냄
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_1.id
  }

  tags = {
    Name = "team5-rt-1"
  }
}

resource "aws_route_table_association" "association_1" {
  # 연결할 서브넷
  subnet_id = aws_subnet.subnet_1.id

  # 연결할 라우트 테이블 지정
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_2" {
  # 연결할 서브넷
  subnet_id = aws_subnet.subnet_2.id

  # 연결할 라우트 테이블 지정
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_security_group" "sg_1" {
  name = "team5-sg-1"
  description = "Allow SSH and HTTP"
  vpc_id = aws_vpc.vpc_1.id

 ingress {
   from_port = 0
   to_port = 0
   protocol = "all" # 모든 프로토콜
   cidr_blocks = ["0.0.0.0/0"] # 모든 IP 허용
 }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "all" # 모든 프로토콜
    cidr_blocks = ["0.0.0.0/0"] # 모든 IP 허용
  }
}

resource "aws_instance" "ec2_1" {
  ami           = "ami-077ad873396d76f6a"
  instance_type = "t2.micro"

  subnet_id = aws_subnet.subnet_1.id
  vpc_security_group_ids = [aws_security_group.sg_1.id]

  associate_public_ip_address = true

  tags = {
    Name = "team5-ec2-1"
  }
}