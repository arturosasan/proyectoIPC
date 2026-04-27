# Running la Safor — IPC 2026

Aplicación de registro y análisis de actividades deportivas al aire libre para el club Running la Safor. Proyecto de la asignatura **Interfaces Persona Computador (IPC)** — UPV · DSIC.

Integrantes del grupo (2D1) : [Carlotta Casanova Pérez](https://github.com/carlotacasanovaa), [Carlos Moldes Peña](https://github.com/Pepitomanos), [Maria Teresa Mones Guillem](https://github.com/teresamones), [Arturo Sarrión Sánchez](https://github.com/arturosasan) 

---

## Índice

- [Running la Safor — IPC 2026](#running-la-safor--ipc-2026)
  - [Índice](#índice)
  - [Funcionalidad implementada](#funcionalidad-implementada)
    - [👤 Categoría 1 — Usuarios](#-categoría-1--usuarios)
    - [🏃 Categoría 2 — Actividades](#-categoría-2--actividades)
    - [🗺️ Categoría 3 — Ruta en el mapa](#️-categoría-3--ruta-en-el-mapa)
    - [📊 Categoría 4 — Adicionales](#-categoría-4--adicionales)
    - [🌍 Categoría 5 — Mapas](#-categoría-5--mapas)
  - [Cómo colaborar en este repositorio](#cómo-colaborar-en-este-repositorio)
    - [1. Clonar el repositorio en NetBeans](#1-clonar-el-repositorio-en-netbeans)
    - [2. Asignarse una issue](#2-asignarse-una-issue)
    - [3. Actualizar tu rama `main` local](#3-actualizar-tu-rama-main-local)
    - [4. Crear una rama para tu tarea](#4-crear-una-rama-para-tu-tarea)
    - [5. Hacer commit y subir cambios](#5-hacer-commit-y-subir-cambios)
    - [6. Abrir una Pull Request](#6-abrir-una-pull-request)

---

## Funcionalidad implementada

El seguimiento del trabajo se hace mediante **issues de GitHub**: hay una issue por cada escenario de uso. Si encuentras un bug relacionado con una funcionalidad ya implementada, abre una issue nueva que la referencie.

### 👤 Categoría 1 — Usuarios

| # | Escenario | Descripción |
|---|-----------|-------------|
| 3.1 | Registrarse | El usuario introduce nickname, email, contraseña, fecha de nacimiento y avatar opcional. El sistema valida los datos y confirma el registro. |
| 3.2 | Autenticarse | El usuario introduce sus credenciales y el sistema le da acceso a la aplicación. |
| 3.3 | Modificar perfil | El usuario puede actualizar su email, contraseña, fecha de nacimiento y avatar. El nickname no es editable. |
| 3.4 | Cerrar sesión | Al cerrar sesión se guardan automáticamente las estadísticas de uso de esa sesión. |
| 3.5 | Historial de sesiones | El usuario puede consultar todas sus sesiones anteriores con duración y estadísticas de uso. |

> [!Note]
>   Para comprobar la base de datos 'sportactivity.db' podeis usar [sqlite3](https://sqlite.org/download.html) 
>   desde la terminal, por ejemplo, para comprobar los usuarios registrados sería así: 
>
> ```bash
> sqlite3 sportactivity.db "SELECT * FROM users;"
> ```


### 🏃 Categoría 2 — Actividades

| # | Escenario | Descripción |
|---|-----------|-------------|
| 4.1 | Registrar actividad | El usuario importa un fichero GPX. Se muestra la ruta en el mapa con inicio en verde y fin en rojo, junto a las estadísticas de la actividad. |
| 4.2 | Añadir anotaciones | Clic derecho sobre el mapa para añadir anotaciones (punto, texto, círculo o línea) con texto y color personalizables. |
| 4.3 | Visualizar actividad | El usuario selecciona una actividad de su lista y visualiza ruta, anotaciones y estadísticas. |
| 4.4 | Acumulado de actividades | Vista con totales del mes: tiempo, distancia, metros de ascenso y descenso. |

### 🗺️ Categoría 3 — Ruta en el mapa

| # | Escenario | Descripción |
|---|-----------|-------------|
| 5.1 | Zoom | El usuario amplía o reduce la vista del mapa con botones +/−. El trazado y las anotaciones se escalan con el mapa. |

### 📊 Categoría 4 — Adicionales

| # | Escenario | Descripción |
|---|-----------|-------------|
| 6.1 | Perfil de desnivel | Gráfica de altitud junto al mapa (eje X: distancia en km, eje Y: altitud en metros). Al pasar el ratón se resalta el punto correspondiente en el mapa. |
| 6.2 | Velocidad sobre el trazado | La velocidad de cada tramo se visualiza codificada por color sobre el trazado. |

### 🌍 Categoría 5 — Mapas

| # | Escenario | Descripción |
|---|-----------|-------------|
| 7.1 | Añadir mapa | El usuario añade un mapa nuevo introduciendo la imagen JPG y las coordenadas de su bounding box. |

---

## Cómo colaborar en este repositorio

El flujo de trabajo es siempre el mismo: **una issue → una rama → una pull request**. No se hace commit directamente sobre `main`.

### 1. Clonar el repositorio en NetBeans

La primera vez que te incorporas al proyecto:

1. En NetBeans, ve a **Team → Git → Clone...**
2. Introduce la URL del repositorio (la encuentras en GitHub, botón verde **Code → HTTPS**).
3. Introduce tus credenciales de GitHub si te las pide.
4. Elige dónde guardarlo en tu máquina y finaliza. NetBeans abrirá el proyecto automáticamente.

> **Nota:** Seguir la guía de poliFormat para generar el *Token de acceso* y añadirlo a Apache NetBeans

---

### 2. Asignarse una issue

Antes de tocar nada de código, entra en la pestaña [**Issues**](https://github.com/arturosasan/proyectoIPC/issues) del repositorio en GitHub:

- Si la tarea ya existe, ábrela y pulsa **Assign yourself** en el panel derecho.
- Si has encontrado un bug, o hace falta hacer cualquier cosa en el proyecto, crea una issue nueva **ANTES DE EMPEZAR A TRABAJAR EN ELLA** *(para no hacer trabajo duplicado)* describiendo qué falla, en qué escenario ocurre y cómo reproducirlo. Si está relacionado con un escenario concreto, referencia su issue escribiendo `#número` en la descripción.

Así todo el mundo sabe en qué está trabajando cada uno y no hay dos personas pisándose el mismo fichero.

---

### 3. Actualizar tu rama `main` local

Antes de ponerte a trabajar, sincroniza tu `main` local con el del repositorio para asegurarte de que partes de la versión más reciente:

1. En NetBeans, cámbiate a la rama `main`: **Team → Git → Branch → Switch Branch...** y selecciona `main`.
2. Ve a **Team → Remote → Pull from Upstream**.

Si no haces esto y alguien subió cambios desde la última vez que clonaste, acabarás trabajando sobre una versión desactualizada y tendrás conflictos al hacer la Pull Request.

---

### 4. Crear una rama para tu tarea

Nunca trabajes directamente en `main`. Para cada issue, crea una rama propia:

1. En NetBeans, ve a **Team → Git → Branch → Create Branch...**
2. Ponle un nombre descriptivo, por ejemplo `feature/registro-usuario` o `fix/bug-zoom-mapa`*(orientativo, usad el nombre que querais)*.
3. Asegúrate de que la rama base sea `main` (o `master` según cómo esté configurado el repo) y pulsa **Create Branch**.

NetBeans cambiará automáticamente a esa rama. A partir de aquí, todos tus cambios van en ella.

---

### 5. Hacer commit y subir cambios

Cuando hayas avanzado algo (no esperes a tener todo terminado para hacer el primer commit):

**Commit:**
1. En NetBeans, ve a **Team → Commit...**.
2. Revisa los archivos que aparecen marcados — son los que han cambiado. Desmarca cualquier fichero generado automáticamente que no sea relevante (todos suelen ser importantes, pero hay que revisarlo por si acaso).
3. Escribe un mensaje de commit claro y en una línea, por ejemplo: `Añade validación de nickname en registro`. Evita mensajes del tipo "cambios" o "arreglado".
4. Pulsa **Commit**.

**Push (subir al repositorio):**
1. Ve a **Team → Remote → Push to Upstream** (o **Push...**).
2. NetBeans te pedirá confirmación del destino — deja los valores por defecto y acepta.

Repite commit + push tantas veces como quieras mientras trabajas en tu rama. **No rompe nada**.

---

### 6. Abrir una Pull Request

Cuando la funcionalidad esté lista para revisarse:

1. Ve al repositorio en GitHub. Verás un aviso amarillo con tu rama y un botón **Compare & pull request** — púlsalo.
2. Escribe un título descriptivo y en la descripción menciona la issue que resuelve escribiendo `Closes #número` (GitHub la cerrará automáticamente al hacer el merge).
3. Asigna a un compañero como revisor en **Reviewers** *(opcional)*.
4. Pulsa **Create Pull Request**.

Cuando se cree la pull request todos los contribuidores del trabajo podremos revisar los cambios hechos. Cuando sea aprobada, se hace el merge a `main`.

---
