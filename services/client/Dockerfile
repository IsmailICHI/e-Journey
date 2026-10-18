FROM node:20-alpine AS build
WORKDIR /workspace
COPY services/client/package*.json ./
RUN npm ci --silent
COPY services/client/ ./
RUN npm run build -- --configuration production

FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html
COPY services/client/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/client/browser/ .
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
