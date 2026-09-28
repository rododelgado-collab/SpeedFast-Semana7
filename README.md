# SpeedFast — Semana 7: JDBC y base de datos MySQL

**Asignatura:** Desarrollo Orientado a Objetos II (PRY2203) — Duoc UC
**Actividad formativa:** Conectando aplicaciones Java con bases de datos mediante JDBC (IL7)
**Estudiante:** Rodolfo Delgado

Aplicación de escritorio Java Swing para la empresa de reparto SpeedFast. En la semana 6 los datos
vivían en listas en memoria; ahora **los pedidos, repartidores y entregas se guardan en MySQL**
usando JDBC (`DriverManager`, `PreparedStatement`, `ResultSet`).

## Requisitos

- JDK 17 o superior
- IntelliJ IDEA (el proyecto es Maven: se abre con *File → Open* sobre la carpeta o el `pom.xml`)
- MySQL Server 8 y MySQL Workbench (`localhost:3306`)
- El conector JDBC se descarga solo con Maven: `mysql-connector-j 9.4.0` (ver `pom.xml`)

## Cómo ejecutar

1. **Crear la base de datos.** En MySQL Workbench abrir `bd/script_estructura.sql` y ejecutarlo
   completo (crea `speedfast_db` y las tablas `repartidor`, `pedido`, `entrega`).
   *Opcional:* `bd/datos_prueba.sql` inserta 3 repartidores y 3 pedidos de ejemplo.
2. **Configurar el acceso.** Copiar `db.properties.example` como `db.properties` (junto al `pom.xml`)
   y escribir la contraseña de `root`. Este archivo no se sube a GitHub (`.gitignore`).
   Sin `db.properties` se usa `root` sin contraseña.
3. **Abrir en IntelliJ**, esperar a que Maven descargue las dependencias (*Reload All Maven Projects*
   si hace falta) y ejecutar `src/main/java/main/Main.java`.

Si la conexión falla, al abrir aparece un aviso con la causa (contraseña incorrecta, base inexistente,
servicio MySQL apagado) y la aplicación permite reintentar cuando MySQL esté disponible.

## Modelo de datos

```
repartidor (id PK, nombre)
pedido     (id PK, direccion, tipo, estado)        tipo: COMIDA | ENCOMIENDA | EXPRESS
                                                   estado: PENDIENTE | EN_REPARTO | ENTREGADO
entrega    (id PK, id_pedido FK -> pedido.id, id_repartidor FK -> repartidor.id, fecha, hora)
```

Un repartidor puede hacer muchas entregas y un pedido puede tener una o varias (por ejemplo, si una
entrega queda interrumpida y se vuelve a asignar). Los IDs son `AUTO_INCREMENT`: los genera MySQL.

## Estructura del proyecto

```
SpeedFast-Semana7/
├── pom.xml                      dependencia mysql-connector-j
├── db.properties.example        plantilla de conexión (db.properties queda fuera de Git)
├── bd/
│   ├── script_estructura.sql    crea speedfast_db y las 3 tablas
│   └── datos_prueba.sql         datos de ejemplo (opcional)
└── src/main/java/
    ├── main/         Main                        comprueba la conexión y abre VentanaPrincipal
    ├── modelo/       Pedido (abstracta), PedidoComida, PedidoEncomienda, PedidoExpress,
    │                 Repartidor, Entrega, TipoPedido, EstadoPedido
    ├── dao/          ConexionBD, PedidoDAO, RepartidorDAO, EntregaDAO, DAOException
    ├── controlador/  ControladorPedidos          valida, llama a los DAO y avisa a las ventanas
    ├── tareas/       TareaEntrega (Runnable)     simula el recorrido en un hilo (semanas 4 y 5)
    └── vista/        VentanaPrincipal, VentanaRegistroPedido, VentanaRepartidores,
                      VentanaListaPedidos, VentanaAsignarRepartidor, Estilo
```

## Capa JDBC

| Clase | Qué hace |
|-------|----------|
| `ConexionBD` | `conectar()` abre la conexión con `DriverManager.getConnection`. `probarConexion()` usa `try-catch-finally` y cierra la conexión en el `finally`. `describirError()` traduce los códigos de MySQL a mensajes claros. Lee `db.properties`. |
| `PedidoDAO` | `guardar(Pedido)` (INSERT con `PreparedStatement` y clave generada), `listarTodos()` (SELECT con `ResultSet` y LEFT JOIN al repartidor), `actualizarEstado`, `contarPorEstado` (GROUP BY), `reiniciarEntregasInterrumpidas`. |
| `RepartidorDAO` | `guardar(Repartidor)` y `listarTodos()` que devuelve `List<Repartidor>` recorriendo un `ResultSet`. |
| `EntregaDAO` | `guardar(Entrega)` registra la relación pedido–repartidor con fecha y hora; `listarTodas()` hace un JOIN con pedido y repartidor. |

Buenas prácticas aplicadas: todas las consultas usan parámetros `?` (sin concatenar texto, así se evita la
inyección SQL), los recursos (`Connection`, `PreparedStatement`, `ResultSet`) se cierran con
`try-with-resources`, y los errores `SQLException` se capturan en el DAO y se relanzan como `DAOException`
con un mensaje que la ventana muestra en un `JOptionPane`.

## Ventanas conectadas a la base de datos

| Ventana | Qué hace con MySQL |
|---------|--------------------|
| Registrar pedido | Dirección + tipo (`JComboBox`) → `INSERT` en `pedido`; el ID lo genera MySQL. |
| Repartidores | Formulario → `INSERT` en `repartidor`; `JTable` con el `SELECT` de todos los repartidores. |
| Listar pedidos y entregas | Dos `JTable` en pestañas: pedidos (con su repartidor) y historial de entregas. Se recargan al cambiar los datos. |
| Asignar repartidor | Combos con pedidos pendientes (leídos de MySQL) y repartidores libres. Al iniciar: `pedido.estado = EN_REPARTO` + `INSERT` en `entrega`; al terminar el hilo: `estado = ENTREGADO`. |
| Principal | Bitácora de actividad y resumen de pedidos por estado (consulta con `GROUP BY`). |

Los datos quedan guardados: al cerrar y volver a abrir la aplicación los pedidos, repartidores y entregas
siguen ahí. Si la aplicación se cierra con una entrega en curso, al reiniciar ese pedido vuelve a
`PENDIENTE` y puede asignarse de nuevo (queda registrado como otro intento en `entrega`).

## Prueba rápida

1. Ejecutar `script_estructura.sql` y abrir la aplicación.
2. *Repartidores* → guardar `Juan` y `María`; aparecen en la tabla con su ID.
3. *Registrar pedido* → guardar un pedido de cada tipo.
4. *Listar pedidos y entregas* → los 3 pedidos aparecen como *Pendiente*.
5. *Asignar repartidor* → elegir pedido y repartidor → *Iniciar entrega*. En la tabla pasa a *En reparto* y,
   pocos segundos después, a *Entregado* (Express 1,5 s, Comida 3 s, Encomienda 6 s); la pestaña *Entregas*
   muestra la fecha y hora.
6. Cerrar y abrir de nuevo la aplicación: los datos siguen en MySQL. También se pueden ver en Workbench con
   `SELECT * FROM pedido;` y `SELECT * FROM entrega;`.

## Continuidad con semanas anteriores

| Semana | Lo que se reutiliza aquí |
|--------|--------------------------|
| 2–4 | Jerarquía `Pedido` abstracta con `PedidoComida`, `PedidoEncomienda`, `PedidoExpress` y `calcularTiempoEntrega()` |
| 4–5 | `EstadoPedido`, cambios de estado `synchronized`, repartidores que entregan en un hilo (`TareaEntrega`) |
| 6 | Interfaz Swing con MVC, observadores y `JTable` con `DefaultTableModel` |
