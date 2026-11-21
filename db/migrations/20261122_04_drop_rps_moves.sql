-- 기존 DB(EC2)용 마이그레이션: 가위바위보 테이블 제거
-- 동점(맞힌 개수 동일)이면 승부를 가리지 않고 두 사람 모두 스탬프를 받으므로 가위바위보를 쓰지 않는다.
--
-- 실행 (EC2, 실행 전 백업):
--   cd ~/hilkkeum && set -a && . ./.env.prod && set +a
--   docker exec -i hilkkeum-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" "$DB_NAME" < db/migrations/20261122_04_drop_rps_moves.sql

SET NAMES utf8mb4;

DROP TABLE IF EXISTS rps_moves;

ALTER TABLE games
    MODIFY status VARCHAR(20) NOT NULL DEFAULT 'SETTING' COMMENT '게임 상태(SETTING 설정 → PLAYING 진행 중 → GUESSING 추리·판정 → SETTLEMENT 결산 → FINISHED 완료)';
