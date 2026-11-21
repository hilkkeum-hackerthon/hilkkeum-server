
import {
  BadRequestException,
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
  ApiParam,
  ApiProperty,
  ApiTags,
} from '@nestjs/swagger';
import {
  ArrayMaxSize,
  ArrayMinSize,
  ArrayUnique,
  IsArray,
  IsInt,
  Min,
} from 'class-validator';
import { RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';

class SelectMissionsDto {
  @ApiProperty({
    example: [1, 2],
    type: [Number],
    description: '선택한 미션 ID 목록 (1~3개)',
  })
  @IsArray()
  @ArrayMinSize(1)
  @ArrayMaxSize(3)
  @ArrayUnique()
  @IsInt({ each: true })
  @Min(1, { each: true })
  missionIds!: number[];
}

interface GameRow extends RowDataPacket {
  id: number;
  status: string;
  mission_count: number;
  date_type: string;
  theme_id: number | null;
}

interface PlayerRow extends RowDataPacket {
  id: number;
  mission_selected_at: Date | null;
}

interface MissionRow extends RowDataPacket {
  id: number;
}


@Injectable()
export class GameMissionsService {
  constructor(private readonly databaseService: DatabaseService) {}

  async selectMissions(
    gameId: number,
    memberToken: string,
    dto: SelectMissionsDto,
  ) {
    const connection = await this.databaseService.pool.getConnection();

    try {
      await connection.beginTransaction();

      // 게임 확인 및 동시 요청 방지
      const [games] = await connection.execute<GameRow[]>(
        `SELECT id, status, mission_count, date_type, theme_id
         FROM games
         WHERE id = ?
         FOR UPDATE`,
        [gameId],
      );

      if (games.length === 0) {
        throw new NotFoundException('게임을 찾을 수 없습니다.');
      }

      const game = games[0];

      if (game.status !== 'SETTING') {
        throw new ConflictException(
          '현재 미션을 선택할 수 없는 게임 상태입니다.',
        );
      }

      // 요청자가 이 게임의 참가자인지 확인
      const [players] = await connection.execute<PlayerRow[]>(
        `SELECT gp.id, gp.mission_selected_at
         FROM game_players gp
         JOIN room_members rm ON rm.id = gp.member_id
         WHERE gp.game_id = ?
           AND rm.member_token = ?
         LIMIT 1`,
        [gameId, memberToken],
      );

      if (players.length === 0) {
        throw new ForbiddenException(
          '해당 게임의 참가자만 미션을 선택할 수 있습니다.',
        );
      }

      const player = players[0];

      if (player.mission_selected_at !== null) {
        throw new ConflictException(
          '이미 미션 선택을 완료했습니다.',
        );
      }

      // 선택 개수 = 게임 설정 개수
      if (dto.missionIds.length !== game.mission_count) {
        throw new BadRequestException(
          `미션을 정확히 ${game.mission_count}개 선택해야 합니다.`,
        );
      }

      // 활성 미션 및 데이트 유형 검증
      const placeholders = dto.missionIds.map(() => '?').join(',');

      let query = `
        SELECT id
        FROM missions
        WHERE id IN (${placeholders})
          AND date_type = ?
          AND is_active = 1
      `;

      const params: (string | number)[] = [
        ...dto.missionIds,
        game.date_type,
      ];

      if (game.theme_id !== null) {
        query += ' AND theme_id = ?';
        params.push(game.theme_id);
      }

      const [missions] = await connection.execute<MissionRow[]>(
        query,
        params,
      );

      if (missions.length !== dto.missionIds.length) {
        throw new BadRequestException(
          '유효하지 않거나 현재 게임에서 선택할 수 없는 미션이 있습니다.',
        );
      }

      // 내 비밀 미션 저장
      for (const missionId of dto.missionIds) {
        await connection.execute(
          `INSERT INTO player_missions
             (game_player_id, mission_id)
           VALUES (?, ?)`,
          [player.id, missionId],
        );
      }

      await connection.execute(
        `UPDATE game_players
         SET mission_selected_at = CURRENT_TIMESTAMP
         WHERE id = ?`,
        [player.id],
      );

      await connection.commit();

      return {
        gameId,
        selectedCount: dto.missionIds.length,
        selectionCompleted: true,
      };
    } catch (error) {
      await connection.rollback();

      if ((error as { code?: string }).code === 'ER_DUP_ENTRY') {
        throw new ConflictException('이미 선택한 미션입니다.');
      }

      throw error;
    } finally {
      connection.release();
    }
  }
}

@ApiTags('게임 미션 선택')
@ApiBearerAuth()
@Controller('games/:gameId/missions')
export class GameMissionsController {
  constructor(
    private readonly gameMissionsService: GameMissionsService,
  ) {}

  @Post()
  @HttpCode(200)
  @ApiOperation({
    summary: '내 비밀 미션 선택',
    description:
      '게임에 설정된 개수만큼 본인의 비밀 미션을 선택합니다.',
  })
  @ApiParam({
    name: 'gameId',
    type: Number,
    example: 1,
  })
  @ApiOkResponse({
    schema: {
      example: {
        gameId: 1,
        selectedCount: 2,
        selectionCompleted: true,
      },
    },
  })
  selectMissions(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Body() dto: SelectMissionsDto,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException('멤버 토큰이 필요합니다.');
    }

    return this.gameMissionsService.selectMissions(
      gameId,
      memberToken,
      dto,
    );
  }
}
