#!/bin/bash
set -euo pipefail

echo "🚀 Iniciando entorno de desarrollo Zero Trust..."

# Verificar que Maven esté disponible
if ! command -v ./mvnw &> /dev/null; then
    echo "❌ Maven wrapper no encontrado"
    exit 1
fi

echo "🏃 Iniciando aplicación con perfil development..."
echo ""
echo "🌐 Endpoints disponibles:"
echo "  • Aplicación: http://localhost:8080"
echo "  • Health: http://localhost:8080/api/health"
echo "  • Info: http://localhost:8080/api/info"
echo "  • H2 Console: http://localhost:8080/h2-console"
echo "  • Actuator: http://localhost:8080/actuator"
echo ""

# Iniciar aplicación
./mvnw spring-boot:run -Dspring-boot.run.profiles=development
