
import {
  ConflictException,
  Controller,
  ForbiddenException,
  Headers,
  HttpCode,
  Injectable,
  NotFoundException,
  Param,
  ParseIntPipe,
  Post,
  UnauthorizedException,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOkResponse,
  ApiOperation,
  ApiTags,
} from '@nestjs/swagger';
import { RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';

interface GameRow extends RowDataPacket {
  id: number;
  status: string;
}

interface PlayerRow extends RowDataPacket {
  id: number;
}

@Injectable()
export class GameEndService {
  constructor(private readonly databaseService: DatabaseService) {}

  async endGame(gameId: number, memberToken: string) {
    const connection =
      await this.databaseService.pool.getConnection();

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
        throw new NotFoundException(
          '게임을 찾을 수 없습니다.',
        );
      }

      const [players] = await connection.execute<PlayerRow[]>(
        `SELECT gp.id
         FROM game_players gp
         JOIN room_members rm ON rm.id = gp.member_id
         WHERE gp.game_id = ?
           AND rm.member_token = ?
         LIMIT 1`,
        [gameId, memberToken],
      );

      if (players.length === 0) {
        throw new ForbiddenException(
          '해당 게임의 참가자만 종료할 수 있습니다.',
        );
      }

      if (games[0].status !== 'PLAYING') {
        throw new ConflictException(
          '현재 종료할 수 없는 게임 상태입니다.',
        );
      }

      await connection.execute(
        `UPDATE games
         SET status = 'GUESSING',
             ended_at = CURRENT_TIMESTAMP
         WHERE id = ?`,
        [gameId],
      );

      await connection.commit();

      return {
        gameId,
        status: 'GUESSING',
        ended: true,
      };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
}

@ApiTags('게임 종료')
@ApiBearerAuth()
@Controller('games')
export class GameEndController {
  constructor(private readonly gameEndService: GameEndService) {}

  @Post(':gameId/end')
  @HttpCode(200)
  @ApiOperation({
    summary: '게임 즉시 종료',
    description:
      '게임 참가자 한 명이 종료하면 상대 승인 없이 추리 단계로 전환됩니다.',
  })
  @ApiOkResponse({
    schema: {
      example: {
        gameId: 1,
        status: 'GUESSING',
        ended: true,
      },
    },
  })
  endGame(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException(
        '멤버 토큰이 필요합니다.',
      );
    }

    return this.gameEndService.endGame(
      gameId,
      memberToken,
    );
  }
}
