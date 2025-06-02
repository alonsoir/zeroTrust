#!/bin/bash
set -euo pipefail

echo "🛑 Deteniendo entorno de desarrollo..."

# Detener contenedores
echo "🐳 Deteniendo contenedores..."
docker-compose down

# Limpiar volúmenes si se especifica
if [[ "${1:-}" == "--clean" ]]; then
    echo "🧹 Limpiando volúmenes y datos..."
    docker-compose down -v
    docker system prune -f --volumes
    echo "✅ Limpieza completa realizada"
else
    echo "💡 Usa '--clean' para eliminar también los volúmenes"
fi

echo "✅ Entorno detenido"
