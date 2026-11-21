
import {
  ConflictException,
  Controller,
  ForbiddenException,
  Get,
  Headers,
  Injectable,
  NotFoundException,
  Param,
  ParseIntPipe,
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
  winner_player_id: number | null;
  decided_by: string | null;
}

interface PlayerRow extends RowDataPacket {
  id: number;
  member_id: number;
  member_token: string;
  result: string | null;
  penalty_id: number | null;
  penalty_content: string | null;
}

@Injectable()
export class GameResultService {
  constructor(
    private readonly databaseService: DatabaseService,
  ) {}

  async getResult(gameId: number, memberToken: string) {
    // 게임 조회
    const [games] =
      await this.databaseService.pool.execute<GameRow[]>(
        `SELECT id, status, winner_player_id, decided_by
         FROM games
         WHERE id = ?`,
        [gameId],
      );

    if (games.length === 0) {
      throw new NotFoundException(
        '게임을 찾을 수 없습니다.',
      );
    }

    const game = games[0];

    // 게임 참가자 및 선택한 벌칙 조회
    const [players] =
      await this.databaseService.pool.execute<PlayerRow[]>(
        `SELECT
           gp.id,
           gp.member_id,
           rm.member_token,
           gp.result,
           gp.penalty_id,
           p.content AS penalty_content
         FROM game_players gp
         JOIN room_members rm
           ON rm.id = gp.member_id
         LEFT JOIN penalties p
           ON p.id = gp.penalty_id
         WHERE gp.game_id = ?
         ORDER BY gp.id ASC`,
        [gameId],
      );

    const me = players.find(
      (player) => player.member_token === memberToken,
    );

    if (!me) {
      throw new ForbiddenException(
        '해당 게임의 참가자만 결과를 조회할 수 있습니다.',
      );
    }

    if (game.status !== 'FINISHED') {
      throw new ConflictException(
        '아직 최종 결과가 확정되지 않았습니다.',
      );
    }

    const winner = players.find(
      (player) => player.id === game.winner_player_id,
    );

    if (!winner) {
      throw new ConflictException(
        '승자 정보가 아직 확정되지 않았습니다.',
      );
    }

    const partner = players.find(
      (player) => player.id !== me.id,
    );

    const loser = players.find(
      (player) => player.id !== winner.id,
    );

    return {
      gameId: game.id,
      status: game.status,
      decidedBy: game.decided_by,
      winnerMemberId: winner.member_id,
      myResult: me.result,
      partnerResult: partner?.result ?? null,
      players: players.map((player) => ({
        memberId: player.member_id,
        result: player.result,
      })),
      penalty:
        winner.penalty_id !== null
          ? {
              id: winner.penalty_id,
              content: winner.penalty_content,
              assignedToMemberId: loser?.member_id ?? null,
            }
          : null,
    };
  }
}

@ApiTags('게임 결과')
@ApiBearerAuth()
@Controller('games')
export class GameResultController {
  constructor(
    private readonly gameResultService: GameResultService,
  ) {}

  @Get(':gameId/result')
  @ApiOperation({
    summary: '최종 게임 결과 조회',
    description:
      '승패 확정 후 승자, 패자, 적용할 벌칙을 조회합니다.',
  })
  @ApiOkResponse({
    schema: {
      example: {
        gameId: 1,
        status: 'FINISHED',
        decidedBy: 'GUESS',
        winnerMemberId: 1,
        myResult: 'WIN',
        partnerResult: 'LOSE',
        players: [
          { memberId: 1, result: 'WIN' },
          { memberId: 2, result: 'LOSE' },
        ],
        penalty: {
          id: 1,
          content: '상대방의 장점 3가지 말하기',
          assignedToMemberId: 2,
        },
      },
    },
  })
  getResult(
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

    return this.gameResultService.getResult(
      gameId,
      memberToken,
    );
  }
}
