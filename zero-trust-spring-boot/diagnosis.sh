# Script de diagnóstico
echo "🔍 DIAGNÓSTICO VAULT vs FALLBACK"
echo "================================"

echo "📊 1. Estado de Vault:"
docker exec -it zero-trust-vault sh -c "
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=dev-root-token
vault status
"

echo "🔐 2. Secretos en Vault:"
docker exec -it zero-trust-vault sh -c "
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=dev-root-token
vault kv get secret/zero-trust-app
"

echo "🌍 3. Variables de entorno en la app:"
docker exec -it zero-trust-app env | grep -E "VAULT|SPRING_CLOUD|JWT"

echo "📱 4. Verificar si la app responde:"
curl -s http://localhost:8080/actuator/health || echo "App no responde"