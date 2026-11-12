# Reto 9: Cerca de mí

Aplicación Android de puntos de interés cercanos a la ubicación actual. Muestra hospitales y clínicas, sitios turísticos y parques en OpenStreetMap. Los lugares se consultan con Overpass y se ordenan por distancia. El radio de 1, 3, 5 o 10 km se conserva en preferencias.

## Ejecutar

1. Abre la raíz del repositorio en Android Studio y selecciona el módulo `Reto9`.
2. Ejecuta en un teléfono con ubicación activa o en un emulador. En el emulador puedes fijar una posición desde Extended Controls > Location.
3. Pulsa **Ubicarme** y concede el permiso. Cambia el radio desde el botón `5 km` y filtra con **Todos**, **Salud**, **Turismo** o **Parques**.
4. Toca una fila de la lista para centrar el mapa y ver el nombre del marcador.

La app no necesita Firebase ni clave de Google Maps. Requiere Internet para los mapas y los lugares en vivo. Si Overpass no responde y la ubicación está cerca de Bogotá, muestra cuatro lugares de ejemplo aproximados, identificados como `Vista de ejemplo · sin datos en vivo`. Estos puntos son solo para demostrar la interfaz; no son datos actualizados ni deben usarse para navegación.
