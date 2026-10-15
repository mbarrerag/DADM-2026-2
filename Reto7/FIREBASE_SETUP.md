# Reto 7: Firebase para jugar entre dos celulares

La app usa Firebase Authentication anónima y Realtime Database. Cada instalación recibe un usuario anónimo; la partida vive en la nube y se actualiza en ambos dispositivos.

## Configuración única

1. Entra a https://console.firebase.google.com/ y crea un proyecto.
2. Agrega una aplicación Android con este package name exacto: `co.edu.unal.tictactoe.reto7`.
3. Descarga `google-services.json` y cópialo en `Reto7/app/google-services.json`.
4. En Authentication > Sign-in method activa `Anonymous`.
5. En Realtime Database crea una base de datos. Puedes iniciar en modo de prueba y luego pegar las reglas de `firebase-rules.json`.
6. En Android Studio sincroniza Gradle y ejecuta la variante `Reto7`.

Para una entrega más segura, después de la demostración usa reglas que exijan autenticación. El archivo `google-services.json` no se incluye en Git porque pertenece al proyecto Firebase personal.

## Prueba con PC y celular

Abre Reto7 en el emulador o en el PC, toca `Crear nuevo juego` y escribe tu nombre. En el celular instala el APK de Reto7, abre la app y toca el juego que aparece en `Juegos disponibles`. El tablero se habilita cuando el segundo jugador se une y cada movimiento aparece en ambos dispositivos.
