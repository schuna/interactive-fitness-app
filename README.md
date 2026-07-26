# Interactive Fitness

대화형 운동 기록 및 추천 Android 앱의 첫 번째 실행 가능한 골격입니다.

## 현재 구현

- Compose 기반 하단 내비게이션: 오늘, 대화, 대시보드, 기록
- Fake Repository의 샘플 운동 기록
- Room 기반 운동 기록 영속 저장
- 피로도, 근육통, 통증을 반영하는 규칙 기반 추천
- 추천 운동과 빠른 운동 기록 및 삭제 확인
- 운동 종류, 제목, 시간, RPE, 상세 내용 입력 및 기존 기록 수정
- 웨이트 세트별 종목, 중량, 반복 수와 RPE 기록
- 달리기·사이클 인터벌별 시간, 거리와 메모 기록
- Room 스키마 v2와 기존 데이터 보존 마이그레이션
- 추천 운동 진행 화면과 단계별 완료 처리
- 60초 휴식 타이머, ±15초 조정 및 건너뛰기
- 운동 취소 확인과 종료 후 Room 기록 저장
- 화면 회전 및 앱 프로세스 재실행 후 진행 세션 복원
- 절대 종료 시각 기반 휴식 타이머 복원
- 저장·삭제 실패 공통 오류 코드와 사용자 오류 대화상자
- 오늘의 추천 운동 완료 표시와 중복 시작 방지
- 추천 ID·날짜를 저장하는 Room 스키마 v3 마이그레이션
- 최대 50건의 개인정보 비포함 오류 기록과 사용자 진단 화면
- Firebase 설정 유무를 감지하는 로컬 전용 안전 모드
- Firebase 익명 인증과 UID별 Firestore 운동 기록 동기화
- 수동 재동기화와 동기화 상태·오류 진단
- 주간 횟수, 시간, 목표 진행률 집계
- 추천 엔진 단위 테스트

운동 기록은 기기의 Room 데이터베이스에 저장되어 앱을 다시 실행해도 유지됩니다.

## 실행

1. Android Studio에서 이 폴더를 엽니다.
2. JDK 17과 Android SDK 36을 선택합니다.
3. Gradle Sync 후 `app` 구성을 실행합니다.

## 다음 구현 순서

1. Health Connect 지원 여부와 권한 화면

## Firebase 연결

Firebase 설정이 없어도 앱은 Room 기반 로컬 전용 모드로 실행됩니다.

1. Firebase Console에서 Android 앱 `com.openai.interactivefitness`를 등록합니다.
2. `google-services.json`을 `app/google-services.json`에 둡니다.
3. Authentication에서 익명 로그인을 활성화합니다.
4. Cloud Firestore를 생성합니다.
5. `firebase deploy --only firestore:rules`로 포함된 보안 규칙을 배포합니다.
6. Gradle Sync 후 앱의 진단 화면에서 Firebase 상태를 확인합니다.

Firebase BoM은 현재 Kotlin 1.9 빌드 체인과 호환되는 `32.8.1`로 고정되어 있습니다.

Firebase와 Health Connect 설정 없이도 로컬 모드가 계속 동작하도록 유지합니다.
