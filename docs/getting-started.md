# 개발 환경 시작하기

## 기술 스택

| 항목 | 버전 |
|---|---|
| Kotlin | 2.3.21 |
| Spring Boot | 4.1.1 |
| Java toolchain | 25 |
| Gradle | 9.7.1 (Wrapper 포함) |

빌드 스크립트는 Gradle Kotlin DSL, 테스트는 JUnit 5 + kotlin-test를 쓴다.
모든 버전은 `gradle/libs.versions.toml` 한 곳에 선언되어 있다.

## 요구 환경

**로컬 JDK가 25가 아니어도 된다.** `settings.gradle.kts`의 foojay-resolver 플러그인이
toolchain에 필요한 JDK 25를 자동으로 내려받아 `~/.gradle/jdks/` 아래에 설치하고,
빌드와 실행은 그 JDK로 이뤄진다. 준비해야 할 것은 Gradle Wrapper를 구동할 JDK 하나뿐이다.

## 빌드 · 실행 · 테스트

```bash
./gradlew build      # 컴파일 + 테스트 + JAR 패키징
./gradlew test       # 테스트만 실행
./gradlew bootRun    # 애플리케이션 실행
```

빌드 산출물은 `build/libs/`에 생성된다.

- `TomServer-0.0.1-SNAPSHOT.jar` — 실행 가능한 fat JAR
- `TomServer-0.0.1-SNAPSHOT-plain.jar` — 컴파일된 클래스만 담긴 JAR

> **참고:** 현재 의존성에 웹 스타터가 없어(`spring-boot-starter`만 사용)
> `bootRun`은 애플리케이션 컨텍스트를 띄운 뒤 곧바로 종료된다.
> 서버를 상주시키려면 `spring-boot-starter-web` 등을 추가해야 한다.

## 프로젝트 구조

```
TomServer
├── build.gradle.kts                 빌드 스크립트 — 플러그인, 의존성, 컴파일 옵션
├── settings.gradle.kts              프로젝트 이름, toolchain 자동 프로비저닝 설정
├── gradle/
│   ├── libs.versions.toml           버전 카탈로그 — 의존성·플러그인 버전 선언
│   └── wrapper/                     Gradle Wrapper
├── docs/                            프로젝트 문서
└── src/
    ├── main/
    │   ├── kotlin/kr/kro/tomchi/tomserver/
    │   │   └── TomServerApplication.kt      진입점 — @SpringBootApplication + main
    │   └── resources/
    │       └── application.properties       애플리케이션 설정
    └── test/kotlin/kr/kro/tomchi/tomserver/
        └── TomServerApplicationTests.kt     컨텍스트 로딩 테스트
```

패키지를 어떻게 나눌지는 [개발 규약](conventions.md#패키지-구조)을 따른다.

## 의존성 추가 방법

버전 카탈로그를 거친다. `build.gradle.kts`에 좌표를 직접 쓰지 않는다.

1. `gradle/libs.versions.toml`의 `[libraries]`에 모듈을 선언한다.
   버전은 Spring Boot BOM이 관리하므로 대개 생략한다.
2. `build.gradle.kts`의 `dependencies`에서 `libs.` 로 참조한다.
   TOML 키의 `-`는 `.`으로 바뀐다 (`spring-boot-starter-web` → `libs.spring.boot.starter.web`).

웹 스타터를 추가하는 예시:

```toml
# gradle/libs.versions.toml
[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.spring.boot.starter.web)
}
```

## 컴파일러 옵션

`build.gradle.kts`에 두 가지가 설정되어 있다.

- `-Xjsr305=strict` — 자바 라이브러리의 nullability 애너테이션을 엄격하게 해석해
  플랫폼 타입 대신 null 안전성을 컴파일 시점에 강제한다.
- `-Xannotation-default-target=param-property` — 생성자 프로퍼티에 붙인 애너테이션을
  파라미터와 프로퍼티 양쪽에 적용한다.

## 참고 링크

- [Gradle 공식 문서](https://docs.gradle.org)
- [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.1/gradle-plugin)
- [OCI 이미지 생성](https://docs.spring.io/spring-boot/4.1.1/gradle-plugin/packaging-oci-image.html)
- [Gradle Build Scans](https://scans.gradle.com#gradle)
