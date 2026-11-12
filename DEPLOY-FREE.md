# Demo pública sin VM ni dominio (100% gratis)

Ruta recomendada si no tienes servidor ni dominio: cada pieza vive en un servicio
gratuito y obtienes URLs HTTPS del propio proveedor.

```
Navegador
   │  https://onda-xxxx.vercel.app        (frontend, CDN)
   ▼
Vercel  ──fetch──►  https://onda-api.onrender.com   (backend Spring Boot, Docker)
                              │
                              ▼
                    MySQL gratuito (TiDB Cloud Starter / Aiven)
```

**Coste:** 0 USD. **Dominio:** no hace falta. **Tarjeta:** no hace falta para el plan
gratuito de Render ni para el de TiDB.

## Qué esperar de este montaje

Es una demo pública, no producción:

- El backend **se duerme tras 15 minutos sin tráfico**. La primera visita después
  tarda ~1 minuto en despertar (Render muestra su propia pantalla de carga).
- El plan free de Render da **0.1 CPU y 512 MB**: suficiente para demostrar, lento
  para uso real.
- La base gratuita tiene **límites de almacenamiento y de operaciones**.
- Las URLs son del proveedor (`*.onrender.com`, `*.vercel.app`).

Si más adelante quieres algo estable 24/7 y con dominio propio, la ruta es
[`DEPLOY.md`](./DEPLOY.md) (Oracle Cloud + Caddy).

---

## Paso 1. Base de datos MySQL gratuita

Render solo regala **Postgres**, y este proyecto usa **MySQL**. Usa un MySQL
gestionado gratuito:

### Opción A: TiDB Cloud Starter (recomendada)

1. Crea cuenta en [tidbcloud.com](https://tidbcloud.com) (login con GitHub, Google
   o email). No pide tarjeta.
2. En **My TiDB** pulsa **Create Resource** → plan **Starter** → *Compatibility
   Mode*: **MySQL Compatible**. Ponle nombre (`onda`), elige la región AWS más
   cercana al backend de Render (si Render está en *oregon*, usa `us-west-2`; si
   no aparece, `us-east-1` funciona igual) y deja el **spending limit en 0** para
   que siga siendo gratis. Pulsa **Create** (~30 s).
3. Abre la instancia → **Connect**:
   - *Connection Type*: **Public**.
   - Si no hay contraseña, pulsa **Generate Password** y guárdala: **solo se
     muestra una vez**.
   - Copia *Host*, *Port* (4000), *User* y *Password*. El usuario **no** es
     `root` a secas: incluye el prefijo de la instancia, tipo `3KxAbc1234.root`.
4. En la misma ventana elige **Web SQL Shell** (o *SQL Editor*) y crea la base:

```sql
CREATE DATABASE IF NOT EXISTS db_bank;
```

5. Arma la cadena JDBC (TiDB exige TLS):

```text
jdbc:mysql://HOST:4000/db_bank?sslMode=REQUIRED&serverTimezone=UTC&characterEncoding=utf8
```

> `sslMode=REQUIRED` cifra la conexión sin exigir un certificado en el JVM.
> Si prefieres validar el certificado, usa `sslMode=VERIFY_IDENTITY`.
>
> Las instancias Starter permiten **todas las IPs por defecto**
> (`Allow_all_public_connections`), así que no hay que configurar firewall:
> Render no tiene IP de salida fija y no hace falta añadir ninguna regla.

6. Comprueba que el host responde antes de tocar Render (en Windows):

```powershell
Test-NetConnection gateway01.xxxx.prod.aws.tidbcloud.com -Port 4000
```

`TcpTestSucceeded : True` significa que hay alguien escuchando. Si sale `False`,
el host o el puerto están mal copiados y Render fallará igual con
`Connection refused`.

### Opción B: otros MySQL gratuitos (no recomendado)

El **plan gratuito de MySQL en Aiven ya está retirado** (se apaga el 21/12/2026),
así que no lo uses para algo nuevo. Alternativas y sus pegas:

| Proveedor | Pegas |
|-----------|-------|
| Aiven MySQL | ya no tiene plan gratis |
| PlanetScale | pide tarjeta |
| db4free.net | sin SLA, borran bases inactivas, TLS opcional |
| freesqldatabase.com | la base caduca a los 30 días |

TiDB Cloud Starter es la única opción gratuita, sin tarjeta y sin caducidad que
además es MySQL puro, así que el código no cambia.

---

## Paso 2. Backend en Render

1. Crea cuenta en [render.com](https://dashboard.render.com/register) con GitHub.
2. **New → Blueprint**.
3. Conecta el repositorio `jhenaodev-pixel/ApiBank` (rama `main`).
   Render lee `render.yaml` y propone el servicio `onda-api`.
4. Completa las variables que el blueprint deja vacías:

| Variable | Valor |
|----------|-------|
| `DB_URL` | `jdbc:mysql://HOST:4000/db_bank?sslMode=REQUIRED&serverTimezone=UTC` |
| `DB_USERNAME` | usuario de la base |
| `DB_PASSWORD` | contraseña de la base |
| `FINNHUB_API_KEY` | opcional, para acciones |
| `CORS_ALLOWED_ORIGINS` | se completa en el paso 4 |

`JWT_SECRET` lo genera Render automáticamente. No lo toques.

5. **Apply**. El primer build tarda 5–10 minutos (compila con Maven).
6. Cuando el estado sea **Live**, anota la URL, por ejemplo:

```text
https://onda-api.onrender.com
```

Si el nombre `onda-api` ya está ocupado, Render te pedirá otro; la URL cambiará y
deberás usarla en el paso 3.

7. Verifica:

```powershell
Invoke-RestMethod https://onda-api.onrender.com/api/v1/health
```

Debe devolver `status = UP`. El healthcheck de Render usa ese endpoint porque
no depende de la base ni de Binance/Finnhub: si dependiera de ellos, un corte del
proveedor marcaría el despliegue como fallido aunque el backend estuviera vivo.

Para comprobar el mercado (llamada externa a Binance):

```powershell
Invoke-RestMethod https://onda-api.onrender.com/api/v1/market/crypto | Select-Object -First 1
```

Debe devolver cotizaciones. La primera llamada puede tardar ~1 minuto porque el
servicio está despertando.

---

## Paso 3. Frontend en Vercel

1. Entra a [vercel.com](https://vercel.com) con GitHub.
2. **Add New → Project → Import** el repositorio `jhenaodev-pixel/ONDA`.
3. Framework: **Vite** (se detecta solo). No cambies build ni output.
4. El repositorio ya incluye `vercel.json` con un proxy de `/api/*` hacia el
   backend, así que **no hace falta ninguna variable**:

```json
{ "source": "/api/:path*", "destination": "https://apibank-9uad.onrender.com/api/:path*" }
```

Si cambias de backend, edita esa línea y haz push. También puedes omitir el
proxy y apuntar el frontend directamente con una variable de entorno:

```text
VITE_API_URL = https://apibank-9uad.onrender.com
```

Márcala para *Production*, *Preview* y *Development*, y **redespliega**: es una
variable de build, así que sin redesplegar Vercel sigue sirviendo el bundle
viejo (el JS no contendrá ninguna URL y las peticiones darán 405).

5. **Deploy**. Obtendrás algo como `https://onda-xxxx.vercel.app`.
6. Cada cambio futuro en `main` se redespliega solo.

> Si ya tenías un deployment temporal, impórtalo como proyecto para conservarlo:
> el panel de Vercel permite reclamar el deployment con el enlace `claim`.

---

## Paso 4. Cerrar CORS

Vuelve a Render → servicio `onda-api` → **Environment**:

```text
CORS_ALLOWED_ORIGINS = https://onda-xxxx.vercel.app
```

Guarda. Render reinicia el servicio automáticamente.

Reglas:

- Sin barra final y con `https://`.
- Para varios orígenes, sepáralos con coma:
  `https://onda.vercel.app,https://onda-git-main-usuario.vercel.app`
- Deja `*` solo mientras pruebas; permite que cualquier web llame a tu API.

---

## Paso 5. Probar

1. Abre `https://onda-xxxx.vercel.app`.
2. Crea una cuenta nueva (recibe $1,000 de bienvenida).
3. Verifica:
   - Resumen con saldo y movimientos.
   - `/operaciones` con la cuenta creada.
   - `/inversiones` con cripto en vivo.
   - Búsqueda de `AAPL` si configuraste Finnhub.
4. Recarga una ruta profunda (`/analiticas`) para confirmar el fallback SPA.

Usuarios existentes en la base nueva:

```text
No hay usuarios hasta que registres el primero desde la web.
```

---

## Actualizaciones

| Cambio | Qué hacer |
|--------|-----------|
| Backend | `git push` a `main` → Render reconstruye solo |
| Frontend | `git push` a `main` → Vercel reconstruye solo |
| Variables | Panel del proveedor → guardar → reinicio automático |

---

## Problemas frecuentes

| Síntoma | Causa | Solución |
|---------|-------|----------|
| Primera carga de 1 minuto | El servicio free está dormido | Es normal; espera o haz una petición previa |
| `502` o `Application failed to respond` | El backend no arrancó | Revisa **Logs** en Render |
| `Communications link failure` | `DB_URL` incorrecto o sin TLS | Usa `sslMode=REQUIRED` y verifica host/puerto |
| `Connection refused` en 0 ms al arrancar | `DB_URL` vacía → Spring usa `localhost:49703`, que dentro del contenedor no existe | Carga `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` reales en Render y reinicia |
| `Unable to determine Dialect without JDBC metadata` | Consecuencia del fallo de conexión, no la causa | Arregla la conexión; este error desaparece solo |
| `Public Key Retrieval is not allowed` | Falta TLS | Añade `sslMode=REQUIRED` a la URL |
| `/api/v1/health` responde pero `/market/crypto` falla | Binance devuelve `451` a IPs de EE. UU. | Ya se usa el espejo `data-api.binance.vision`; puedes sobreescribirlo con `BINANCE_BASE_URL` |
| `Table 'db_bank.a1_0' doesn't exist` | TiDB resuelve el alias que Hibernate pone en el `SELECT ... FOR UPDATE` (`from accounts a1_0 ... for update`) como si fuera una tabla | Ya se usa SQL nativo sin alias en `findByAccountNumberForUpdate` y `findForUpdate`. Si reaparece, busca `@Lock(PESSIMISTIC_WRITE)` y cámbialo por `FOR UPDATE` nativo |
| Los `INSERT` y las lecturas funcionan pero depositar/transferir da `500` | Ese alias solo aparece en las consultas con bloqueo | Mismo arreglo que la fila anterior |
| La base nueva está vacía | Es lo esperado: `ddl-auto: update` crea las tablas al arrancar | Regístrate desde el frontend y aparecerán usuarios/cuentas |
| Error de CORS en el navegador | Origen distinto al configurado | Copia la URL exacta de Vercel en `CORS_ALLOWED_ORIGINS` |
| `Access denied for user` | Usuario/clave de la base | Revisa `DB_USERNAME` y `DB_PASSWORD` |
| Render suspende el servicio | Se agotaron las 750 horas free del mes | Espera al siguiente mes o pasa a plan pago |
| Build falla en Maven | Memoria del build | Reintenta; Render da más RAM en build que en runtime |

Si el backend muere al arrancar por memoria, baja el consumo en `render.yaml`:

```yaml
dockerCommand: java -XX:MaxRAMPercentage=50 -XX:+UseSerialGC -jar /app/app.jar
```

---

## Resumen de URLs

| Pieza | URL | Panel |
|-------|-----|-------|
| Frontend | `https://onda-xxxx.vercel.app` | vercel.com |
| Backend | `https://onda-api.onrender.com` | dashboard.render.com |
| Base de datos | `gateway01.xxxx.prod.aws.tidbcloud.com:4000` | tidbcloud.com |

Con esto tienes la web pública, accesible desde cualquier dispositivo, sin tu PC
encendido y sin pagar nada.
