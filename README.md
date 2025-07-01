Zero Trust.txt

📘 Arquitectura Zero Trust: Guía de Implementación en Entornos Java


## 1. Introducción

Arquitectura Zero Trust en entornos Java: una necesidad urgente

La seguridad moderna no puede basarse en suposiciones como “detrás del firewall estamos seguros” o “si alguien accede a la app, debe ser confiable”. Las brechas de seguridad más costosas de la última década ocurrieron después del acceso inicial, a través del movimiento lateral, sesiones persistentes, tokens inseguros o configuraciones por defecto. El modelo de confianza implícita está roto.

Zero Trust surge como una respuesta directa a este problema. No es una tecnología, sino un modelo arquitectónico que se rige por una regla fundamental:

"Nunca confíes, en nadie ni en nada. Siempre mide y verifica. Puede este proceso o este supuesto usuario hacer ésto a esta hora con este fichero?"
Aplicado a una arquitectura de aplicaciones Java modernas, esto significa repensar por completo cómo autenticar, autorizar, auditar y comunicar procesos, usuarios y servicios.

¿Por qué esta guía?

Porque incluso los entornos con Spring Boot, Spring Security, OAuth2, JWTs y firewalls bien configurados siguen siendo vulnerables cuando la seguridad se implementa por inercia y no por diseño.

El objetivo de esta guía es:

Exponer los errores más comunes de seguridad en aplicaciones modernas.
Mostrar cómo migrar progresivamente hacia un modelo Zero Trust real.
Proveer un marco conceptual y práctico para desarrollar con seguridad de verdad.
Construir un servidor Java de referencia, que siga las buenas prácticas más estrictas posibles, sin confiar ciegamente en frameworks ni componentes.
Principios que nos guiarán

Esta guía sigue una línea clara:

Principio					Implicación práctica
Autenticación contextual	No basta con saber quién eres; necesitamos saber desde dónde, cuándo, y en qué contexto estás actuando.
Privilegios mínimos			Cada acción se evalúa por separado. No hay acceso por “rol”, sino por necesidad contextual y validada.
Revocabilidad total			Todo token o sesión debe poder revocarse instantáneamente. No se acepta nada que dependa de tiempos de expiración.
Auditoría radical			Cada acceso, intento fallido o modificación relevante debe ser registrado. Sin excepciones.
Desacoplamiento funcional	No mezclar responsabilidades: la UI no maneja tokens, el backend no infiere confianza por IP, los servicios no comparten secretos.
Defensa sin JavaScript		La seguridad no debe depender de JavaScript ni ejecutarse en el navegador. JS es útil, pero no debe tener poder.

Lo que vamos a abandonar

Mecanismo heredado					Por qué lo evitamos
Cookies de sesión					Son vulnerables a CSRF, robo de sesión, y no ofrecen control sobre la revocación.
JWTs persistentes					Los tokens no revocables son una puerta abierta al abuso.
@PreAuthorize("hasRole('ADMIN')")	Decisiones estáticas y centralizadas que no representan el contexto real.
Confianza por red interna			El atacante ya está adentro. No hay redes “seguras” en el modelo Zero Trust.
Autenticación en el frontend		Delegar autenticación al navegador es como dejar las llaves bajo el felpudo.

## 1.1 Mitos comunes sobre Zero Trust

❌ Creencia errónea										✅ Realidad Zero Trust
"Zero Trust es un producto que compras."				Zero Trust es un modelo arquitectónico y cultural. No se compra, se diseña y se adopta.
"Ya uso HTTPS y JWT, así que estoy en Zero Trust."		Zero Trust no es un protocolo; es una filosofía que exige controles profundos, no solo cifrado o tokens.
"Mi red interna es segura, el peligro viene de fuera."	En Zero Trust no hay red confiable. Todo y todos deben ser verificados siempre, incluso dentro del perímetro.
"Uso roles, así que ya tengo control de acceso."		Zero Trust va más allá del RBAC. Requiere evaluar contexto, no solo identidad.
"Instalar un firewall es suficiente."					El firewall es parte de una defensa, pero no reemplaza la validación constante de cada actor y cada petición.
"Zero Trust es sólo para empresas grandes."				Todo sistema expuesto o distribuido se beneficia. Es más urgente cuanto más dinámico o accesible es tu entorno.
"Implementar Zero Trust es imposible en Java."			No solo es posible, es necesario. Java es flexible, pero hay que liberarse de malas prácticas heredadas.


🏷️ ¿Qué es RBAC?

RBAC (Role-Based Access Control, o control de acceso basado en roles) es un modelo clásico de autorización donde los permisos se asignan a roles, y los usuarios heredan esos permisos según el rol que tengan.

🔧 Cómo funciona (resumen técnico):
Definís roles: por ejemplo, ADMIN, USER, SUPPORT.
Asignás permisos a roles: ADMIN puede borrar usuarios, USER puede leer su perfil.
Asignás usuarios a roles: Juan tiene el rol USER, Laura tiene el rol ADMIN.
Entonces, en tiempo de ejecución, el sistema evalúa:

“¿Este usuario tiene el rol correcto para esta acción?”
🚫 Problemas de RBAC en entornos modernos

Aunque es un modelo muy usado (especialmente en Spring con anotaciones como @PreAuthorize("hasRole('ADMIN')")), tiene limitaciones serias cuando hablamos de Zero Trust:

Problema	Por qué es un riesgo
Es estático	Los roles no cambian con el contexto: si alguien tiene ADMIN, lo es siempre, incluso si está desde una IP sospechosa o a las 3am.
Es global	No se adapta a permisos por recurso, por hora, por operación. Falta granularidad.
Es fácil de abusar	Un atacante que robe una sesión de ADMIN obtiene acceso total. No hay segundas barreras.
No tiene trazabilidad	Los roles no explican por qué alguien accede a algo. Solo dicen que puede. Esto dificulta la auditoría.
✅ Alternativa: ABAC

ABAC (Attribute-Based Access Control) es el modelo más compatible con Zero Trust. Evalúa permisos en base a:

Atributos del usuario (id, departamento, certificado)
Atributos del recurso (tipo, sensibilidad, propietario)
Atributos del contexto (ubicación, hora, dispositivo, riesgo)
En ABAC no decimos: “Juan es ADMIN”,
Decimos: “Juan puede modificar esta entidad, si su token fue emitido hace menos de 5 minutos, desde su dispositivo autorizado, en horario laboral, y el recurso le pertenece.”

📘 2. Fundamentos de Zero Trust: Principios clave y beneficios

## ¿Qué significa realmente "Zero Trust"?

El término Zero Trust suele usarse mal o de forma superficial. No significa paranoia, ni desconfiar de todo y todos de forma irracional. Significa que la confianza nunca se otorga implícitamente, sino que debe ser verificada constantemente, en función del contexto actual.

El objetivo de Zero Trust no es negar el acceso, sino dar acceso con inteligencia y control fino, en todo momento.

## Principios clave de Zero Trust

Estos son los pilares sobre los que se construye una arquitectura Zero Trust bien diseñada:

1. Verificación continua
"Nunca confíes por defecto. Siempre verifica."
Cada acción, cada petición, cada llamada a una API debe pasar por un mecanismo de verificación. No hay sesiones duraderas ni confianza persistente.

2. Acceso con privilegios mínimos (Principio de menor privilegio)
"Cada actor debe tener solo el acceso estrictamente necesario."
Los permisos se otorgan por necesidad contextual, no por rol. Incluso los servicios entre sí deben negociar permisos explícitos y limitados.

3. Control basado en atributos (ABAC)
"El contexto importa."
El acceso se determina en función de múltiples atributos: identidad, hora, ubicación, tipo de dispositivo, nivel de riesgo, sensibilidad del recurso, etc.

4. Visibilidad y auditoría continua
"No se puede proteger lo que no se puede ver."
Cada acción debe dejar huella. No se puede permitir la ejecución de operaciones sensibles sin trazabilidad. La auditoría no es opcional: es una parte activa de la seguridad.

5. Desconfianza hacia la red
"La red interna no es segura."
No importa si una petición viene desde una IP interna o un microservicio dentro del clúster. Cada actor debe ser autenticado y autorizado explícitamente. La red no es el perímetro; la identidad es el nuevo perímetro.

6. No delegar seguridad al cliente
"El navegador no es un agente confiable."
Todo lo que implique seguridad debe ocurrir en el servidor. JavaScript puede mostrar resultados, pero nunca debe manejar lógica crítica como autenticación, verificación de permisos o gestión de tokens.

## Beneficios reales de adoptar Zero Trust

Más allá del cumplimiento normativo y las tendencias del mercado, el modelo Zero Trust aporta beneficios técnicos y operativos concretos:

Beneficio										Impacto real
🔐 Reducción drástica de superficie de ataque	Al no confiar por defecto, cada vector necesita validación explícita. El atacante no puede moverse libremente.
🔎 Auditoría clara y trazable					Cada operación puede vincularse a una identidad, acción, recurso y contexto. Ideal para análisis forense.
⚙️ Menor dependencia de soluciones mágicas		No necesitas un WAF complejo o una VPN para proteger lo que no se confía de entrada. Todo se valida en la app.
🧩 Arquitectura modular y segura por diseño		Al tener capas bien definidas, podés desacoplar servicios sin perder el control de acceso ni seguridad.
⏱️ Respuesta más rápida a incidentes			Puedes y debes revocar accesos, tokens o flujos sin reiniciar sistemas ni cerrar sesiones.
🛡️ Defensa activa contra ataques internos		Usuarios o servicios internos comprometidos tienen menos margen para causar daño.

## ¿Qué cambia al adoptar Zero Trust?

Aspecto						Modelo tradicional				Zero Trust

Control de acceso			Por rol, muchas veces implícito	Por contexto, siempre explícito
Confianza en red interna	Alta							Nula
Seguridad en cliente		Alta dependencia				Solo visual, sin poder
Tokens						Largos, poco controlables		Breves, revocables, firmados y auditables
Verificación				En el login						En cada acción



🛠️ 3. Implementación en el Backend (Java)

En esta sección profundizamos en cómo aplicar Zero Trust de forma concreta en el backend utilizando Java y Spring Boot (versiones 3.4.4 / 3.5.0 LTS). La meta es construir un servidor resistente, observable, controlado y libre de suposiciones peligrosas.

## 3.1. Autenticación y Autorización

❌ Qué evitar
1. Cookies de sesión tradicionales: mantienen una sesión larga y son propensas a robo (XSS, CSRF, secuestro).
2. JWTs de larga duración: una vez emitidos, son imposibles de revocar sin complejos workarounds.
3. Autenticación dependiente del frontend: nunca delegar la seguridad al navegador o al cliente móvil.

✅ Recomendaciones y estado del arte
🔐 Tokens de corta duración con revocación dinámica
1. Emite tokens (por ejemplo, JWT) válidos por pocos minutos (5–15).
2. Implementa un token de refresco revocable.
3. Usa un almacenamiento centralizado (Redis o base de datos ligera) para poder invalidar tokens activos.

🔍 Introspección de tokens con OAuth2/OpenID Connect

1. Utiliza un Identity Provider (IdP) moderno como Keycloak, Auth0, Okta o ForgeRock.
2. Habilita introspección de tokens (/introspect) para verificar validez, contexto y permisos en tiempo real.

🧬 MFA con FIDO2 / WebAuthn

1. Integra una segunda capa de autenticación con dispositivos de hardware, autenticadores biométricos o navegador compatible con WebAuthn.
Esto reduce enormemente el riesgo de suplantación de identidad incluso si una contraseña o token se filtra.

## 3.2. Control de Acceso

❌ Qué evitar
1. @PreAuthorize("hasRole('ADMIN')") y similares: estáticos, frágiles, sin contexto.
2. Lógica de permisos dispersa: difícil de auditar, propensa a errores.

✅ Recomendaciones
🎯 ABAC (Attribute-Based Access Control)

Evalúa permisos dinámicamente según:
	Usuario → ID, departamento, ownership, MFA activa
	Recurso → Sensibilidad, propietario
	Contexto → IP, ubicación, hora, nivel de riesgo
	Se puede implementar como un servicio REST que evalúe políticas (/canAccess) o con una librería embebida.

🧠 Centralización de políticas de acceso

1. Define políticas en el Identity Provider o en un servicio dedicado de decisiones (PDP).
2. Usa formatos como OPA (Open Policy Agent) o XACML si necesitas expresividad y auditoría completa.

## 3.3. Seguridad en la Comunicación

✅ Recomendaciones
🔒 mTLS (Mutual TLS)

1. Obliga a que cada microservicio tenga un certificado propio.
2. Verifica identidad no solo del cliente, sino también del servidor. Ideal para clusters internos y sistemas sensibles.

🧰 API Gateway con seguridad declarativa

Utiliza Spring Cloud Gateway, Kong, NGINX o Envoy para:
1. Terminar TLS
2. Aplicar rate limiting, CORS, validación de tokens, cabeceras, etc.
3. Registrar y auditar todas las llamadas

A FUEGO!: El Gateway se convierte en el único punto de entrada expuesto públicamente.

🧪 Frameworks y dependencias recomendadas
Área				Herramienta

Identity Provider	Keycloak / Auth0 / Ory
MFA					WebAuthn4J, Spring Security FIDO2
Autorización ABAC	Spring Authorization Server + OPA
Token Storage		Redis, Hazelcast, JDBC
Auditoría			OpenTelemetry, ELK stack, Spring Actuator
Observabilidad		Micrometer + Prometheus/Grafana
Seguridad HTTP		Spring Security con configuraciones explícitas de CORS, CSP, referrer-policy

## 🔁 3.4. Políticas de revocación, expiración y trazabilidad

En una arquitectura Zero Trust, el control continuo sobre los accesos no es opcional. 
Cualquier sistema que no permita revocar, monitorear y trazar acciones en tiempo real es una puerta abierta al movimiento lateral, 
persistencia y exfiltración de datos.

🎯 Objetivos de esta sección

1. Minimizar el tiempo de exposición de credenciales.
2. Garantizar que los accesos sean efímeros, verificables y revocables.
3. Detectar accesos anómalos con trazabilidad y análisis forense.

⏳ Políticas de expiración de tokens

✔️ Buenas prácticas
1. Access tokens de corta duración: 5 a 15 minutos máximo.
2. Refresh tokens reutilizables pero revocables: con rotación automática.
3. Expiración dependiente del riesgo/contexto: menos duración si:
	3.1 No hay MFA.
	3.2 IP desconocida.
	3.3 Dispositivo no verificado.
	3.4 Acceso nocturno o fuera de la región habitual.

⚠️ Evitar
1. Tokens con expiración de días o semanas.
2. Tokens que no puedan revocarse antes de expirar.
3. Manejo de sesión dependiente de cookies persistentes.

❌ Revocación activa

🧠 ¿Por qué es importante?
Sin revocación activa, un token robado es válido hasta que expira, aunque el usuario haya cerrado sesión, 
cambiado la contraseña o se haya detectado actividad sospechosa.

🛠️ Implementaciones recomendadas
1. Token revocation list (TRL): Lista negra de tokens revocados (por ID o hash). Se consulta en cada request.
2. Access tokens validados vía introspección (RFC 7662): permite saber si un token sigue activo, su audiencia y sus scopes.
3. Refresh token rotación + detección de reuso: si se detecta reutilización de un refresh token anterior, 
se asume compromiso y se revocan todos los tokens.

📜 Trazabilidad (Auditoría y registros)

🔍 Qué auditar?
1. Emisión y uso de tokens (access y refresh).
2. Accesos a recursos protegidos (endpoint + ID de usuario + resultado).
3. Cambios en los permisos (grants, revocaciones).
4. Accesos fallidos, reintentos y patrones inusuales.
5. Fallos de MFA o cambios en el método de autenticación.

🛠️ Herramientas y enfoques

1. Spring Actuator + Logback + MDC: para enriquecer logs con información contextual (usuario, IP, operación).
2. OpenTelemetry / Micrometer: métricas y trazas distribuidas por servicio.
3. ELK (Elasticsearch + Logstash + Kibana) o Grafana Loki: análisis visual y alerta sobre logs.
4. Sistemas SIEM (Security Information and Event Management): como Wazuh, Splunk o Graylog.

📁 Buenas prácticas
1. Agregar un X-Audit-Id por request que se propague entre servicios.
2. Incluir hashes de payloads sensibles para verificar que no han sido alterados.
3. Mantener trazas por usuario, recurso y tipo de operación.

📌 Revocación desde el backend (no desde el cliente)

Todo mecanismo de revocación debe:

1. Ser centralizado y controlado desde el servidor.
2. Poder ejecutarse automáticamente en respuesta a eventos de seguridad.
3. No depender de que el cliente “colabore” (como cerrar sesión en la interfaz).

A FUEGO!
⚠️ Nunca delegues la revocación al navegador ni al frontend. El cliente no es de confianza.

💡 Política de sesión y rotación sugerida (Zero Trust baseline)

Tipo de Token	Duración Máxima		Rotación		Revocación
Access Token	5–15 minutos		No				Por TRL o introspección
Refresh Token	24h–7 días			Sí (cada uso)	Reutilización detectada = revocación inmediata
MFA Session		1 hora (máximo)		No				Al cerrar navegador o inactividad prolongada


🎨 4. Implementación en el Frontend

El frontend en una arquitectura Zero Trust debe tratarse como no confiable por defecto. 
El navegador del usuario puede estar comprometido, mal configurado o manipulando los datos 
que muestra y envía. Asume que habrá usuarios avanzados que van a tratar de explotar el sistema usando 
tecnología como Burp Suite y parecidas. 

El objetivo aquí es blindar la interfaz tanto contra ataques del lado del cliente 
(XSS, clickjacking, inyección) como contra usos indebidos de los datos y tokens.

🧱 4.1. Content Security Policy (CSP) y Subresource Integrity (SRI)

🎯 Objetivo
Prevenir ejecución de scripts maliciosos, carga de recursos de fuentes no confiables y protección 
contra técnicas de phishing avanzado y clickjacking.

✅ Recomendaciones
📜 Definir políticas CSP estrictas

Una Content Security Policy (CSP) es una cabecera HTTP que indica al navegador qué tipos de contenido 
están permitidos, desde qué orígenes, y cómo deben tratarse.

Ejemplo básico (versión inicial para endurecer):

	Content-Security-Policy:
	  default-src 'self';
	  script-src 'self' https://cdn.trusted.com;
	  style-src 'self' 'unsafe-inline';
	  object-src 'none';
	  frame-ancestors 'none';
	  base-uri 'none';
	  report-uri /csp-report

🧷 Usar SRI (Subresource Integrity)

Cuando se cargan recursos externos (JS, CSS), usa atributos integrity y crossorigin para asegurar que el contenido no ha sido alterado.

	<script
	  src="https://cdn.trusted.com/app.js"
	  integrity="sha384-oqVuAfXRKap7fdgcCY5uykM6+R9GqQ8K/uxgQf4cZO4="
	  crossorigin="anonymous">
	</script>

El hash se genera sobre el contenido original.
El navegador rechaza la carga si el contenido fue modificado (ataque de intermediario, compromiso del CDN, etc.).

❌ Evitar
1. script-src 'unsafe-inline' o eval() sin justificación.
2. Cargar JS de múltiples orígenes no verificados.
3. Usar formularios embebidos de terceros sin sandbox.

🔗 4.2. Desacoplamiento del Frontend y Backend

🎯 Objetivo
Evitar que el frontend dependa del backend para lógica de presentación o decisiones de seguridad. 
En Zero Trust, cada capa debe asumir que la otra puede estar comprometida.

✅ Recomendaciones
⚙️ Separar lógica y presentación

Usa frameworks modernos como Next.js, Astro, SvelteKit, Nuxt.js, que permiten SSR (Server-Side Rendering) 
o SSG (Static Site Generation).

Esto permite:

	1. Generar contenido seguro en el servidor (mejor para SEO, control de errores).

	2. Evitar exposición de lógica de negocio en el navegador.
	
	3. Reducción de superficie XSS.

🔐 Tokens de corta duración

1. El frontend nunca jamás, bajo ningún concepto debería mantener una sesión larga.
2. Almacenar tokens en memoria (no en localStorage ni sessionStorage).
3. Si es necesario persistencia temporal, usar cookies HTTP-only + SameSite=Strict y expirar en minutos.

A FUEGO! ✋ Evitar que el frontend administre seguridad

Toda validación de permisos, contexto y recursos debe realizarse en el backend.
El frontend solo muestra información aprobada para el usuario autenticado.

🧪 Herramientas recomendadas

Área					Herramienta
Análisis de CSP			CSP Evaluator (https://csp-evaluator.withgoogle.com)
SRI generator			SRI Hash Generator (https://www.srihash.org)
Frameworks SSR/SSG		Next.js, Astro, Nuxt.js, SvelteKit
Testing de seguridad	OWASP ZAP, Lighthouse, SonarQube (https://www.incibe.es/incibe-cert/seminarios-web/uso-owasp-zap#)

🛡️ Supervisión de Seguridad Frontend desde Backend (Zero Trust)

Una de las mayores fuentes de brechas de seguridad proviene del hecho de que muchos backends asumen que el frontend es seguro por defecto. 
En una arquitectura Zero Trust, esta suposición es inaceptable. 
El backend debe actuar como verificador de última instancia, no solo de datos, sino también del contexto en el que se entrega el contenido.

✅ Actividad 1: Exigir que CSP y SRI estén activos en producción

🧩 ¿Por qué importa?
CSP mitiga ataques XSS, clickjacking y data injection.
SRI protege frente a compromisos de terceros (CDNs alterados, recursos cacheados maliciosamente).
🛠️ Cómo verificarlo desde el backend
✔️ 1. CSP entregado como cabecera HTTP

Asegúrate de que los entornos de producción incluyan una cabecera como esta (personalizable según el proyecto):

	Content-Security-Policy:
	  default-src 'self';
	  script-src 'self' https://cdn.trusted.com;
	  style-src 'self' 'unsafe-inline';
	  object-src 'none';
	  frame-ancestors 'none';
	  base-uri 'none';

Verifica esto:

Usa curl o httpie:
	curl -I https://tu-app.com

Automatiza con un test:
	MockMvc mockMvc = ...;
	mockMvc.perform(get("/"))
	       .andExpect(header().exists("Content-Security-Policy"));

✔️ 2. Auditar recursos externos con SRI

Busca <script> o <link> que carguen desde dominios externos.
Exige que incluyan integrity y crossorigin.
Ejemplo correcto:

	<script src="https://cdn.trusted.com/app.js"
	        integrity="sha384-..."
	        crossorigin="anonymous">
	</script>

Herramientas:

SRI Hash Generator
Escáneres automáticos tipo Lighthouse o SonarQube.
✅ Actividad 2: Supervisar cómo se entregan los tokens al frontend

⚠️ Contexto
Muchos frontends almacenan tokens de acceso en localStorage, lo que los hace vulnerables a XSS. También los exponen al JavaScript del navegador.

🔐 Mejores prácticas de entrega y almacenamiento
Opción segura (ideal)

Backend entrega un cookie HTTP-only, Secure, SameSite=Strict, que contiene un token de corta duración.
El frontend no puede acceder directamente al token (está blindado contra JS malicioso).

Ejemplo con Spring Security:
	ResponseCookie cookie = ResponseCookie.from("Access-Token", token)
	    .httpOnly(true)
	    .secure(true)
	    .sameSite("Strict")
	    .path("/")
	    .maxAge(Duration.ofMinutes(15))
	    .build();

	response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

Opción 2 (menos ideal, pero controlable)

Token entregado en el cuerpo de la respuesta al login.
Frontend lo guarda en memory-only (variable en RAM, no en localStorage).
Cada interacción refresca el token si es necesario.

Nunca:

1. Usar localStorage para tokens.
2. Exponer el token como parte de una ruta (e.g., /auth/callback?token=...).

✅ Actividad 3: Validar que el frontend no expone secretos ni lógica de seguridad

🔍 Cómo auditar desde backend (o como arquitecto Zero Trust)
✔️ 1. Revisión de código y análisis estático

Analizar bundles JS en producción para detectar:

1)Keys API.
2) Tokens hardcodeados.
3)Comprobaciones de roles/clientes (deben hacerse en backend).
4) eval() o Function().

Ejemplo con herramientas:

	1. source-map-explorer

	2. grep -r "API_KEY" ./dist

✔️ 2. Validar que el frontend no haga control de permisos

Asegúrate de que el backend no confíe en claims (como roles o scopes) transmitidos por el frontend.

Los controles de acceso deben basarse en:
1. Tokens verificados vía introspección o validación JWT.
2. Contexto de sesión gestionado en backend.

📋 Checklist de revisión frontend desde Zero Trust (para backend)

Verificación						¿Cómo se comprueba?
CSP activa							curl -I https://app.com debe incluir Content-Security-Policy
Recursos externos con SRI			Visualizar HTML y verificar integrity y crossorigin
Token entregado como cookie segura	Revisar Set-Cookie y headers
No uso de localStorage para tokens	Revisión del bundle JS o navegador
No eval() ni Function()				Análisis estático del bundle
Ningún secreto expuesto				grep sobre código fuente o bundles
Control de acceso solo en backend	Revisar que roles/permisos no dependan del frontend

🧠 Conclusión

Aunque no desarrolles el frontend, el backend tiene la responsabilidad de garantizar que los tokens estén protegidos, 
que las políticas de seguridad estén activas, y que la interfaz de usuario no se convierta en un punto débil de la 
arquitectura Zero Trust.

Con respecto a herramientas como wget, curl y demás, que os veo venir:

En una arquitectura Zero Trust real, ningún nodo (ni frontend, ni backend, ni siquiera los sistemas de CI/CD) 
debe tener ninguna herramienta instalada en producción que permita:

1) Carga dinámica de recursos de terceros.
2) Ejecución arbitraria (eval, curl, wget, npm install, etc.).
3) Comunicación saliente no controlada.

💻 ¿Dónde sí usar herramientas de análisis o validación?

Las herramientas que mencioné —como curl, grep, source-map-explorer, etc.— no deben correr en producción. Su lugar correcto es:

🧪 1. En la estación del operador (auditor, desarrollador, DevSecOps)
Clonar el código fuente del frontend o backend.
Generar los bundles localmente.
Revisar los headers o CSP haciendo peticiones contra entornos de staging o producción, desde una red controlada.
El operador tiene una estación de trabajo de confianza, que está sujeta a controles de DLP, endpoint protection, acceso con MFA y monitoreo continuo.

🏗️ 2. En pipelines de CI/CD
Las verificaciones pueden automatizarse antes del despliegue.
El entorno CI puede tener acceso limitado solo a:
Pull Requests.
Builds temporales.
Linters, escáneres, pruebas.
Ejemplo: Incluir un paso en CI que escanee el bundle frontend buscando secretos, recursos sin SRI o CSP mal configuradas. 
Pero ese pipeline nunca debe tener permisos para modificar producción directamente sin paso humano intermedio.

🧨 ¿Y en producción?

Producción no debe permitir:

Navegación arbitraria (ni siquiera desde terminal).
Ejecución de herramientas genéricas (curl, pip, npm, etc.).
Carga de librerías en caliente.
Shells abiertas sin autenticación fuerte y trazabilidad completa.
Incluso los contenedores deben ejecutarse con:

rootless.
Capacidad de red saliente restringida.
Lista blanca de destinos de red salientes (por ejemplo, solo al IdP o sistemas internos).
🧠 Resumen

Entorno								¿Se permite usar herramientas de análisis?	Requisitos
👩‍💻 Estación de trabajo del operador	✅ Sí										MFA, monitoreo, DLP
🛠️ CI/CD							✅ Sí, controlado							Entorno aislado, sin acceso a secretos de prod
🔐 Producción						❌ Nunca									Solo binarios auditados, salida de red controlada

5. Monitoreo y Auditoría
Recomendaciones:
Implementar soluciones de logging y monitoreo (e.g., ELK, Splunk) para auditar accesos y detectar anomalías.
Registrar y analizar cada intento de acceso a recursos sensibles.
6. Alternativas a Spring Framework
Motivación: Explorar frameworks que ofrezcan una mejor alineación con los principios de Zero Trust.
Alternativas:
Micronaut: Diseñado para microservicios y funciones serverless, con arranque rápido y bajo consumo de memoria.
Quarkus: Optimizado para contenedores y Kubernetes, con tiempos de arranque rápidos y bajo uso de memoria.
Helidon: Ofrece un enfoque moderno y ligero para microservicios Java.
Dropwizard: Combina varias bibliotecas para crear aplicaciones RESTful de alto rendimiento.
Javalin: Un framework web ligero y flexible para Java y Kotlin.
7. Conclusión
La implementación de una arquitectura Zero Trust en entornos Java requiere un enfoque integral que abarque desde la autenticación y autorización hasta la seguridad en la comunicación y el frontend.
La elección del framework adecuado es crucial para facilitar la adopción de estos principios y garantizar una seguridad robusta y escalable.



📘 Glosario básico de términos Zero Trust

Término									Definición clara y concisa
Zero Trust								Modelo de seguridad que no confía por defecto en ningún actor, interno o externo, y exige verificación continua de identidad, contexto y permisos.
ABAC (Attribute-Based Access Control)	Control de acceso basado en atributos contextuales (hora, ubicación, dispositivo, sensibilidad del recurso, etc.). Más flexible que RBAC.
RBAC (Role-Based Access Control)		Control de acceso clásico basado en roles asignados a usuarios. Limitado para escenarios dinámicos o críticos.
Privilegios mínimos						Principio según el cual cada entidad debe tener solo los permisos estrictamente necesarios, y solo por el tiempo requerido.
Identidad								Conjunto verificable de atributos que definen a un actor (usuario, servicio o máquina) en el sistema. Puede incluir claves, certificados, biometría, etc.
Contexto								Información adicional que rodea una petición: hora, lugar, IP, tipo de dispositivo, historial, nivel de riesgo, etc.
Perímetro								Borde o frontera lógica de un sistema. En Zero Trust, el perímetro ya no es la red, sino la identidad y el contexto.
Token									Representación digital de una sesión o identidad. Puede ser efímero (válido por minutos), firmada, revocable, o no.
Revocación								Capacidad de invalidar un token o credencial antes de que expire naturalmente. Fundamental en Zero Trust.
Microservicio confiable					Concepto rechazado por Zero Trust: ningún servicio se considera confiable sin validación explícita y continua.
Auditoría								Registro detallado de acciones, accesos y fallos. En Zero Trust, no es opcional: es una función de seguridad activa.
Movimiento lateral						Técnica donde un atacante, una vez dentro, se desplaza entre sistemas internos con credenciales o accesos robados. Zero Trust lo combate frontalmente.


📘 Como podemos impedir activamente que un atacante que haya conseguido infiltrarse, cargar su rootkit?

Esto va al núcleo del enfoque Zero Trust: suponemos que el atacante entrará, y nos preparamos para limitar el daño, detectar su presencia y evitar persistencia.

Un rootkit implica que el atacante quiere ganar control del sistema operativo o ocultar su presencia. Por tanto, debemos cerrar todos los caminos que permiten modificar el entorno de ejecución o el kernel.

🔐 Estrategias activas para impedir la carga de un rootkit

🧱 1. Evita ejecución con privilegios
✅ Usa contenedores rootless

El proceso dentro del contenedor no tiene UID 0, ni siquiera dentro del namespace.
Reduce casi a cero la capacidad de modificar el sistema operativo anfitrión.
	
	docker run --user 1000:1000 myimage

✅ Usa seccomp, AppArmor o SELinux para restringir syscalls

Puedes bloquear llamadas como ptrace, insmod, mmap, clone, etc.
Estas son típicamente utilizadas por rootkits para:

1. Inyectar código.
2. Interceptar syscalls.
3. Escalar privilegios.

	docker run --security-opt seccomp=seccomp-profile.json myimage

Ejemplo de perfil seccomp que bloquea llamadas peligrosas:

	{
	  "defaultAction": "SCMP_ACT_ERRNO",
	  "syscalls": [
	    { "names": ["read", "write", "exit", "sigreturn"], "action": "SCMP_ACT_ALLOW" }
	  ]
	}

🛡️ 2. Habilita medidas del kernel para protección activa
✅ Kernel Lockdown Mode (Linux)

1. Bloquea acceso a interfaces como /dev/mem, ioport, etc.
2. Impide inyecciones en el kernel.

Activa esto con:

	GRUB_CMDLINE_LINUX="lockdown=confidentiality"

✅ Secure Boot + Signed Kernel Modules

1. Solo se pueden cargar módulos con firma digital válida.
2. Previene rootkits del tipo loadable kernel module (LKM).

🔒 3. Inmutabilidad y verificación del sistema de archivos
✅ Usa sistemas de solo lectura (read-only rootfs)

El contenedor o la máquina no puede modificar su propio filesystem.

	FROM scratch
	COPY app /
	CMD ["/app"]

O con Docker:

	docker run --read-only myimage

✅ Verificación con dm-verity o IMA

Sistemas que validan la integridad del File System al montar.
Detectan alteraciones en tiempo de ejecución.
🎯 4. Evita caminos comunes de inyección
❌ Bloquea herramientas peligrosas:

	bash, gcc, ld, curl, wget, python, perl, npm, etc.
En entornos Zero Trust, no deberían existir ni como dependencias.

✅ Usa distros minimalistas (e.g., Wolfi, Alpine)

Superficies de ataque reducidas.
Evitan shells por defecto.
No incluyen intérpretes o compiladores.
📡 5. Supervisión activa del entorno
✅ Usa detección de comportamiento con:

eBPF (Falco, Tracee): detecta eventos del kernel que revelan actividad sospechosa.
Auditd / OSQuery / Wazuh: registros de eventos detallados.
Integrity Monitoring: hashing periódico del filesystem crítico.
✅ Controla accesos y comandos ejecutados

Centraliza logs de shell (si existiese).
Usa shells restringidas (rbash, noshell) o desactiva shells completamente.

🧠 Conclusión

Un atacante puede infiltrarse, pero:

Medida							Impide
Rootless + seccomp				Escalar privilegios, usar syscalls clave
Read-only filesystem			Persistencia, modificación de binarios
No herramientas externas		Compilar o bajar payloads
Kernel Lockdown + Secure Boot	Cargar módulos rootkit
eBPF/Falco						Ocultarse o evadir monitoreo


🧩 Plan de implementación Zero Trust contra rootkits

✅ 1. Dockerfile base seguro con sistema de solo lectura
Usando Wolfi o Alpine.
Sin shell, sin interpretes, sin gestor de paquetes.
✅ 2. Perfil seccomp personalizado
Bloquea syscalls comunes usadas por rootkits o técnicas de evasión.
✅ 3. Política AppArmor o SELinux opcional
Complemento de aislamiento para mayor defensa en profundidad.
✅ 4. Restricción de red saliente y filesystem
Docker --read-only, --cap-drop, y reglas de red.
✅ 5. Integración con Falco o Tracee para detección
Para sandboxing dinámico y alertas ante comportamiento sospechoso.
✅ 6. Checklist de hardening para imágenes y nodos
Sin binarios peligrosos (curl, bash, etc.).
Evitar permisos innecesarios en tiempo de ejecución.


🐋 Dockerfile seguro basado en Wolfi (rootless + read-only + sin herramientas peligrosas)

🎯 Objetivo:
Imagen mínima.
Sin intérpretes, shells o herramientas que permitan exfiltración o modificación.
Ejecutado con UID sin privilegios.
Sistema de archivos de solo lectura en tiempo de ejecución.

🔧 Dockerfile de ejemplo (Wolfi + Java app)

	# Usa Wolfi como base: https://github.com/wolfi-dev/os
	FROM cgr.dev/chainguard/jre:latest@sha256:<digest>

	# Crear un usuario sin privilegios
	RUN adduser -S -u 1000 appuser

	# Copiar la app precompilada (sin herramientas de build)
	COPY --chown=appuser:appuser app.jar /app/app.jar

	# Cambiar al usuario no root
	USER appuser

	# Punto de entrada
	CMD ["java", "-jar", "/app/app.jar"]

📌 Nota: Reemplaza @sha256:<digest> por el hash real (se recomienda fijar el digest para reproducibilidad y evitar actualizaciones inesperadas).

🛡️ Opciones de seguridad al ejecutar el contenedor

	docker run \
	  --read-only \
	  --cap-drop=ALL \
	  --security-opt=no-new-privileges \
	  --security-opt seccomp=seccomp-zero-trust.json \
	  --user 1000:1000 \
	  --tmpfs /tmp \
	  my-zero-trust-app

🔒 Explicación de flags:

| Flag                  | Propósito                                            |
| --------------------- | ---------------------------------------------------- |
| `--read-only`         | Evita escritura en disco del contenedor              |
| `--cap-drop=ALL`      | Elimina todas las capacidades del kernel             |
| `--no-new-privileges` | Impide escalado de privilegios dentro del contenedor |
| `--seccomp=...`       | Aplica perfil personalizado de syscalls permitidas   |
| `--user 1000:1000`    | Corre como usuario no root                           |
| `--tmpfs /tmp`        | Permite uso temporal de `/tmp` en memoria            |


✅ Checklist de la imagen

 No incluye curl, wget, bash, sh, gcc, python, npm, etc.
 Solo incluye el runtime Java necesario.
 Usa UID fijo y sin permisos especiales.
 No incluye herramientas de red ni compiladores.

 ## Como impido que en esa imagen alguien pueda instalar algo?

Para evitar cualquier intento de instalación o modificación dentro de un contenedor Zero Trust, debes bloquear tanto las herramientas como las rutas que un atacante podría usar. Vamos a repasarlo por capas, combinando imagen segura con políticas activas de contención.

🧱 1. Usa una imagen sin gestor de paquetes

Distribuciones como Wolfi, Distroless, o incluso Alpine minimal pueden construirse sin apk, apt, yum, etc.

✔ Ejemplo:
En la imagen cgr.dev/chainguard/jre, no existen:

apk, bash, wget, curl, python, gcc, ld, pip, etc.
Esto impide al atacante descargar, compilar o ejecutar herramientas.
🔒 2. Sistema de archivos de solo lectura

Con --read-only:

1. Se bloquea toda escritura al sistema de archivos, incluyendo:
2. instalación de nuevos binarios.
3. creación de scripts temporales.

	docker run --read-only --tmpfs /tmp ...

El contenedor solo puede escribir en /tmp, pero este está montado en memoria, por lo que se borra al parar el contenedor.

⛔ 3. Elimina todas las capacidades del kernel

Con --cap-drop=ALL eliminas el acceso a:

Montar archivos (CAP_SYS_ADMIN)
Cargar módulos (CAP_SYS_MODULE)
Ejecutar chroot, ptrace, mknod, etc.
Esto bloquea caminos típicos de instalación y modificación.

🔐 4. Bloquea syscalls peligrosas con seccomp

Con un perfil seccomp, puedes denegar syscalls como:

1. clone (evita creación de procesos complejos)
2. ptrace (no se puede observar o inyectar en otros procesos)
3. mmap, mprotect, bpf, keyctl, etc.

Así incluso si un atacante logra insertar algo en memoria, no podrá ejecutarlo ni usar mecanismos avanzados del kernel.

❌ 5. Sin intérpretes = sin código dinámico

No incluir:

sh, bash, python, perl, node, php…
Esto evita que scripts maliciosos puedan ejecutarse aunque logren infiltrarse.
✈️ 6. Bloquear acceso a la red saliente

Si tu aplicación no necesita conexiones externas:

	--network none

O mejor aún, con políticas de firewall (iptables / eBPF) que solo permitan el tráfico necesario.

🧬 Resultado: Ataque frustrado

Incluso si un atacante logra ejecutar algo dentro de tu contenedor:

No puede escribir en disco.
No puede descargar binarios.
No puede usar intérpretes.
No puede abrir sockets.
No puede persistir.
No puede escalar privilegios.
No puede ocultarse con rootkits.

🔐 Plantillas Zero Trust contra rootkits

✅ Dockerfile seguro
✅ Comando docker run con todas las restricciones
🧩 docker-compose.yml con políticas equivalentes
🐳 Docker Swarm stack (docker stack deploy) con configuraciones seguras
☸️ Manifiestos Kubernetes (Pod, Deployment, SecurityContext, SeccompProfile, etc.)

🐋 1. Dockerfile seguro basado en Wolfi (Zero Trust)

	# Imagen mínima sin herramientas peligrosas
	FROM cgr.dev/chainguard/jre:latest@sha256:<digest>

	# Crear usuario sin privilegios
	RUN adduser -S -u 1000 appuser

	# Copiar JAR de la app (compilado fuera del contenedor)
	COPY --chown=appuser:appuser app.jar /app/app.jar

	# Usar usuario no root
	USER appuser

	# Directorio de trabajo (opcional si la app lo requiere)
	WORKDIR /app

	# Ejecutar la aplicación
	CMD ["java", "-jar", "/app/app.jar"]

Reemplaza @sha256:<digest> con el hash específico de la imagen para reproducibilidad.

🛡️ 2. Comando docker run con hardening completo

	docker run \
	  --read-only \
	  --tmpfs /tmp \
	  --cap-drop=ALL \
	  --security-opt=no-new-privileges \
	  --security-opt seccomp=/path/to/seccomp-zero-trust.json \
	  --user 1000:1000 \
	  --name myapp \
	  --network none \
	  my-zero-trust-image

🔍 ¿Qué hace cada flag?

| Flag                               | Propósito                                                       |
| ---------------------------------- | --------------------------------------------------------------- |
| `--read-only`                      | El sistema de archivos es inmutable                             |
| `--tmpfs /tmp`                     | Monta `/tmp` en memoria para evitar errores de apps que lo usan |
| `--cap-drop=ALL`                   | Elimina todas las capacidades del kernel                        |
| `--security-opt=no-new-privileges` | Evita `setuid`, `sudo`, escalado en tiempo de ejecución         |
| `--security-opt seccomp=...`       | Carga perfil personalizado de syscalls permitidas               |
| `--user 1000:1000`                 | Usa un usuario sin privilegios                                  |
| `--network none`                   | Aísla completamente la red (opcional, depende de tu app)        |


Vamos a crear dos variantes del archivo docker-compose.yml siguiendo una estrategia Zero Trust:

✅ Versión con acceso interno entre servicios (mínimo y seguro)
🔒 Versión completamente aislada de la red (sin conectividad alguna)
🧩 1. docker-compose.yml con red interna segura

	version: '3.8'

	services:
	  backend:
	    image: my-zero-trust-image:latest
	    container_name: backend
	    user: "1000:1000"
	    read_only: true
	    tmpfs:
	      - /tmp
	    cap_drop:
	      - ALL
	    security_opt:
	      - no-new-privileges:true
	      - seccomp=/etc/docker/seccomp-zero-trust.json
	    networks:
	      - internal
	    depends_on:
	      - db

	  db:
	    image: postgres:16-alpine
	    container_name: database
	    environment:
	      POSTGRES_USER: secureuser
	      POSTGRES_PASSWORD: securepass
	      POSTGRES_DB: myapp
	    volumes:
	      - db_data:/var/lib/postgresql/data
	    networks:
	      - internal

	volumes:
	  db_data:

	networks:
	  internal:
	    driver: bridge
	    internal: true

🔐 Seguridad destacada:
Solo una red privada interna (internal: true) — sin acceso a Internet.
read_only, cap_drop, tmpfs, seccomp, user aplicados al backend.
El contenedor db es accesible solo desde backend.
🔒 2. docker-compose.yml con red completamente aislada

Este contenedor no podrá comunicarse ni hacia fuera ni hacia adentro.

	version: '3.8'

	services:
	  isolated-backend:
	    image: my-zero-trust-image:latest
	    container_name: isolated-backend
	    user: "1000:1000"
	    read_only: true
	    tmpfs:
	      - /tmp
	    cap_drop:
	      - ALL
	    security_opt:
	      - no-new-privileges:true
	      - seccomp=/etc/docker/seccomp-zero-trust.json
	    network_mode: none

Vamos ahora con la versión para Docker Swarm, utilizando docker stack deploy. En este entorno, la configuración se realiza desde un archivo docker-compose.yml pero con algunas diferencias sutiles. Mostraré dos variantes como antes:

🐳 Docker Swarm con red interna segura

	version: '3.8'

	services:
	  backend:
	    image: my-zero-trust-image:latest
	    deploy:
	      replicas: 1
	      restart_policy:
	        condition: on-failure
	      resources:
	        limits:
	          memory: 512M
	        reservations:
	          memory: 256M
	    user: "1000:1000"
	    read_only: true
	    tmpfs:
	      - /tmp
	    cap_drop:
	      - ALL
	    security_opt:
	      - no-new-privileges:true
	      - seccomp=/etc/docker/seccomp-zero-trust.json
	    networks:
	      - internal
	    depends_on:
	      - db

	  db:
	    image: postgres:16-alpine
	    environment:
	      POSTGRES_USER: secureuser
	      POSTGRES_PASSWORD: securepass
	      POSTGRES_DB: myapp
	    volumes:
	      - db_data:/var/lib/postgresql/data
	    networks:
	      - internal

	volumes:
	  db_data:

	networks:
	  internal:
	    driver: overlay
	    internal: true

⚠️ Nota: Swarm solo acepta redes overlay, incluso para servicios en un solo nodo. internal: true evita que tengan salida a Internet.

🔒 Docker Swarm con contenedor completamente aislado

	version: '3.8'

	services:
	  isolated-backend:
	    image: my-zero-trust-image:latest
	    deploy:
	      replicas: 1
	      restart_policy:
	        condition: on-failure
	    user: "1000:1000"
	    read_only: true
	    tmpfs:
	      - /tmp
	    cap_drop:
	      - ALL
	    security_opt:
	      - no-new-privileges:true
	      - seccomp=/etc/docker/seccomp-zero-trust.json
	    network_mode: "none"

❗ En Swarm, network_mode: "none" no está oficialmente soportado por docker stack deploy. 

En ese caso, hay que usar directamente docker run, o un Pod de K8s con hostNetwork: false y sin redes.

Vamos a crear un perfil Seccomp personalizado Zero Trust optimizado para contenedores Java seguros. Este perfil bloquea por defecto todos los syscalls peligrosos, permite solo lo estrictamente necesario y es ideal para apps que corren sobre un JRE sin acceso a red, sin privilegios, y con sistema de archivos de solo lectura.

🧱 seccomp-zero-trust.json

Guarda esto como /etc/docker/seccomp-zero-trust.json o en cualquier ruta accesible desde tus máquinas.

	{
	  "defaultAction": "SCMP_ACT_ERRNO",
	  "architectures": [
	    "SCMP_ARCH_X86_64"
	  ],
	  "syscalls": [
	    {
	      "names": [
	        "read", "write", "exit", "exit_group", "rt_sigreturn",
	        "brk", "mmap", "munmap", "mremap", "mprotect", "arch_prctl",
	        "clone", "futex", "set_tid_address", "set_robust_list",
	        "sigaltstack", "rt_sigaction", "rt_sigprocmask",
	        "nanosleep", "clock_gettime", "gettimeofday", "getpid",
	        "getppid", "getuid", "getgid", "geteuid", "getegid",
	        "getcwd", "readlink", "openat", "close", "newfstatat",
	        "lseek", "pread64", "pwrite64", "fcntl", "pipe2", "dup",
	        "dup2", "dup3", "readv", "writev", "lstat", "stat", "fstat",
	        "uname", "prlimit64", "getrandom", "sysinfo"
	      ],
	      "action": "SCMP_ACT_ALLOW"
	    }
	  ]
	}

🛡️ Características de este perfil

| Protección                       | Detalles                                                                 |
| -------------------------------- | ------------------------------------------------------------------------ |
| `defaultAction: SCMP_ACT_ERRNO`  | Deniega todos los syscalls no explícitamente permitidos                  |
| Permite syscalls esenciales      | Solo los necesarios para procesos Java y operaciones básicas del sistema |
| **Bloquea por defecto:**         | `ptrace`, `reboot`, `mount`, `init_module`, `kexec_load`, `clone3`, etc. |
| Compatible con imágenes rootless | No requiere capacidades elevadas ni procesos privilegiados               |

📌 Cómo usarlo en docker run

	docker run \
	  --security-opt seccomp=/etc/docker/seccomp-zero-trust.json \
	  my-zero-trust-image

📌 En docker-compose.yml o docker stack

	security_opt:
	  - seccomp=/etc/docker/seccomp-zero-trust.json

## Kubernetes

Vamos a crear una plantilla Kubernetes Zero Trust de nivel alto, que bloquea todo lo innecesario para reducir al máximo la superficie de ataque. Dividiremos el trabajo en dos partes:

✅ Pod con acceso interno controlado a otros servicios (ej. base de datos)
🔒 Pod completamente aislado y endurecido
En ambos casos aplicaremos:

runAsNonRoot, readOnlyRootFilesystem, capabilities, seccompProfile
AppArmor (si estás en un nodo con soporte, como Ubuntu)
Restricciones de red con NetworkPolicy (si el cluster las soporta)
🧩 1. Pod Zero Trust con acceso interno mínimo

	apiVersion: apps/v1
	kind: Deployment
	metadata:
	  name: backend
	spec:
	  replicas: 1
	  selector:
	    matchLabels:
	      app: backend
	  template:
	    metadata:
	      labels:
	        app: backend
	      annotations:
	        container.apparmor.security.beta.kubernetes.io/backend: localhost/backend-apparmor
	    spec:
	      securityContext:
	        runAsNonRoot: true
	        seccompProfile:
	          type: Localhost
	          localhostProfile: seccomp-zero-trust.json
	      containers:
	      - name: backend
	        image: my-zero-trust-image:latest
	        securityContext:
	          readOnlyRootFilesystem: true
	          allowPrivilegeEscalation: false
	          capabilities:
	            drop:
	              - ALL
	          runAsUser: 1000
	        volumeMounts:
	          - name: tmp
	            mountPath: /tmp
	        resources:
	          limits:
	            memory: "512Mi"
	            cpu: "500m"
	        ports:
	          - containerPort: 8080
	      volumes:
	        - name: tmp
	          emptyDir:
	            medium: "Memory"

🔐 Notas:
Usa seccomp-zero-trust.json, que debes montar en /var/lib/kubelet/seccomp/profiles/.
La política AppArmor debe estar instalada en /etc/apparmor.d/backend-apparmor en el nodo.
Este pod puede comunicarse con otros (ej. una base de datos) pero solo si la red lo permite.

🔒 2. Pod completamente aislado

Este no tiene red, ni disco de escritura, ni acceso a privilegios.

	apiVersion: v1
	kind: Pod
	metadata:
	  name: isolated-backend
	  annotations:
	    container.apparmor.security.beta.kubernetes.io/backend: localhost/backend-apparmor
	spec:
	  securityContext:
	    runAsNonRoot: true
	    seccompProfile:
	      type: Localhost
	      localhostProfile: seccomp-zero-trust.json
	  containers:
	  - name: backend
	    image: my-zero-trust-image:latest
	    securityContext:
	      readOnlyRootFilesystem: true
	      allowPrivilegeEscalation: false
	      capabilities:
	        drop:
	          - ALL
	      runAsUser: 1000
	    volumeMounts:
	      - name: tmp
	        mountPath: /tmp
	    resources:
	      limits:
	        memory: "256Mi"
	        cpu: "200m"
	    command: ["java"]
	    args: ["-jar", "app.jar"]
	  volumes:
	    - name: tmp
	      emptyDir:
	        medium: "Memory"
	  dnsPolicy: None
	  hostNetwork: false
	  hostPID: false
	  hostIPC: false
	  restartPolicy: Never

📜 NetworkPolicy de aislamiento

	apiVersion: networking.k8s.io/v1
	kind: NetworkPolicy
	metadata:
	  name: deny-all-egress
	spec:
	  podSelector: {}
	  policyTypes:
	  - Egress

Esto evita que cualquier pod se comunique con el exterior (si CNI soporta políticas).

🛡️ Perfil AppArmor: backend-apparmor

Guarda esto como /etc/apparmor.d/backend-apparmor en todos los nodos del cluster que vayan a ejecutar este pod.

	# /etc/apparmor.d/backend-apparmor

	#include <tunables/global>

	profile backend-apparmor flags=(attach_disconnected) {
	  # Denegar todo por defecto
	  deny /** mrwklx,

	  # Permitir lectura de librerías del sistema y del contenedor
	  /lib/** r,
	  /lib64/** r,
	  /usr/lib/** r,
	  /usr/lib64/** r,
	  /usr/share/** r,
	  /etc/** r,

	  # Permitir ejecución de la app Java
	  /usr/bin/java rix,
	  /app/** mr,

	  # Permitir acceso a /tmp si se usa tmpfs
	  /tmp/** rw,

	  # Permitir acceso de solo lectura a /proc
	  /proc/* r,
	  /proc/sys/** r,
	  /proc/self/** r,

	  # Permitir acceso necesario a /dev/null, /dev/urandom
	  /dev/null rw,
	  /dev/urandom r,

	  # Bloquear carga de módulos del kernel
	  deny /sbin/insmod x,
	  deny /sbin/modprobe x,
	  deny /usr/sbin/insmod x,
	  deny /usr/sbin/modprobe x,

	  # Denegar uso de ptrace
	  deny ptrace,

	  # Activar el modo de confinamiento
	  capability chown,
	  capability setuid,
	  capability setgid,
	  deny capability sys_admin,
	  deny capability sys_module,
	  deny capability net_raw,

	  # Perfil en modo de ejecución forzada
	  profile backend-apparmor flags=(attach_disconnected) {
	    # Ya estamos dentro, este bloque refuerza aislamiento
	  }
	}

✅ Aplicar y forzar el perfil

1. Recargar AppArmor:

	sudo apparmor_parser -r /etc/apparmor.d/backend-apparmor

2. Verificar que está activo:

	sudo aa-status

3. Usarlo desde Kubernetes (ya lo tienes en las plantillas como anotación):

	annotations:
  container.apparmor.security.beta.kubernetes.io/backend: localhost/backend-apparmor

📦 Empaquetar los manifiestos como Helm charts

📦 Helm Chart: zero-trust-backend

Estructura de archivos:

	zero-trust-backend/
	├── Chart.yaml
	├── values.yaml
	└── templates/
	    ├── deployment.yaml
	    ├── pod-isolated.yaml
	    └── networkpolicy.yaml

🧾 Chart.yaml

	apiVersion: v2
	name: zero-trust-backend
	description: Backend endurecido con Zero Trust (Spring Boot + Java)
	type: application
	version: 0.1.0
	appVersion: "1.0.0"

🔧 values.yaml

	replicaCount: 1

	image:
	  repository: my-zero-trust-image
	  tag: latest
	  pullPolicy: IfNotPresent

	security:
	  apparmorProfile: backend-apparmor
	  seccompProfile: seccomp-zero-trust.json

	resources:
	  limits:
	    memory: "512Mi"
	    cpu: "500m"

	containerPort: 8080

📄 templates/deployment.yaml

	apiVersion: apps/v1
	kind: Deployment
	metadata:
	  name: {{ .Release.Name }}-backend
	spec:
	  replicas: {{ .Values.replicaCount }}
	  selector:
	    matchLabels:
	      app: {{ .Release.Name }}-backend
	  template:
	    metadata:
	      labels:
	        app: {{ .Release.Name }}-backend
	      annotations:
	        container.apparmor.security.beta.kubernetes.io/backend: localhost/{{ .Values.security.apparmorProfile }}
	    spec:
	      securityContext:
	        runAsNonRoot: true
	        seccompProfile:
	          type: Localhost
	          localhostProfile: {{ .Values.security.seccompProfile }}
	      containers:
	      - name: backend
	        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
	        imagePullPolicy: {{ .Values.image.pullPolicy }}
	        securityContext:
	          readOnlyRootFilesystem: true
	          allowPrivilegeEscalation: false
	          capabilities:
	            drop:
	              - ALL
	          runAsUser: 1000
	        ports:
	          - containerPort: {{ .Values.containerPort }}
	        resources:
	{{ toYaml .Values.resources | indent 10 }}
	        volumeMounts:
	          - name: tmp
	            mountPath: /tmp
	      volumes:
	        - name: tmp
	          emptyDir:
	            medium: "Memory"

🔒 templates/networkpolicy.yaml

	apiVersion: networking.k8s.io/v1
	kind: NetworkPolicy
	metadata:
	  name: {{ .Release.Name }}-deny-egress
	spec:
	  podSelector:
	    matchLabels:
	      app: {{ .Release.Name }}-backend
	  policyTypes:
	  - Egress

☁️ Despliegue

	helm install backend ./zero-trust-backend

✅ Requisitos de versiones

⚙️ Kubernetes
Versión mínima recomendada: v1.25.x
Versión óptima (LTS estable a día de hoy): v1.28.x o v1.29.x
Las versiones >=1.25 eliminan definitivamente APIs obsoletas y permiten usar seccomp nativamente vía seccompProfile, así como políticas de red modernas.
Verifica tu versión:

	kubectl version --short

📦 Helm
Versión mínima recomendada: v3.8.0
Versión óptima (estable hoy): v3.14.x
Desde Helm 3.8 puedes usar plantillas más complejas y mejores validaciones. Además, ya no se requiere Tiller desde Helm 3.0 (por razones de seguridad, muy relevante para Zero Trust).

	helm version --short

📌 Resumen de versiones seguras

| Componente | Mínima segura | Óptima (2025)         |
| ---------- | ------------- | --------------------- |
| Kubernetes | `v1.25`       | `v1.28.x` / `v1.29.x` |
| Helm       | `v3.8.0`      | `v3.14.x`             |


🚀 Opción recomendada para laboratorio local

✅ Minikube con configuración Zero Trust
Minikube te permite usar AppArmor, seccomp, cgroups v2 y más desde una sola máquina. Ideal para reproducir lo que después irá a producción en Kubernetes real (EKS, GKE, AKS, etc).

🛠️ Script de instalación de Minikube con todo preconfigurado

Guarda esto como start-zero-trust-lab.sh y dale permisos de ejecución:

	#!/bin/bash

	set -e

	# Requisitos mínimos
	MINIKUBE_VERSION=v1.33.1
	K8S_VERSION=v1.29.2
	PROFILE=zero-trust

	# Crea cluster Minikube con políticas seguras
	minikube delete -p $PROFILE || true
	minikube start -p $PROFILE \
	  --kubernetes-version=$K8S_VERSION \
	  --driver=docker \
	  --container-runtime=containerd \
	  --apiserver-authorization-mode=RBAC \
	  --cni=flannel \
	  --extra-config=apiserver.enable-admission-plugins=NamespaceLifecycle,LimitRanger,ServiceAccount,DefaultStorageClass,ResourceQuota,NodeRestriction \
	  --addons=default-storageclass \
	  --no-vtx-check \
	  --force

	# Activar perfiles AppArmor y seccomp (root del host)
	sudo apparmor_parser -r /etc/apparmor.d/backend-apparmor
	echo "✅ AppArmor cargado."

	# Copiar perfil seccomp (ajustar ruta si es necesario)
	mkdir -p ~/.minikube/files/etc/security/profiles/seccomp
	cp seccomp-zero-trust.json ~/.minikube/files/etc/security/profiles/seccomp/

	# Reiniciar pod para aplicar perfiles
	minikube ssh -p $PROFILE -- sudo systemctl restart containerd
	echo "✅ Seccomp cargado."

	# Contexto actual para kubectl
	kubectl config use-context minikube

	# Resultado
	kubectl version --short
	helm version --short

🧪 Verificación

Luego puedes lanzar tu chart:

helm install backend ./zero-trust-backend
kubectl get pods
kubectl describe pod backend-xxxx


Verifica en los eventos y anotaciones que:

Se aplicó el seccompProfile: localhost/seccomp-zero-trust.json
Se aplicó el apparmorProfile: localhost/backend-apparmor
No hay acceso a red no declarada
No hay capacidad de escalar privilegios ni montar rootfs

🔐 1. seccomp-zero-trust.json (perfil personalizado)

Este perfil bloquea syscalls potencialmente peligrosas que los rootkits suelen aprovechar:

{
  "defaultAction": "SCMP_ACT_ERRNO",
  "archMap": [
    { "architecture": "SCMP_ARCH_X86_64", "subArchitectures": [] }
  ],
  "syscalls": [
    { "names": ["read", "write", "exit", "exit_group", "rt_sigreturn", "futex", "clock_gettime"], "action": "SCMP_ACT_ALLOW" },
    { "names": ["epoll_wait", "epoll_ctl", "epoll_create1", "eventfd2", "accept4", "recvfrom", "sendto"], "action": "SCMP_ACT_ALLOW" },
    { "names": ["socket", "connect", "setsockopt", "getsockopt"], "action": "SCMP_ACT_ALLOW" },
    { "names": ["openat", "close", "readlink", "fstat", "stat", "lstat", "mmap", "munmap"], "action": "SCMP_ACT_ALLOW" },
    { "names": ["nanosleep", "getpid", "getppid", "uname", "brk", "access"], "action": "SCMP_ACT_ALLOW" },
    { "names": ["getuid", "geteuid", "getgid", "getegid", "setgid", "setuid"], "action": "SCMP_ACT_ALLOW" },
    { "names": ["sigaltstack", "sigaction", "sigprocmask"], "action": "SCMP_ACT_ALLOW" }
  ]
}


💡 Este perfil es muy restrictivo y adecuado para procesos que no requieren llamadas al sistema complejas, como un servidor HTTP puro sin shell ni compilación en tiempo de ejecución.

🐳 2. Imagen base segura de Spring Boot (para Zero Trust)

Este Dockerfile está basado en wolfi, con Java 21 LTS, sin shell ni herramientas de desarrollo, sin root, y apto para ejecutarse como no root desde Kubernetes:

FROM cgr.dev/chainguard/jre:latest@sha256:fae6a2e... AS runtime

USER nonroot
WORKDIR /app
COPY target/myapp.jar app.jar

ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

🛡️ Notas de seguridad:

Imagen verificada, mínima, sin bash ni wget ni curl.
Solo el JRE, ideal para Spring Boot empaquetado en fat jar.
Usuario sin privilegios.
Soporte para seccomp, AppArmor y read-only filesystem.

Vamos a construir un Jenkinsfile reproducible, seguro y alineado con Zero Trust para tu myapp.jar, usando:

Construcción reproducible (sin acceso root, sin cambios dinámicos).
Dependencias verificadas.
Firma del binario final (jar).
Imagen Docker minimalista con política seccomp y sin herramientas de shell.
Publicación opcional firmada en un registry confiable (p.ej., GHCR o Harbor).
✅ Jenkinsfile: Construcción Segura para Spring Boot (Java 21 LTS + Docker + Zero Trust)



pipeline {
  agent any

  environment {
    IMAGE_NAME = 'ghcr.io/tuusuario/myapp'
    IMAGE_TAG = '1.0.0'
    JAVA_HOME = tool name: 'jdk-21', type: 'jdk'
    PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build JAR') {
      steps {
        sh '''
          ./mvnw clean verify -DskipTests
        '''
      }
    }

    stage('Firmar JAR (SHA256)') {
      steps {
        sh '''
          sha256sum target/myapp.jar > target/myapp.jar.sha256
        '''
      }
    }

    stage('Build Docker Image (reproducible)') {
      steps {
        writeFile file: 'Dockerfile', text: '''
          FROM cgr.dev/chainguard/jre:latest
          USER nonroot
          WORKDIR /app
          COPY target/myapp.jar app.jar
          ENTRYPOINT ["java", "-XX:+AlwaysActAsServerClassMachine", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
        '''
        sh '''
          docker build \
            --no-cache \
            --pull \
            --build-arg JAR_FILE=target/myapp.jar \
            -t $IMAGE_NAME:$IMAGE_TAG .
        '''
      }
    }

    stage('Escanear Imagen (Trivy/Cosign)') {
      steps {
        sh '''
          trivy image --exit-code 1 --severity CRITICAL,HIGH $IMAGE_NAME:$IMAGE_TAG
        '''
      }
    }

    stage('Firmar Imagen (Sigstore/Cosign)') {
      steps {
        withCredentials([file(credentialsId: 'cosign-key', variable: 'COSIGN_KEY')]) {
          sh '''
            cosign sign --key $COSIGN_KEY $IMAGE_NAME:$IMAGE_TAG
          '''
        }
      }
    }

    stage('Push a Registro (GHCR u otro)') {
      when {
        expression { return params.PUSH_IMAGE == true }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'registry-creds', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
          sh '''
            echo $PASSWORD | docker login ghcr.io -u $USERNAME --password-stdin
            docker push $IMAGE_NAME:$IMAGE_TAG
          '''
        }
      }
    }
  }

  post {
    failure {
      mail to: 'equipo@tudominio.com',
           subject: "[Jenkins] Fallo en construcción segura - ${env.JOB_NAME}",
           body: "Revisar build ${env.BUILD_URL}"
    }
  }
}
🧱 Requisitos para usarlo

Herramientas en Jenkins Agent:
Java 21
Maven
Docker
Trivy
Cosign
Credenciales necesarias:
registry-creds: usuario/contraseña del registry (GHCR, Harbor…)
cosign-key: clave privada (cosign.key) para firmar imágenes
🧩 Complementos opcionales

Firmado de jar con GPG.
Inspección binaria reproducible (reproducible-builds.org).
Policy-as-code en CI (con Conftest o [Kyverno]).

🧭 ¿Qué estamos construyendo?

En un enfoque Zero Trust realista y profundo, no basta con proteger el runtime. También debemos asegurar la cadena de suministro: desde el código fuente hasta el contenedor que se despliega.

Aquí entra en juego el concepto de Supply Chain Security.

🧱 Términos clave explicados

🔑 1. Cosign (parte del ecosistema Sigstore)
¿Qué es?
Una herramienta para firmar y verificar contenedores y artefactos (como .jar) usando claves criptográficas o identidad OIDC (GitHub Actions, Google, etc.).
¿Por qué importa?
Garantiza que una imagen o artefacto no ha sido modificado desde que fue construido por una fuente confiable.
Lo usaremos para:
Firmar tu imagen Docker.
Firmar el .jar si lo deseas.
Publicar y verificar esas firmas automáticamente en el registro.
🪪 2. SBOM (Software Bill of Materials)
¿Qué es?
Un archivo que lista todas las dependencias y componentes del software (paquetes, librerías, etc.).
¿Por qué importa?
Permite saber exactamente qué contiene tu aplicación. Es clave para:
Cumplimiento normativo.
Respuesta ante vulnerabilidades (CVE).
Auditorías de seguridad.
Formatos comunes:
CycloneDX (preferido en entornos modernos)
SPDX
🧰 3. Trivy
¿Qué es?
Un escáner de seguridad open-source muy completo.
¿Para qué sirve?
Detecta vulnerabilidades en:
Imágenes Docker
Dependencias de proyectos Java (pom.xml)
Archivos de configuración (Dockerfile, Kubernetes YAML)
Archivos SBOM
Complemento natural a SBOM y Cosign.
🔒 4. SLSA (Supply-chain Levels for Software Artifacts)
¿Qué es?
Un estándar de Google y otros para asegurar la cadena de suministro con niveles crecientes de garantías.
SLSA 1 a 4, de menor a mayor confianza:
Código versionado.
Build automatizado (no manual).
Build reproducible.
Build hermético y verificado.
¿Por qué importa?
Porque dice “puedes confiar en este binario porque puedo probar de dónde viene”.
🧪 5. Reproducible Builds
¿Qué es?
Técnica que asegura que compilar el mismo código dos veces genera exactamente el mismo binario.
¿Por qué importa?
Garantiza que nadie ha inyectado código malicioso durante el build.
🔐 ¿Qué haremos exactamente?

En tu Jenkinsfile y proceso de CI:

Construiremos el JAR de forma reproducible (sin timestamps ni valores dinámicos).
Generaremos un SBOM con CycloneDX.
Escanearemos todo (imagen y dependencias) con Trivy.
Firmaremos la imagen y opcionalmente el .jar con Cosign.
Emitiremos una firma SLSA Provenance (con GitHub Actions o Jenkins + Rekor).

¿Qué son las  políticas SLSA o inyección de etiquetas SBOM/CycloneDX? 

🧭 ¿Qué estamos construyendo?

En un enfoque Zero Trust realista y profundo, no basta con proteger el runtime. 
También debemos asegurar la cadena de suministro: desde el código fuente hasta el contenedor que se despliega.

Aquí entra en juego el concepto de Supply Chain Security, es decir, tenemos que asegurar que el fichero compilado es el 
que se ha compilado y que no ha sido modificado.

🧱 Términos clave explicados

🔑 1. Cosign (parte del ecosistema Sigstore)
¿Qué es?
Una herramienta para firmar y verificar contenedores y artefactos (como .jar) usando claves criptográficas o identidad OIDC (GitHub Actions, Google, etc.).
¿Por qué importa?
Garantiza que una imagen o artefacto no ha sido modificado desde que fue construido por una fuente confiable.
Lo usaremos para:
Firmar tu imagen Docker.
Firmar el .jar si lo deseas.
Publicar y verificar esas firmas automáticamente en el registro.
🪪 2. SBOM (Software Bill of Materials)
¿Qué es?
Un archivo que lista todas las dependencias y componentes del software (paquetes, librerías, etc.).
¿Por qué importa?
Permite saber exactamente qué contiene tu aplicación. Es clave para:
Cumplimiento normativo.
Respuesta ante vulnerabilidades (CVE).
Auditorías de seguridad.
Formatos comunes:
CycloneDX (preferido en entornos modernos)
SPDX
🧰 3. Trivy
¿Qué es?
Un escáner de seguridad open-source muy completo.
¿Para qué sirve?
Detecta vulnerabilidades en:
Imágenes Docker
Dependencias de proyectos Java (pom.xml)
Archivos de configuración (Dockerfile, Kubernetes YAML)
Archivos SBOM
Complemento natural a SBOM y Cosign.
🔒 4. SLSA (Supply-chain Levels for Software Artifacts)
¿Qué es?
Un estándar de Google y otros para asegurar la cadena de suministro con niveles crecientes de garantías.
SLSA 1 a 4, de menor a mayor confianza:
Código versionado.
Build automatizado (no manual).
Build reproducible.
Build hermético y verificado.
¿Por qué importa?
Porque dice “puedes confiar en este binario porque puedo probar de dónde viene”.
🧪 5. Reproducible Builds
¿Qué es?
Técnica que asegura que compilar el mismo código dos veces genera exactamente el mismo binario.
¿Por qué importa?
Garantiza que nadie ha inyectado código malicioso durante el build.
🔐 ¿Qué haremos exactamente?

En tu Jenkinsfile y proceso de CI:

Construiremos el JAR de forma reproducible (sin timestamps ni valores dinámicos).
Generaremos un SBOM con CycloneDX.
Escanearemos todo (imagen y dependencias) con Trivy.
Firmaremos la imagen y opcionalmente el .jar con Cosign.
Emitiremos una firma SLSA Provenance (con GitHub Actions o Jenkins + Rekor).

Veremos un un flujo completo con esto en Jenkins, GitHub Actions o ambos? 
¿Y también generamos las claves Cosign y configuramos el registry GHCR

## 🚀 Estado de Implementación Práctica

### Proyecto de Referencia: zero-trust-spring-boot

Este repositorio incluye una **implementación práctica** que demuestra los conceptos teóricos explicados arriba:

#### ✅ Ya Implementado
- **HashiCorp Vault integrado** con Spring Cloud Vault
- **Gestión centralizada de secretos** (JWT, DB credentials)
- **Bootstrap context** para carga temprana de configuración
- **Docker Compose** con servicios endurecidos
- **Multi-perfil** (dev/test/prod) con diferentes niveles de seguridad

#### 🔄 En Desarrollo Activo
- **Vault modo producción** (TLS, AppRole, policies)
- **Tokens de corta duración** con renovación automática
- **ABAC implementation** (Attribute-Based Access Control)
- **Contenedores hardened** con seccomp/AppArmor

[📖 Ver implementación detallada](./zero-trust-spring-boot/README.md)

(Nos quedamos aquí...)

Según Claude,...
Fortalezas del documento
Enfoque práctico y realista: El documento no se queda en teoría, sino que proporciona implementaciones concretas con código, configuraciones Docker, Kubernetes, y ejemplos de Spring Boot.
Estructura progresiva: Comienza con conceptos fundamentales y avanza hacia implementaciones técnicas específicas, lo que facilita la comprensión.
Cobertura integral: Abarca desde principios arquitectónicos hasta detalles de implementación en frontend, backend, contenedores y CI/CD.
Enfoque de defensa en profundidad: Combina múltiples capas de seguridad (seccomp, AppArmor, políticas de red, etc.).

Observaciones y sugerencias
1. Clarificación de términos técnicos
El documento introduce muchos conceptos avanzados (ABAC, mTLS, seccomp, etc.) que podrían beneficiarse de explicaciones más graduales para audiencias menos técnicas.
2. Ejemplos de migración
Sería valioso incluir un caso de estudio que muestre cómo migrar una aplicación tradicional (con sesiones de cookies) hacia el modelo Zero Trust propuesto.
3. Consideraciones de rendimiento
El documento podría abordar el impacto en rendimiento de implementar todas estas medidas de seguridad y cómo optimizarlas.
4. Gestión de secretos
Aunque menciona tokens y revocación, podría profundizar más en la gestión segura de secretos (usando HashiCorp Vault, AWS Secrets Manager, etc.).

Aspectos destacables
Rechazo de suposiciones peligrosas: Correctamente identifica y rechaza prácticas comunes pero inseguras (localStorage para tokens, confianza en redes internas, etc.).
Implementaciones concretas: Los ejemplos de Docker, Kubernetes y seccomp son muy valiosos para la implementación práctica.
Énfasis en auditoría: Reconoce la importancia de la trazabilidad y monitoreo continuo.

Por ejemplo, podrías estar interesado en:

Estrategias de migración gradual hacia Zero Trust
Implementación específica de ABAC en Spring Security
Configuración de monitoreo y alertas para arquitecturas Zero Trust
Integración con Identity Providers específicos

El documento es una excelente base para implementar Zero Trust en entornos Java modernos.

## 🎯 Lecciones Aprendidas - Sesión 06/06/2025

### ✅ Problemas Críticos Resueltos

#### 🔧 **Spring Cloud Vault en Tests**
**Problema:** Spring Cloud Vault intentaba conectarse durante la ejecución de tests, causando fallos.
```yaml
# ❌ Error: Failed to load ApplicationContext - Vault connection required
# ✅ Solución: Deshabilitación explícita por perfil
spring:
  config:
    activate:
      on-profile: test
  cloud:
    vault:
      enabled: false
    bootstrap:
      enabled: false
    config:
      enabled: false
```

#### 🔒 **Configuración de Seguridad Flexible**
**Implementación:** Property-driven security configuration
```java
// SecurityConfig.java - Approach unificado
@Configuration
@EnableWebSecurity
@ConfigurationProperties(prefix = "app.security")
public class SecurityConfig {
    private boolean requireAuthForHealthEndpoints = false;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (requireAuthForHealthEndpoints) {
            // Configuración para tests de seguridad
            return http.authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/**").authenticated())
                .httpBasic(httpBasic -> {})
                .build();
        } else {
            // Configuración por defecto (desarrollo/producción)
            return http.authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/health", "/api/info").permitAll())
                .build();
        }
    }
}
```

#### 🆕 **Spring Security 6.1+ Syntax Migration**
**Actualización:** Eliminación de warnings de deprecación
```java
// ❌ Sintaxis deprecada
.headers(headers -> headers
    .frameOptions().sameOrigin()
    .contentSecurityPolicy("default-src 'self'"))

// ✅ Sintaxis moderna Spring Security 6.1+
.headers(headers -> headers
    .frameOptions(frame -> frame.sameOrigin())
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")))
```

### 🧪 **Estrategia de Testing Robusta**

#### **Perfiles de Testing Implementados**
```yaml
# application.yml - Configuración por perfiles

---
# PERFIL "test" - Tests unitarios normales  
spring:
  config:
    activate:
      on-profile: test
  cloud:
    vault:
      enabled: false
app:
  security:
    require-auth-for-health-endpoints: false  # Endpoints públicos

---
# PERFIL "test-security" - Tests de seguridad
spring:
  config:
    activate:
      on-profile: test-security
  cloud:
    vault:
      enabled: false
  security:
    user:
      name: testuser
      password: testpass
      roles: USER
app:
  security:
    require-auth-for-health-endpoints: true  # Endpoints protegidos
```

#### **Configuración de Tests Validada**
```java
// ✅ Tests unitarios normales
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.vault.enabled=false"
})

// ✅ Tests de seguridad específicos  
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-security")
@TestPropertySource(properties = {
    "spring.cloud.vault.enabled=false"
    // app.security.require-auth-for-health-endpoints=true ya en perfil
})
```

#### **Ejemplo de Test de Seguridad Robusto**
```java
@Test
void healthEndpointShouldRequireAuthentication() throws Exception {
    // ✅ Sin auth → 401 Unauthorized
    mockMvc.perform(get("/api/health"))
            .andExpect(status().isUnauthorized());
}

@Test
void healthEndpointShouldReturnOkWithBasicAuth() throws Exception {
    // ✅ Con auth → 200 OK
    mockMvc.perform(get("/api/health")
            .with(httpBasic("testuser", "testpass")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
}
```

### 📊 **Configuración de Endpoints Dinámicos**
| Endpoint | Público (default) | Con Property=true | Configurable Via |
|----------|-------------------|-------------------|------------------|
| `/api/health` | ✅ | 🔒 | `app.security.require-auth-for-health-endpoints` |
| `/api/info` | ✅ | 🔒 | `app.security.require-auth-for-health-endpoints` |
| `/actuator/**` | ✅ | ✅ | No configurable |

---

## 🚧 Estado Actualizado - Fase 1 Completada (06/06/2025)

### ✅ **Completado en esta Sesión**
- [x] **Configuración de seguridad flexible** - Properties-driven con Spring Security 6.1+
- [x] **Tests de seguridad completos** - Suite robusta con perfiles `test` y `test-security`
- [x] **Spring Security modernizado** - Sintaxis 6.1+ sin warnings de deprecación
- [x] **Test isolation** - Vault deshabilitado automáticamente en tests
- [x] **Properties-driven security** - Configuración dinámica sin múltiples `@Profile`
- [x] **Estrategia de testing definida** - Separación clara entre unit tests y security tests

### 🔄 **Próxima Sesión - Prioridades**
- [ ] **TokenService completo** con validación JWT
- [ ] **Endpoints de autenticación** (/auth/login, /auth/refresh, /auth/validate)
- [ ] **Middleware JWT** para requests autenticados
- [ ] **Rotación automática de tokens** desde Vault

---

## 🔍 Troubleshooting - Nuevas Soluciones

### **Tests fallan con Vault (✅ RESUELTO)**
```bash
# ❌ Error: Failed to load ApplicationContext
# java.lang.IllegalStateException: Cannot create authentication mechanism for TOKEN

# ✅ Solución implementada:
# 1. Verificar perfil correcto según tipo de test
# Tests normales: @ActiveProfiles("test") 
# Tests de seguridad: @ActiveProfiles("test-security")

# 2. Asegurar properties de deshabilitación de Vault
@TestPropertySource(properties = {
    "spring.cloud.vault.enabled=false",
    "spring.cloud.bootstrap.enabled=false", 
    "spring.cloud.config.enabled=false"
})

# 3. Verificar credenciales de test funcionan
curl -u testuser:testpass http://localhost:8080/api/health

# 4. Ejecutar tests específicos
./mvnw test -Dtest=*SecurityTest -Dspring.profiles.active=test-security
```

### **Spring Security Deprecation Warnings (✅ RESUELTO)**
```java
// ❌ Warning: 'frameOptions()' is deprecated since version 6.1
// ❌ Warning: 'contentSecurityPolicy(String)' is deprecated since version 6.1

// ✅ Solución: Actualizado a sintaxis moderna
.headers(headers -> headers
    .frameOptions(frame -> frame.sameOrigin())
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'")))
```

### **Configuración de Seguridad No Aplica**
```bash
# ❌ Problema: Property app.security.require-auth-for-health-endpoints no funciona
# ✅ Verificar:

# 1. SecurityConfig tiene @ConfigurationProperties
@ConfigurationProperties(prefix = "app.security")
public class SecurityConfig {

# 2. Property está en application.yml del perfil correcto
app:
  security:
    require-auth-for-health-endpoints: true

# 3. Verificar valor aplicado en runtime
curl -v http://localhost:8080/api/health
# Si devuelve 401 → Property aplicada correctamente
# Si devuelve 200 → Verificar configuración
```

---

## 🧪 Comandos de Testing Actualizados

```bash
# Tests unitarios sin seguridad (endpoints públicos)
./mvnw test -Dspring.profiles.active=test

# Tests de seguridad con autenticación (endpoints protegidos)  
./mvnw test -Dtest=*SecurityTest -Dspring.profiles.active=test-security

# Verificar configuración de security aplicada
./mvnw test -Dtest=HealthControllerSecurityTest -Dlogging.level.org.springframework.security=DEBUG

# Tests completos con diferentes perfiles
./mvnw verify -Dspring.profiles.active=test,test-security

# Validar que Vault está deshabilitado en tests
./mvnw test -Dspring.profiles.active=test -Dlogging.level.org.springframework.cloud.vault=DEBUG
```
---

## 🧪 Evaluación de Tests mediante Mutation Testing

### 🎯 Objetivo

En entornos Zero Trust, **la confianza en el sistema no debe derivar de suposiciones** como “los tests están verdes” o “tenemos 90% de cobertura”. La verdadera seguridad comienza cuando se puede verificar que los tests:

* Cubren casos relevantes.
* Detectan fallos lógicos.
* Protegen contra regresiones silenciosas.

Para ello, empleamos **mutation testing**, una técnica diseñada no para validar el código de producción, sino **la calidad de nuestros tests**.

---

### 🧬 ¿Qué es Mutation Testing?

Mutation testing consiste en **alterar automáticamente el código de producción** (introduciendo pequeños errores conocidos como *mutantes*) y comprobar si los tests son capaces de detectarlos.

#### Ejemplo:

```java
// Código original
if (a > b) return "mayor";

// Mutación automática
if (a < b) return "mayor";
```

Si los tests **no fallan tras este cambio**, significa que **el mutante ha sobrevivido** y el test **no valida correctamente la lógica**.

---

### 🚫 ¿Qué no es?

* No evalúa la calidad del diseño del código.
* No evalúa seguridad, escalabilidad o patrones de arquitectura.
* No reemplaza a SonarQube, PMD, linters o herramientas de análisis estático.

---

### ✅ ¿Qué detecta?

* Tests que **no hacen aserciones**.
* Tests que **ejecutan código sin verificar resultados**.
* Tests con **entradas irrelevantes** que no disparan condiciones lógicas clave.
* Tests que pasan por el código pero **no lo validan de forma significativa**.

---

### 🧠 Por qué lo usamos

En Zero Trust, **nada debe darse por supuesto**. Si un test no es capaz de detectar que una condición ha cambiado de `>` a `<`, entonces no tenemos garantía de que el sistema responderá correctamente ante modificaciones o ataques.

Mutation testing **revela tests inútiles** que habrían pasado cualquier cambio lógico. Es una defensa activa contra la falsa confianza.

---

### 🛠️ Herramienta recomendada: **PITEST**

**Java**:

* [https://pitest.org/](https://pitest.org/)
* Se integra fácilmente con Maven o Gradle.
* Informa de mutaciones sobrevivientes.
* Revela debilidades reales en los tests, incluso con cobertura alta.

```xml
<!-- Ejemplo básico para Maven -->
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>1.15.2</version>
  <configuration>
    <targetClasses>
      <param>com.tuservicio.*</param>
    </targetClasses>
  </configuration>
</plugin>
```

Ejecutar:

```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

---

### 🧩 Complementos recomendados

| Objetivo                               | Herramienta                  |
| -------------------------------------- | ---------------------------- |
| **Calidad del código**                 | SonarQube, PMD               |
| **Validación estructural**             | ArchUnit                     |
| **Seguridad en dependencias**          | OWASP Dependency-Check, Snyk |
| **Resiliencia en tiempo de ejecución** | Chaos Engineering, Fuzzing   |

---

### 📌 Recomendaciones prácticas

* Aplica mutation testing como parte del pipeline de CI/CD.
* Trata cada mutante sobreviviente como un *test smell*.
* No te obsesiones con alcanzar el 100%: apunta a matar los mutantes más críticos primero.
* Documenta y refuerza los tests cuando un mutante sobrevive.


---

## 🧠 Guía rápida: ¿Ignorar o reforzar un mutante sobreviviente?

Mutation testing con PITEST nos ayuda a verificar si nuestros tests detectan errores reales. Pero **no todos los mutantes sobrevivientes son igual de importantes**.

Usa esta tabla para tomar decisiones informadas:

---

### ✅ **Refuerza el test si...**

| Escenario                                                                                                   | Acción recomendada                                                                                                |
| ----------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| El mutante cambia la **lógica de negocio principal** (`+ → -`, `if → false`, `return x → return 0`).        | Añade un test con inputs que disparen ese flujo. Asegúrate de validar el resultado con aserciones significativas. |
| El código mutado está en un **método con reglas de negocio** (controladores, servicios, lógica de negocio). | Asegura cobertura funcional y valida los efectos colaterales (persistencia, excepciones, etc.).                   |
| El test actual **pasa por el código mutado pero no hace ninguna validación**.                               | Añade aserciones o refactoriza el test para comprobar resultados.                                                 |
| El mutante sobrevive en una **condición importante** (`a > b → a <= b`).                                    | Añade casos límite (`a == b`, `a < b`, `a > b`) para cubrir todos los caminos.                                    |

---

### ⚪️ **Puedes ignorarlo si...** (pero documenta la razón)

| Escenario                                                                     | Justificación aceptable                                                        |
| ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| El código mutado está en un **getter, setter o constructor trivial**.         | No aporta lógica significativa; opcionalmente exclúyelo con `excludedMethods`. |
| La mutación ocurre en una **clase DTO o POJO sin lógica**.                    | Se puede ignorar o excluir por configuración.                                  |
| El código pertenece a una **dependencia externa, librería o proxy**.          | Exclúyelo por clase o paquete. No es código propio.                            |
| La mutación causa **errores internos (timeouts, excepciones no relevantes)**. | Revisa el test, pero si no tiene impacto funcional, puede ignorarse.           |
| El código mutado está en una **función de logging, métricas o trazas**.       | No afecta la lógica del negocio.                                               |

---

### 🛑 Casos donde **no debes ignorar el mutante**

| Caso                                                                                | Por qué importa                                             |
| ----------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| El mutante cambia un `return true` a `return false` en una validación.              | Puede permitir bypass de seguridad o decisiones erróneas.   |
| El test pasa sin aserciones.                                                        | El test **no prueba nada**, aunque pase. Es un test muerto. |
| Hay solo un test para un método con múltiples flujos (`if`, `else`, `throw`, etc.). | Falta cobertura de ramas. Refuerza con más casos.           |

---

### 🛠️ Recomendaciones adicionales

* Usa `excludedClasses` y `excludedMethods` con criterio: **no abuses de ellos para silenciar problemas reales**.
* Mantén los informes de PITEST como parte del pipeline, pero revisa manualmente las mutaciones sobrevivientes clave.
* Documenta en cada PR por qué un mutante sobreviviente es aceptable si decides ignorarlo.

---

### 📋 Ejemplo de comentario justificando un mutante ignorado

```markdown
🔶 PITEST mutante sobreviviente en `UserDto.getEmail()`
→ Ignorado porque es un getter sin lógica. No tiene sentido probar cambios como `return null` o `return ""`.

Configurado en `<excludedMethods>` como `get*`
```

---


