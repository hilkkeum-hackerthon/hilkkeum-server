
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
import { GamesService } from './games.service';
import { CreateGameDto } from './dto/create-game.dto';

@Controller('games')
export class GamesController {
  constructor(private readonly gamesService: GamesService) {}

  // ① 게임 생성
  @Post()
  createGame(
    @Body() createGameDto: CreateGameDto,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken = authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException('멤버 토큰이 필요합니다.');
    }

    return this.gamesService.createGame(createGameDto, memberToken);
  }

  // ② 게임 상태·진행현황 조회
  @Get(':gameId')
  getGameStatus(
    @Param('gameId', ParseIntPipe) gameId: number,
    @Headers('authorization') authorization?: string,
  ) {
    const memberToken = authorization?.match(/^Bearer\s+(\S+)$/i)?.[1];

    if (!memberToken) {
      throw new UnauthorizedException('멤버 토큰이 필요합니다.');
    }

    return this.gamesService.getGameStatus(gameId, memberToken);
  }
}
