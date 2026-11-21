# Stage 1: Build
FROM node:22-alpine AS builder
WORKDIR /app
RUN corepack enable

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build && pnpm prune --prod

# Stage 2: Runtime
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/package.json ./

# 보안: non-root 유저로 실행 (node 이미지 기본 유저)
USER node

EXPOSE 3000

# NestJS 기본 빌드 결과 경로. Express면 빌드 결과 진입 파일로 변경 (예: dist/index.js)
CMD ["node", "dist/main.js"]
