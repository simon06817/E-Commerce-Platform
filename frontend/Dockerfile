FROM node:20-alpine AS build

ENV NPM_CONFIG_REGISTRY=https://registry.npmmirror.com

WORKDIR /app
RUN npm install -g pnpm@10.34.5

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
RUN pnpm install --frozen-lockfile --store-dir /pnpm/store

COPY . .
RUN pnpm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
