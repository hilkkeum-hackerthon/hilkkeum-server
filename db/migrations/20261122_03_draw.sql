-- 기존 DB(EC2)용 마이그레이션: 무승부(맞힌 개수 동일) 규칙 반영 — 컬럼 설명만 변경 (데이터 · 구조 변경 없음)
-- 무승부면 벌칙 없이 두 사람 모두 스탬프 1개 적립.
-- Node 기록 규칙: games.status='FINISHED', winner_player_id=NULL, decided_by='DRAW', game_players.result='DRAW'
--
-- 실행 (EC2):
--   cd ~/hilkkeum && set -a && . ./.env.prod && set +a
--   docker exec -i hilkkeum-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" "$DB_NAME" < db/migrations/20261122_03_draw.sql

SET NAMES utf8mb4;

ALTER TABLE games
    MODIFY decided_by VARCHAR(10) NULL COMMENT '승부 결정 방식(GUESS 맞힌 수 비교 승리 / DRAW 무승부 — 맞힌 개수 동일, 벌칙 없이 두 사람 모두 스탬프)';

ALTER TABLE game_players
    MODIFY result VARCHAR(10) NULL COMMENT '결과(WIN/LOSE/DRAW) — DRAW면 벌칙 없음';
