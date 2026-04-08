# ESTÁGIO 1: Compilação do Angular (Node.js)
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Lembra-te que alterámos o angular.json para trocar os environments no modo de produção!
RUN npm run build --configuration=production

# ESTÁGIO 2: Servidor Web (Nginx)
FROM nginx:alpine
# Copia o build do Angular (Atenção ao caminho gerado pelas versões novas do Angular!)
COPY --from=build /app/dist/gestor-flow-web/browser /usr/share/nginx/html
# Copia a nossa configuração mágica de Proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80