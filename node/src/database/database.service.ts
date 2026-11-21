
import {
  Injectable,
  Logger,
  OnModuleDestroy,
  OnModuleInit,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createPool, type Pool } from 'mysql2/promise';

@Injectable()
export class DatabaseService
  implements OnModuleInit, OnModuleDestroy
{
  private readonly logger = new Logger(DatabaseService.name);

  readonly pool: Pool;

  constructor(private readonly configService: ConfigService) {
    this.pool = createPool({
      host: this.configService.getOrThrow<string>('DB_HOST'),
      port: Number(
        this.configService.getOrThrow<string>('DB_PORT'),
      ),
      user: this.configService.getOrThrow<string>('DB_USERNAME'),
      password: this.configService.getOrThrow<string>('DB_PASSWORD'),
      database: this.configService.getOrThrow<string>('DB_NAME'),
      waitForConnections: true,
      connectionLimit: 5,
    });
  }

  async onModuleInit() {
    await this.pool.query('SELECT 1');
    this.logger.log('MySQL 연결 성공');
  }

  async onModuleDestroy() {
    await this.pool.end();
  }
}
