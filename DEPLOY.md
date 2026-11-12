# Despliegue permanente y gratuito

Guía para publicar ONDA con una URL fija, HTTPS automático y sin depender de tu PC.

**Stack:** Oracle Cloud Always Free (VM Ubuntu) + Docker Compose + Caddy + DuckDNS.
**Coste:** 0 USD. **Tiempo:** 30–45 minutos la primera vez.

```
Internet
   │  https://onda-demo.duckdns.org  (TLS de Let's Encrypt)
   ▼
[Caddy :80/:443]  ──►  [frontend nginx]  ──►  [backend :8080]  ──►  [MySQL]
   (puerto público)      (sin puerto)          (sin puerto)        (sin puerto)
```

Solo Caddy queda expuesto. MySQL y el backend viven en la red interna de Docker.

---

## 0. Antes de empezar

Necesitas:

- Cuenta en [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/).
- Un subdominio gratuito en [DuckDNS](https://www.duckdns.org) (o un dominio propio).
- Los dos repositorios en GitHub:
  - Backend: `https://github.com/jhenaodev-pixel/ApiBank`
  - Frontend: `https://github.com/jhenaodev-pixel/ONDA`

> **Importante:** la clave de Finnhub quedó publicada en el historial de Git.
> Genérala de nuevo en [finnhub.io](https://finnhub.io) y usa solo la nueva.

---

## 1. Crear la VM en Oracle Cloud

1. Entra a **Compute → Instances → Create instance**.
2. Image: **Ubuntu 24.04**.
3. Shape: **VM.Standard.A1.Flex** (Ampere, Always Free).
   - 2 OCPU y 12 GB de RAM es cómodo; 1 OCPU / 6 GB funciona bien.
   - Si aparece “Out of capacity”, cambia la zona de disponibilidad o reintenta más tarde.
   - La opción `VM.Standard.E2.1.Micro` (1 GB) **no** es recomendable: MySQL + Spring Boot
     no caben sin swap.
4. Boot volume: 50–100 GB.
5. Descarga la clave SSH privada (`.key`) y guarda la **IP pública**.

Conéctate:

```powershell
ssh -i ruta\a\la\clave.key ubuntu@IP_PUBLICA
```

---

## 2. Abrir los puertos 80 y 443

### 2.1 Consola de Oracle

**Networking → Virtual Cloud Networks → tu VCN → Security Lists → Default Security List**
→ *Add Ingress Rules*:

| Source | Protocol | Destination Port |
|--------|----------|------------------|
| `0.0.0.0/0` | TCP | `80` |
| `0.0.0.0/0` | TCP | `443` |

### 2.2 Firewall de la instancia (Ubuntu)

Las imágenes de Oracle traen `iptables` restrictivo. En la VM:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

Si `netfilter-persistent` no existe:

```bash
sudo apt update && sudo apt install -y iptables-persistent
sudo netfilter-persistent save
```

> No uses solo `ufw`: Docker escribe sus propias reglas y puede saltárselo.

---

## 3. Instalar Docker

```bash
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
docker --version
docker compose version
```

---

## 4. Dominio gratuito con DuckDNS

1. Inicia sesión en [duckdns.org](https://www.duckdns.org).
2. Crea un subdominio, por ejemplo `onda-demo`.
3. En **current ip**, escribe la IP pública de la VM y pulsa *update ip*.

Comprueba desde tu equipo:

```powershell
nslookup onda-demo.duckdns.org
```

Debe devolver la IP de la VM. Si usas un dominio propio, crea un registro `A`
apuntando a esa IP.

---

## 5. Subir el proyecto a la VM

La estructura debe quedar con las dos carpetas hermanas (el `Dockerfile` del
frontend se construye desde `../onda-frontend`):

```bash
mkdir -p ~/onda && cd ~/onda
git clone https://github.com/jhenaodev-pixel/ApiBank.git
git clone https://github.com/jhenaodev-pixel/ONDA.git onda-frontend
```

```text
~/onda/ApiBank
~/onda/onda-frontend
```

Si prefieres copiar desde Windows:

```powershell
scp -i clave.key -r C:\Users\juanp\Documents\ApiBank ubuntu@IP_PUBLICA:~/onda/
scp -i clave.key -r C:\Users\juanp\Documents\onda-frontend ubuntu@IP_PUBLICA:~/onda/
```

---

## 6. Configurar variables de entorno

```bash
cd ~/onda/ApiBank
cp .env.prod.example .env
nano .env
```

Contenido:

```dotenv
APP_DOMAIN=onda-demo.duckdns.org
MYSQL_ROOT_PASSWORD=<clave larga>
JWT_SECRET=<secreto>
FINNHUB_API_KEY=<clave nueva>
CORS_ALLOWED_ORIGINS=
```

Genera secretos fuertes:

```bash
openssl rand -base64 48
```

| Variable | Para qué sirve |
|----------|----------------|
| `APP_DOMAIN` | Dominio del certificado HTTPS |
| `MYSQL_ROOT_PASSWORD` | Contraseña de MySQL (la usa también el backend) |
| `JWT_SECRET` | Firma de los tokens: Base64 de 32+ bytes o texto de 32+ caracteres |
| `FINNHUB_API_KEY` | Cotizaciones de acciones (opcional) |
| `CORS_ALLOWED_ORIGINS` | Solo si otro dominio consume la API |

`.env` está en `.gitignore`: nunca se sube.

> `MYSQL_ROOT_PASSWORD` solo se aplica al crear el volumen por primera vez. Si la
> cambias después, MySQL conserva la anterior; actualiza el `.env` o recrea el
> volumen (esto borra los datos).

---

## 7. Levantar el stack

```bash
cd ~/onda/ApiBank
docker compose -f docker-compose.prod.yml up -d --build
```

El primer build tarda 5–10 minutos. Luego:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f caddy
```

Caddy pedirá el certificado en la primera petición HTTPS. Espera a ver líneas
como `certificate obtained successfully`.

Abre:

```text
https://onda-demo.duckdns.org
```

---

## 8. Verificar

```bash
curl -I https://onda-demo.duckdns.org
curl -s https://onda-demo.duckdns.org/api/v1/market/crypto | head -c 200
docker compose -f docker-compose.prod.yml ps
```

Comprueba también desde el navegador:

- Login con un usuario existente o registro nuevo.
- Resumen con cuentas y movimientos.
- Búsqueda de acciones (`AAPL`).

Los usuarios existentes se consultan igual que en local:

```bash
docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" onda-mysql \
  mysql -uroot -e "USE db_bank; SELECT id, first_name, last_name, email, role FROM users;"
```

---

## 9. Operación diaria

### Ver estado y logs

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail 100 backend
docker compose -f docker-compose.prod.yml logs --tail 100 caddy
```

### Actualizar tras un `git push`

```bash
cd ~/onda/ApiBank && git pull
cd ~/onda/onda-frontend && git pull
cd ~/onda/ApiBank
docker compose -f docker-compose.prod.yml up -d --build
```

### Copia de seguridad de la base de datos

```bash
mkdir -p ~/backups
docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" onda-mysql \
  mysqldump -uroot --single-transaction db_bank > ~/backups/db_bank-$(date +%F).sql
```

Programa una copia diaria:

```bash
crontab -e
```

```cron
0 3 * * * cd /home/ubuntu/onda/ApiBank && set -a && . ./.env && set +a && docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" onda-mysql mysqldump -uroot --single-transaction db_bank > /home/ubuntu/backups/db_bank-$(date +\%F).sql
```

Restaurar:

```bash
docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" onda-mysql \
  mysql -uroot db_bank < ~/backups/db_bank-2026-11-12.sql
```

### Reiniciar todo

```bash
docker compose -f docker-compose.prod.yml restart
```

Los contenedores usan `restart: unless-stopped`, así que vuelven solos al
reiniciar la VM.

---

## 10. Seguridad

- Solo 80/443 quedan expuestos. MySQL y el backend no publican puertos.
- `MYSQL_ROOT_PASSWORD` y `JWT_SECRET` viven en `.env`, fuera de Git.
- El backend corre como usuario sin privilegios dentro del contenedor.
- Caddy añade HSTS, `X-Content-Type-Options`, `X-Frame-Options` y `Referrer-Policy`.
- Rota la clave de Finnhub que estuvo publicada.
- Si el repositorio es público, revisa que no haya más secretos en el historial:

```bash
git log -p --all | grep -i "api_key\|password\|secret" | head
```

---

## 11. Solución de problemas

| Síntoma | Causa habitual | Solución |
|---------|----------------|----------|
| `502 Bad Gateway` | Backend aún arrancando o caído | `docker compose -f docker-compose.prod.yml logs backend` |
| Error de certificado | DNS sin propagar o puerto 443 cerrado | Revisa `nslookup` y las reglas de Oracle + `iptables` |
| `connection refused` en 80/443 | Falta la regla en la Security List o en `iptables` | Repite el paso 2 |
| La VM se queda sin memoria | Shape de 1 GB | Usa A1.Flex con ≥ 6 GB |
| Acciones no disponibles | Falta `FINNHUB_API_KEY` | Añádela al `.env` y recrea el backend |
| `IllegalStateException: El secreto JWT...` | `JWT_SECRET` demasiado corto | Usa `openssl rand -base64 48` |
| El backend no conecta tras cambiar la contraseña | MySQL conservó la clave del volumen | Vuelve a la clave anterior o recrea el volumen |
| Caddy no renueva el certificado | Volumen `caddy_data` borrado | `docker compose ... up -d --force-recreate caddy` |

Recrear solo un servicio tras cambiar `.env`:

```bash
docker compose -f docker-compose.prod.yml up -d --force-recreate backend
```

---

## 12. Opción B: frontend en Vercel + API en la VM

Si quieres conservar el frontend en Vercel, la VM pasa a servir solo la API.

1. Crea otro subdominio DuckDNS (por ejemplo `onda-api`) apuntando a la misma IP.
2. Añade a `Caddyfile`:

```text
onda-api.duckdns.org {
	reverse_proxy backend:8080
}
```

3. En `.env`:

```dotenv
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app
```

4. Recrea Caddy y el backend:

```bash
docker compose -f docker-compose.prod.yml up -d --force-recreate caddy backend
```

5. En Vercel, define `VITE_API_URL=https://onda-api.duckdns.org` y vuelve a desplegar.

---

## 13. Lista de comprobación final

- [ ] VM Always Free creada y accesible por SSH.
- [ ] Puertos 80/443 abiertos en Oracle **y** en `iptables`.
- [ ] Dominio DuckDNS apuntando a la IP.
- [ ] `.env` con contraseñas nuevas y clave de Finnhub rotada.
- [ ] `docker compose -f docker-compose.prod.yml up -d --build` sin errores.
- [ ] `https://tu-dominio` carga con candado válido.
- [ ] Login y datos reales funcionando.
- [ ] Copia de seguridad programada.
