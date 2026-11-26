# Guía para el diseño en Figma (CE b)

Hay que montar 3 componentes con variantes y 3 pantallas. Los valores salen del código (`Comun.kt`), así que Figma y la app coinciden. En Dev Mode, 1 px = 1 dp y, en el texto, 1 px = 1 sp.

## Variables de color
| Nombre | Hex | Uso |
|--------|-----|-----|
| fondo | #101418 | Fondo de las pantallas |
| superficie | #1C2228 | Tarjetas y bloques |
| texto | #FFFFFF | Texto principal |
| textoSuave | #B0BEC5 | Texto de apoyo |
| correcto | #26A69A | Encuadre correcto, esqueleto bien, reconocido |
| aviso | #FFB300 | Pausa, «Baja más», no entendido |
| error | #E53935 | Sin persona, escuchando, terminar |
| primario | #42A5F5 | Botones y rutinas |

Comprueba el contraste con Stark o Contrast (nivel AA): blanco sobre #101418 cumple de sobra.

## Tipografía
Contador 96 · Principal 48 · Título 32 · Apoyo 22 (mínimo 20).

## Componentes (propiedad Variant `estado`)
1. **Tarjeta de permiso**: `sin-pedir` (#FFF3E0), `concedido` (#E0F7F4), `denegado` (#FDE7E7). → `EstadoPermiso`
2. **Indicador de voz**: `inactivo` (punto gris, «Micrófono en espera»), `escuchando` (punto rojo con ondas, «Te escucho…»), `reconocido` (verde, «✔ "pausa" → Pausa»), `no-entendido` (ámbar, «No te he entendido. Prueba: «pausa»»). → `EstadoIndicador`
3. **Marcador de encuadre**: `sin-persona` (#E53935, «No te veo: ponte delante de la cámara»), `parcial` (#FFB300, «Aléjate: necesito verte de la cabeza a los pies»), `correcto` (#26A69A, «Te veo bien»). → `Encuadre`

Opcional, para el estado «movimiento incorrecto» del Tema 3: esqueleto verde (#26A69A) / naranja (#FFB300) con el consejo «Baja más».

## Pantallas (usa siempre instancias de los componentes)
1. **Inicio**: título «Sin Tocar», 2 Tarjetas de permiso (cámara y micrófono) y 2 tarjetas de rutina.
2. **Entrenamiento**: cámara con esqueleto y Marcador de encuadre; ejercicio + ⏱; «3 / 8» a 96; mensaje de estado; pista «Di «pausa» o cruza los brazos 2 s»; Indicador de voz; barra de brazos en cruz; botones ▶/⏸, ⏭, ■, ?.
3. **Resumen**: repeticiones por ejercicio, tiempo, últimas órdenes con su origen, botón ☆ Favorito e Indicador de voz.

## Prototipo
En la pestaña Prototype: Inicio → (tocar rutina) → Entrenamiento → (tocar ■) → Resumen → (Volver) → Inicio.
Añade una **nota**: «La voz y los gestos se simulan con toques: Figma no reconoce voz ni postura».
