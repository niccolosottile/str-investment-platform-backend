#!/bin/bash

echo "🚀 Starting STR Platform Backend Setup..."
echo ""

# Check if Java 21 is installed
if ! java -version 2>&1 | grep -q "version \"21"; then
    echo "❌ Java 21 is required but not found"
    echo "   Install from: https://adoptium.net/"
    exit 1
fi

echo "✅ Java 21 detected"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running"
    echo "   Please start Docker Desktop"
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Create .env if it doesn't exist
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
    echo "⚠️  Please edit .env and add your MAPBOX_SECRET_TOKEN"
fi

# Start infrastructure
echo "🐳 Starting PostgreSQL, Redis, and RabbitMQ..."
docker-compose up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 10

# Build the application
echo "🔨 Building application..."
./gradlew build -x test

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Setup complete!"
    echo ""
    echo "To run the application:"
    echo "  ./gradlew bootRun"
    echo ""
    echo "Useful URLs:"
    echo "  API: http://localhost:8080/api/health"
    echo "  Swagger: http://localhost:8080/swagger-ui.html"
    echo "  RabbitMQ: http://localhost:15672 (str_user/str_password)"
    echo ""
    echo "To stop infrastructure:"
    echo "  docker-compose down"
else
    echo "❌ Build failed. Check the errors above."
    exit 1
fi
