
import {
  Body,
  Controller,
  Get,
  Headers,
  Param,
  ParseIntPipe,
  Post,
  UnauthorizedException,
} from '@nestjs/common';

import {
  ApiBearerAuth,
  ApiCreatedResponse,
  ApiOkResponse,
  ApiOperation,
  ApiParam,
  ApiTags,
} from '@nestjs/swagger';

import { GamesService } from './games.service';
import { CreateGameDto } from './dto/create-game.dto';

@ApiTags('게임')
@ApiBearerAuth()
@Controller('games')
export class GamesController {
  constructor(private readonly gamesService: GamesService) {}

  // ① 게임 생성
  // POST /api/v1/games
  @Post()
  @ApiOperation({
    summary: '게임 생성 및 기본 설정',
    description: '방장이 데이트 유형과 미션 개수(1~3개)를 설정하여 게임을 생성합니다.',
  })
  @ApiCreatedResponse({
    description: '게임 생성 성공',
    schema: {
      example: {
        gameId: 1,
        roomId: 1,
        dateType: 'OFFLINE',
        missionCount: 2,
        status: 'SETTING',
      },
    },
  })
  createGame(
    @Body() createGameDto: CreateGameDto,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException('멤버 토큰이 필요합니다.');
    }

    return this.gamesService.createGame(
      createGameDto,
      memberToken,
    );
  }

  // ② 게임 상태·진행현황 조회
  // GET /api/v1/games/{gameId}
  @Get(':gameId')
  @ApiOperation({
    summary: '게임 상태 및 진행현황 조회',
    description: '폴링 방식으로 게임 상태와 양측의 선택 완료 여부를 조회합니다.',
  })
  @ApiParam({
    name: 'gameId',
    type: Number,
    example: 1,
    description: '조회할 게임 ID',
  })
  @ApiOkResponse({
    description: '게임 상태 조회 성공',
    schema: {
      example: {
        gameId: 1,
        status: 'SETTING',
        dateType: 'OFFLINE',
        missionCount: 2,
        mySelectionCompleted: false,
        partnerSelectionCompleted: false,
      },
    },
  })
  getGameStatus(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken =
      authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException('멤버 토큰이 필요합니다.');
    }

    return this.gamesService.getGameStatus(
      gameId,
      memberToken,
    );
  }
}
