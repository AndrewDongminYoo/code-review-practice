# 코드 리뷰 연습

이 저장소는 Spring Boot 구매 API와 Flutter 스튜디오 검색 컨트롤러를 소재로 만든 모의 코드 리뷰 연습 자료입니다. 특정 회사의 실제 채용 시험이나 기출 문제를 재현하지 않습니다.

## 시작하기

1. 먼저 [START_HERE.txt](START_HERE.txt)에서 연습 순서와 답안 기준을 확인합니다.
2. Spring 연습은 [spring/README.md](spring/README.md)와 [spring/CONTRACTS.md](spring/CONTRACTS.md)를 읽습니다.
3. Flutter 연습은 [flutter/README.md](flutter/README.md)와 [flutter/studio_repository.dart](flutter/studio_repository.dart)를 읽습니다.
4. 코드를 검토하며 발생 조건, 영향, 최소 수정, 회귀 테스트를 정리합니다.
5. 직접 답안을 작성한 뒤 [REVIEW_NOTES.txt](REVIEW_NOTES.txt)의 예시와 비교합니다.

## 원본 코드와 수정본

현재 브랜치의 Spring 서비스와 Flutter 컨트롤러에는 리뷰에서 발견한 결함의 수정이 반영되어 있습니다. 원본 결함 코드는 저장소 첫 커밋 `17d774b`에 보존되어 있습니다. 원본 파일은 `git show 17d774b:spring/PurchaseService.java`와 `git show 17d774b:flutter/search_controller.dart` 명령으로 확인할 수 있습니다.

## 파일 구성

- `START_HERE.txt`는 전체 연습 순서와 작성할 답안 형식을 안내합니다.
- `spring/README.md`와 `spring/CONTRACTS.md`는 Spring 연습 시나리오와 확정된 동작 전제를 설명합니다.
- `spring/PurchaseService.java`는 리뷰 지적을 반영한 구매 서비스 예제입니다.
- `flutter/README.md`와 `flutter/studio_repository.dart`는 검색 컨트롤러의 시나리오와 저장소 계약을 설명합니다.
- `flutter/search_controller.dart`는 요청 순서, 오류 상태, 컨트롤러 수명주기를 처리하는 수정 예제입니다.
- `REVIEW_NOTES.txt`에는 우선순위별 리뷰와 회귀 테스트 제안이 있습니다.

## 실행 및 검증 범위

이 저장소는 실행 가능한 애플리케이션이 아니라 코드 검토용 예제입니다. Spring 자료에는 전체 애플리케이션, 빌드 설정, 데이터베이스, 컨트롤러, 엔티티와 저장소 구현이 포함되지 않아 독립적으로 빌드하거나 통합 테스트할 수 없습니다.

Flutter 자료에는 완성된 앱과 동작 테스트가 포함되지 않습니다. 수정된 컨트롤러는 `dart format flutter/search_controller.dart`와 `flutter analyze`를 통과했지만, 이 검사는 검색 경합과 화면 동작을 검증하지 않습니다.
