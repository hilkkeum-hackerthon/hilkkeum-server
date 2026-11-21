
import { Module } from '@nestjs/common';
import { DatabaseModule } from '../database/database.module';
import { GamesController } from './games.controller';
import { MissionsController } from './missions.controller';
import { PenaltiesController } from './penalties.controller';
import {
  GameMissionsController,
  GameMissionsService,
} from './game-missions.controller';
import {
  GamePenaltiesController,
  GamePenaltiesService,
} from './game-penalties.controller';
import {
  GameEndController,
  GameEndService,
} from './game-end.controller';
import {
  GameGuessesController,
  GameGuessesService,
} from './game-guesses.controller';
import {
  GameReceivedGuessesController,
  GameReceivedGuessesService,
} from './game-received-guesses.controller';
import {
  GameJudgmentsController,
  GameJudgmentsService,
} from './game-judgments.controller';
import {
  GameResultController,
  GameResultService,
} from './game-result.controller';
import { GamesService } from './games.service';

@Module({
  imports: [DatabaseModule],
  controllers: [
    GamesController,
    MissionsController,
    PenaltiesController,
    GameMissionsController,
    GamePenaltiesController,
    GameEndController,
    GameGuessesController,
    GameReceivedGuessesController,
    GameJudgmentsController,
    GameResultController,
  ],
  providers: [
    GamesService,
    GameMissionsService,
    GamePenaltiesService,
    GameEndService,
    GameGuessesService,
    GameReceivedGuessesService,
    GameJudgmentsService,
    GameResultService,
  ],
})
export class GamesModule {}
