#!/bin/bash
set -euo pipefail

echo "🧪 Ejecutando suite completa de tests..."

# Tests unitarios
echo "🔬 Tests unitarios..."
./mvnw test -Dtest="**/*Test"

# Tests de integración
echo "🔗 Tests de integración..."
./mvnw test -Dtest="**/*IntegrationTest"

# Tests de seguridad
echo "🔒 Tests de seguridad..."
./mvnw test -Dtest="**/security/*"

echo "✅ Todos los tests completados!"
echo "📋 Resultados en: target/surefire-reports/"
