
import {
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
  constructor(private readonly databaseService: DatabaseService) {}

  // ============================================
  // ① 게임 생성 및 기본 설정
  // POST /api/v1/games
  // ============================================
  async createGame(dto: CreateGameDto, memberToken: string) {
    const connection = await this.databaseService.pool.getConnection();

    try {
      await connection.beginTransaction();

      // 1. 요청자 확인
      const [requesters] = await connection.execute<MemberRow[]>(
        `SELECT id, role
         FROM room_members
         WHERE room_id = ? AND member_token = ?
         LIMIT 1`,
        [dto.roomId, memberToken],
      );

      if (requesters.length === 0) {
        throw new UnauthorizedException(
          '유효하지 않은 멤버 토큰입니다.',
        );
      }

      // 방장(CREATOR)만 게임을 시작할 수 있음
      if (requesters[0].role !== 'CREATOR') {
        throw new ForbiddenException(
          '방장만 게임을 시작할 수 있습니다.',
        );
      }

      // 2. 방 상태 확인 및 잠금
      const [rooms] = await connection.execute<RoomRow[]>(
        `SELECT id, status
         FROM rooms
         WHERE id = ?
         FOR UPDATE`,
        [dto.roomId],
      );

      if (rooms.length === 0 || rooms[0].status !== 'ACTIVE') {
        throw new ConflictException(
          '게임을 시작할 수 없는 방입니다.',
        );
      }

      // 3. 참가자 2명 확인
      const [members] = await connection.execute<MemberRow[]>(
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
      const [existingGames] = await connection.execute<IdRow[]>(
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

      // 6. 게임 생성
      const [result] = await connection.execute<ResultSetHeader>(
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

      // 7. 게임 참가자 두 명 등록
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

      // 8. 모든 저장이 성공했을 때 확정
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

      // DB의 중복 게임 방지 제약조건
      if ((error as { code?: string }).code === 'ER_DUP_ENTRY') {
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
  // ② 게임 상태·진행현황 조회
  // GET /api/v1/games/{gameId}
  // ============================================
  async getGameStatus(gameId: number, memberToken: string) {
    // 1. 요청자가 해당 게임 참가자인지 확인
    const [games] = await this.databaseService.pool.execute<
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

    // 2. 두 참가자의 미션·벌칙 선택 상태 확인
    const [players] = await this.databaseService.pool.execute<
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

    // 3. 내 게임 참가자 정보
    const myPlayer = players.find(
      (player) => player.member_id === game.member_id,
    );

    // 4. 상대방 게임 참가자 정보
    const partnerPlayer = players.find(
      (player) => player.member_id !== game.member_id,
    );

    // 5. 미션과 벌칙을 모두 선택했는지 검사
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

    // 6. 게임 진행 상태 반환
    return {
      gameId: game.id,
      status: game.status,
      dateType: game.date_type,
      missionCount: game.mission_count,
      mySelectionCompleted: isSelectionCompleted(myPlayer),
      partnerSelectionCompleted: isSelectionCompleted(partnerPlayer),
    };
  }
}
