#!/bin/bash
set -euo pipefail

echo "🚀 Construyendo Zero Trust Application..."

# Verificar prerrequisitos
command -v java >/dev/null 2>&1 || { echo "❌ Java no encontrado"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "❌ Docker no encontrado"; exit 1; }

# Limpiar construcciones anteriores
echo "🧹 Limpiando..."
./mvnw clean

# Ejecutar tests
echo "🧪 Ejecutando tests..."
./mvnw test

# Compilar aplicación
echo "🏗️ Construyendo aplicación..."
./mvnw package -DskipTests

# Verificar que el JAR se creó
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Error: JAR no encontrado"
    exit 1
fi

# Construir imagen Docker
echo "🐳 Construyendo imagen Docker..."
docker build -t zero-trust-app:latest .

# Verificar imagen
docker images zero-trust-app:latest

echo "✅ Construcción completada!"
echo "📦 JAR: $JAR_FILE"
echo "🐳 Imagen: zero-trust-app:latest"
echo ""
echo "Para ejecutar:"
echo "  ./scripts/start-dev.sh"
