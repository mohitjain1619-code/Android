FROM node:20-alpine

WORKDIR /app

# Install dependencies first (cache layer)
COPY package.json package-lock.json* ./
RUN npm install

# Copy source
COPY . .

# Expose port
EXPOSE 3000

# Default command (overridden by docker-compose for dev)
CMD ["node", "server.js"]
