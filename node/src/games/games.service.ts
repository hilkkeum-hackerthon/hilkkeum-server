
import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { ResultSetHeader, RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';
import { CreateGameDto } from './dto/create-game.dto';

interface RoomRow extends RowDataPacket {
  id: number;
  status: string;
}

interface MemberRow extends RowDataPacket {
  id: number;
  role: string;
}

interface IdRow extends RowDataPacket {
  id: number;
}

interface CountRow extends RowDataPacket {
  total: number | string;
}

interface GameStatusRow extends RowDataPacket {
  id: number;
  status: string;
  date_type: string;
  mission_count: number;
  member_id: number;
}

interface GamePlayerStatusRow extends RowDataPacket {
  member_id: number;
  mission_selected_at: Date | null;
  penalty_id: number | null;
}

@Injectable()
export class GamesService {
  constructor(
    private readonly databaseService: DatabaseService,
  ) {}

  // ============================================
  // 1. 게임 생성 및 기본 설정
  // POST /api/v1/games
  // ============================================
  async createGame(dto: CreateGameDto, memberToken: string) {
    const connection =
      await this.databaseService.pool.getConnection();

    try {
      await connection.beginTransaction();

      // 1. 요청자 확인
      const [requesters] =
        await connection.execute<MemberRow[]>(
          `SELECT id, role
           FROM room_members
           WHERE room_id = ?
             AND member_token = ?
           LIMIT 1`,
          [dto.roomId, memberToken],
        );

      if (requesters.length === 0) {
        throw new UnauthorizedException(
          '유효하지 않은 멤버 토큰입니다.',
        );
      }

      // 2. 방 상태 확인 및 잠금
      const [rooms] =
        await connection.execute<RoomRow[]>(
          `SELECT id, status
           FROM rooms
           WHERE id = ?
           FOR UPDATE`,
          [dto.roomId],
        );

      if (
        rooms.length === 0 ||
        rooms[0].status !== 'ACTIVE'
      ) {
        throw new ConflictException(
          '게임을 시작할 수 없는 방입니다.',
        );
      }

      // 3. 참가자 2명 확인
      const [members] =
        await connection.execute<MemberRow[]>(
          `SELECT id, role
           FROM room_members
           WHERE room_id = ?
           ORDER BY id`,
          [dto.roomId],
        );

      if (members.length !== 2) {
        throw new ConflictException(
          '참가자 2명이 필요합니다.',
        );
      }

      // 4. 진행 중인 게임 중복 확인
      const [existingGames] =
        await connection.execute<IdRow[]>(
          `SELECT id
           FROM games
           WHERE room_id = ?
             AND status <> 'FINISHED'
           LIMIT 1`,
          [dto.roomId],
        );

      if (existingGames.length > 0) {
        throw new ConflictException(
          '이미 진행 중인 게임이 있습니다.',
        );
      }

      // 5. 미션 개수 및 데이트 유형 확인
      if (
        !['ONLINE', 'OFFLINE'].includes(dto.dateType)
      ) {
        throw new BadRequestException(
          '데이트 유형은 ONLINE 또는 OFFLINE이어야 합니다.',
        );
      }

      if (
        !Number.isInteger(dto.missionCount) ||
        dto.missionCount < 1 ||
        dto.missionCount > 3
      ) {
        throw new BadRequestException(
          '미션 개수는 1~3개여야 합니다.',
        );
      }

      // 6. 선택 가능한 미션이 충분한지 확인
      const [missionCounts] =
        await connection.execute<CountRow[]>(
          `SELECT COUNT(*) AS total
           FROM missions
           WHERE date_type = ?
             AND is_active = 1`,
          [dto.dateType],
        );

      const availableMissionCount =
        Number(missionCounts[0].total);

      if (availableMissionCount < dto.missionCount) {
        throw new ConflictException(
          `선택 가능한 미션이 부족합니다. 현재 ${availableMissionCount}개입니다.`,
        );
      }

      // 7. 선택 가능한 벌칙이 있는지 확인
      const [penaltyCounts] =
        await connection.execute<CountRow[]>(
          `SELECT COUNT(*) AS total
           FROM penalties
           WHERE is_active = 1`,
        );

      if (Number(penaltyCounts[0].total) === 0) {
        throw new ConflictException(
          '선택 가능한 벌칙이 없습니다.',
        );
      }

      // 8. 테마 없이 게임 생성
      const [result] =
        await connection.execute<ResultSetHeader>(
          `INSERT INTO games
           (
             room_id,
             started_by_member_id,
             date_type,
             mission_count,
             status
           )
           VALUES (?, ?, ?, ?, 'SETTING')`,
          [
            dto.roomId,
            requesters[0].id,
            dto.dateType,
            dto.missionCount,
          ],
        );

      const gameId = result.insertId;

      // 9. 두 참가자 등록
      await connection.execute(
        `INSERT INTO game_players (game_id, member_id)
         VALUES (?, ?), (?, ?)`,
        [
          gameId,
          members[0].id,
          gameId,
          members[1].id,
        ],
      );

      await connection.commit();

      return {
        gameId,
        roomId: dto.roomId,
        dateType: dto.dateType,
        missionCount: dto.missionCount,
        status: 'SETTING',
      };
    } catch (error) {
      await connection.rollback();

      if (
        (error as { code?: string }).code ===
        'ER_DUP_ENTRY'
      ) {
        throw new ConflictException(
          '이미 진행 중인 게임이 있습니다.',
        );
      }

      throw error;
    } finally {
      connection.release();
    }
  }

  // ============================================
  // 2. 게임 상태 및 진행 현황 조회
  // GET /api/v1/games/{gameId}
  // ============================================
  async getGameStatus(
    gameId: number,
    memberToken: string,
  ) {
    // 1. 게임 참가자인지 확인
    const [games] =
      await this.databaseService.pool.execute<
        GameStatusRow[]
      >(
        `SELECT
           g.id,
           g.status,
           g.date_type,
           g.mission_count,
           rm.id AS member_id
         FROM games g
         JOIN room_members rm
           ON rm.room_id = g.room_id
          AND rm.member_token = ?
         WHERE g.id = ?
         LIMIT 1`,
        [memberToken, gameId],
      );

    if (games.length === 0) {
      throw new NotFoundException(
        '게임이 존재하지 않거나 접근 권한이 없습니다.',
      );
    }

    const game = games[0];

    // 2. 두 참가자의 미션·벌칙 선택 상태 조회
    const [players] =
      await this.databaseService.pool.execute<
        GamePlayerStatusRow[]
      >(
        `SELECT
           member_id,
           mission_selected_at,
           penalty_id
         FROM game_players
         WHERE game_id = ?`,
        [gameId],
      );

    // 3. 본인과 상대방 구분
    const myPlayer = players.find(
      (player) => player.member_id === game.member_id,
    );

    const partnerPlayer = players.find(
      (player) => player.member_id !== game.member_id,
    );

    // 4. 미션과 벌칙 선택 완료 여부
    const isSelectionCompleted = (
      player?: GamePlayerStatusRow,
    ): boolean => {
      if (!player) {
        return false;
      }

      return (
        player.mission_selected_at !== null &&
        player.penalty_id !== null
      );
    };

    // 5. 게임 상태 반환
    return {
      gameId: game.id,
      status: game.status,
      dateType: game.date_type,
      missionCount: game.mission_count,
      mySelectionCompleted:
        isSelectionCompleted(myPlayer),
      partnerSelectionCompleted:
        isSelectionCompleted(partnerPlayer),
    };
  }
}
