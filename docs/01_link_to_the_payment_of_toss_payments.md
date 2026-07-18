# Toss Payments 결제 연동하기

<img src="/imges/payment-widget-flow.png">

## API 키 발급하기

[Toss Payments 개발자 센터](https://docs.tosspayments.com/reference/using-api/api-keys)

>토스페이먼츠는 개발자의 연동 편의를 위해 라이브 환경과 비슷한 테스트 환경을 제공하고 있다. 테스트 환경에서는 카드 번호와 같은 실제 결제 정보를 입력해도, 결제는 가상으로 승인돼요. 따라서 테스트 환경에서는 결제가 승인되어도 실제 결제수단에서 돈이 출금되지 않는다.

API KEY 의 시크릿 키 부분을 가져와서 다음과 같이 어플리케이션에서 사용할 수 있도록 application.yaml 에 추가해준다. 

```yaml
PSP:
  toss:
    secretKey: test_...
    url: https://api.tosspayments.com
```

## 결제 위젯 연동하기

Toss에서 제공하는 다음 [토스 결제 연동 샘플 프로젝트](https://github.com/tosspayments/tosspayments-sample)를 참고해서 작성하면 된다.

- 결제 연동을 담당하는 `Checkout.html` 페이지를 가지고 온다.

```html
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="utf-8" />
    <link rel="icon" href="https://static.toss.im/icons/png/4x/icon-toss-logo.png" />
    <link rel="stylesheet" type="text/css" href="/style.css" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>토스페이먼츠 샘플 프로젝트</title>
    <!-- SDK 추가 -->
    <script src="https://js.tosspayments.com/v2/standard"></script>
</head>

<body>
<!-- 주문서 영역 -->
<div class="wrapper">
    <div class="box_section" style="padding: 40px 30px 50px 30px; margin-top: 30px; margin-bottom: 50px">
        <!-- 결제 UI -->
        <div id="payment-method"></div>
        <!-- 이용약관 UI -->
        <div id="agreement"></div>
        <!-- 쿠폰 체크박스 -->
        <div style="padding-left: 30px">
            <div class="checkable typography--p">
                <label for="coupon-box" class="checkable__label typography--regular"
                ><input id="coupon-box" class="checkable__input" type="checkbox" aria-checked="true" /><span class="checkable__label-text">5,000원 쿠폰 적용</span></label
                >
            </div>
        </div>
        <!-- 결제하기 버튼 -->
        <button class="button" id="payment-button" style="margin-top: 30px">결제하기</button>
    </div>
</div>
<script>
    main();

    async function main() {
        const button = document.getElementById("payment-button");
        const coupon = document.getElementById("coupon-box");
        const amount = {
            currency: "KRW",
            value: 50000,
        };
        // ------  결제위젯 초기화 ------
        // TODO: clientKey는 개발자센터의 결제위젯 연동 키 > 클라이언트 키로 바꾸세요.
        // TODO: 구매자의 고유 아이디를 불러와서 customerKey로 설정하세요. 이메일・전화번호와 같이 유추가 가능한 값은 안전하지 않습니다.
        // @docs https://docs.tosspayments.com/sdk/v2/js#토스페이먼츠-초기화
        const clientKey = "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
        const customerKey = generateRandomString();
        const tossPayments = TossPayments(clientKey);
        // 회원 결제
        // @docs https://docs.tosspayments.com/sdk/v2/js#tosspaymentswidgets
        const widgets = tossPayments.widgets({
            customerKey,
        });
        // 비회원 결제
        // const widgets = tossPayments.widgets({customerKey: TossPayments.ANONYMOUS});

        // ------  주문서의 결제 금액 설정 ------
        // TODO: 위젯의 결제금액을 결제하려는 금액으로 초기화하세요.
        // TODO: renderPaymentMethods, renderAgreement, requestPayment 보다 반드시 선행되어야 합니다.
        // @docs https://docs.tosspayments.com/sdk/v2/js#widgetssetamount
        await widgets.setAmount(amount);

        await Promise.all([
            // ------  결제 UI 렌더링 ------
            // @docs https://docs.tosspayments.com/sdk/v2/js#widgetsrenderpaymentmethods
            widgets.renderPaymentMethods({
                selector: "#payment-method",
                // 렌더링하고 싶은 결제 UI의 variantKey
                // 결제 수단 및 스타일이 다른 멀티 UI를 직접 만들고 싶다면 계약이 필요해요.
                // @docs https://docs.tosspayments.com/guides/v2/payment-widget/admin#새로운-결제-ui-추가하기
                variantKey: "DEFAULT",
            }),
            // ------  이용약관 UI 렌더링 ------
            // @docs https://docs.tosspayments.com/sdk/v2/js#widgetsrenderagreement
            widgets.renderAgreement({
                selector: "#agreement",
                variantKey: "AGREEMENT",
            }),
        ]);

        // ------  주문서의 결제 금액이 변경되었을 경우 결제 금액 업데이트 ------
        // @docs https://docs.tosspayments.com/sdk/v2/js#widgetssetamount
        coupon.addEventListener("change", async function () {
            if (coupon.checked) {
                await widgets.setAmount({
                    currency: "KRW",
                    value: amount.value - 5000,
                });

                return;
            }

            await widgets.setAmount({
                currency: "KRW",
                value: amount.value,
            });
        });

        // ------ '결제하기' 버튼 누르면 결제창 띄우기 ------
        // @docs https://docs.tosspayments.com/sdk/v2/js#widgetsrequestpayment
        button.addEventListener("click", async function () {
            // 결제를 요청하기 전에 orderId, amount를 서버에 저장하세요.
            // 결제 과정에서 악의적으로 결제 금액이 바뀌는 것을 확인하는 용도입니다.
            await widgets.requestPayment({
                orderId: generateRandomString(),
                orderName: "토스 티셔츠 외 2건",
                successUrl: window.location.origin + "/widget/success.html",
                failUrl: window.location.origin + "/fail.html",
                customerEmail: "customer123@gmail.com",
                customerName: "김토스",
                // 가상계좌 안내, 퀵계좌이체 휴대폰 번호 자동 완성에 사용되는 값입니다. 필요하다면 주석을 해제해 주세요.
                // customerMobilePhone: "01012341234",
            });
        });
    }

    function generateRandomString() {
        return window.btoa(Math.random()).slice(0, 20);
    }
</script>
</body>
</html>
```
**결제 연동을 하는 Checkout.html 에서 다음 변수 부분을 수정하면 된다:**
- **amount** ← 이후 서버에서 결제 금액을 가지고 온 후 설정
- **clientKey** ← Toss Payment API KEY 에서 발급받은 clientKey 로 설정
- **orderId** ← 결제 주문을 식별할 Unique 한 orderId 를 만들고 이를 서버에서 가지고 온 후 설정
- **orderName** ← 결제 주문의 이름을 서버에서 가지고 온 후 설정
- **successUrl** ← 결제가 성공한 이후 리다이렉션 될 URL 을 설정
- **failUrl** ← 결제가 실패한 이후 리다이렉션 될 URL 을 설정

결제가 실패 했을 때 리다이렉션 되는 fail.html 와 성공 했을 때 리다이렉션 되는 success.html 도 설정해주면 된다. 