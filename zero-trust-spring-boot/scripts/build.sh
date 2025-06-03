#!/bin/bash
set -euo pipefail

echo "🚀 Construyendo Zero Trust Application..."

# Verificar prerrequisitos
command -v java >/dev/null 2>&1 || { echo "❌ Java no encontrado"; exit 1; }

# Limpiar construcciones anteriores
echo "🧹 Limpiando..."
./mvnw clean

# Ejecutar tests
echo "🧪 Ejecutando tests..."
./mvnw test

# Compilar aplicación
echo "🏗️ Construyendo aplicación..."
./mvnw package -DskipTests

echo "✅ Construcción completada!"

# Verificar JAR
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [ -f "$JAR_FILE" ]; then
    echo "📦 JAR creado: $JAR_FILE"
else
    echo "❌ Error: JAR no encontrado"
    exit 1
fi
