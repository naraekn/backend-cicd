Backend-CICD
====

spring boot 어플리케이션을 배포하는 과정을 정리했습니다.

## Docker 란 무엇인가

도커는 어플리케이션을 개발하고 운반하고 실행하기 위한 오픈 플랫폼이다. 도커는 당신으로 하여금 인프라로부터 당신의 어플리케이션을 격리시키는 것을 가능하게하여 빠르게 소프트웨어를 전달할 수 있도록 해준다.

도커는 "컨테이너"라고 부르는 격리된 환경에서 어플리케이션을 묶고 실행시킬 수 있도록 해준다. 격리와 보안은 많은 컨테이너들이 주어진 호스트에서 동시에 실행되는 것을 가능하게 해준다.


## Docker File 만들기

도커는 `Dockerfile`에 있는 명령어를 자동으로 읽어 이미지를 빌드할 수 있다.
다음은 spring boot 애플리케이션 실행을 위한 이미지를 만드는 도커파일이다.

```dockerfile
FROM openjdk:15.0.1 AS builder

COPY . .

RUN ["./gradlew", "assemble"]

FROM openjdk:15.0.1

COPY --from=builder /app/build/libs/app.jar .

CMD ["java", "-jar", "app.jar"]
```

* `FROM openjdk:15.0.1 AS builder`: 어떤 이미지로 만들지 선택함
* `COPY . .`: 빌드하기 위해서 모든파일을 컨테이너로 복사한 후 빌드를 실행함 (현재폴더 > 컨테이너의 현재폴더)
* `RUN ["./gradlew", "assemble"]`: 명령어 실행 (jar 파일을 만든다)
* `FROM openjdk:15.0.1`: 우리가 실행시킬 이미지임
* `COPY --from=builder /app/build/libs/app.jar .`: 빌드 이미지로부터 만들어진 jar 파일 복사함
* `CMD ["java", "-jar", "app.jar"]`: 서버 실행함
* `CMD` 대신 `ENTRYPOINT` 쓰기를 추천함. `CMD`는 여러개의 명령이 있는 경우 마지막 명령만 실행하기 때문.


## 이미지 빌드 / 실행 / 푸시 하기

```bash
docker build -t "image-name"
docker run --rm -p 8080:8080 cat-toy-shop 
docker tag cat-toy-shop naraekn/spring-week8:version1
docker push naraekn/spring-week8:version1
```

* `build`: docker 파일로부터 이미지를 생성하는 명령어
    * `-t`: 이미지에 대한 태그 설정
* `run`: 지정된 이미지에 대한 컨테이너를 생성하고 실행시킨다. (create + start)
    * `--rm`: exit하면 컨테이너를 자동으로 제거해준다.
* `tag`: 해당 이미지의 태그이름을 변경한다 (원격저장소/프로젝트 이름이 들어가도록)
* `push`: 도커허브로 푸시한다.


## CI 구축하기 - 01

먼저 ci환경을 개발환경과 동일한 jdk로 세팅해주고 테스트를 실행해준다. 이 때 gradlew에 대한 실행권한을 부여해줘야한다. (도커파일에서 assemble안해주면 ci에서 해줘야한다 - 이렇게하면 속도를 줄일수 있음)

```yml
name: CI

on:
  push:
    branches:
      - main
  pull_request:

jobs:
  build:
    runs-on: ubuntu-20.04
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 15
        uses: actions/setup-java@v2
        with:
          java-version: '15'
          distribution: 'adopt'

      - name: Grant execution permission for gradlew
        run: chmod +x gradlew
      - name: Test
        run: ./gradlew test
      # - name: Build with Gradle
      #   run: ./gradlew assemble
```

위 과정을 github action 코드로 바꿔주자.
아래 코드는 이미지 빌드해서 도커허브에 푸시하는 CI 코드로, 태그의 이름을 깃 해시 값으로 설정하도록 했다.
(참고로 `chmod +x` 명령어는 프로그램을 실행할 권한을 부여하는 것을 말한다.)

```yml
- name: Docker build
  run: |
    docker login -u ${{ secrets.DOCKER_USERNAME }} -p ${{ secrets.PASSWORD }}
    docker build -t codesoom-cicd . 
    docker tag codesoom-cicd naraekn/codesoom-cicd:${GITHUB_SHA::7}
    docker push naraekn/codesoom-cicd:${GITHUB_SHA::7}
```

1) 도커 로그인
2) 도커 이미지 빌드
3) 도커 태그 (codesoom-cicd > 내가 push할 레포에 이미지 태그는 깃 해시로 설정)
4) 내 도커 허브로 푸시


## EC2 세팅하기

* EC2 띄운다 (이건 알아서..)
* 키페어 발급받고 나면 키페어를 `.ssh` 폴더에 넣고 (아무 폴더여도 상관없긴함) 해당 파일의 권한을 `chmod 600 파일이름` 으로 변경한다
* EC2 서버에 접속해서 docker를 설치한다
* 서버에 들어가서 docker 설치하기

    ```bash
    sudo yum update -y
    sudo yum install -y docker
    sudo service docker start
    sudo systemctl enable docker
    sudo usermod -a -G docker ec2-user
    ```
* docker 설치한 후 `exit` 명령어로 서버에서 나왔다가 다시 접속해본다.

## EC2에서 도커 이미지 받아오기

* `docker ps`
    * 현재 실행되고 있는 컨테이너 목록을 볼 수 있다
* `docker pull naraekn/spring-week8:version1`
    * 도커 허브로부터 이미지를 받아온다
* `docker stop server`
    * 만약, 이전에 실행시키던 컨테이너가 있다면 해당 컨테이너를 stop 시키자
    * "컨테이너"를 중단시키는 명령어다.
* `docker tag naraekn/spring-week8:version1 spring-week8`
    * 받아온 도커 "이미지"의 이름을 변경해준다
* `docker run -d -p 80:8080 --rm --name server naraekn/spring-week8:version1`
    * `-d`: --detach로 해당 컨테이너가 백그라운드에서 실행되고 컨테이너 아이디를 프린트하도록 하는 명령어이다
    * `-p`: 컨테이너의 포트를 설정해준다 (실행하는 컴퓨터의 포트 : 컨테이너의 포트)
    * `--name`: "컨테이너"의 이름을 지정해준다
* 이미지를 실행시키고 EC2 주소로 들어가서확인해보자!


## Spring boot 어플리케이션에 Maria DB 붙이기

먼저 spring boot application에 mariadb를 세팅해주고 실행이 잘되는지 확인해봐야한다. 아래와같이 `build.gradle`에 mariadb를 추가해주고, `application.yml`파일을 세팅해준다.

이때, 로컬환경에서 테스트용으로 실행시킬때는 `localhost`로 설정해준다. 만약, 해당 어플리케이션도 도커컨테이너에 띄워서 실행시키고 싶으면 `host.docker.internal`로 설정해줘야한다.

```groovy
runtimeOnly 'org.mariadb.jdbc:mariadb-java-client:2.7.4'
```

```yml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/backend-cicd
    username: root
    password: root-password
  jpa:
    hibernate:
      ddl-auto: update
```

먼저, 도커네트워크를 생성해주고 mariadb 이미지를 받아서 실행한다.

```bash
 docker network create -d bridge my-network
```

```bash
docker run -d --name mariadb-cicd \
  --network my-network \
  -p 3306:3306 \
  -v ~/data/backend-cicd:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=backend-cicd \
  mariadb \     
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

* `docker run [OPTIONS] IMAGE [COMMAND] [ARGS...]`
    * 먼저 컨테이너 레이어를 생성하고 실행시켜줍니다.
    * 중지된 컨테이너는 `docker start`를 사용하여 이전의 모든 변경사항을 유지한 상태로 다시 시작할 수 있습니다.

    * `-d, --detach`: 컨테이너를 백그라운드에서 실행하고 컨테이너 아이디를 프린트하는 옵션
    * `--name`: 컨테이너에 이름을 부여하는 옵션
    * `--rm`: 해당 컨테이너를 exit하면 자동으로 "컨테이너"를 삭제하는 옵션
    * `-v, --volume`: 설정해준 디렉토리 구조로 편입시킴) 영속화시킬 수 있는 옵션 `-v ~/data/spring-week8:/var/lib/mysql` 와같이 설정해주며 TO:FROM
    * `-p, --publish`: 컨테이너의 프트를 해당 호스트로 배포함
    * `-it`: 컨테이너를 종료하지 않은 채로 터미널의 입력을 계속 컨테이너로 전달할 때 사용함.
    * `--network`
        * 해당 컨테이너를 네트워크에 연결한다.
    * `-e, --env`: 환경변수를 설정하는 옵션
    * `-v`: "Mount Volume"라는 뜻으로 공유폴더같은 개념으로 생각하면 이해가 쉽다.
        * ~/data/backend-cicd: 내 컴퓨터의 파일
        * /var/lib/mysql: 도커 안에있는 파일
        * 컨테이너에서 수정을해도 내 컴퓨터의 해당경로에 가서 수정을 해도 동시에 수정이된다! (공유폴더, 파일개념으로 생각하기)

## EC2에 MariaDB 세팅하기

로컬에서 잘 실행되는 것을 확인했다면 EC2에도 mariadb를 세팅해주자
같은 방식으로 네트워크를 생성하고 mariadb 이미지를 받아와 실행시킨다.

```bash
 docker network create -d bridge my-network
```

```bash
docker run -d --name mariadb \
  --network my-network \
  -p 3306:3306 \
  -v ~/data/backend-cicd:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=backend-cicd \
  mariadb \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

```

그런 다음 `application.yml`을 생성해준다.
이때 도커 컨테이너의 ip 값을 넣어줘야한다. `ifconfig`에 나오는 컨테이너 ip를 찾아보자
아래와 같이 나올텐데 `inet`에 해당하는 부분이다.
> docker0: flags=4099<UP,BROADCAST,MULTICAST>  mtu 1500
>   inet xxx.xxx.xxx.xxx  netmask xxx.xxx.xxx.xxx  broadcast xxx.xxx.xxx.xxx

```yml
spring:
  datasource:
    url: jdbc:mariadb://(container ip):3306/backend-cicd
    username: root
    password: root-password
  jpa:
    hibernate:
      ddl-auto: update
```

이제 spring boot application 이미지를 받아오고 (pull) 실행시켜주자 (run). 이때 EC2에 만들어놓은 application.yml을 마운트볼륨 해준다. 앞에 경로가 내 컴퓨터의 경로로 "application.yml"파일의 경로를 넣어주면 된다.

`docker run -it -p 80:8080 --network my-network -v $(pwd)/setting/application.yml:/application.yml backend-cicd`

그런 다음 이제 EC2의 퍼블릭 주소로 들어가서 잘 되는지 확인해보자!

## CI 구축하기 - 02

이제 MariaDB까지 잘 세팅이됐으니 자동 배포가되도록 세팅해보자.
CI/CD를 구축해서 이미지를 받아오고 이미지를 컨테이너에 띄우는 것을 자동화시키자.

이때, CI/CD를 실행시킬 때 aws 서버에 접근할 수 있도록 ssh 키를 만들어주자.
ssh 키 만들고 ec2 서버에가서 `.ssh/authorized_keys`에 public key를 붙여넣어 ssh 키를 등록해준다. (aws 기존 키를 사용하는 것이 아니라 이렇게 새로 만들어서 해주는 이유는 보안때문이다!)

그런다음 github에도 host, private key, username들을 등록해주자 이때 host는 `ec2-52-78....com`와 같이 Public IPv4 DNS을 써주면 된다.

`ssh-keygen -t rsa -b 4096 -C "your_email@example.com"`

```yml
      - name: Deploy
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.AWS_HOST }}
          username: ${{ secrets.AWS_USERNAME }}
          key: ${{ secrets.AWS_PRIVATE_KEY }}
          envs: REPO,IMAGE,GITHUB_SHA
          script: |
            docker pull naraekn/backend-cicd:${GITHUB_SHA::7}
            docker tag naraekn/backend-cicd:${GITHUB_SHA::7} backend-cicd
            docker stop server
            docker container rm server
            docker run -it -d \
              -p 80:8080 --name server --restart always \
              --network my-network \
              -v $(pwd)/setting/application.yml:/application.yml \
              backend-cicd

```

## 로드밸런서 및 도메인 연결하기

1. EC2 메뉴에서 로드밸런서에 들어간다
2. Application Load Balancer 선택
3. 이름 설정, mappings에서 최소 두개 선택
4. "Liseners and routing"에서 "Create target group" 선택해서 타켓팅하는 EC2를 포함하는 타겟그룹 생성
5. 타겟그룹 생성시 Protocol HTTP, Protocol version은 HTTP1, Health check path 작성후 해당 EC2 선택한 다음 "include as pending below" 누르기
6. "Listeners and routing"으로 다시 가서 "HTTPS" 선택후 해당 타겟그룹으로 forward 시켜줌
7. 그런다음 SSL certificate 선택 (없으면 만들어줘야함)
8. 로드밸런서 생성후 리스너를 추가한다 (80을 HTTPS 443으로 리다이렉트하도록)

위 과정을 다 하고 나서 Load balancer가 잘 연결됐는지 확인해주자.
해당 loadbalancer의 DNS 이름으로 들어가보자 - http, https둘다 되는지 확인!

그런다음 Route 53에가서 해당 로드밸런서를 도메인에 연결해주면 된다.

## 프로파일 설정하기

만약 spring에서 `SpringBootTest`와 같이 데이터베이스를 필요로 하는 테스트를 하는 경우에는 추가 설정이 필요하다.
실제 실행시에는 mariadb를 사용하고 테스트를 하는 경우에만 h2 database를 사용하도록 설정해주자.

먼저 h2-database를 `build.gradle`에 추가하자 (테스트 말고 실행시에도 h2 데이터베이스가 되는 것을 확인해보고 싶어서 `testImplementation` 대신 `runtimeOnly`로 세팅했다)

```groovy
runtimeOnly 'com.h2database:h2:1.4.200'
```

그런 다음 `application.yml` 파일에서 profile을 설정해주자
나는 spring 환경변수가 "test"일 때는 h2 데이터베이스를 사용하도록 설정했다.
(참고로 프로파일 설정이 최신 spring boot 부터는 바꼈으니 주의하자! - [Config file processing in Spring Boot 2.4](https://spring.io/blog/2020/08/14/config-file-processing-in-spring-boot-2-4))

```yml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3307/backend-cicd
    username: root
    password: password
  jpa:
    hibernate:
      ddl-auto: update

---

spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:h2:mem:backend-cicd
  jpa:
    hibernate:
      ddl-auto: create
      
```

그리고 test profile로 테스트를 실행하고 싶다면 터미널에서 아래와같이 실행시킨다.
(`clean`을 붙이는 이유는 이전에 빌드된 것을 사용하지 않고 새로 빌드하기 위해서이다)

```bash
 SPRING_PROFILES_ACTIVE=test ./gradlew clean test
```

마지막으로 CI도 수정해주자
```yml
- name: Test
        run: SPRING_PROFILES_ACTIVE=test ./gradlew test
```

드디어 끝!
