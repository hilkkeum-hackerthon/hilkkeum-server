-- 기존 DB(EC2)용 마이그레이션: 스탬프 3개 → 소원권 자동 발급 규칙 반영
-- 새로 만드는 DB는 db/init 만으로 같은 결과가 되므로 이 파일을 실행하지 않는다.
--
-- 변경 내용
--  1. room_members.stamp_count: 0~3 제약 추가, 설명 변경 (3개면 자동 발급 · 사용/만료 시 0)
--  2. wish_coupons.status: 설명 변경 (AVAILABLE / USED / EXPIRED, 승인 단계 없음)
--
-- 실행 (EC2, 실행 전 백업):
--   cd ~/hilkkeum && set -a && . ./.env.prod && set +a
--   docker exec -i hilkkeum-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" "$DB_NAME" < db/migrations/20261122_02_auto_coupon.sql

SET NAMES utf8mb4;

-- 범위를 벗어난 값이 있으면 제약 추가가 실패하므로 먼저 보정
UPDATE room_members SET stamp_count = 3 WHERE stamp_count > 3;
UPDATE room_members SET stamp_count = 0 WHERE stamp_count < 0;

ALTER TABLE room_members
    MODIFY stamp_count INT NOT NULL DEFAULT 0 COMMENT '스탬프 수(0~3). 3개가 되면 소원권 자동 발급(스탬프 유지), 소원권 사용·만료 시 0',
    ADD CONSTRAINT chk_room_members_stamp_count CHECK (stamp_count BETWEEN 0 AND 3);

ALTER TABLE wish_coupons
    MODIFY status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '상태(AVAILABLE 사용 가능 / USED 보유자가 사용 / EXPIRED 1개월 경과) — 최대 1장 보유';
