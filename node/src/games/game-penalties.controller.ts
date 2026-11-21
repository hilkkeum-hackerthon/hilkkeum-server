
import {
  BadRequestException,
  Body,
  ConflictException,
  Controller,
  ForbiddenException,
  Headers,
  Injectable,
  NotFoundException,
  Param,
  ParseIntPipe,
  Post,
  UnauthorizedException,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOperation,
  ApiProperty,
  ApiTags,
} from '@nestjs/swagger';
import { IsInt, Min } from 'class-validator';
import { RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';

class SelectPenaltyDto {
  @ApiProperty({ example: 1 })
  @IsInt()
  @Min(1)
  penaltyId!: number;
}

interface GameRow extends RowDataPacket {
  id: number;
  status: string;
}

interface PlayerRow extends RowDataPacket {
  id: number;
  penalty_id: number | null;
}

interface PenaltyRow extends RowDataPacket {
  id: number;
}

interface StatusRow extends RowDataPacket {
  status: string;
}

@Injectable()
export class GamePenaltiesService {
  constructor(private readonly databaseService: DatabaseService) {}

  async selectPenalty(
    gameId: number,
    memberToken: string,
    penaltyId: number,
  ) {
    const connection = await this.databaseService.pool.getConnection();

    try {
      await connection.beginTransaction();

      const [games] = await connection.execute<GameRow[]>(
        `SELECT id, status
         FROM games
         WHERE id = ?
         FOR UPDATE`,
        [gameId],
      );

      if (games.length === 0) {
        throw new NotFoundException('게임을 찾을 수 없습니다.');
      }

      if (games[0].status !== 'SETTING') {
        throw new ConflictException(
          '현재 벌칙을 선택할 수 없는 상태입니다.',
        );
      }

      const [players] = await connection.execute<PlayerRow[]>(
        `SELECT gp.id, gp.penalty_id
         FROM game_players gp
         JOIN room_members rm ON rm.id = gp.member_id
         WHERE gp.game_id = ?
           AND rm.member_token = ?
         LIMIT 1`,
        [gameId, memberToken],
      );

      if (players.length === 0) {
        throw new ForbiddenException(
          '해당 게임의 참가자가 아닙니다.',
        );
      }

      if (players[0].penalty_id !== null) {
        throw new ConflictException(
          '이미 벌칙을 선택했습니다.',
        );
      }

      const [penalties] = await connection.execute<PenaltyRow[]>(
        `SELECT id
         FROM penalties
         WHERE id = ? AND is_active = 1`,
        [penaltyId],
      );

      if (penalties.length === 0) {
        throw new BadRequestException(
          '유효하지 않은 벌칙입니다.',
        );
      }

      await connection.execute(
        `UPDATE game_players
         SET penalty_id = ?
         WHERE id = ?`,
        [penaltyId, players[0].id],
      );

      // 두 사람이 미션과 벌칙을 모두 선택하면 게임 진행
      await connection.execute(
        `UPDATE games
         SET status = 'PLAYING',
             started_at = CURRENT_TIMESTAMP
         WHERE id = ?
           AND status = 'SETTING'
           AND (
             SELECT COUNT(*)
             FROM game_players
             WHERE game_id = ?
           ) = 2
           AND (
             SELECT COUNT(*)
             FROM game_players
             WHERE game_id = ?
               AND mission_selected_at IS NOT NULL
               AND penalty_id IS NOT NULL
           ) = 2`,
        [gameId, gameId, gameId],
      );

      const [statuses] = await connection.execute<StatusRow[]>(
        `SELECT status FROM games WHERE id = ?`,
        [gameId],
      );

      await connection.commit();

      return {
        gameId,
        penaltyId,
        selectionCompleted: true,
        status: statuses[0].status,
      };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
}

@ApiTags('게임 벌칙 선택')
@ApiBearerAuth()
@Controller('games/:gameId/penalty')
export class GamePenaltiesController {
  constructor(
    private readonly gamePenaltiesService: GamePenaltiesService,
  ) {}

  @Post()
  @ApiOperation({
    summary: '내 벌칙 선택',
    description:
      '본인의 벌칙을 선택합니다. 두 참가자가 모든 선택을 마치면 게임이 시작됩니다.',
  })
  selectPenalty(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Body() dto: SelectPenaltyDto,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException(
        '멤버 토큰이 필요합니다.',
      );
    }

    return this.gamePenaltiesService.selectPenalty(
      gameId,
      memberToken,
      dto.penaltyId,
    );
  }
}
