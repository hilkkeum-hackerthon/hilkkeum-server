
import { Injectable, ServiceUnavailableException } from '@nestjs/common';
import { CreateGameDto } from './dto/create-game.dto';

@Injectable()
export class GamesService {
  createGame(dto: CreateGameDto) {
    // 추후 DB 연결 시 사용할 요청 데이터
    const { roomId, dateType, missionCount, themeId } = dto;

    // DB 연결 전에는 성공 응답을 반환하지 않음
    throw new ServiceUnavailableException(
      `게임 생성 DB 연결 대기 중: roomId=${roomId}, dateType=${dateType}, missionCount=${missionCount}, themeId=${themeId ?? '없음'}`,
    );
  }
}
