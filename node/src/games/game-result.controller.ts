
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
    // 1. 게임 조회
    const [games] =
      await this.databaseService.pool.execute<GameRow[]>(
        `SELECT
           id,
           status,
           winner_player_id,
           decided_by
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

    // 2. 참가자와 선택한 벌칙 조회
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

    // 3. 요청자가 게임 참가자인지 확인
    const me = players.find(
      (player) => player.member_token === memberToken,
    );

    if (!me) {
      throw new ForbiddenException(
        '해당 게임의 참가자만 결과를 조회할 수 있습니다.',
      );
    }

    // 4. 게임 종료 여부 확인
    if (game.status !== 'FINISHED') {
      throw new ConflictException(
        '아직 최종 결과가 확정되지 않았습니다.',
      );
    }

    // 5. 무승부 여부 및 승자 확인
    const isDraw = game.decided_by === 'DRAW';

    const winner = players.find(
      (player) => player.id === game.winner_player_id,
    );

    if (!isDraw && !winner) {
      throw new ConflictException(
        '승자 정보가 아직 확정되지 않았습니다.',
      );
    }

    // 6. 상대방 및 패자 확인
    const partner = players.find(
      (player) => player.id !== me.id,
    );

    const loser = winner
      ? players.find(
          (player) => player.id !== winner.id,
        )
      : null;

    // 7. 결과 반환
    return {
      gameId: game.id,
      status: game.status,
      decidedBy: game.decided_by,

      // 무승부면 승자 없음
      winnerMemberId: winner?.member_id ?? null,

      myResult: me.result,
      partnerResult: partner?.result ?? null,

      players: players.map((player) => ({
        memberId: player.member_id,
        result: player.result,
      })),

      // 무승부면 벌칙 없음
      penalty:
        winner && winner.penalty_id !== null
          ? {
              id: winner.penalty_id,
              content: winner.penalty_content,
              assignedToMemberId:
                loser?.member_id ?? null,
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
      '게임 종료 후 승패와 벌칙을 조회합니다. ' +
      '정답을 맞히면 GUESS로 승패가 확정됩니다. ' +
      '양쪽 추리가 모두 오답이면 DRAW로 종료되며, ' +
      '양쪽 모두 벌칙이 없습니다. ' +
      '무승부 시 양쪽 스탬프 지급은 Spring Boot 연동 대상입니다.',
  })
  @ApiOkResponse({
    description: '최종 게임 결과 조회 성공',
    content: {
      'application/json': {
        schema: {
          type: 'object',
          properties: {
            gameId: {
              type: 'integer',
              example: 1,
            },
            status: {
              type: 'string',
              enum: ['FINISHED'],
            },
            decidedBy: {
              type: 'string',
              enum: ['GUESS', 'DRAW'],
            },
            winnerMemberId: {
              type: 'integer',
              nullable: true,
            },
            myResult: {
              type: 'string',
              enum: ['WIN', 'LOSE', 'DRAW'],
            },
            partnerResult: {
              type: 'string',
              enum: ['WIN', 'LOSE', 'DRAW'],
              nullable: true,
            },
            players: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  memberId: {
                    type: 'integer',
                  },
                  result: {
                    type: 'string',
                    enum: ['WIN', 'LOSE', 'DRAW'],
                  },
                },
              },
            },
            penalty: {
              type: 'object',
              nullable: true,
              properties: {
                id: {
                  type: 'integer',
                },
                content: {
                  type: 'string',
                },
                assignedToMemberId: {
                  type: 'integer',
                  nullable: true,
                },
              },
            },
          },
        },
        examples: {
          win: {
            summary: '정답을 맞힌 경우 - 승리',
            value: {
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
                content: '상대방 칭찬 3가지 하기',
                assignedToMemberId: 2,
              },
            },
          },
          lose: {
            summary: '상대가 정답을 맞힌 경우 - 패배',
            value: {
              gameId: 1,
              status: 'FINISHED',
              decidedBy: 'GUESS',
              winnerMemberId: 2,
              myResult: 'LOSE',
              partnerResult: 'WIN',
              players: [
                { memberId: 1, result: 'LOSE' },
                { memberId: 2, result: 'WIN' },
              ],
              penalty: {
                id: 2,
                content: '상대방 칭찬 3가지 하기',
                assignedToMemberId: 1,
              },
            },
          },
          draw: {
            summary: '양쪽 추리가 모두 오답인 경우 - 무승부',
            value: {
              gameId: 2,
              status: 'FINISHED',
              decidedBy: 'DRAW',
              winnerMemberId: null,
              myResult: 'DRAW',
              partnerResult: 'DRAW',
              players: [
                { memberId: 1, result: 'DRAW' },
                { memberId: 2, result: 'DRAW' },
              ],
              penalty: null,
            },
          },
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
