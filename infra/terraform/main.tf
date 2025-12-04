terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# 디폴드 리전 설정
provider "aws" {
  region = "ap-northeast-2"
}

# VPC_1
resource "aws_vpc" "vpc_1" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-vpc-1"
  }
}

# 퍼블릭 서브넷 (Subnet_1)
resource "aws_subnet" "subnet_1" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true # 퍼블릭 IP 자동 할당

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-subnet-1-public"
  }
}

# 프라이빗 서브넷 (Subnet_2)
resource "aws_subnet" "subnet_2" {
  vpc_id            = aws_vpc.vpc_1.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "ap-northeast-2a"

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-subnet-2-private"
  }
}

# 프라이빗 서브넷 (Subnet_3)
resource "aws_subnet" "subnet_3" {
  vpc_id            = aws_vpc.vpc_1.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "ap-northeast-2b"

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-subnet-3-private"
  }
}

# 인터넷 게이트 웨이
resource "aws_internet_gateway" "igw_1" {
  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-igw-1"
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
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-rt-1"
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

resource "aws_route_table_association" "association_3" {
  subnet_id = aws_subnet.subnet_3.id

  route_table_id = aws_route_table.rt_1.id
}

resource "aws_security_group" "sg_1" {
  name   = "team5-sg-1"
  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-sg-1"
  }

  # SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # 필요 시 특정 IP로 제한 가능
  }

  # HTTP
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # WebRTC UDP
  ingress {
    from_port   = 10000
    to_port     = 20000
    protocol    = "udp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # NPM (port 81)
  ingress {
    from_port   = 81
    to_port     = 81
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # MySQL (port 3306)
  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 아웃바운드 모든 프로토콜
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "all"
    cidr_blocks = ["0.0.0.0/0"] # 모든 IP 허용
  }
}

# Coturn 서버 전용 보안 그룹 (Security Group)
resource "aws_security_group" "coturn_sg" {
  name        = "team5-coturn-server-sg"
  description = "Allow WebRTC TURN server traffic"
  vpc_id      = aws_vpc.vpc_1.id

  ingress {
    description = "SSH for maintenance"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "TURN Listening Port (TCP)"
    from_port   = 3478
    to_port     = 3478
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "TURN Listening Port (UDP)"
    from_port   = 3478
    to_port     = 3478
    protocol    = "udp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "TURN Media Relay Ports (UDP)"
    from_port   = 49152
    to_port     = 65535
    protocol    = "udp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-coturn-sg"
  }
}

# EC2 역할 생성
resource "aws_iam_role" "ec2_role_1" {
  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-ec2-role-1"
  }

  # 이 역할에 대한 신뢰 정책 설정. EC2 서비스가 이 역할을 가정할 수 있도록 설정
  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
  EOF
}

# EC2 역할에 AmazonEC2RoleforSSM 정책을 부착
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

# IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "instance_profile_1" {
  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-instance-profile-1"
  }

  role = aws_iam_role.ec2_role_1.name
}

# EC2 실행마다 적용할 작업
locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
# 가상 메모리 4GB 설정
sudo dd if=/dev/zero of=/swapfile bs=128M count=32
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 환경변수 세팅(/etc/environment)
echo "PASSWORD=${var.password_1}" >> /etc/environment
echo "DOMAIN=${var.catfe_domain_1}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN_OWNER=${var.github_access_token_1_owner}" >> /etc/environment
echo "GITHUB_ACCESS_TOKEN=${var.github_access_token_1}" >> /etc/environment

# EC2 환경변수 등록
source /etc/environment

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# 도커 네트워크 생성
docker network create common

# redis 설치
docker run -d \
  --name redis_1 \
  --restart unless-stopped \
  --network common \
  -p 6379:6379 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/redis_1/volumes/data:/data \
  redis --requirepass ${var.password_1}

# NginX 설치
docker run -d \
  --name npm_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -e INITIAL_ADMIN_EMAIL=admin@npm.com \
  -e INITIAL_ADMIN_PASSWORD=${var.password_1} \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest

# ghcr.io 로그인
echo "${var.github_access_token_1}" | docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin

END_OF_FILE
}

# EC2 인스턴스 생성
resource "aws_instance" "ec2_1" {
  ami           = "ami-077ad873396d76f6a"
  instance_type = "t3.micro"

  subnet_id              = aws_subnet.subnet_1.id
  vpc_security_group_ids = [aws_security_group.sg_1.id]

  associate_public_ip_address = true

  # 인스턴스에 IAM 역할 설정
  iam_instance_profile = aws_iam_instance_profile.instance_profile_1.name

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-ec2-1"
  }

  # 루트 불륨 설정
  root_block_device {
    volume_type = "gp3"
    volume_size = 12
  }

  # EC2 실행 시, 작업진행
  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}

resource "aws_instance" "coturn_server" {
  ami                         = "ami-02835aed2a5cb1d2a" # 서울 리전 Ubuntu 22.04 LTS
  instance_type               = "t3.micro"
  subnet_id                   = aws_subnet.subnet_1.id
  vpc_security_group_ids      = [aws_security_group.coturn_sg.id]
  associate_public_ip_address = true

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-coturn-server"
  }

  # EC2 부팅 시 Coturn 자동 설치 및 설정 스크립트
  user_data = <<-EOF
              #!/bin/bash
              apt-get update
              apt-get install -y coturn

              PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
              PRIVATE_IP=$(curl -s http://169.254.169.254/latest/meta-data/local-ipv4)

              cat <<EOT > /etc/turnserver.conf
              listening-port=3478
              external-ip=$PUBLIC_IP/$PRIVATE_IP

              # 동적 인증을 위한 비밀키 설정
              use-auth-secret
              static-auth-secret=${var.turn_shared_secret}

              lt-cred-mech
              realm=${var.catfe_domain_1}
              log-file=/var/log/turnserver.log
              verbose
              fingerprint
              no-multicast-peers
              EOT

              systemctl restart coturn
              systemctl enable coturn
              EOF
}

# 3. 결과 출력 (Output - Turn 서버 IP 주소 출력)
output "coturn_server_public_ip" {
  description = "The public IP address of the Coturn server."
  value       = aws_instance.coturn_server.public_ip
}

# RDS용 Security Group
resource "aws_security_group" "rds_sg_1" {
  name        = "team5-rds-sg-1"
  description = "Allow All"
  vpc_id      = aws_vpc.vpc_1.id

  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-rds-sg-1"
  }
}

# RDS Subnet Group
resource "aws_db_subnet_group" "db_subnet_group" {
  name       = "team5-db-subnet-group"
  subnet_ids = [aws_subnet.subnet_2.id, aws_subnet.subnet_3.id]

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-db-subnet-group"
  }
}

resource "aws_db_instance" "mysql" {
  identifier        = "team5-mysql"
  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp2"

  db_name  = "${var.db_name}"
  username = "${var.db_username}"
  password = "${var.db_password}"

  db_subnet_group_name   = aws_db_subnet_group.db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.rds_sg_1.id]

  # RDS 퍼블릭 액세스 허용
  publicly_accessible = true

  multi_az = false

  # 자동 백업 보관 기간
  backup_retention_period = 1

  # 삭제 시 최종 스냅샷 생성 여부
  skip_final_snapshot = true

  tags = {
    Key   = "TEAM"
    Value = "devcos-team05"
    Name  = "team5-mysql"
  }
}

# EC2 역할에 AmazonS3FullAccess 정책을 부착
resource "aws_iam_role_policy_attachment" "s3_full_access" {
  role = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# S3 접근 권한 추가
resource "aws_s3_bucket_public_access_block" "public-access" {
  bucket = aws_s3_bucket.s3_1.id

  block_public_acls = false
  block_public_policy = false
  ignore_public_acls = false
  restrict_public_buckets = false
}

# S3 접근 정책 추가
resource "aws_s3_bucket_policy" "bucket-policy" {
  bucket = aws_s3_bucket.s3_1.id

  depends_on = [
    aws_s3_bucket_public_access_block.public-access
  ]

  policy = <<POLICY
{
  "Version":"2012-10-17",
  "Statement":[
    {
      "Sid":"PublicRead",
      "Effect":"Allow",
      "Principal": "*",
      "Action":["s3:GetObject"],
      "Resource":["arn:aws:s3:::${aws_s3_bucket.s3_1.id}/*"]
    }
  ]
}
POLICY
}

# S3 인스턴스 생성
resource "aws_s3_bucket" "s3_1" {
  bucket = "catfe-s3-1"
  tags = {
    Key = "TEAM"
    Value = "devcos-team05"
    Name = "team5-s3-1"
  }
}