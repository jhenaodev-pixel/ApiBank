# ApiBank + ONDA

Sistema bancario de demostración con backend Spring Boot, base de datos MySQL,
inversión en criptomonedas/acciones y un frontend con la identidad visual **ONDA**.

```
ApiBank/
├─ src/                 Backend Spring Boot (API REST)
├─ Dockerfile           Imagen multi-stage del backend
├─ docker-compose.yml   MySQL + backend + frontend
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
