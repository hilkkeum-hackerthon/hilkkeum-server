
import {
  Controller,
  Get,
  Headers,
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

interface PenaltyRow extends RowDataPacket {
  id: number;
  content: string;
}

interface MemberRow extends RowDataPacket {
  id: number;
}

@ApiTags('벌칙')
@ApiBearerAuth()
@Controller('penalties')
export class PenaltiesController {
  constructor(private readonly databaseService: DatabaseService) {}

  // ④ 벌칙 목록 조회
  // GET /api/v1/penalties
  @Get()
  @ApiOperation({
    summary: '벌칙 목록 조회',
    description: 'DB에 등록된 활성 벌칙 목록을 조회합니다.',
  })
  @ApiOkResponse({
    description: '벌칙 목록 조회 성공',
    schema: {
      example: {
        penalties: [
          { id: 1, content: '상대방의 장점 3가지 말하기' },
          { id: 2, content: '10초 동안 눈을 마주치며 칭찬하기' },
        ],
      },
    },
  })
  async getPenalties(
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException('멤버 토큰이 필요합니다.');
    }

    // 유효한 멤버인지 확인
    const [members] =
      await this.databaseService.pool.execute<MemberRow[]>(
        `SELECT id
         FROM room_members
         WHERE member_token = ?
         LIMIT 1`,
        [memberToken],
      );

    if (members.length === 0) {
      throw new UnauthorizedException(
        '유효하지 않은 멤버 토큰입니다.',
      );
    }

    // 활성 벌칙 목록 조회
    const [penalties] =
      await this.databaseService.pool.execute<PenaltyRow[]>(
        `SELECT id, content
         FROM penalties
         WHERE is_active = 1
         ORDER BY sort_order ASC, id ASC`,
      );

    return {
      penalties: penalties.map((penalty) => ({
        id: penalty.id,
        content: penalty.content,
      })),
    };
  }
}
