
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
  mission_count: number;
}

interface PlayerRow extends RowDataPacket {
  id: number;
}

interface GuessRow extends RowDataPacket {
  id: number;
  content: string;
}

@Injectable()
export class GameReceivedGuessesService {
  constructor(private readonly databaseService: DatabaseService) {}

  async getReceivedGuesses(gameId: number, memberToken: string) {
    // 게임 존재 여부 확인
    const [games] = await this.databaseService.pool.execute<GameRow[]>(
      `SELECT id, status, mission_count
       FROM games
       WHERE id = ?`,
      [gameId],
    );

    if (games.length === 0) {
      throw new NotFoundException('게임을 찾을 수 없습니다.');
    }

    if (['SETTING', 'PLAYING'].includes(games[0].status)) {
      throw new ConflictException(
        '게임 종료 후 상대의 추리를 조회할 수 있습니다.',
      );
    }

    // 현재 사용자가 게임 참가자인지 확인
    const [players] = await this.databaseService.pool.execute<PlayerRow[]>(
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
        '해당 게임의 참가자가 아닙니다.',
      );
    }

    const myPlayerId = players[0].id;

    // 상대방이 나에게 보낸 추리만 조회
    const [guesses] = await this.databaseService.pool.execute<GuessRow[]>(
      `SELECT id, content
       FROM guesses
       WHERE game_id = ?
         AND target_player_id = ?
       ORDER BY id ASC`,
      [gameId, myPlayerId],
    );

    return {
      gameId,
      guesses: guesses.map((guess, index) => ({
        guessId: guess.id,
        guessNo: index + 1,
        content: guess.content,
      })),
      partnerSubmissionCompleted:
        guesses.length === Number(games[0].mission_count),
    };
  }
}

@ApiTags('상대 미션 추리')
@ApiBearerAuth()
@Controller('games/:gameId/received-guesses')
export class GameReceivedGuessesController {
  constructor(
    private readonly service: GameReceivedGuessesService,
  ) {}

  @Get()
  @ApiOperation({
    summary: '상대가 보낸 추리 조회',
    description:
      '상대방이 내 미션에 대해 제출한 추리 목록을 조회합니다.',
  })
  @ApiOkResponse({
    schema: {
      example: {
        gameId: 1,
        guesses: [
          {
            guessId: 1,
            guessNo: 1,
            content: '먼저 사진 찍자고 하기',
          },
        ],
        partnerSubmissionCompleted: true,
      },
    },
  })
  getReceivedGuesses(
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

    return this.service.getReceivedGuesses(
      gameId,
      memberToken,
    );
  }
}
