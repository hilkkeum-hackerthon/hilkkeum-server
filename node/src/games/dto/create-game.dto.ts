
import { IsEnum, IsInt, IsOptional, Min, Max } from 'class-validator';

export enum DateType {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
}

export class CreateGameDto {
  @IsInt()
  @Min(1)
  roomId!: number;

  @IsEnum(DateType)
  dateType!: DateType;

  @IsInt()
  @Min(1)
  @Max(3)
  missionCount!: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  themeId?: number;
}
