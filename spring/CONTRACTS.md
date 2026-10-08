# 변경되지 않은 기존 코드와 동작 계약

아래 내용은 이 연습에서 확정된 전제입니다. 생략된 설정이나 반복적인 코드는 리뷰 대상이 아닙니다.

## 실행 환경과 요청 처리 경계

- Java 17, Spring Boot 3.5 계열, Spring Framework 6.2, Spring Data JPA 3.5를 사용합니다. 버전이 다른 애플리케이션의 기본값을 가정하지 말고, 여기 명시된 동작을 기준으로 판단하세요.
- 일반적인 Spring 프록시 기반 트랜잭션 관리가 활성화되어 있습니다. 컨트롤러는 주입받은 프록시 `PurchaseService` 빈의 public `buy` 메서드를 호출합니다. 자기 호출(self-invocation), 직접 생성한 서비스 인스턴스, 바깥쪽 트랜잭션은 없습니다. 서비스 메서드에 명시한 `rollbackFor` 외에 별도의 롤백 규칙이나 전역 `ALL_EXCEPTIONS` 설정은 없습니다.
- 두 저장소와 `PurchaseService`의 `@PersistenceContext` `EntityManager`는 같은 트랜잭션 매니저를 사용합니다. 데이터베이스의 격리 수준은 READ COMMITTED이며 일반적인 행 갱신을 수행합니다. SERIALIZABLE 격리 수준이나 별도로 숨겨진 애플리케이션 차원의 재고 보호 장치는 없습니다.
- 인증된 요청을 처리하는 컨트롤러는 검증된 보안 principal에서 `callerId`를 얻습니다. 요청 본문에서 가져오지 않습니다. 컨트롤러는 `callerId`와 검증을 통과한 요청을 `buy`에 전달하고, 성공 시 HTTP 201을 반환합니다. principal은 유효한 구매자 계정을 식별합니다. 인증 과정 자체에서 이 값과 `buyerId`를 비교하지는 않습니다.
- 요청 검증 단계는 필수 필드 누락, 0 이하의 ID, 1–10 범위를 벗어난 수량, 0.01–1000000.00 범위를 벗어나거나 소수점 이하가 두 자리를 초과하는 `unitPrice`를 HTTP 400으로 거절합니다. 검증을 통과한 요청만 서비스에 도달합니다. JSON의 금액은 십진수로 전달되며 `BigDecimal`로 파싱됩니다.
- 요청과 응답의 구조는 다음과 같습니다.

```java
record BuyRequest(long listingId, long buyerId, int quantity, BigDecimal unitPrice) {}
record PurchaseView(long id, long buyerId, BigDecimal total) {}
```

- 테스트 시나리오에서 사용되는 구매자 ID는 모두 존재하는 계정을 가리킵니다. 구매자 계정이 없거나 별도의 사용자 식별 서비스가 실패하는 경우는 범위 밖입니다.
- 기존 controller advice는 `NotFound`를 404, `OutOfStock`을 409, `Forbidden`을 403, `PaymentDeclined`를 422로 매핑합니다. 앞의 세 예외는 `RuntimeException`을 상속합니다. **`PaymentDeclined`는 `Exception`을 직접 상속하는 checked exception입니다.** 예외는 서비스 메서드 밖으로 전파된 후 controller advice에서 처리됩니다.

## 영속 모델과 저장소

`Listing`은 일반적인 변경 가능한 JPA 엔티티입니다. 자동 생성되는 `Long id`, null이 아닌 양수 `BigDecimal unitPrice`, 0 이상의 `int remaining`을 갖습니다. 코드에 등장하는 getter와 `setRemaining(int)`은 단순한 접근자입니다. 한 구매 요청을 처리하는 동안 상품 가격은 바뀌지 않습니다. 버전 컬럼, 엔티티 콜백, 동기화 처리, 잠금 애너테이션은 없습니다. 데이터베이스에는 재고가 음수가 되지 않도록 하는 제약조건이 있지만, 재고와 구매 행 개수를 연결하는 제약조건은 없습니다.

`Purchase`는 자동 생성되는 `Long id`, `long buyerId`, `long listingId`, `int quantity`, `BigDecimal total`을 갖는 JPA 엔티티입니다. 인자가 네 개인 생성자는 ID를 제외한 네 필드를 설정합니다. 서비스에서 사용하는 getter는 모두 존재합니다. 저장소의 `save`는 반환 전에 ID를 부여합니다. 서로 다른 구매자의 개별 구매를 거절할 만한 유일성 제약조건은 없습니다.

```java
interface ListingRepository extends JpaRepository<Listing, Long> {}
interface PurchaseRepository extends JpaRepository<Purchase, Long> {}
```

`ListingRepository.findById`는 별도의 잠금을 요청하지 않는 일반적인 JPA 조회이며, 수정된 `PurchaseService`는 이를 재고 확인용으로 사용하지 않습니다. 서비스는 `EntityManager.find(Listing.class, id, LockModeType.PESSIMISTIC_WRITE)`로 상품을 읽어 트랜잭션이 끝날 때까지 행 잠금을 유지합니다. `saveAndFlush`는 현재 트랜잭션 안에서 대기 중인 변경을 데이터베이스로 flush하며, 해당 트랜잭션을 독립적으로 커밋하지 않습니다. 갱신 시에는 엔티티에서 계산한 `remaining`의 절대값을 기록합니다. SQL에서 원자적으로 재고를 차감하는 방식은 아닙니다. 모든 저장소 연산은 서비스의 트랜잭션에 참여합니다.

## 결제 컴포넌트

```java
interface PaymentStub {
    void authorize(long buyerId, BigDecimal total) throws PaymentDeclined;
}
```

이 컴포넌트는 로컬에서 동작하며 결과가 결정적인 테스트용 스텁입니다. 전달받은 구매자에 대해 테스트용 결제 거절 플래그를 확인한 뒤, 정상 반환하거나 새로운 `PaymentDeclined`를 던집니다. 트랜잭션에 참여하지는 않지만, **영속 상태를 변경하거나 외부에 영향을 주는 동작도 없습니다.** 실제 돈을 청구하거나 예약 내역을 저장하거나 다른 서비스를 호출하지 않습니다. 재고, 계정 소유권, 가격을 검증하지도 않습니다. 전달받은 `total`이 이 연습에서 승인을 요청한 금액이며, 구매 결과에도 그 금액이 기록됩니다.

실제 결제사의 재시도, 분산 트랜잭션, 카드 정보, 네트워크 실패, 멱등성 요구사항은 고려하지 마세요. 각 요청은 의도적으로 발생한 별개의 구매입니다. 다만 서로 다른 구매자의 동시 요청도 재고 규칙을 지켜야 합니다. 캐시, 비동기 처리, 재시도 인터셉터는 없습니다.
