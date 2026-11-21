
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

@Injectable()
export class GameJudgmentsService {
  constructor(private readonly databaseService: DatabaseService) {}

  async judgeGuess(
    gameId: number,
    memberToken: string,
    dto: JudgeGuessDto,
  ) {
    const connection =
      await this.databaseService.pool.getConnection();

    try {
      await connection.beginTransaction();

      // 게임 확인 및 동시 판정 방지
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

      if (games[0].status !== 'GUESSING') {
        throw new ConflictException(
          '현재 정답을 확인할 수 없는 게임 상태입니다.',
        );
      }

      // 현재 사용자가 게임 참가자인지 확인
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
          '해당 게임의 참가자가 아닙니다.',
        );
      }

      // 판정할 추리 조회
      const [guesses] = await connection.execute<GuessRow[]>(
        `SELECT id, writer_player_id,
                target_player_id, is_correct
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

      // 내 미션에 대한 상대의 추리만 판정 가능
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

      // 정답 여부 저장
      await connection.execute(
        `UPDATE guesses
         SET is_correct = ?
         WHERE id = ?`,
        [dto.isCorrect ? 1 : 0, guess.id],
      );

      if (dto.isCorrect) {
        // 정답이면 추리 작성자가 즉시 승리
        await connection.execute(
          `UPDATE games
           SET status = 'FINISHED',
               winner_player_id = ?,
               decided_by = 'GUESS'
           WHERE id = ?`,
          [guess.writer_player_id, gameId],
        );

        await connection.execute(
          `UPDATE game_players
           SET result = CASE
             WHEN id = ? THEN 'WIN'
             ELSE 'LOSE'
           END
           WHERE game_id = ?`,
          [guess.writer_player_id, gameId],
        );
      }

      await connection.commit();

      return {
        gameId,
        guessId: guess.id,
        isCorrect: dto.isCorrect,
        status: dto.isCorrect ? 'FINISHED' : 'GUESSING',
        winnerPlayerId: dto.isCorrect
          ? guess.writer_player_id
          : null,
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
      '미션 소유자가 정답을 확인합니다. 정답이면 추리 작성자가 즉시 승리합니다.',
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
