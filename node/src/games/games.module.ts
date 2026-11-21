
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
import { GamesService } from './games.service';

@Module({
  imports: [DatabaseModule],
  controllers: [
    GamesController,
    MissionsController,
    PenaltiesController,
    GameMissionsController,
    GamePenaltiesController,
  ],
  providers: [
    GamesService,
    GameMissionsService,
    GamePenaltiesService,
  ],
})
export class GamesModule {}
