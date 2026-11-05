# Módulo de inversiones — ApiBank

Este documento describe el apartado de inversión agregado a **ApiBank**: consulta de
mercado, compra/venta de criptomonedas y acciones, y portafolio con rentabilidad.

- **Criptomonedas:** API pública de [Binance](https://developers.binance.com/docs/binance-spot-api-docs) (no requiere clave).
- **Acciones:** API de [Finnhub](https://finnhub.io/docs/api) (requiere clave gratuita).

---

## 1. Configuración

### 1.1 Finnhub

1. Cree una cuenta gratuita en <https://finnhub.io/register> y copie su API key.
2. Defina la variable de entorno antes de iniciar la aplicación.

**PowerShell (Windows):**

```powershell
$env:FINNHUB_API_KEY = "su_api_key"
.\mvnw.cmd spring-boot:run
```

**Bash / Linux / macOS:**

```bash
export FINNHUB_API_KEY="su_api_key"
./mvnw spring-boot:run
```

También puede escribir la clave directamente en `src/main/resources/application.yaml`
en `app.market-data.finnhub.api-key` (no se recomienda subirla al repositorio).

### 1.2 Variables relevantes

```yaml
app:
  market-data:
    binance:
      base-url: https://api.binance.com
      default-symbols: [BTCUSDT, ETHUSDT, ...]   # lista del endpoint /market/crypto
    finnhub:
      base-url: https://finnhub.io/api/v1
      api-key: ${FINNHUB_API_KEY:}
```

Si no hay API key de Finnhub, **las criptomonedas siguen funcionando** y los endpoints
de acciones responden `502 Bad Gateway` con un mensaje explicativo.

La base de datos no requiere cambios manuales: `spring.jpa.hibernate.ddl-auto=update`
crea las tablas nuevas (`investment_positions` e `investment_trades`).

---

## 2. Endpoints de mercado (públicos, sin JWT)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/market/crypto` | Precios de las criptomonedas configuradas |
| GET | `/api/v1/market/crypto/{symbol}` | Precio de una criptomoneda (`BTC` o `BTCUSDT`) |
| GET | `/api/v1/market/crypto/{symbol}/history?interval=1d&limit=30` | Velas históricas |
| GET | `/api/v1/market/stocks/search?q=apple&limit=10` | Busca acciones en Finnhub |
| GET | `/api/v1/market/stocks/{symbol}` | Cotización de una acción (`AAPL`) |

### Ejemplos

```bash
# Criptomoneda (BTC se interpreta como BTCUSDT)
curl "http://localhost:8080/api/v1/market/crypto/BTC"

# Histórico diario, últimas 30 velas
curl "http://localhost:8080/api/v1/market/crypto/ETHUSDT/history?interval=1d&limit=30"

# Buscar acciones
curl "http://localhost:8080/api/v1/market/stocks/search?q=apple"

# Cotización de Apple
curl "http://localhost:8080/api/v1/market/stocks/AAPL"
```

Respuesta de `GET /api/v1/market/crypto/BTC`:

```json
{
  "symbol": "BTCUSDT",
  "assetType": "CRYPTO",
  "price": 77013.32,
  "change": -377.48,
  "changePercent": -0.488,
  "openPrice": 77390.80,
  "highPrice": 78063.30,
  "lowPrice": 76388.88,
  "previousClose": 77390.80,
  "volume": 12853.51,
  "quoteVolume": 992098307.06,
  "currency": "USDT",
  "quotedAt": "2026-11-04T01:18:44"
}
```

---

## 3. Endpoints de inversión (requieren JWT)

Envíe el token en la cabecera: `Authorization: Bearer <token>`.

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/v1/investments/buy` | Compra un activo con dinero de una cuenta |
| POST | `/api/v1/investments/sell` | Vende unidades y abona el dinero en la cuenta |
| GET | `/api/v1/investments/portfolio` | Portafolio valorizado y rentabilidad |
| GET | `/api/v1/investments/positions` | Posiciones abiertas |
| GET | `/api/v1/investments/history` | Historial de operaciones (`?symbol=...&assetType=...`) |

### 3.1 Comprar

Se puede indicar **`quantity`** (unidades del activo) **o** **`amount`** (dinero a
invertir), pero no ambos.

```bash
curl -X POST "http://localhost:8080/api/v1/investments/buy" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "100085497260041",
    "symbol": "BTC",
    "assetType": "CRYPTO",
    "amount": 100.00,
    "description": "Mi primera compra"
  }'
```

Comprar acciones (la cantidad se interpreta en unidades de la acción):

```bash
curl -X POST "http://localhost:8080/api/v1/investments/buy" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "100085497260041",
    "symbol": "AAPL",
    "assetType": "STOCK",
    "quantity": 2
  }'
```

Respuesta (`201 Created`):

```json
{
  "id": 1,
  "reference": "99c0eb0f-75f7-41b6-8b1e-56900f5ec7a9",
  "symbol": "ETHUSDT",
  "assetType": "CRYPTO",
  "tradeType": "BUY",
  "quantity": 0.01104372,
  "price": 2263.73,
  "totalAmount": 25.00,
  "realizedProfit": null,
  "accountNumber": "100085497260041",
  "accountBalance": 975.00,
  "description": null,
  "timestamp": "2026-11-03T20:19:50.962746"
}
```

### 3.2 Vender

Para vender solo se usa **`quantity`** (unidades). El campo `amount` no aplica.

```bash
curl -X POST "http://localhost:8080/api/v1/investments/sell" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "100085497260041",
    "symbol": "BTCUSDT",
    "assetType": "CRYPTO",
    "quantity": 0.001
  }'
```

La respuesta incluye `realizedProfit`: la ganancia o pérdida realizada en esa venta
calculada contra el precio promedio de compra.

### 3.3 Portafolio

```bash
curl "http://localhost:8080/api/v1/investments/portfolio" \
  -H "Authorization: Bearer $TOKEN"
```

```json
{
  "totalInvested": 100.00,
  "totalCurrentValue": 101.35,
  "totalProfitLoss": 1.35,
  "totalProfitLossPercent": 1.35,
  "fullyValued": true,
  "valuedAt": "2026-11-04T01:20:15",
  "positions": [
    {
      "id": 1,
      "symbol": "BTCUSDT",
      "assetType": "CRYPTO",
      "quantity": 0.00129861,
      "averagePrice": 77013.32,
      "investedAmount": 100.00,
      "currentPrice": 77500.00,
      "currentValue": 100.64,
      "profitLoss": 0.64,
      "profitLossPercent": 0.64
    }
  ]
}
```

- `totalInvested`: dinero invertido en las posiciones abiertas.
- `totalCurrentValue`: valor de mercado actual.
- `totalProfitLoss`: ganancia/pérdida no realizada.
- `fullyValued`: `false` si algún activo no pudo valorizarse (por ejemplo, Finnhub
  sin clave o límite de peticiones). En ese caso la rentabilidad se calcula solo
  con las posiciones que sí tienen precio.

### 3.4 Historial

```bash
# Todas las operaciones
curl "http://localhost:8080/api/v1/investments/history" -H "Authorization: Bearer $TOKEN"

# Filtrar por activo (para cripto se recomienda enviar assetType)
curl "http://localhost:8080/api/v1/investments/history?symbol=BTC&assetType=CRYPTO" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 4. Cómo funciona internamente

1. Se normaliza el símbolo (`BTC` → `BTCUSDT`, `aapl` → `AAPL`).
2. Se consulta el precio actual en Binance o Finnhub.
3. Se valida la cuenta (debe pertenecer al usuario) y el saldo.
4. Se abre una transacción corta con bloqueo pesimista de la cuenta y de la
   posición, se debita/abona el dinero y se actualiza la posición.
5. Se registran dos movimientos:
   - `investment_trades`: la operación de inversión (cantidad, precio, total, ganancia realizada).
   - `transactions`: el movimiento bancario con tipo `INVESTMENT_BUY` o
     `INVESTMENT_SELL`, para que aparezca también en el historial de la cuenta.

El precio se consulta **antes** de abrir la transacción de base de datos, para no
mantener bloqueos mientras se espera a un proveedor externo.

### Archivos principales

```
client/BinanceClient.java                     Cliente HTTP de Binance
client/FinnhubClient.java                     Cliente HTTP de Finnhub
config/MarketDataProperties.java              Propiedades de las APIs
controller/MarketController.java              Consulta de mercado (público)
controller/InvestmentController.java          Compra, venta y portafolio
entity/InvestmentPosition.java                Posición abierta por usuario/activo
entity/InvestmentTrade.java                   Historial de operaciones
repository/InvestmentPositionRepository.java  Consultas y bloqueo de posiciones
service/impl/MarketDataServiceImpl.java       Normalización y mapeo de precios
service/impl/InvestmentServiceImpl.java       Reglas de compra/venta y portafolio
service/impl/InvestmentTradeExecutor.java     Ejecución transaccional y bloqueos
```

---

## 5. Notas y limitaciones

- **Moneda:** las criptomonedas se cotizan en `USDT` y las acciones en `USD`. El
  proyecto no realiza conversión de divisas; el monto se descuenta de la cuenta
  como si estuviera en la misma unidad.
- **Símbolos:** en cripto se puede usar `BTC` o el par completo `BTCUSDT`. En
  acciones se usa el ticker de Finnhub (`AAPL`, `MSFT`, `TSLA`, ...).
- **Redondeo:** el total de la operación se redondea a 2 decimales; la cantidad
  se guarda con 8 decimales (cripto) y el precio con hasta 10.
- **Límites gratuitos:** Finnhub limita las peticiones por minuto. Si se alcanza
  el límite, el endpoint responde `502` con un mensaje claro. Binance también
  puede limitar peticiones si se consulta de forma masiva.
- **Venta total:** al vender toda la posición, esta se elimina del portafolio,
  pero la operación permanece en el historial.
- **Swagger:** con la aplicación en marcha, la documentación interactiva está en
  <http://localhost:8080/swagger-ui.html>.

---

## 6. Ejecución con Docker (puertos fijos)

Todo el sistema (MySQL + backend + frontend) se levanta con **Docker Compose** desde
la raíz del proyecto. Los puertos en el host son siempre los mismos, así que la API
siempre queda en `http://localhost:8080` y la interfaz en `http://localhost:3000`.

### 6.1 Requisitos

- Docker Desktop instalado y en ejecución.
- Puerto `8080`, `3000` y `3307` libres en el host.

### 6.2 Levantar

```powershell
cd C:\Users\juanp\Documents\ApiBank
docker compose up -d --build
```

| Servicio | Contenedor | Host → Contenedor | URL |
|----------|-----------|-------------------|-----|
| Backend (Spring Boot) | `apibank-backend` | `8080 → 8080` | http://localhost:8080 |
| Frontend (nginx) | `apibank-frontend` | `3000 → 80` | http://localhost:3000 |
| MySQL 8.4 | `apibank-mysql` | `3307 → 3306` | `localhost:3307` |

- El backend espera a que MySQL pase su *healthcheck* (`depends_on.condition: service_healthy`).
- Los tres servicios usan `restart: unless-stopped`, por lo que **vuelven a arrancar
  automáticamente** con Docker Desktop o ante un reinicio del equipo.
- Los datos de MySQL viven en el volumen `apibank_mysql_data` y sobreviven a
  `docker compose down` (use `docker compose down -v` para borrarlos).

### 6.3 Clave de Finnhub

El archivo `.env` de la raíz alimenta la variable `FINNHUB_API_KEY` que consume
`docker-compose.yml`:

```dotenv
FINNHUB_API_KEY=su_api_key
```

Sin clave, las criptomonedas funcionan con normalidad y las acciones muestran un
aviso en la interfaz (`502` explicativo desde la API). Después de cambiar el `.env`:

```powershell
docker compose up -d --force-recreate backend
```

### 6.4 Comandos útiles

```powershell
docker compose ps                       # estado de los contenedores
docker compose logs -f backend          # logs del backend en vivo
docker compose logs -f frontend         # logs de nginx
docker compose restart backend          # reiniciar solo el backend
docker compose down                     # detener (conserva datos)
docker compose up -d --build            # reconstruir y levantar
```

### 6.5 Variables del backend

`docker-compose.yml` define la conexión a MySQL dentro de la red de Compose:

```yaml
DB_URL: jdbc:mysql://mysql:3306/db_bank?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME: root
DB_PASSWORD: root
```

`application.yaml` las lee con valores por defecto, de modo que el mismo JAR también
funciona sin Docker apuntando a `localhost:49703`.

---

## 7. Frontend ONDA

El frontend vive en `C:\Users\juanp\Documents\onda-frontend` (carpeta hermana de
`ApiBank`) y consume esta misma API.

- **Stack:** React 19 + TypeScript + Vite 8, iconos `lucide-react`, servido por nginx.
- **Puerto fijo:** `http://localhost:3000`.
- **Misma origen, sin CORS:** nginx publica `/api/` hacia `http://backend:8080`
  (ver `../onda-frontend/nginx.conf`), así que el navegador llama a `http://localhost:3000/api/...`.
- **Diseño:** identidad ONDA (lavanda/azul claro, navegación tipo píldora, tarjetas
  redondeadas de 28 px, degradados y tipografías Manrope/Sora).

### Pantallas

| Ruta | Contenido |
|------|-----------|
| `/` | Resumen: patrimonio, cuentas, flujo, últimos movimientos y cripto destacadas |
| `/operaciones` | Depósitos, retiros, transferencias e historial filtrable |
| `/inversiones` | Portafolio, compra/venta de cripto (Binance) y acciones (Finnhub) |
| `/analiticas` | Flujo neto mensual, asignación y ranking de posiciones |

### Desarrollo local (sin Docker)

```powershell
cd C:\Users\juanp\Documents\onda-frontend
npm install
npm run dev          # http://localhost:5173 (Vite redirige /api a localhost:8080)
```

Para apuntar a otra API, cree `onda-frontend/.env`:

```dotenv
VITE_API_URL=http://localhost:8080
```

Déjelo vacío (o sin archivo) para usar el mismo origen y el proxy de nginx/Vite.

### Compilar sin Docker

```powershell
npm run build        # genera dist/
```

### Reconstruir el contenedor del frontend

```powershell
cd C:\Users\juanp\Documents\ApiBank
docker compose build frontend
docker compose up -d frontend
```
