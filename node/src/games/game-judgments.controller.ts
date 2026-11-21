
import {
  Body,
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
  ApiProperty,
  ApiTags,
} from '@nestjs/swagger';
import { IsBoolean, IsInt, Min } from 'class-validator';
import { RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';

class JudgeGuessDto {
  @ApiProperty({ example: 1 })
  @IsInt()
  @Min(1)
  guessId!: number;

  @ApiProperty({ example: true })
  @IsBoolean()
  isCorrect!: boolean;
}

interface GameRow extends RowDataPacket {
  id: number;
  status: string;
  mission_count: number | null;
}

interface PlayerRow extends RowDataPacket {
  id: number;
}

interface GuessRow extends RowDataPacket {
  id: number;
  writer_player_id: number;
  target_player_id: number;
  is_correct: number | null;
}

interface GuessStatsRow extends RowDataPacket {
  total: number;
  pending: number;
  correct: number;
}

@Injectable()
export class GameJudgmentsService {
  constructor(
    private readonly databaseService: DatabaseService,
  ) {}

  async judgeGuess(
    gameId: number,
    memberToken: string,
    dto: JudgeGuessDto,
  ) {
    const connection =
      await this.databaseService.pool.getConnection();

    try {
      await connection.beginTransaction();

      // 1. 게임 조회 및 동시 판정 방지
      const [games] = await connection.execute<GameRow[]>(
        `SELECT id, status, mission_count
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

      const game = games[0];

      if (game.status !== 'GUESSING') {
        throw new ConflictException(
          '현재 정답을 확인할 수 없는 게임 상태입니다.',
        );
      }

      // 2. 현재 사용자가 게임 참가자인지 확인
      const [players] =
        await connection.execute<PlayerRow[]>(
          `SELECT gp.id
           FROM game_players gp
           JOIN room_members rm
             ON rm.id = gp.member_id
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

      // 3. 판정할 추리 조회
      const [guesses] =
        await connection.execute<GuessRow[]>(
          `SELECT
             id,
             writer_player_id,
             target_player_id,
             is_correct
           FROM guesses
           WHERE id = ?
             AND game_id = ?
           LIMIT 1`,
          [dto.guessId, gameId],
        );

      if (guesses.length === 0) {
        throw new NotFoundException(
          '추리 내용을 찾을 수 없습니다.',
        );
      }

      const guess = guesses[0];

      // 4. 본인에게 제출된 추리만 판정 가능
      if (guess.target_player_id !== players[0].id) {
        throw new ForbiddenException(
          '본인에게 제출된 추리만 확인할 수 있습니다.',
        );
      }

      if (guess.is_correct !== null) {
        throw new ConflictException(
          '이미 정답 여부를 확인한 추리입니다.',
        );
      }

      // 5. 정답 여부 저장
      await connection.execute(
        `UPDATE guesses
         SET is_correct = ?
         WHERE id = ?`,
        [dto.isCorrect ? 1 : 0, guess.id],
      );

      let status: 'GUESSING' | 'FINISHED' = 'GUESSING';
      let winnerPlayerId: number | null = null;

      if (dto.isCorrect) {
        // 6-A. 정답이면 추리 작성자가 즉시 승리
        winnerPlayerId = guess.writer_player_id;

        await connection.execute(
          `UPDATE games
           SET status = 'FINISHED',
               winner_player_id = ?,
               decided_by = 'GUESS'
           WHERE id = ?`,
          [winnerPlayerId, gameId],
        );

        await connection.execute(
          `UPDATE game_players
           SET result = CASE
             WHEN id = ? THEN 'WIN'
             ELSE 'LOSE'
           END
           WHERE game_id = ?`,
          [winnerPlayerId, gameId],
        );

        status = 'FINISHED';
      } else {
        // 6-B. 오답이면 전체 추리의 판정 현황 확인
        const [stats] =
          await connection.execute<GuessStatsRow[]>(
            `SELECT
               COUNT(*) AS total,
               COALESCE(
                 SUM(
                   CASE
                     WHEN is_correct IS NULL THEN 1
                     ELSE 0
                   END
                 ),
                 0
               ) AS pending,
               COALESCE(
                 SUM(
                   CASE
                     WHEN is_correct = 1 THEN 1
                     ELSE 0
                   END
                 ),
                 0
               ) AS correct
             FROM guesses
             WHERE game_id = ?`,
            [gameId],
          );

        // 두 사람이 각각 mission_count개씩 추리해야 함
        const expectedGuessCount =
          game.mission_count !== null
            ? game.mission_count * 2
            : null;

        const allGuessesSubmitted =
          expectedGuessCount !== null &&
          Number(stats[0].total) === expectedGuessCount;

        const allGuessesJudged =
          Number(stats[0].pending) === 0;

        const allWrong =
          Number(stats[0].correct) === 0;

        // 두 사람 모두 제출 + 전부 판정 + 정답 0개
        if (
          allGuessesSubmitted &&
          allGuessesJudged &&
          allWrong
        ) {
          await connection.execute(
            `UPDATE games
             SET status = 'FINISHED',
                 winner_player_id = NULL,
                 decided_by = 'DRAW'
             WHERE id = ?`,
            [gameId],
          );

          await connection.execute(
            `UPDATE game_players
             SET result = 'DRAW'
             WHERE game_id = ?`,
            [gameId],
          );

          status = 'FINISHED';
        }
      }

      await connection.commit();

      return {
        gameId,
        guessId: guess.id,
        isCorrect: dto.isCorrect,
        status,
        winnerPlayerId,
      };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
}

@ApiTags('추리 정답 확인')
@ApiBearerAuth()
@Controller('games')
export class GameJudgmentsController {
  constructor(
    private readonly service: GameJudgmentsService,
  ) {}

  @Post(':gameId/judgments')
  @HttpCode(200)
  @ApiOperation({
    summary: '추리 정답 확인 및 승패 결정',
    description:
      '정답이면 추리 작성자가 즉시 승리합니다. ' +
      '양쪽 추리를 모두 제출하고 모든 판정이 오답이면 ' +
      'DRAW로 게임을 종료합니다.',
  })
  @ApiOkResponse({
    schema: {
      example: {
        gameId: 1,
        guessId: 1,
        isCorrect: true,
        status: 'FINISHED',
        winnerPlayerId: 1,
      },
    },
  })
  judgeGuess(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Body() dto: JudgeGuessDto,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException(
        '멤버 토큰이 필요합니다.',
      );
    }

    return this.service.judgeGuess(
      gameId,
      memberToken,
      dto,
    );
  }
}
