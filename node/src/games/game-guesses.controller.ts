
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
  ApiCreatedResponse,
  ApiOperation,
  ApiProperty,
  ApiTags,
} from '@nestjs/swagger';
import {
  ArrayMaxSize,
  ArrayMinSize,
  IsArray,
  IsInt,
  IsString,
  Matches,
  MaxLength,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';
import { RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';

class GuessDto {
  @ApiProperty({ example: 1 })
  @IsInt()
  @Min(1)
  guessNo!: number;

  @ApiProperty({
    example: '먼저 사진 찍자고 제안하기',
    maxLength: 30,
  })
  @IsString()
  @MaxLength(30)
  @Matches(/\S/, {
    message: '추리 내용은 비워둘 수 없습니다.',
  })
  content!: string;
}

class SubmitGuessesDto {
  @ApiProperty({ type: [GuessDto] })
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(3)
  @ValidateNested({ each: true })
  @Type(() => GuessDto)
  guesses!: GuessDto[];
}

interface GameRow extends RowDataPacket {
  id: number;
  status: string;
  mission_count: number;
}

interface PlayerRow extends RowDataPacket {
  id: number;
  member_token: string;
}

interface ExistingGuessRow extends RowDataPacket {
  id: number;
}

@Injectable()
export class GameGuessesService {
  constructor(private readonly databaseService: DatabaseService) {}

  async submitGuesses(
    gameId: number,
    memberToken: string,
    dto: SubmitGuessesDto,
  ) {
    const connection =
      await this.databaseService.pool.getConnection();

    try {
      await connection.beginTransaction();

      // 게임 상태 확인
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
          '현재 추리를 제출할 수 없는 상태입니다.',
        );
      }

      // 게임 참가자 확인
      const [players] = await connection.execute<PlayerRow[]>(
        `SELECT gp.id, rm.member_token
         FROM game_players gp
         JOIN room_members rm ON rm.id = gp.member_id
         WHERE gp.game_id = ?`,
        [gameId],
      );

      const myPlayer = players.find(
        (player) => player.member_token === memberToken,
      );

      if (!myPlayer) {
        throw new ForbiddenException(
          '해당 게임의 참가자가 아닙니다.',
        );
      }

      const partner = players.find(
        (player) => player.id !== myPlayer.id,
      );

      if (!partner) {
        throw new ConflictException(
          '상대방 참가자 정보가 없습니다.',
        );
      }

      // 미션 개수 검증
      if (dto.guesses.length !== game.mission_count) {
        throw new BadRequestException(
          `추리를 정확히 ${game.mission_count}개 제출해야 합니다.`,
        );
      }

      // 추리 번호는 1부터 순서대로
      const orderedGuesses = [...dto.guesses].sort(
        (a, b) => a.guessNo - b.guessNo,
      );

      const invalidNumbers = orderedGuesses.some(
        (guess, index) => guess.guessNo !== index + 1,
      );

      if (invalidNumbers) {
        throw new BadRequestException(
          '추리 번호는 1부터 중복 없이 입력해야 합니다.',
        );
      }

      // 같은 사람이 이미 제출했는지 확인
      const [existing] =
        await connection.execute<ExistingGuessRow[]>(
          `SELECT id
           FROM guesses
           WHERE game_id = ?
             AND writer_player_id = ?
           LIMIT 1`,
          [gameId, myPlayer.id],
        );

      if (existing.length > 0) {
        throw new ConflictException(
          '이미 추리를 제출했습니다.',
        );
      }

      // 상대 미션에 대한 추리 저장
      for (const guess of orderedGuesses) {
        await connection.execute(
          `INSERT INTO guesses
             (game_id, writer_player_id,
              target_player_id, content)
           VALUES (?, ?, ?, ?)`,
          [
            gameId,
            myPlayer.id,
            partner.id,
            guess.content.trim(),
          ],
        );
      }

      await connection.commit();

      return {
        gameId,
        submittedCount: orderedGuesses.length,
        submissionCompleted: true,
      };
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }
}

@ApiTags('상대 미션 추리')
@ApiBearerAuth()
@Controller('games/:gameId/guesses')
export class GameGuessesController {
  constructor(
    private readonly gameGuessesService: GameGuessesService,
  ) {}

  @Post()
  @ApiOperation({
    summary: '상대 미션 추리 제출',
    description:
      '게임 종료 후 상대방의 비밀 미션을 추리하여 제출합니다.',
  })
  @ApiCreatedResponse({
    schema: {
      example: {
        gameId: 1,
        submittedCount: 2,
        submissionCompleted: true,
      },
    },
  })
  submitGuesses(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Body() dto: SubmitGuessesDto,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException(
        '멤버 토큰이 필요합니다.',
      );
    }

    return this.gameGuessesService.submitGuesses(
      gameId,
      memberToken,
      dto,
    );
  }
}
