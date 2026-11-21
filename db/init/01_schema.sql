-- 힐끔(hilkkeum) MySQL 스키마
-- MySQL 컨테이너 최초 기동 시(볼륨이 비어 있을 때) 자동 실행됨
-- 이미 실행된 뒤 스키마를 바꾸려면 마이그레이션 SQL을 직접 실행하거나 볼륨을 초기화해야 함

SET NAMES utf8mb4;

CREATE TABLE rooms (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '룸 ID',
    invite_code       VARCHAR(6)   NULL     COMMENT '초대 코드(6자). 방 성립 후에도 보관 → 재입력 시 "이미 사용된 코드" 안내',
    status            VARCHAR(10)  NOT NULL DEFAULT 'PENDING' COMMENT '상태(PENDING/ACTIVE/CLOSED)',
    invite_expires_at DATETIME     NULL     COMMENT '초대 코드 만료일시(미사용: 방 성립 전까지 코드 유효)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rooms_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '룸(커플)';

CREATE TABLE room_members (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '멤버 ID',
    room_id      BIGINT       NOT NULL COMMENT '룸 ID',
    role         VARCHAR(10)  NOT NULL COMMENT '역할(CREATOR/JOINER)',
    member_token VARCHAR(64)  NOT NULL COMMENT '식별 토큰(앱 보안저장소 저장)',
    stamp_count  INT          NOT NULL DEFAULT 0 COMMENT '스탬프 수(3개 차감 시 소원권 교환)',
    joined_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '참여일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_room_members_token (member_token),
    UNIQUE KEY uk_room_members_room_role (room_id, role),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES rooms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '룸 멤버';

CREATE TABLE themes (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '테마 ID',
    name        VARCHAR(50)   NOT NULL COMMENT '테마명',
    description VARCHAR(255)  NULL     COMMENT '설명',
    sort_order  INT           NOT NULL DEFAULT 0 COMMENT '정렬순서',
    is_active   TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '사용여부',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '테마';

CREATE TABLE missions (
    id         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '미션 ID',
    theme_id   BIGINT        NOT NULL COMMENT '테마 ID',
    date_type  VARCHAR(10)   NOT NULL COMMENT '데이트 유형(ONLINE/OFFLINE)',
    content    VARCHAR(255)  NOT NULL COMMENT '미션 내용',
    sort_order INT           NOT NULL DEFAULT 0 COMMENT '정렬순서',
    is_active  TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '사용여부',
    PRIMARY KEY (id),
    KEY ix_missions_theme_date_type (theme_id, date_type),
    CONSTRAINT fk_missions_theme FOREIGN KEY (theme_id) REFERENCES themes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '미션';

CREATE TABLE penalties (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '벌칙 ID',
    content     VARCHAR(255)  NOT NULL COMMENT '벌칙 내용',
    sort_order  INT           NOT NULL DEFAULT 0 COMMENT '정렬순서',
    is_active   TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '사용여부',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '벌칙';

CREATE TABLE games (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '게임 ID',
    room_id              BIGINT       NOT NULL COMMENT '룸 ID',
    started_by_member_id BIGINT       NOT NULL COMMENT '라운드를 만든 방장(CREATOR) 멤버 ID — 방장만 설정 가능',
    date_type            VARCHAR(10)  NULL     COMMENT '데이트 유형(ONLINE/OFFLINE)',
    theme_id             BIGINT       NULL     COMMENT '테마(미션 종류) ID',
    mission_count        INT          NULL     COMMENT '미션 개수(1~3, 두 사람 공통)',
    status               VARCHAR(20)  NOT NULL DEFAULT 'WAITING' COMMENT '라운드 상태(WAITING 대기/SETTING 설정/PLAYING 진행 중/END_REQUESTED 종료 요청/SCORING 추리·채점/SETTLEMENT 결산/FINISHED 완료)',
    winner_player_id     BIGINT       NULL     COMMENT '승자 플레이어 ID',
    decided_by           VARCHAR(10)  NULL     COMMENT '승부 결정 방식(SCORE/RPS)',
    started_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '시작일시',
    end_requested_by_member_id BIGINT NULL     COMMENT '종료를 요청한 멤버 ID',
    end_requested_at     DATETIME     NULL     COMMENT '종료 요청일시',
    ended_at             DATETIME     NULL     COMMENT '종료 확정일시(추리/채점 시작)',
    stamp_awarded_at     DATETIME     NULL     COMMENT '승자 스탬프 적립일시(중복 적립 방지)',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    active_room_id       BIGINT       AS (CASE WHEN status <> 'FINISHED' THEN room_id END) STORED COMMENT '진행 중 게임의 룸 ID(종료 시 NULL)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_games_one_active_per_room (active_room_id),
    CONSTRAINT chk_games_mission_count CHECK (mission_count IS NULL OR mission_count BETWEEN 1 AND 3),
    CONSTRAINT chk_games_status CHECK (status IN ('WAITING', 'SETTING', 'PLAYING', 'END_REQUESTED', 'SCORING', 'SETTLEMENT', 'FINISHED')),
    CONSTRAINT fk_games_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_games_started_by FOREIGN KEY (started_by_member_id) REFERENCES room_members (id),
    CONSTRAINT fk_games_end_requested_by FOREIGN KEY (end_requested_by_member_id) REFERENCES room_members (id),
    CONSTRAINT fk_games_theme FOREIGN KEY (theme_id) REFERENCES themes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '게임';

CREATE TABLE game_players (
    id                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '플레이어 ID',
    game_id              BIGINT        NOT NULL COMMENT '게임 ID',
    member_id            BIGINT        NOT NULL COMMENT '멤버 ID',
    penalty_id           BIGINT        NULL     COMMENT '상대에게 줄 벌칙 ID(내가 선택)',
    score                INT           NULL     COMMENT '최종 점수',
    result               VARCHAR(10)   NULL     COMMENT '결과(WIN/LOSE)',
    mission_selected_at  DATETIME      NULL     COMMENT '미션 선택 완료일시',
    score_submitted_at   DATETIME      NULL     COMMENT '채점 입력 완료일시',
    judged_at            DATETIME      NULL     COMMENT '추리 판정 완료일시',
    penalty_done_at      DATETIME      NULL     COMMENT '벌칙 수행일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_players_game_member (game_id, member_id),
    CONSTRAINT fk_game_players_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT fk_game_players_member FOREIGN KEY (member_id) REFERENCES room_members (id),
    CONSTRAINT fk_game_players_penalty FOREIGN KEY (penalty_id) REFERENCES penalties (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '게임 플레이어';

CREATE TABLE player_missions (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '선택 미션 ID',
    game_player_id BIGINT      NOT NULL COMMENT '플레이어 ID',
    mission_id     BIGINT      NOT NULL COMMENT '미션 ID',
    is_completed   TINYINT(1)  NULL     COMMENT '수행 여부(채점 전 NULL)',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_player_missions (game_player_id, mission_id),
    CONSTRAINT fk_player_missions_player FOREIGN KEY (game_player_id) REFERENCES game_players (id),
    CONSTRAINT fk_player_missions_mission FOREIGN KEY (mission_id) REFERENCES missions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '플레이어 선택 미션';

CREATE TABLE guesses (
    id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '추리 ID',
    game_id          BIGINT      NOT NULL COMMENT '게임 ID',
    writer_player_id BIGINT      NOT NULL COMMENT '추리한 플레이어 ID',
    target_player_id BIGINT      NOT NULL COMMENT '미션 주인 플레이어 ID',
    content          TEXT        NOT NULL COMMENT '추리 내용(수기)',
    is_correct       TINYINT(1)  NULL     COMMENT '정답 여부(미션 주인 판정)',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (id),
    CONSTRAINT fk_guesses_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT fk_guesses_writer FOREIGN KEY (writer_player_id) REFERENCES game_players (id),
    CONSTRAINT fk_guesses_target FOREIGN KEY (target_player_id) REFERENCES game_players (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '추리';

CREATE TABLE rps_moves (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '가위바위보 ID',
    game_id        BIGINT       NOT NULL COMMENT '게임 ID',
    round_no       INT          NOT NULL COMMENT '판 번호(비기면 +1)',
    game_player_id BIGINT       NOT NULL COMMENT '플레이어 ID',
    choice         VARCHAR(10)  NOT NULL COMMENT '낸 손(ROCK/PAPER/SCISSORS)',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rps_moves (game_id, round_no, game_player_id),
    CONSTRAINT fk_rps_moves_game FOREIGN KEY (game_id) REFERENCES games (id),
    CONSTRAINT fk_rps_moves_player FOREIGN KEY (game_player_id) REFERENCES game_players (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '가위바위보';

CREATE TABLE wish_coupons (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '소원권 ID',
    room_id         BIGINT        NOT NULL COMMENT '룸 ID',
    owner_member_id BIGINT        NOT NULL COMMENT '보유 멤버 ID',
    issued_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급일시(스탬프 교환 시각)',
    expires_at       DATETIME      NOT NULL COMMENT '만료일시(발급+1개월)',
    status           VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE' COMMENT '상태(AVAILABLE/USE_REQUESTED/USED/EXPIRED)',
    wish_content     VARCHAR(255)  NULL     COMMENT '소원 내용',
    use_requested_at DATETIME      NULL     COMMENT '사용 요청일시',
    responded_at     DATETIME      NULL     COMMENT '상대 응답일시(승인/거절)',
    used_at          DATETIME      NULL     COMMENT '사용 완료일시(승인 시)',
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    PRIMARY KEY (id),
    KEY ix_wish_coupons_owner (owner_member_id, status, expires_at),
    CONSTRAINT fk_wish_coupons_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_wish_coupons_owner FOREIGN KEY (owner_member_id) REFERENCES room_members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT = '소원권';

ALTER TABLE games ADD CONSTRAINT fk_games_winner FOREIGN KEY (winner_player_id) REFERENCES game_players (id);
