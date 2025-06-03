#!/bin/bash
set -euo pipefail

echo "🐳 Building Zero Trust Docker image with layers..."

# Construir JAR localmente primero
echo "🏗️ Building application with layers..."
./mvnw clean package -DskipTests -Dspring-boot.build-image.layered=true

# Verificar que el JAR existe
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: JAR no encontrado en target/"
    exit 1
fi

echo "📦 JAR encontrado: $JAR_FILE"

# Verificar layers
echo "📋 Checking layers..."
java -Djarmode=layertools -jar "$JAR_FILE" list

# Crear Dockerfile temporal con nombre específico
JAR_NAME=$(basename "$JAR_FILE")
sed "s/target\/\*\.jar/target\/$JAR_NAME/g" Dockerfile > Dockerfile.tmp

# Construir imagen Docker con Dockerfile temporal
echo "🐳 Building Docker image..."
docker build -f Dockerfile.tmp -t zero-trust-app:latest .

# Limpiar
rm Dockerfile.tmp

# Mostrar tamaño
echo "📊 Image size:"
docker images zero-trust-app:latest

echo "✅ Build completed!"
echo "🚀 To run: docker-compose up -d"