# Interactive Fitness

대화형 운동 기록 및 추천 Android 앱의 첫 번째 실행 가능한 골격입니다.

## 현재 구현

- Compose 기반 하단 내비게이션: 오늘, 대화, 대시보드, 기록
- Fake Repository의 샘플 운동 기록
- Room 기반 운동 기록 영속 저장
- 피로도, 근육통, 통증을 반영하는 규칙 기반 추천
- 추천 운동과 빠른 운동 기록 및 삭제 확인
- 운동 종류, 제목, 시간, RPE, 상세 내용 입력 및 기존 기록 수정
- 주간 횟수, 시간, 목표 진행률 집계
- 추천 엔진 단위 테스트

운동 기록은 기기의 Room 데이터베이스에 저장되어 앱을 다시 실행해도 유지됩니다.

## 실행

1. Android Studio에서 이 폴더를 엽니다.
2. JDK 17과 Android SDK 36을 선택합니다.
3. Gradle Sync 후 `app` 구성을 실행합니다.

## 다음 구현 순서

1. 운동 진행 및 세트/인터벌 입력
2. 화면 상태 복원과 오류 모델
3. Firebase Authentication 및 Firestore 동기화
4. Health Connect 지원 여부와 권한 화면

Firebase와 Health Connect 설정 없이도 로컬 모드가 계속 동작하도록 유지합니다.
