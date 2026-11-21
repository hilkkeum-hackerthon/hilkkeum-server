
import {
  Controller,
  Get,
  ParseEnumPipe,
  Query,
} from '@nestjs/common';
import {
  ApiOkResponse,
  ApiOperation,
  ApiQuery,
  ApiTags,
} from '@nestjs/swagger';
import { RowDataPacket } from 'mysql2';
import { DatabaseService } from '../database/database.service';
import { DateType } from './dto/create-game.dto';

interface MissionRow extends RowDataPacket {
  id: number;
  content: string;
}

@ApiTags('미션')
@Controller('missions')
export class MissionsController {
  constructor(private readonly databaseService: DatabaseService) {}

  @Get()
  @ApiOperation({
    summary: '미션 목록 조회',
    description: '데이트 유형에 맞는 활성 미션을 DB에서 조회합니다.',
  })
  @ApiQuery({
    name: 'dateType',
    enum: DateType,
    required: true,
  })
  @ApiOkResponse({
    description: '미션 목록 조회 성공',
    schema: {
      example: {
        missions: [
          { id: 1, content: '평소와 다른 애칭으로 불러보기' },
          { id: 2, content: '먼저 사진 찍자고 제안하기' },
        ],
      },
    },
  })
  async getMissions(
    @Query('dateType', new ParseEnumPipe(DateType))
    dateType: DateType,
  ) {
    let sql = `
      SELECT id, content
      FROM missions
      WHERE date_type = ?
        AND is_active = 1
    `;

    const params: (string | number)[] = [dateType];

    sql += ' ORDER BY sort_order ASC, id ASC';

    const [missions] =
      await this.databaseService.pool.execute<MissionRow[]>(
        sql,
        params,
      );

    return {
      missions: missions.map((mission) => ({
        id: mission.id,
        content: mission.content,
      })),
    };
  }
}
