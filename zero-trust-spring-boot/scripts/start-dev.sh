#!/bin/bash
set -euo pipefail

echo "🚀 Iniciando entorno de desarrollo Zero Trust..."

# Verificar prerrequisitos
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker no está funcionando. Por favor, inicia Docker."
    exit 1
fi

# Función para verificar puertos
check_port() {
    local port=$1
    local service=$2
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null; then
        echo "⚠️ Puerto $port ya está en uso ($service). ¿Continuar? (y/N)"
        read -r response
        if [[ ! "$response" =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# Verificar puertos
check_port 8080 "Aplicación"
check_port 5432 "PostgreSQL"
check_port 6379 "Redis"
check_port 9092 "Kafka"

# Levantar servicios de infraestructura
echo "🐳 Iniciando servicios de infraestructura..."
docker-compose up -d postgres redis kafka zookeeper

# Esperar que los servicios estén listos
echo "⏳ Esperando que los servicios estén listos..."
sleep 15

# Verificar servicios
echo "🔍 Verificando servicios..."

# PostgreSQL
until docker-compose exec -T postgres pg_isready -U zerotrust -d zerotrust; do
    echo "⏳ Esperando PostgreSQL..."
    sleep 2
done
echo "✅ PostgreSQL listo"

# Redis
until docker-compose exec -T redis redis-cli --raw incr ping > /dev/null 2>&1; do
    echo "⏳ Esperando Redis..."
    sleep 2
done
echo "✅ Redis listo"

echo "✅ Todos los servicios están listos!"

# Iniciar aplicación
echo ""
echo "🏃 Iniciando aplicación Zero Trust..."
echo "📝 Logs de la aplicación aparecerán a continuación..."
echo ""
echo "🌐 Endpoints disponibles:"
echo "  • Aplicación: http://localhost:8080"
echo "  • Health: http://localhost:8080/api/health"
echo "  • Info: http://localhost:8080/api/info"
echo "  • H2 Console: http://localhost:8080/h2-console"
echo "  • Actuator: http://localhost:8080/actuator"
echo ""

# Iniciar aplicación con perfil de desarrollo
./mvnw spring-boot:run -Dspring-boot.run.profiles=development
