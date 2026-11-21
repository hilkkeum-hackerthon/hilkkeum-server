
import {
  Controller,
  ForbiddenException,
  Get,
  Headers,
  Injectable,
  Param,
  ParseIntPipe,
  UnauthorizedException,
} from '@nestjs/common';
import {
  ApiBearerAuth,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiTags,
} from '@nestjs/swagger';
import { RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';

interface MemberRow extends RowDataPacket {
  id: number;
}

interface GameRow extends RowDataPacket {
  id: number;
  status: string;
}

@Injectable()
export class GameActiveService {
  constructor(
    private readonly databaseService: DatabaseService,
  ) {}

  async getActiveGame(
    roomId: number,
    memberToken: string,
  ) {
    // 해당 방의 참가자인지 확인
    const [members] =
      await this.databaseService.pool.execute<MemberRow[]>(
        `SELECT id
         FROM room_members
         WHERE room_id = ?
           AND member_token = ?
         LIMIT 1`,
        [roomId, memberToken],
      );

    if (members.length === 0) {
      throw new ForbiddenException(
        '해당 방의 참가자가 아닙니다.',
      );
    }

    // 현재 진행 중인 게임 조회
    const [games] =
      await this.databaseService.pool.execute<GameRow[]>(
        `SELECT id, status
         FROM games
         WHERE room_id = ?
           AND status <> 'FINISHED'
         ORDER BY id DESC
         LIMIT 1`,
        [roomId],
      );

    return {
      roomId,
      gameId: games[0]?.id ?? null,
      status: games[0]?.status ?? null,
    };
  }
}

@ApiTags('게임')
@ApiBearerAuth()
@Controller('games/room')
export class GameActiveController {
  constructor(
    private readonly service: GameActiveService,
  ) {}

  @Get(':roomId/active')
  @ApiOperation({
    summary: '현재 진행 중인 게임 조회',
    description:
      '같은 방의 참가자가 현재 게임 ID와 상태를 조회합니다.',
  })
  @ApiParam({
    name: 'roomId',
    type: Number,
    example: 1,
  })
  @ApiOkResponse({
    schema: {
      example: {
        roomId: 1,
        gameId: 2,
        status: 'PLAYING',
      },
    },
  })
  getActiveGame(
    @Param('roomId', ParseIntPipe) roomId: number,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException(
        '멤버 토큰이 필요합니다.',
      );
    }

    return this.service.getActiveGame(
      roomId,
      memberToken,
    );
  }
}
