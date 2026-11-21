
import { IsEnum, IsIn, IsInt, IsOptional, Min } from 'class-validator';

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
  @IsIn([3, 5])
  missionCount!: number;

  @IsOptional()
  @IsInt()
  @Min(1)
  themeId?: number;
}
