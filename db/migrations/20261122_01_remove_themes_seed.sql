-- 기존 DB(EC2)용 마이그레이션: db/init/01_schema.sql · 02_seed.sql 변경분을 적용한다.
-- 새로 만드는 DB는 db/init 만으로 같은 결과가 되므로 이 파일을 실행하지 않는다.
--
-- 변경 내용
--  1. 테마 제거 (themes 테이블, missions.theme_id, games.theme_id)
--  2. 미션: 온/오프라인 각 7개 고정 (date_type + sort_order 1~7 유일)
--  3. 벌칙: 공통 5개 고정 (sort_order 1~5 유일)
--  4. 게임 상태: SETTING → PLAYING → GUESSING → SETTLEMENT → FINISHED (Node 흐름과 일치)
--  5. 종료 요청 단계 제거: 한 명이라도 종료를 누르면 즉시 종료 (end_requested_* 컬럼 제거)
--  6. 미션 수행 여부 제거: 모든 미션은 성공으로 간주 (player_missions.is_completed 제거)
--  7. 미션 · 벌칙 시드
--
-- 실행 (EC2):
--   cd ~/hilkkeum && set -a && . ./.env.prod && set +a
--   docker exec -i hilkkeum-mysql mysql -uroot -p"$DB_ROOT_PASSWORD" "$DB_NAME" < db/migrations/20261122_01_remove_themes_seed.sql

SET NAMES utf8mb4;

-- ── games ─────────────────────────────────────────────
ALTER TABLE games
    DROP FOREIGN KEY fk_games_theme,
    DROP FOREIGN KEY fk_games_end_requested_by,
    DROP CHECK chk_games_status;

UPDATE games SET status = 'SETTING'  WHERE status = 'WAITING';
UPDATE games SET status = 'GUESSING' WHERE status IN ('END_REQUESTED', 'SCORING');

ALTER TABLE games
    DROP COLUMN theme_id,
    DROP COLUMN end_requested_by_member_id,
    DROP COLUMN end_requested_at,
    MODIFY started_by_member_id BIGINT NOT NULL COMMENT '게임을 시작한 방장(CREATOR) 멤버 ID — 방장만 시작 가능',
    MODIFY status VARCHAR(20) NOT NULL DEFAULT 'SETTING' COMMENT '게임 상태(SETTING 설정 → PLAYING 진행 중 → GUESSING 추리·판정 → SETTLEMENT 결산(동점 가위바위보) → FINISHED 완료)',
    MODIFY ended_at DATETIME NULL COMMENT '종료일시 — 한 명이라도 종료를 누르면 즉시 종료(추리 단계 시작)',
    ADD CONSTRAINT chk_games_status CHECK (status IN ('SETTING', 'PLAYING', 'GUESSING', 'SETTLEMENT', 'FINISHED'));

-- ── missions / themes ─────────────────────────────────
ALTER TABLE missions DROP FOREIGN KEY fk_missions_theme;

ALTER TABLE missions
    DROP INDEX ix_missions_theme_date_type,
    DROP COLUMN theme_id,
    MODIFY date_type VARCHAR(10) NOT NULL COMMENT '데이트 유형(ONLINE/OFFLINE) — 유형별 7개 고정',
    MODIFY sort_order INT NOT NULL COMMENT '정렬순서(1~7)',
    ADD UNIQUE KEY uk_missions_date_type_order (date_type, sort_order),
    ADD CONSTRAINT chk_missions_date_type CHECK (date_type IN ('ONLINE', 'OFFLINE')),
    ADD CONSTRAINT chk_missions_sort_order CHECK (sort_order BETWEEN 1 AND 7);

DROP TABLE themes;

-- ── penalties ─────────────────────────────────────────
ALTER TABLE penalties
    MODIFY content VARCHAR(255) NOT NULL COMMENT '벌칙 내용 (온/오프라인 공통 5개)',
    MODIFY sort_order INT NOT NULL COMMENT '정렬순서(1~5)',
    ADD UNIQUE KEY uk_penalties_order (sort_order),
    ADD CONSTRAINT chk_penalties_sort_order CHECK (sort_order BETWEEN 1 AND 5);

-- ── game_players / player_missions ────────────────────
ALTER TABLE game_players
    MODIFY score_submitted_at DATETIME NULL COMMENT '추리 제출 완료일시';

ALTER TABLE player_missions
    DROP COLUMN is_completed,
    COMMENT = '플레이어 선택 미션 (모두 수행한 것으로 간주, 상대가 추리)';

-- ── seed ──────────────────────────────────────────────
INSERT INTO missions (date_type, content, sort_order) VALUES
    ('OFFLINE', '상대방에게 오늘의 칭찬을 세 번 말하기', 1),
    ('OFFLINE', '먼저 함께 사진 찍자고 제안하기', 2),
    ('OFFLINE', '평소와 다른 애칭으로 한 번 불러보기', 3),
    ('OFFLINE', '상대방이 좋아하는 음식을 먼저 추천하기', 4),
    ('OFFLINE', '먼저 손잡자고 제안하기', 5),
    ('OFFLINE', '상대방의 오늘 옷차림을 구체적으로 칭찬하기', 6),
    ('OFFLINE', '데이트 중 상대방에게 고맙다고 말하기', 7),
    ('ONLINE',  '상대방에게 먼저 안부 메시지 보내기', 1),
    ('ONLINE',  '귀여운 이모티콘을 먼저 보내기', 2),
    ('ONLINE',  '상대방을 구체적으로 칭찬하는 메시지 보내기', 3),
    ('ONLINE',  '함께 찍은 사진을 먼저 보내기', 4),
    ('ONLINE',  '음성 메시지로 고맙다고 전하기', 5),
    ('ONLINE',  '먼저 영상통화를 제안하기', 6),
    ('ONLINE',  '다음 데이트에서 하고 싶은 일 말하기', 7);

INSERT INTO penalties (content, sort_order) VALUES
    ('귀여운 포즈로 사진 한 장 찍기', 1),
    ('상대방이 고른 노래 한 소절 부르기', 2),
    ('10초 동안 눈을 마주치며 칭찬하기', 3),
    ('상대방에게 짧은 손편지 써 주기', 4),
    ('다음 데이트 간식 사 주기', 5);
