#!/bin/bash

echo "🔄 Inicializando Vault..."

# Esperar a que Vault esté listo
echo "⏳ Esperando a que Vault esté disponible..."
until curl -s http://localhost:8200/v1/sys/health > /dev/null 2>&1; do
  echo "   Vault no está listo aún..."
  sleep 2
done

echo "✅ Vault está disponible!"

# Configurar variables
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=dev-root-token

echo "🔧 Configurando secretos en Vault..."

# Configurar secretos para la aplicación
docker exec -i zero-trust-vault sh << 'EOF'
export VAULT_ADDR=http://localhost:8200
export VAULT_TOKEN=dev-root-token

# Verificar estado de Vault
echo "📊 Estado de Vault:"
vault status

# Configurar secretos
echo "🔐 Configurando secretos para zero-trust-app..."
vault kv put secret/zero-trust-app \
  jwt.secret="zero-trust-super-secure-jwt-secret-key-must-be-at-least-64-characters-long" \
  database.host="postgres" \
  database.port="5432" \
  database.name="zerotrust" \
  database.username="zerotrust" \
  database.password="secure_password"

echo "✅ Secretos configurados exitosamente!"

# Verificar secretos
echo "🔍 Verificando secretos:"
vault kv get secret/zero-trust-app
EOF

echo "🎉 Inicialización de Vault completada!"