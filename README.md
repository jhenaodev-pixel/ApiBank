# ApiBank + ONDA

Sistema bancario de demostración con backend Spring Boot, base de datos MySQL,
inversión en criptomonedas/acciones y un frontend con la identidad visual **ONDA**.

```
ApiBank/
├─ src/                 Backend Spring Boot (API REST)
├─ Dockerfile           Imagen multi-stage del backend
├─ docker-compose.yml   MySQL + backend + frontend (desarrollo)
├─ docker-compose.prod.yml  Stack con Caddy y HTTPS (producción)
├─ Caddyfile            Proxy HTTPS automático
├─ DEPLOY.md            Guía de despliegue permanente gratuito
└─ INVERSIONES.md       Documentación de mercado, inversiones y Docker

Documents/
└─ onda-frontend/       Frontend ONDA (React + Vite + nginx)
```

## Inicio rápido con Docker

Requisitos: Docker Desktop en ejecución.

```powershell
cd C:\Users\juanp\Documents\ApiBank
docker compose up -d --build
```

| Servicio | URL | Puerto host |
|----------|-----|-------------|
| Frontend ONDA | http://localhost:3000 | `3000` |
| Backend API | http://localhost:8080 | `8080` |
| Swagger UI | http://localhost:8080/swagger-ui.html | `8080` |
| MySQL | `localhost:3307` | `3307` |

Los contenedores usan `restart: unless-stopped`: se encienden solos al abrir Docker
Desktop y siempre en los mismos puertos.

### Clave de Finnhub (acciones)

Edite `.env` en la raíz:

```dotenv
FINNHUB_API_KEY=su_api_key
```

Luego recree el backend:

```powershell
docker compose up -d --force-recreate backend
```

Sin clave, las criptomonedas (Binance) siguen funcionando y la interfaz muestra que
las acciones no están disponibles.

## Funcionalidades

- Registro e inicio de sesión con JWT.
- Cuentas de ahorro/corriente, depósitos, retiros y transferencias.
- Historial de movimientos filtrable.
- Mercado en vivo: cripto desde Binance y acciones desde Finnhub.
- Compra/venta de activos, portafolio valorizado y ganancia/pérdida.
- Panel de analíticas: asignación, flujo neto mensual y ranking de posiciones.
- Diseño responsive con navegación tipo píldora y tarjetas redondeadas.

## Publicar el frontend en Vercel

Vercel solo puede servir el frontend (Vite/estático). El backend Spring Boot + MySQL
debe seguir ejecutándose aparte, por ejemplo en Docker sobre una VM o en su equipo.

1. Exponga el backend por HTTPS (Cloudflare Tunnel para una demo):
   `cloudflared tunnel --url http://localhost:8080`.
2. En Vercel, importe el repositorio del frontend (`ONDA`) y defina la variable de
   entorno de build `VITE_API_URL` con la URL pública del backend.
3. El backend acepta ese origen gracias a CORS configurable:

```dotenv
# ApiBank/.env o variable del contenedor
CORS_ALLOWED_ORIGINS=https://su-frontend.vercel.app,http://localhost:3000
```

Use `*` solo para demos. El frontend llama directamente a `VITE_API_URL`, sin proxy
serverless. Si `VITE_API_URL` queda vacío, usa el mismo origen (nginx/Vite).

## URL permanente y HTTPS

Para una URL fija sin depender de tu equipo, siga [`DEPLOY.md`](./DEPLOY.md):
VM Always Free de Oracle Cloud + `docker-compose.prod.yml` + Caddy con
certificado de Let's Encrypt y un subdominio gratuito de DuckDNS.

```bash
cp .env.prod.example .env
docker compose -f docker-compose.prod.yml up -d --build
```

## Desarrollo local

Backend (requiere MySQL en `localhost:49703` o ajustar `DB_URL`):

```powershell
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd C:\Users\juanp\Documents\onda-frontend
npm install
npm run dev      # http://localhost:5173, proxy /api -> http://localhost:8080
```

## Documentación

Consulte [`INVERSIONES.md`](./INVERSIONES.md) para el detalle de los endpoints de
mercado/inversión, la configuración de Finnhub, los puertos fijos de Docker y las
pantallas del frontend.
